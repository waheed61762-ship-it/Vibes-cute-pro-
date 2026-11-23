package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.util.UnstableApi
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOut
import com.example.data.VibeCutRepository
import com.example.engine.VibeCutMediaServices
import com.example.model.MainTab
import com.example.ui.UiState
import com.example.ui.VibeCutViewModel
import com.example.ui.components.AdExperienceDialog
import com.example.ui.components.PaywallSheet
import com.example.ui.screens.AdminPanelSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.InfoTabScreen
import com.example.ui.screens.InviteTabScreen
import com.example.ui.screens.MyClipsTabScreen
import com.example.ui.screens.StudioTabScreen
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PrismCutTheme
import com.example.ui.theme.TextMuted
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            PrismCutTheme {
                VibeCutRootHost()
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VibeCutRootHost() {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val authScope = rememberCoroutineScope()

    val authFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var isCheckingSilentAuth by remember { mutableStateOf(currentUser == null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isCheckingSilentAuth = false
                    authError = null
                },
                onUnauthenticated = {
                    isCheckingSilentAuth = false
                },
                scope = authScope,
            )
        } else {
            isCheckingSilentAuth = false
        }
    }

    val user = currentUser
    if (isCheckingSilentAuth && user == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = ElectricCyan)
        }
    } else if (user == null) {
        AuthScreen(
            isAuthenticating = isAuthenticating,
            authError = authError,
            onGoogleSignInClick = {
                isAuthenticating = true
                authError = null
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        isAuthenticating = false
                        authError = null
                    },
                    onAuthError = { err ->
                        isAuthenticating = false
                        authError = err
                    },
                    onAuthCancelled = {
                        isAuthenticating = false
                    },
                    scope = authScope,
                )
            },
        )
    } else {
        key(user.uid) {
            val app = context.applicationContext as Application
            val exportScope = remember(user.uid) {
                CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            }
            DisposableEffect(user.uid) {
                onDispose { exportScope.cancel() }
            }

            val factory = remember(user.uid) {
                viewModelFactory {
                    initializer {
                        val repo = VibeCutRepository(app)
                        val mediaServices = VibeCutMediaServices(app, exportScope)
                        VibeCutViewModel(
                            repository = repo,
                            mediaServices = mediaServices,
                            currentUserId = user.uid,
                        )
                    }
                }
            }
            val viewModel: VibeCutViewModel = viewModel(key = user.uid, factory = factory)

            VibeCutAuthenticatedApp(
                viewModel = viewModel,
                userEmail = user.email,
                onSignOutClick = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = authScope,
                    )
                },
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VibeCutAuthenticatedApp(
    viewModel: VibeCutViewModel,
    userEmail: String?,
    onSignOutClick: () -> Unit,
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val monetizationConfig by viewModel.monetizationConfig.collectAsStateWithLifecycle()
    val userProfileUi by viewModel.userProfileState.collectAsStateWithLifecycle()
    val myClipsUi by viewModel.myClipsState.collectAsStateWithLifecycle()
    val confirmedReferrals by viewModel.confirmedReferrals.collectAsStateWithLifecycle()

    val linkInput by viewModel.linkInput.collectAsStateWithLifecycle()
    val isResolvingLink by viewModel.isResolvingLink.collectAsStateWithLifecycle()
    val isLoadingMedia by viewModel.isLoadingMedia.collectAsStateWithLifecycle()
    val resolvedLinkMedia by viewModel.resolvedLinkMedia.collectAsStateWithLifecycle()
    val studioSourceClip by viewModel.studioSourceClip.collectAsStateWithLifecycle()
    val clipLength by viewModel.clipLength.collectAsStateWithLifecycle()
    val aspectFormat by viewModel.aspectFormat.collectAsStateWithLifecycle()
    val clipCount by viewModel.clipCount.collectAsStateWithLifecycle()
    val discoveryMode by viewModel.discoveryMode.collectAsStateWithLifecycle()
    val cropMode by viewModel.cropMode.collectAsStateWithLifecycle()
    val qualityTier by viewModel.qualityTier.collectAsStateWithLifecycle()
    val watermarkEnabled by viewModel.watermarkEnabled.collectAsStateWithLifecycle()
    val plannedSegments by viewModel.plannedSegments.collectAsStateWithLifecycle()
    val batchState by viewModel.batchState.collectAsStateWithLifecycle()

    val adSessionState by viewModel.adSessionState.collectAsStateWithLifecycle()
    val showPaywallSheet by viewModel.showPaywallSheet.collectAsStateWithLifecycle()

    // Full Editor states
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val selectedClipIndex by viewModel.selectedClipIndex.collectAsStateWithLifecycle()
    val activeToolTab by viewModel.activeToolTab.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val exportConfig by viewModel.exportConfig.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()

    var showAdminPanel by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val studioGalleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.loadGalleryVideoForStudio(uri)
        }
    }

    val addClipsPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addClipsToProject(uris)
        }
    }

    val userProfile = (userProfileUi as? UiState.Success)?.data
    val hasBatchAccess = viewModel.hasBatchAccess(clipCount, userProfile, monetizationConfig)
    val hasQualityAccess = viewModel.hasQualityAccess(qualityTier, userProfile, monetizationConfig)
    val hasWatermarkAccess = viewModel.hasWatermarkRemovalAccess(userProfile, monetizationConfig)

    val project = activeProject
    if (project != null) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { _ ->
            EditorScreen(
                project = project,
                player = player,
                selectedClipIndex = selectedClipIndex,
                activeTab = activeToolTab,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                capabilities = viewModel.capabilities,
                exportConfig = exportConfig,
                exportState = exportState,
                onBack = viewModel::closeEditor,
                onPausePlayback = viewModel::pausePlayback,
                onPlayPause = viewModel::playPause,
                onSeekTo = viewModel::seekTo,
                onStepRelative = viewModel::stepRelative,
                onSelectClip = viewModel::selectClip,
                onSelectTab = viewModel::selectToolTab,
                onAspectRatioChange = viewModel::setAspectRatio,
                onAddClipsClick = {
                    addClipsPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                    )
                },
                onTrimChange = viewModel::updateSelectedClipTrim,
                onSplitAtPlayhead = viewModel::splitAtCurrentPlayhead,
                onSpeedChange = viewModel::updateSelectedClipSpeed,
                onToggleMute = viewModel::toggleSelectedClipMute,
                onRotate90 = viewModel::rotateSelectedClip90,
                onMoveClip = viewModel::moveSelectedClip,
                onDuplicateClip = viewModel::duplicateSelectedClip,
                onDeleteClip = viewModel::deleteSelectedClip,
                onFilterChange = viewModel::updateSelectedClipFilter,
                onApplyFilterToAll = viewModel::applyFilterToAllClips,
                onAdjustmentsChange = viewModel::updateSelectedClipAdjustments,
                onResetAdjustments = viewModel::resetSelectedClipAdjustments,
                onSpecialEffectChange = viewModel::updateSelectedClipSpecialEffect,
                onApplyEffectToAll = viewModel::applySpecialEffectToAllClips,
                onTransitionChange = viewModel::updateClipTransition,
                onTextOverlayChange = viewModel::updateSelectedClipTextOverlay,
                onUpdateExportConfig = viewModel::updateExportConfig,
                onStartExport = viewModel::startExport,
                onCancelExport = viewModel::cancelExport,
                onResetExportState = viewModel::resetExportState,
            )
        }
    } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg),
        ) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = ObsidianBg,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CarbonSurface)
                                .navigationBarsPadding(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            listOf(
                                                ElectricCyan.copy(alpha = 0.25f),
                                                com.example.ui.theme.VioletGlow.copy(alpha = 0.25f),
                                                ElectricCyan.copy(alpha = 0.15f),
                                            ),
                                        ),
                                    ),
                            )
                            NavigationBar(
                                containerColor = CarbonSurface,
                                contentColor = Color.White,
                                tonalElevation = 0.dp,
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = selectedTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { viewModel.selectMainTab(tab) },
                                        icon = { Icon(tabIcon(tab, selected), contentDescription = tab.label) },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        },
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ElectricCyan,
                                            selectedTextColor = ElectricCyan,
                                            indicatorColor = ElectricCyanContainer,
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding(),
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = CarbonSurface,
                            contentColor = Color.White,
                            modifier = Modifier
                                .fillMaxHeight()
                                .navigationBarsPadding(),
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = selectedTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectMainTab(tab) },
                                    icon = { Icon(tabIcon(tab, selected), contentDescription = tab.label) },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ElectricCyan,
                                        selectedTextColor = ElectricCyan,
                                        indicatorColor = ElectricCyanContainer,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                    ),
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "vibe_main_tab_transition",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) { activeTab ->
                        when (activeTab) {
                            MainTab.STUDIO -> StudioTabScreen(
                                linkInput = linkInput,
                                isResolvingLink = isResolvingLink,
                                isLoadingMedia = isLoadingMedia,
                                resolvedLinkMedia = resolvedLinkMedia,
                                sourceClip = studioSourceClip,
                                clipLength = clipLength,
                                aspectFormat = aspectFormat,
                                clipCount = clipCount,
                                discoveryMode = discoveryMode,
                                cropMode = cropMode,
                                qualityTier = qualityTier,
                                watermarkEnabled = watermarkEnabled,
                                plannedSegments = plannedSegments,
                                batchState = batchState,
                                userProfile = userProfile,
                                monetizationConfig = monetizationConfig,
                                hasBatchAccess = hasBatchAccess,
                                hasQualityAccess = hasQualityAccess,
                                hasWatermarkAccess = hasWatermarkAccess,
                                onLinkInputChange = viewModel::updateLinkInput,
                                onInspectLink = viewModel::loadVideoFromLink,
                                onPickGalleryVideo = {
                                    studioGalleryPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                                    )
                                },
                                onSelectClipLength = viewModel::selectClipLength,
                                onSelectAspectFormat = viewModel::selectAspectFormat,
                                onSelectClipCount = viewModel::selectClipCount,
                                onSelectDiscoveryMode = viewModel::selectDiscoveryMode,
                                onSelectCropMode = viewModel::selectCropMode,
                                onSelectQualityTier = viewModel::selectQualityTier,
                                onToggleWatermark = viewModel::toggleWatermark,
                                onRequestUnlock = viewModel::requestFeatureUnlock,
                                onGenerateClips = viewModel::generateClipsBatch,
                                onResetBatchState = viewModel::resetBatchState,
                                onOpenFullEditor = viewModel::openStudioClipInFullEditor,
                                onViewMyClips = { viewModel.selectMainTab(MainTab.MY_CLIPS) },
                            )

                            MainTab.MY_CLIPS -> MyClipsTabScreen(
                                clipsState = myClipsUi,
                                onOpenInEditor = viewModel::openGeneratedClipInEditor,
                                onDeleteClip = viewModel::deleteGeneratedClip,
                                onSwitchToStudio = { viewModel.selectMainTab(MainTab.STUDIO) },
                            )

                            MainTab.INVITE -> InviteTabScreen(
                                userProfile = userProfile,
                                confirmedReferrals = confirmedReferrals,
                                onClaimInviteCode = viewModel::claimInviteCode,
                                onShowToast = viewModel::showStatus,
                            )

                            MainTab.INFO -> InfoTabScreen(
                                monetizationConfig = monetizationConfig,
                                userProfile = userProfile,
                                userEmail = userEmail,
                                onOpenAdminPanel = { showAdminPanel = true },
                                onOpenSubscriptionPaywall = viewModel::openSubscriptionPaywall,
                                onSignOutClick = onSignOutClick,
                                onShowToast = viewModel::showStatus,
                            )
                        }
                    }
                }
            }
        }
    }

    AdExperienceDialog(
        state = adSessionState,
        bonusCredits = userProfile?.bonusCredits ?: 0L,
        onRetry = viewModel::retryCurrentAd,
        onCompleteUnlock = viewModel::completeAdAndUnlock,
        onSpendBonusCredit = viewModel::spendCreditToUnlock,
        onDismiss = viewModel::dismissAdDialog,
    )

    if (showPaywallSheet && !monetizationConfig.adSupportedMonetization) {
        PaywallSheet(
            bonusCredits = userProfile?.bonusCredits ?: 0L,
            onSubscribe = viewModel::subscribeToPlan,
            onSpendBonusCredit = viewModel::spendCreditToUnlock,
            onDismiss = viewModel::dismissPaywall,
        )
    }

    if (showAdminPanel) {
        AdminPanelSheet(
            currentConfig = monetizationConfig,
            onSaveLive = viewModel::saveAdminMonetizationConfig,
            onDismiss = { showAdminPanel = false },
        )
    }
}

private fun tabIcon(tab: MainTab, selected: Boolean): ImageVector = when (tab) {
    MainTab.STUDIO -> if (selected) Icons.Rounded.AutoAwesomeMotion else Icons.Outlined.AutoAwesomeMotion
    MainTab.MY_CLIPS -> if (selected) Icons.Rounded.VideoLibrary else Icons.Outlined.VideoLibrary
    MainTab.INVITE -> if (selected) Icons.Rounded.CardGiftcard else Icons.Outlined.CardGiftcard
    MainTab.INFO -> if (selected) Icons.Rounded.Info else Icons.Outlined.Info
}
