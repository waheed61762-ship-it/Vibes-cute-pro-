package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AdPlacementType
import com.example.model.GeneratedClipRecord
import com.example.model.MonetizationConfig
import com.example.model.ReferralClaimDoc
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class VibeCutRepository(
    private val db: FirebaseFirestore,
    private val localStore: MonetizationLocalStore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
        localStore = MonetizationLocalStore(context.applicationContext),
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun observeLocalMonetizationFallback(): Flow<MonetizationConfig> = localStore.configFlow

    fun observeMonetizationConfig(): Flow<MonetizationConfig?> = flow {
        val uid = requireUserId()
        val path = "app_config/monetization"
        emitAll(
            db.collection("app_config")
                .document("monetization")
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) {
                        null
                    } else {
                        snapshot.toObject(
                            MonetizationConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                        )?.enforceMutualExclusion()
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                },
        )
    }

    suspend fun saveMonetizationConfigLive(config: MonetizationConfig): Result<Boolean> {
        val path = "app_config/monetization"
        val normalized = config.enforceMutualExclusion()
        localStore.save(normalized)

        return try {
            val user = auth.currentUser ?: error("Sign in required.")
            val email = user.email?.takeIf { it.isNotBlank() } ?: "admin@vibecut.app"
            val docRef = db.collection("app_config").document("monetization")
            val existing = docRef.get().await()
            val payload = mapOf(
                "adSupportedMonetization" to normalized.adSupportedMonetization,
                "subscriptionEnabled" to normalized.subscriptionEnabled,
                "activeAdNetwork" to normalized.activeAdNetwork,
                "startIoAppId" to normalized.startIoAppId.trim().take(64),
                "startIoTestMode" to normalized.startIoTestMode,
                "unityGameId" to normalized.unityGameId.trim().take(64),
                "unityRewardedPlacementId" to normalized.unityRewardedPlacementId.trim().take(64),
                "unityInterstitialPlacementId" to normalized.unityInterstitialPlacementId.trim().take(64),
                "unityTestMode" to normalized.unityTestMode,
                "adsterraDirectLinkUrl" to normalized.adsterraDirectLinkUrl.trim().take(500),
                "adsterraBannerTagId" to normalized.adsterraBannerTagId.trim().take(120),
                "adsterraInterstitialTagId" to normalized.adsterraInterstitialTagId.trim().take(120),
                "adsterraTestMode" to normalized.adsterraTestMode,
                "interstitialEveryNBatches" to normalized.interstitialEveryNBatches.coerceIn(1, 20),
                "updatedByEmail" to email.take(120),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (existing.exists()) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload + ("createdAt" to FieldValue.serverTimestamp())).await()
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val userPath = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val displayName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Creator").take(80)
            if (!snap.exists()) {
                val inviteCode = generateInviteCode(uid)
                val userPayload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "inviteCode" to inviteCode,
                    "referralCount" to 0L,
                    "bonusCredits" to 2L,
                    "adUnlockedBatches" to 0L,
                    "adUnlockedHd" to false,
                    "adUnlockedWatermarkFree" to false,
                    "subscriptionActive" to false,
                    "subscriptionPlan" to SubscriptionPlan.NONE.name,
                    "batchesProcessed" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(userPayload).await()
                registerReferralCodeIfAbsent(inviteCode, uid, displayName)
            } else {
                val existingCode = snap.getString("inviteCode")
                if (!existingCode.isNullOrBlank()) {
                    registerReferralCodeIfAbsent(existingCode, uid, displayName)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userPath)
            Result.failure(e)
        }
    }

    private suspend fun registerReferralCodeIfAbsent(code: String, uid: String, ownerName: String) {
        val codeRef = db.collection("referral_codes").document(code)
        val snap = runCatching { codeRef.get().await() }.getOrNull()
        if (snap == null || !snap.exists()) {
            runCatching {
                codeRef.set(
                    mapOf(
                        "code" to code,
                        "ownerUid" to uid,
                        "ownerName" to ownerName.take(80),
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed." }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                },
        )
    }

    fun observeMyClips(userId: String): Flow<List<GeneratedClipRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed." }
        val path = "users/$userId/clips"
        emitAll(
            db.collection("users").document(userId).collection("clips")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GeneratedClipRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    suspend fun saveGeneratedClip(userId: String, record: GeneratedClipRecord): Result<String> {
        var path = "users/$userId/clips"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = db.collection("users").document(uid).collection("clips").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to record.title.trim().ifEmpty { "VibeCut Clip" }.take(120),
                "sourceLabel" to record.sourceLabel.trim().ifEmpty { "Gallery Video" }.take(200),
                "formatLabel" to record.formatLabel.trim().ifEmpty { "Reels 9:16" }.take(40),
                "aspectRatio" to record.aspectRatio.trim().ifEmpty { "9:16" }.take(24),
                "durationMs" to record.durationMs.coerceIn(200L, 3_600_000L),
                "resolutionLabel" to record.resolutionLabel.trim().ifEmpty { "360p" }.take(40),
                "outputUriString" to record.outputUriString.trim().take(500),
                "displayPath" to record.displayPath.trim().take(240),
                "fileSizeMb" to record.fileSizeMb.coerceIn(0.0, 50_000.0),
                "discoveryMode" to record.discoveryMode,
                "cropMode" to record.cropMode,
                "watermarkApplied" to record.watermarkApplied,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteGeneratedClip(userId: String, clipId: String): Result<Unit> {
        val path = "users/$userId/clips/$clipId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            require(clipId.isNotBlank()) { "Clip ID required." }
            db.collection("users").document(uid).collection("clips").document(clipId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeConfirmedReferrals(userId: String): Flow<List<ReferralClaimDoc>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "referral_claims"
        emitAll(
            db.collection("referral_claims")
                .whereEqualTo("inviterUid", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReferralClaimDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    suspend fun syncConfirmedInviterCredits(
        userId: String,
        recordedReferralCount: Long,
        confirmedReferralCount: Long,
    ): Result<Unit> {
        if (confirmedReferralCount <= recordedReferralCount) return Result.success(Unit)
        val delta = confirmedReferralCount - recordedReferralCount
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            db.collection("users").document(uid).update(
                mapOf(
                    "referralCount" to confirmedReferralCount,
                    "bonusCredits" to FieldValue.increment(delta * 5L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimReferralCode(
        userId: String,
        claimantName: String,
        rawCode: String,
    ): Result<String> {
        val normalized = rawCode.trim().uppercase(Locale.US)
        if (!normalized.matches(Regex("^[A-Z0-9\\-]{4,24}$"))) {
            return Result.failure(IllegalArgumentException("Enter a valid invite code (e.g. VC-XXXXXX)."))
        }
        val claimPath = "referral_claims/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val existingClaim = db.collection("referral_claims").document(uid).get().await()
            if (existingClaim.exists()) {
                return Result.failure(IllegalStateException("Welcome referral bonus already claimed on this account."))
            }

            val codeSnap = db.collection("referral_codes").document(normalized).get().await()
            if (!codeSnap.exists()) {
                return Result.failure(IllegalArgumentException("Invite code '$normalized' was not found."))
            }
            val inviterUid = codeSnap.getString("ownerUid")
                ?: return Result.failure(IllegalStateException("Invite code record is incomplete."))
            if (inviterUid == uid) {
                return Result.failure(IllegalArgumentException("You cannot claim your own invite code."))
            }

            val claimPayload = mapOf(
                "claimantUid" to uid,
                "claimantName" to claimantName.trim().ifEmpty { "New Creator" }.take(80),
                "inviterUid" to inviterUid,
                "code" to normalized,
                "creditsAwarded" to 5L,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            db.collection("referral_claims").document(uid).set(claimPayload).await()
            db.collection("users").document(uid).update(
                mapOf(
                    "referredByCode" to normalized,
                    "bonusCredits" to FieldValue.increment(5L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()

            val ownerName = codeSnap.getString("ownerName") ?: "your friend"
            Result.success("Joined via $ownerName! +5 Bonus Credits awarded.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, claimPath)
            Result.failure(e)
        }
    }

    suspend fun grantAdReward(userId: String, placement: AdPlacementType): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            when (placement) {
                AdPlacementType.REWARDED_BATCH -> updates["adUnlockedBatches"] = FieldValue.increment(1L)
                AdPlacementType.REWARDED_QUALITY -> updates["adUnlockedHd"] = true
                AdPlacementType.REWARDED_WATERMARK -> updates["adUnlockedWatermarkFree"] = true
                AdPlacementType.INTERSTITIAL_BATCH -> Unit
            }
            if (updates.size > 1) {
                db.collection("users").document(uid).update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun spendBonusCreditForUnlock(userId: String, placement: AdPlacementType): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val userDoc = db.collection("users").document(uid).get().await()
            val credits = userDoc.getLong("bonusCredits") ?: 0L
            if (credits < 1L) {
                return Result.failure(IllegalStateException("No bonus credits available."))
            }
            val updates = mutableMapOf<String, Any>(
                "bonusCredits" to (credits - 1L),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            when (placement) {
                AdPlacementType.REWARDED_BATCH -> updates["adUnlockedBatches"] = FieldValue.increment(1L)
                AdPlacementType.REWARDED_QUALITY -> updates["adUnlockedHd"] = true
                AdPlacementType.REWARDED_WATERMARK -> updates["adUnlockedWatermarkFree"] = true
                AdPlacementType.INTERSTITIAL_BATCH -> Unit
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun activateSubscription(userId: String, plan: SubscriptionPlan): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val active = plan != SubscriptionPlan.NONE
            db.collection("users").document(uid).update(
                mapOf(
                    "subscriptionActive" to active,
                    "subscriptionPlan" to plan.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementBatchesProcessed(userId: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            db.collection("users").document(uid).update(
                mapOf(
                    "batchesProcessed" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun generateInviteCode(uid: String): String {
        val clean = uid.replace(Regex("[^A-Za-z0-9]"), "").uppercase(Locale.US)
        val suffix = clean.take(6).padEnd(6, 'V')
        return "VC-$suffix"
    }
}
