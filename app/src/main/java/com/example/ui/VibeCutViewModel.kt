package com.example.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import com.example.ads.AdOrchestrator
import com.example.ads.AdSessionState
import com.example.data.VibeCutRepository
import com.example.engine.ClipBatchPlanner
import com.example.engine.CompositionBuilder
import com.example.engine.VibeCutMediaServices
import com.example.engine.VideoLinkResolver
import com.example.model.AdPlacementType
import com.example.model.AspectRatioPreset
import com.example.model.BatchGenerationState
import com.example.model.ClipDiscoveryMode
import com.example.model.ClipLengthOption
import com.example.model.ClipTextOverlay
import com.example.model.ClipTransition
import com.example.model.ColorAdjustments
import com.example.model.CropMode
import com.example.model.DeviceEncoderCapabilities
import com.example.model.EditorToolTab
import com.example.model.ExportConfig
import com.example.model.ExportResolution
import com.example.model.ExportUiState
import com.example.model.GeneratedClipRecord
import com.example.model.MainTab
import com.example.model.MonetizationConfig
import com.example.model.PlannedClipSegment
import com.example.model.ProjectDraft
import com.example.model.ReferralClaimDoc
import com.example.model.ResolvedLinkMedia
import com.example.model.SocialAspectFormat
import com.example.model.SpecialEffect
import com.example.model.StudioQualityTier
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.example.model.VideoClip
import com.example.model.VideoCodecOption
import com.example.model.VideoFilter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@OptIn(UnstableApi::class)
class VibeCutViewModel(
    private val repository: VibeCutRepository,
    private val mediaServices: VibeCutMediaServices,
    val currentUserId: String,
) : ViewModel() {

    val capabilities: DeviceEncoderCapabilities = mediaServices.capabilities
    val exportState: StateFlow<ExportUiState> = mediaServices.exportState

    private val _selectedTab = MutableStateFlow(MainTab.STUDIO)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Local preview override if a non-admin tests Admin Panel toggles locally
    private val _localOverrideMonetization = MutableStateFlow<MonetizationConfig?>(null)

    val monetizationConfig: StateFlow<MonetizationConfig> = combine(
        repository.observeMonetizationConfig()
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Monetization config listener notice", fsErr ?: error)
                emit(null)
            },
        repository.observeLocalMonetizationFallback(),
        _localOverrideMonetization,
    ) { remote, local, override ->
        (override ?: remote ?: local).enforceMutualExclusion()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MonetizationConfig().enforceMutualExclusion(),
    )

    val userProfileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .combine(MutableStateFlow(Unit)) { profile, _ ->
            if (profile != null) {
                UiState.Success(profile)
            } else {
                UiState.Loading
            }
        }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = UiState.Loading,
        )

    val myClipsState: StateFlow<UiState<List<GeneratedClipRecord>>> = repository.observeMyClips(currentUserId)
        .combine(MutableStateFlow(Unit)) { list, _ -> UiState.Success(list) as UiState<List<GeneratedClipRecord>> }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).localizedMessage ?: "Failed to load clips"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = UiState.Loading,
        )

    val confirmedReferrals: StateFlow<List<ReferralClaimDoc>> = repository.observeConfirmedReferrals(currentUserId)
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    // Studio state
    private val _linkInput = MutableStateFlow("")
    val linkInput: StateFlow<String> = _linkInput.asStateFlow()

    private val _isResolvingLink = MutableStateFlow(false)
    val isResolvingLink: StateFlow<Boolean> = _isResolvingLink.asStateFlow()

    private val _resolvedLinkMedia = MutableStateFlow<ResolvedLinkMedia?>(null)
    val resolvedLinkMedia: StateFlow<ResolvedLinkMedia?> = _resolvedLinkMedia.asStateFlow()

    private val _studioSourceClip = MutableStateFlow<VideoClip?>(null)
    val studioSourceClip: StateFlow<VideoClip?> = _studioSourceClip.asStateFlow()

    private val _clipLength = MutableStateFlow(ClipLengthOption.SEC_30)
    val clipLength: StateFlow<ClipLengthOption> = _clipLength.asStateFlow()

    private val _aspectFormat = MutableStateFlow(SocialAspectFormat.REELS_9_16)
    val aspectFormat: StateFlow<SocialAspectFormat> = _aspectFormat.asStateFlow()

    private val _clipCount = MutableStateFlow(2)
    val clipCount: StateFlow<Int> = _clipCount.asStateFlow()

    private val _discoveryMode = MutableStateFlow(ClipDiscoveryMode.VIRAL_CLIPS)
    val discoveryMode: StateFlow<ClipDiscoveryMode> = _discoveryMode.asStateFlow()

    private val _cropMode = MutableStateFlow(CropMode.FILL_CROP)
    val cropMode: StateFlow<CropMode> = _cropMode.asStateFlow()

    private val _qualityTier = MutableStateFlow(StudioQualityTier.SD_360P)
    val qualityTier: StateFlow<StudioQualityTier> = _qualityTier.asStateFlow()

    private val _watermarkEnabled = MutableStateFlow(true)
    val watermarkEnabled: StateFlow<Boolean> = _watermarkEnabled.asStateFlow()

    private val _batchState = MutableStateFlow<BatchGenerationState>(BatchGenerationState.Idle)
    val batchState: StateFlow<BatchGenerationState> = _batchState.asStateFlow()

    val plannedSegments: StateFlow<List<PlannedClipSegment>> = combine(
        _studioSourceClip,
        _resolvedLinkMedia,
        _clipLength,
        _clipCount,
        _discoveryMode,
    ) { clip, resolved, length, count, mode ->
        val duration = clip?.sourceDurationMs ?: resolved?.durationHintMs ?: 180_000L
        ClipBatchPlanner.planSegments(
            sourceDurationMs = duration,
            clipLength = length,
            requestedCount = count,
            discoveryMode = mode,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ClipBatchPlanner.planSegments(180_000L, ClipLengthOption.SEC_30, 2, ClipDiscoveryMode.VIRAL_CLIPS),
    )

    // Ad State
    private val _adSessionState = MutableStateFlow<AdSessionState>(AdSessionState.Idle)
    val adSessionState: StateFlow<AdSessionState> = _adSessionState.asStateFlow()
    private var pendingPlacement: AdPlacementType? = null
    private var pendingAdUnlockCallback: (() -> Unit)? = null

    private val _showPaywallSheet = MutableStateFlow(false)
    val showPaywallSheet: StateFlow<Boolean> = _showPaywallSheet.asStateFlow()

    // Full Timeline Editor State
    private val _drafts = MutableStateFlow<List<ProjectDraft>>(emptyList())
    val drafts: StateFlow<List<ProjectDraft>> = _drafts.asStateFlow()

    private val _activeProject = MutableStateFlow<ProjectDraft?>(null)
    val activeProject: StateFlow<ProjectDraft?> = _activeProject.asStateFlow()

    private val _selectedClipIndex = MutableStateFlow(0)
    val selectedClipIndex: StateFlow<Int> = _selectedClipIndex.asStateFlow()

    private val _activeToolTab = MutableStateFlow(EditorToolTab.EDIT)
    val activeToolTab: StateFlow<EditorToolTab> = _activeToolTab.asStateFlow()

    private val _isLoadingMedia = MutableStateFlow(false)
    val isLoadingMedia: StateFlow<Boolean> = _isLoadingMedia.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _exportConfig = MutableStateFlow(
        ExportConfig(
            resolution = ExportResolution.FHD_1080P,
            fps = 30,
            codec = if (capabilities.supports4k) VideoCodecOption.H265 else VideoCodecOption.H264,
            bitrateMbps = ExportResolution.FHD_1080P.defaultBitrateMbps,
        ),
    )
    val exportConfig: StateFlow<ExportConfig> = _exportConfig.asStateFlow()

    private val _playerState = MutableStateFlow<CompositionPlayer?>(null)
    val playerState: StateFlow<CompositionPlayer?> = _playerState.asStateFlow()

    private var compositionRebuildJob: Job? = null
    private var positionTickerJob: Job? = null
    private var batchJob: Job? = null

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { repository.ensureUserProfile(it) }
            _drafts.value = mediaServices.projectStorage.loadDrafts()
        }
        // Automatically sync confirmed cross-device referrals into inviter's bonus credits
        viewModelScope.launch {
            combine(userProfileState, confirmedReferrals) { profileUi, claims ->
                val profile = (profileUi as? UiState.Success)?.data
                profile to claims.size.toLong()
            }.collect { (profile, claimCount) ->
                if (profile != null && claimCount > profile.referralCount) {
                    repository.syncConfirmedInviterCredits(currentUserId, profile.referralCount, claimCount)
                }
            }
        }
    }

    fun selectMainTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
    }

    fun updateLinkInput(url: String) {
        _linkInput.value = url
    }

    fun loadVideoFromLink() {
        val raw = _linkInput.value.trim()
        if (raw.isEmpty()) {
            _statusMessage.value = "Paste a YouTube, TikTok, Facebook, Instagram, or direct MP4 link first."
            return
        }
        viewModelScope.launch {
            _isResolvingLink.value = true
            val res = VideoLinkResolver.resolve(raw)
            _isResolvingLink.value = false
            res.onSuccess { resolved ->
                _resolvedLinkMedia.value = resolved
                if (resolved.directStreamPlayable && !resolved.playableStreamUrl.isNullOrBlank()) {
                    _studioSourceClip.value = VideoClip(
                        uriString = resolved.playableStreamUrl,
                        displayName = resolved.title,
                        sourceDurationMs = resolved.durationHintMs,
                        width = 1920,
                        height = 1080,
                    )
                }
                _statusMessage.value = resolved.statusNote
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not inspect link."
            }
        }
    }

    fun loadGalleryVideoForStudio(uri: Uri) {
        viewModelScope.launch {
            _isLoadingMedia.value = true
            val clips = mediaServices.inspectUris(listOf(uri))
            _isLoadingMedia.value = false
            val clip = clips.firstOrNull()
            if (clip == null) {
                _statusMessage.value = "Could not inspect selected gallery video."
                return@launch
            }
            _resolvedLinkMedia.value = null
            _studioSourceClip.value = clip
            _statusMessage.value = "Loaded '${clip.displayName}' (${formatDurationShort(clip.sourceDurationMs)})"
        }
    }

    fun selectClipLength(option: ClipLengthOption) {
        _clipLength.value = option
    }

    fun selectAspectFormat(format: SocialAspectFormat) {
        _aspectFormat.value = format
    }

    fun selectClipCount(count: Int) {
        _clipCount.value = count.coerceIn(1, 40)
    }

    fun selectDiscoveryMode(mode: ClipDiscoveryMode) {
        _discoveryMode.value = mode
    }

    fun selectCropMode(mode: CropMode) {
        _cropMode.value = mode
    }

    fun selectQualityTier(tier: StudioQualityTier) {
        _qualityTier.value = tier
    }

    fun toggleWatermark(enabled: Boolean) {
        if (enabled) {
            _watermarkEnabled.value = true
            return
        }
        val profile = (userProfileState.value as? UiState.Success)?.data
        val config = monetizationConfig.value
        val unlocked = hasWatermarkRemovalAccess(profile, config)
        if (unlocked) {
            _watermarkEnabled.value = false
        } else {
            requestFeatureUnlock(AdPlacementType.REWARDED_WATERMARK) {
                _watermarkEnabled.value = false
            }
        }
    }

    fun hasBatchAccess(count: Int, profile: UserProfile?, config: MonetizationConfig): Boolean {
        if (count <= 2) return true
        if (config.subscriptionEnabled) return profile?.subscriptionActive == true
        return (profile?.adUnlockedBatches ?: 0L) > 0L || profile?.subscriptionActive == true
    }

    fun hasQualityAccess(tier: StudioQualityTier, profile: UserProfile?, config: MonetizationConfig): Boolean {
        if (tier.freeTier) return true
        if (config.subscriptionEnabled) return profile?.subscriptionActive == true
        return profile?.adUnlockedHd == true || profile?.subscriptionActive == true
    }

    fun hasWatermarkRemovalAccess(profile: UserProfile?, config: MonetizationConfig): Boolean {
        if (config.subscriptionEnabled) return profile?.subscriptionActive == true
        return profile?.adUnlockedWatermarkFree == true || profile?.subscriptionActive == true
    }

    fun requestFeatureUnlock(placement: AdPlacementType, onUnlocked: () -> Unit) {
        val config = monetizationConfig.value
        if (!config.adSupportedMonetization) {
            pendingAdUnlockCallback = onUnlocked
            _showPaywallSheet.value = true
            return
        }
        launchAdFlow(placement, onUnlocked)
    }

    fun openSubscriptionPaywall() {
        if (!monetizationConfig.value.adSupportedMonetization) {
            pendingAdUnlockCallback = null
            _showPaywallSheet.value = true
        }
    }

    fun dismissPaywall() {
        _showPaywallSheet.value = false
    }

    fun subscribeToPlan(plan: SubscriptionPlan) {
        viewModelScope.launch {
            val res = repository.activateSubscription(currentUserId, plan)
            res.onSuccess {
                _showPaywallSheet.value = false
                _statusMessage.value = "${plan.title} activated!"
                pendingAdUnlockCallback?.invoke()
                pendingAdUnlockCallback = null
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Subscription update failed."
            }
        }
    }

    fun spendCreditToUnlock(placement: AdPlacementType) {
        viewModelScope.launch {
            val res = repository.spendBonusCreditForUnlock(currentUserId, placement)
            res.onSuccess {
                _showPaywallSheet.value = false
                _adSessionState.value = AdSessionState.Idle
                _statusMessage.value = "1 Bonus Credit applied • ${placement.title} unlocked!"
                pendingAdUnlockCallback?.invoke()
                pendingAdUnlockCallback = null
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "No bonus credits available."
            }
        }
    }

    private fun launchAdFlow(placement: AdPlacementType, onUnlocked: () -> Unit) {
        val config = monetizationConfig.value
        if (!config.adSupportedMonetization) {
            onUnlocked()
            return
        }
        pendingPlacement = placement
        pendingAdUnlockCallback = onUnlocked
        val network = config.networkEnum()
        _adSessionState.value = AdSessionState.Loading(
            network = network,
            placement = placement,
            statusLabel = "Requesting ${network.displayName} ad...",
        )
        viewModelScope.launch {
            _adSessionState.value = AdOrchestrator.requestAd(config, placement)
        }
    }

    fun retryCurrentAd() {
        val placement = pendingPlacement ?: return
        val callback = pendingAdUnlockCallback ?: {}
        launchAdFlow(placement, callback)
    }

    fun completeAdAndUnlock(isGraceRecovery: Boolean) {
        val placement = pendingPlacement
        val callback = pendingAdUnlockCallback
        _adSessionState.value = AdSessionState.Idle
        pendingPlacement = null
        pendingAdUnlockCallback = null

        viewModelScope.launch {
            if (placement != null && placement != AdPlacementType.INTERSTITIAL_BATCH) {
                repository.grantAdReward(currentUserId, placement)
            }
            if (isGraceRecovery) {
                _statusMessage.value = "Non-blocking pass applied so you can continue smoothly."
            } else if (placement != null && placement != AdPlacementType.INTERSTITIAL_BATCH) {
                _statusMessage.value = "${placement.title} unlocked!"
            }
            callback?.invoke()
        }
    }

    fun dismissAdDialog() {
        _adSessionState.value = AdSessionState.Idle
        pendingPlacement = null
        pendingAdUnlockCallback = null
    }

    fun generateClipsBatch() {
        val sourceClip = _studioSourceClip.value
        if (sourceClip == null) {
            _statusMessage.value = if (_resolvedLinkMedia.value != null) {
                "Pick the downloaded MP4 from Gallery (or paste a direct .mp4 URL) to render clips."
            } else {
                "Load a direct video link or pick a gallery video first."
            }
            return
        }

        val profile = (userProfileState.value as? UiState.Success)?.data
        val config = monetizationConfig.value
        val count = _clipCount.value
        val quality = _qualityTier.value
        val wantNoWatermark = !_watermarkEnabled.value

        if (!hasBatchAccess(count, profile, config)) {
            requestFeatureUnlock(AdPlacementType.REWARDED_BATCH) { generateClipsBatch() }
            return
        }
        if (!hasQualityAccess(quality, profile, config)) {
            requestFeatureUnlock(AdPlacementType.REWARDED_QUALITY) { generateClipsBatch() }
            return
        }
        if (wantNoWatermark && !hasWatermarkRemovalAccess(profile, config)) {
            requestFeatureUnlock(AdPlacementType.REWARDED_WATERMARK) { generateClipsBatch() }
            return
        }

        val segments = plannedSegments.value
        val format = _aspectFormat.value
        val crop = _cropMode.value
        val discovery = _discoveryMode.value
        val watermark = _watermarkEnabled.value

        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            var successCount = 0
            val total = segments.size
            for ((idx, seg) in segments.withIndex()) {
                _batchState.value = BatchGenerationState.Running(
                    currentClipIndex = idx + 1,
                    totalClips = total,
                    overallPercent = ((idx * 100) / total.coerceAtLeast(1)).coerceIn(5, 95),
                    statusLabel = "Rendering clip ${idx + 1} of $total (${quality.label} • ${format.label})...",
                )
                val segClip = sourceClip.copy(
                    id = UUID.randomUUID().toString(),
                    displayName = seg.title,
                    trimStartMs = seg.trimStartMs,
                    trimEndMs = seg.trimEndMs,
                )
                val draft = ProjectDraft(
                    title = seg.title,
                    aspectRatio = format.preset,
                    cropMode = crop,
                    watermarkEnabled = watermark,
                    clips = listOf(segClip),
                )
                val exportCfg = ExportConfig(
                    resolution = quality.exportResolution,
                    fps = 30,
                    codec = VideoCodecOption.H264,
                    bitrateMbps = quality.exportResolution.defaultBitrateMbps,
                )
                val rendered = mediaServices.exportSegmentSync(
                    project = draft,
                    config = exportCfg,
                    filePrefix = "VibeCut_${idx + 1}",
                )
                rendered.onSuccess { res ->
                    successCount++
                    repository.saveGeneratedClip(
                        currentUserId,
                        GeneratedClipRecord(
                            userId = currentUserId,
                            title = seg.title,
                            sourceLabel = sourceClip.displayName,
                            formatLabel = format.label,
                            aspectRatio = format.ratioLabel,
                            durationMs = seg.durationMs,
                            resolutionLabel = quality.label,
                            outputUriString = res.outputUriString,
                            displayPath = res.displayPath,
                            fileSizeMb = res.fileSizeMb,
                            discoveryMode = discovery.name,
                            cropMode = crop.name,
                            watermarkApplied = watermark,
                        ),
                    )
                }.onFailure { err ->
                    _batchState.value = BatchGenerationState.Error(
                        err.localizedMessage ?: "Rendering failed at clip ${idx + 1}.",
                    )
                    return@launch
                }
            }

            repository.incrementBatchesProcessed(currentUserId)
            val totalBatches = ((profile?.batchesProcessed ?: 0L) + 1L).toInt()
            _batchState.value = BatchGenerationState.Done(
                generatedCount = successCount,
                savedSummary = "Saved $successCount clip${if (successCount == 1) "" else "s"} to Movies/VibeCut & My Clips.",
            )

            if (config.adSupportedMonetization &&
                config.interstitialEveryNBatches > 0 &&
                totalBatches % config.interstitialEveryNBatches == 0
            ) {
                launchAdFlow(AdPlacementType.INTERSTITIAL_BATCH) {
                    _selectedTab.value = MainTab.MY_CLIPS
                }
            }
        }
    }

    fun resetBatchState() {
        batchJob?.cancel()
        _batchState.value = BatchGenerationState.Idle
    }

    fun openStudioClipInFullEditor() {
        val clip = _studioSourceClip.value ?: return
        val draft = ProjectDraft(
            title = clip.displayName,
            aspectRatio = _aspectFormat.value.preset,
            cropMode = _cropMode.value,
            watermarkEnabled = _watermarkEnabled.value,
            clips = listOf(clip),
        )
        _selectedClipIndex.value = 0
        _activeToolTab.value = EditorToolTab.EDIT
        setActiveProject(draft, persist = true, seekMs = 0L)
    }

    fun openGeneratedClipInEditor(record: GeneratedClipRecord) {
        val uri = runCatching { Uri.parse(record.outputUriString) }.getOrNull() ?: return
        createProjectFromUris(listOf(uri))
    }

    fun deleteGeneratedClip(clipId: String) {
        viewModelScope.launch {
            val res = repository.deleteGeneratedClip(currentUserId, clipId)
            res.onSuccess {
                _statusMessage.value = "Clip removed from My Clips."
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Failed to delete clip."
            }
        }
    }

    fun claimInviteCode(rawCode: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val name = profile?.displayName ?: "New Creator"
        viewModelScope.launch {
            val res = repository.claimReferralCode(currentUserId, name, rawCode)
            res.onSuccess { msg ->
                _statusMessage.value = msg
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "Could not claim invite code."
            }
        }
    }

    fun saveAdminMonetizationConfig(config: MonetizationConfig) {
        val enforced = config.enforceMutualExclusion()
        viewModelScope.launch {
            val res = repository.saveMonetizationConfigLive(enforced)
            res.onSuccess {
                _localOverrideMonetization.value = null
                val modeDesc = if (enforced.adSupportedMonetization) {
                    "Ad-Supported ON (Subscriptions hidden for all users)"
                } else {
                    "Ad-Supported OFF (Paid Subscriptions enabled for all users)"
                }
                _statusMessage.value = "Saved live for all users • $modeDesc"
            }.onFailure { err ->
                _localOverrideMonetization.value = enforced
                _statusMessage.value = if (err.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                    "Cloud live sync requires Admin email (waheed61762@gmail.com). Applied locally on this device."
                } else {
                    "Applied locally; cloud sync notice: ${err.localizedMessage ?: "offline"}"
                }
            }
        }
    }

    // --- Full Timeline Editor Functions (Preserved & Unified) ---

    fun selectToolTab(tab: EditorToolTab) {
        _activeToolTab.value = tab
    }

    fun selectClip(index: Int) {
        val project = _activeProject.value ?: return
        if (index in project.clips.indices) {
            _selectedClipIndex.value = index
            seekTo(clipStartOffsetMs(project, index))
        }
    }

    fun createProjectFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isLoadingMedia.value = true
            val clips = mediaServices.inspectUris(uris)
            _isLoadingMedia.value = false
            if (clips.isEmpty()) {
                _statusMessage.value = "Could not load selected video files."
                return@launch
            }
            val title = "Cut ${SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())}"
            val draft = ProjectDraft(title = title, clips = clips)
            _selectedClipIndex.value = 0
            _activeToolTab.value = EditorToolTab.EDIT
            setActiveProject(draft, persist = true, seekMs = 0L)
        }
    }

    fun addClipsToProject(uris: List<Uri>) {
        val current = _activeProject.value ?: return
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isLoadingMedia.value = true
            val newClips = mediaServices.inspectUris(uris)
            _isLoadingMedia.value = false
            if (newClips.isEmpty()) {
                _statusMessage.value = "Could not read selected clips."
                return@launch
            }
            val updated = current.copy(
                updatedAtMillis = System.currentTimeMillis(),
                clips = current.clips + newClips,
            )
            _selectedClipIndex.value = updated.clips.lastIndex
            setActiveProject(updated, persist = true, seekMs = _currentPositionMs.value)
        }
    }

    fun openDraft(draft: ProjectDraft) {
        viewModelScope.launch {
            _isLoadingMedia.value = true
            val hydrated = draft.clips.map { mediaServices.hydrateClipThumbnails(it) }
            _isLoadingMedia.value = false
            _selectedClipIndex.value = 0
            _activeToolTab.value = EditorToolTab.EDIT
            setActiveProject(draft.copy(clips = hydrated), persist = false, seekMs = 0L)
        }
    }

    fun closeEditor() {
        releasePlayer()
        _activeProject.value = null
        _currentPositionMs.value = 0L
        _isPlaying.value = false
        mediaServices.resetExportState()
    }

    fun setAspectRatio(preset: AspectRatioPreset) {
        val current = _activeProject.value ?: return
        if (current.aspectRatio == preset) return
        setActiveProject(
            current.copy(aspectRatio = preset, updatedAtMillis = System.currentTimeMillis()),
            persist = true,
            seekMs = _currentPositionMs.value,
        )
    }

    fun updateSelectedClipTrim(trimStartMs: Long, trimEndMs: Long, debounceRebuild: Boolean = true) {
        updateSelectedClip(debounceRebuild = debounceRebuild) { clip ->
            val maxDur = clip.sourceDurationMs
            val clampedStart = trimStartMs.coerceIn(0L, (maxDur - 300L).coerceAtLeast(0L))
            val clampedEnd = trimEndMs.coerceIn(clampedStart + 300L, maxDur)
            clip.copy(trimStartMs = clampedStart, trimEndMs = clampedEnd)
        }
    }

    fun splitAtCurrentPlayhead() {
        val project = _activeProject.value ?: return
        val playhead = _currentPositionMs.value
        val (targetIdx, offsetInClipMs) = findClipAtTimelinePosition(project, playhead) ?: return
        val clip = project.clips[targetIdx]
        val sourceOffsetMs = (offsetInClipMs * clip.speed).toLong()
        val splitPointSourceMs = clip.trimStartMs + sourceOffsetMs

        if (splitPointSourceMs - clip.trimStartMs < 300L || clip.trimEndMs - splitPointSourceMs < 300L) {
            _statusMessage.value = "Move playhead away from clip edges (at least 0.3s) to split."
            return
        }

        val firstHalf = clip.copy(id = UUID.randomUUID().toString(), trimEndMs = splitPointSourceMs)
        val secondHalf = clip.copy(
            id = UUID.randomUUID().toString(),
            trimStartMs = splitPointSourceMs,
            transitionIn = ClipTransition.NONE,
        )
        val updatedClips = project.clips.toMutableList().apply {
            removeAt(targetIdx)
            add(targetIdx, secondHalf)
            add(targetIdx, firstHalf)
        }
        _selectedClipIndex.value = targetIdx + 1
        setActiveProject(
            project.copy(clips = updatedClips, updatedAtMillis = System.currentTimeMillis()),
            persist = true,
            seekMs = playhead,
        )
        _statusMessage.value = "Clip split into 2 segments"
    }

    fun updateSelectedClipFilter(filter: VideoFilter, intensity: Float = 1.0f) {
        updateSelectedClip(debounceRebuild = true) {
            it.copy(filter = filter, filterIntensity = intensity.coerceIn(0f, 1f))
        }
    }

    fun applyFilterToAllClips() {
        val project = _activeProject.value ?: return
        val selected = project.clips.getOrNull(_selectedClipIndex.value) ?: return
        val updated = project.copy(
            updatedAtMillis = System.currentTimeMillis(),
            clips = project.clips.map { it.copy(filter = selected.filter, filterIntensity = selected.filterIntensity) },
        )
        setActiveProject(updated, persist = true, seekMs = _currentPositionMs.value)
        _statusMessage.value = "${selected.filter.displayName} applied to all clips"
    }

    fun updateSelectedClipSpecialEffect(effect: SpecialEffect, intensity: Float = 0.75f) {
        updateSelectedClip(debounceRebuild = true) {
            it.copy(specialEffect = effect, effectIntensity = intensity.coerceIn(0f, 1f))
        }
    }

    fun applySpecialEffectToAllClips() {
        val project = _activeProject.value ?: return
        val selected = project.clips.getOrNull(_selectedClipIndex.value) ?: return
        val updated = project.copy(
            updatedAtMillis = System.currentTimeMillis(),
            clips = project.clips.map {
                it.copy(specialEffect = selected.specialEffect, effectIntensity = selected.effectIntensity)
            },
        )
        setActiveProject(updated, persist = true, seekMs = _currentPositionMs.value)
        _statusMessage.value = "${selected.specialEffect.displayName} applied to all clips"
    }

    fun updateSelectedClipAdjustments(adjustments: ColorAdjustments) {
        updateSelectedClip(debounceRebuild = true) { it.copy(adjustments = adjustments) }
    }

    fun resetSelectedClipAdjustments() {
        updateSelectedClip(debounceRebuild = false) { it.copy(adjustments = ColorAdjustments()) }
    }

    fun updateSelectedClipSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        updateSelectedClip(debounceRebuild = false) { it.copy(speed = clamped) }
    }

    fun toggleSelectedClipMute() {
        updateSelectedClip(debounceRebuild = false) { it.copy(isMuted = !it.isMuted) }
    }

    fun rotateSelectedClip90() {
        updateSelectedClip(debounceRebuild = false) { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) }
    }

    fun updateClipTransition(clipIndex: Int, transition: ClipTransition) {
        val project = _activeProject.value ?: return
        if (clipIndex !in project.clips.indices) return
        val updatedClips = project.clips.toMutableList().apply {
            this[clipIndex] = this[clipIndex].copy(transitionIn = transition)
        }
        setActiveProject(
            project.copy(clips = updatedClips, updatedAtMillis = System.currentTimeMillis()),
            persist = true,
            seekMs = clipStartOffsetMs(project, clipIndex),
        )
    }

    fun updateSelectedClipTextOverlay(overlay: ClipTextOverlay?) {
        updateSelectedClip(debounceRebuild = false) {
            it.copy(textOverlay = overlay?.takeIf { o -> o.text.isNotBlank() })
        }
    }

    fun moveSelectedClip(delta: Int) {
        val project = _activeProject.value ?: return
        val idx = _selectedClipIndex.value
        val target = idx + delta
        if (idx !in project.clips.indices || target !in project.clips.indices) return
        val mutable = project.clips.toMutableList()
        val item = mutable.removeAt(idx)
        mutable.add(target, item)
        _selectedClipIndex.value = target
        setActiveProject(project.copy(clips = mutable, updatedAtMillis = System.currentTimeMillis()), true, 0L)
    }

    fun duplicateSelectedClip() {
        val project = _activeProject.value ?: return
        val idx = _selectedClipIndex.value
        val clip = project.clips.getOrNull(idx) ?: return
        val copy = clip.copy(id = UUID.randomUUID().toString(), transitionIn = ClipTransition.NONE)
        val mutable = project.clips.toMutableList().apply { add(idx + 1, copy) }
        _selectedClipIndex.value = idx + 1
        setActiveProject(project.copy(clips = mutable, updatedAtMillis = System.currentTimeMillis()), true, _currentPositionMs.value)
    }

    fun deleteSelectedClip() {
        val project = _activeProject.value ?: return
        if (project.clips.size <= 1) {
            _statusMessage.value = "A project must keep at least 1 clip."
            return
        }
        val idx = _selectedClipIndex.value
        if (idx !in project.clips.indices) return
        val mutable = project.clips.toMutableList().apply { removeAt(idx) }
        _selectedClipIndex.value = idx.coerceAtMost(mutable.lastIndex)
        setActiveProject(project.copy(clips = mutable, updatedAtMillis = System.currentTimeMillis()), true, 0L)
    }

    fun playPause() {
        val player = _playerState.value ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            val total = _activeProject.value?.totalDurationMs ?: 0L
            if (total > 0L && player.currentPosition >= total - 150L) {
                player.seekTo(0L)
            }
            player.play()
        }
    }

    fun pausePlayback() {
        _playerState.value?.pause()
    }

    fun seekTo(ms: Long) {
        val total = _activeProject.value?.totalDurationMs ?: 0L
        val clamped = ms.coerceIn(0L, total)
        _currentPositionMs.value = clamped
        _playerState.value?.seekTo(clamped)
    }

    fun stepRelative(deltaMs: Long) {
        seekTo(_currentPositionMs.value + deltaMs)
    }

    fun updateExportConfig(transform: (ExportConfig) -> ExportConfig) {
        _exportConfig.value = transform(_exportConfig.value)
    }

    fun startExport() {
        val project = _activeProject.value ?: return
        pausePlayback()
        mediaServices.startExport(project, _exportConfig.value)
    }

    fun cancelExport() {
        mediaServices.cancelExport()
    }

    fun resetExportState() {
        mediaServices.resetExportState()
    }

    private fun updateSelectedClip(debounceRebuild: Boolean, transform: (VideoClip) -> VideoClip) {
        val project = _activeProject.value ?: return
        val idx = _selectedClipIndex.value
        val existing = project.clips.getOrNull(idx) ?: return
        val updatedClip = transform(existing)
        val nextList = project.clips.toMutableList().apply { this[idx] = updatedClip }
        val updatedProject = project.copy(clips = nextList, updatedAtMillis = System.currentTimeMillis())
        _activeProject.value = updatedProject
        persistDraft(updatedProject)
        if (debounceRebuild) {
            scheduleDebouncedPlayerRebuild(updatedProject, _currentPositionMs.value)
        } else {
            rebuildPlayerNow(updatedProject, _currentPositionMs.value)
        }
    }

    private fun setActiveProject(project: ProjectDraft, persist: Boolean, seekMs: Long) {
        _activeProject.value = project
        if (persist) persistDraft(project)
        rebuildPlayerNow(project, seekMs)
    }

    private fun persistDraft(draft: ProjectDraft) {
        viewModelScope.launch {
            val current = _drafts.value.toMutableList()
            val existingIdx = current.indexOfFirst { it.id == draft.id }
            if (existingIdx >= 0) current[existingIdx] = draft else current.add(0, draft)
            val sorted = current.sortedByDescending { it.updatedAtMillis }
            _drafts.value = sorted
            mediaServices.projectStorage.saveDrafts(sorted)
        }
    }

    private fun scheduleDebouncedPlayerRebuild(project: ProjectDraft, seekMs: Long) {
        compositionRebuildJob?.cancel()
        compositionRebuildJob = viewModelScope.launch {
            delay(140L)
            rebuildPlayerNow(project, seekMs)
        }
    }

    private fun rebuildPlayerNow(project: ProjectDraft, seekMs: Long) {
        compositionRebuildJob?.cancel()
        val composition = CompositionBuilder.buildForPreview(project) ?: return
        val wasPlaying = _playerState.value?.isPlaying == true
        val targetSeek = seekMs.coerceIn(0L, project.totalDurationMs.coerceAtLeast(0L))

        Handler(Looper.getMainLooper()).post {
            val oldPlayer = _playerState.value
            _playerState.value = null
            runCatching { oldPlayer?.release() }

            runCatching {
                val newPlayer = mediaServices.createPlayer()
                newPlayer.repeatMode = Player.REPEAT_MODE_ALL
                newPlayer.addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            _statusMessage.value = "Preview error: ${error.localizedMessage ?: "codec error"}"
                        }
                    },
                )
                newPlayer.setComposition(composition)
                newPlayer.prepare()
                if (targetSeek > 0L) newPlayer.seekTo(targetSeek)
                if (wasPlaying) newPlayer.play()
                _playerState.value = newPlayer
                startPositionTicker(newPlayer)
            }.onFailure { err ->
                _statusMessage.value = "Could not initialize preview: ${err.localizedMessage ?: "unsupported media"}"
            }
        }
    }

    private fun startPositionTicker(player: CompositionPlayer) {
        positionTickerJob?.cancel()
        positionTickerJob = viewModelScope.launch(Dispatchers.Main) {
            while (isActive && _playerState.value === player) {
                if (player.isPlaying) {
                    _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                }
                delay(50L)
            }
        }
    }

    private fun releasePlayer() {
        compositionRebuildJob?.cancel()
        positionTickerJob?.cancel()
        Handler(Looper.getMainLooper()).post {
            val oldPlayer = _playerState.value
            _playerState.value = null
            runCatching { oldPlayer?.release() }
        }
    }

    private fun clipStartOffsetMs(project: ProjectDraft, index: Int): Long {
        var sum = 0L
        for (i in 0 until index.coerceAtMost(project.clips.size)) {
            sum += project.clips[i].effectiveDurationMs
        }
        return sum
    }

    private fun findClipAtTimelinePosition(project: ProjectDraft, posMs: Long): Pair<Int, Long>? {
        var cursor = 0L
        project.clips.forEachIndexed { index, clip ->
            val end = cursor + clip.effectiveDurationMs
            if (posMs <= end || index == project.clips.lastIndex) {
                val offset = (posMs - cursor).coerceIn(0L, clip.effectiveDurationMs)
                return index to offset
            }
            cursor = end
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        batchJob?.cancel()
        mediaServices.cancelExport()
        releasePlayer()
    }

    private companion object {
        const val TAG = "VibeCutVM"
    }
}
