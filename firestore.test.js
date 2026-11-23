const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_irfan";
const ADMIN_EMAIL = "waheed61762@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validMonetizationPayload(ts, adSupported = true) {
  return {
    adSupportedMonetization: adSupported,
    subscriptionEnabled: !adSupported,
    activeAdNetwork: "START_IO",
    startIoAppId: "210382301",
    startIoTestMode: true,
    unityGameId: "5700112",
    unityRewardedPlacementId: "Rewarded_Android",
    unityInterstitialPlacementId: "Interstitial_Android",
    unityTestMode: true,
    adsterraDirectLinkUrl: "https://example.com/direct-link",
    adsterraBannerTagId: "banner_320x50",
    adsterraInterstitialTagId: "interstitial_main",
    adsterraTestMode: true,
    interstitialEveryNBatches: 2,
    updatedByEmail: ADMIN_EMAIL,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validUserPayload(uid, inviteCode, ts) {
  return {
    userId: uid,
    displayName: "Creator",
    inviteCode,
    referralCount: 0,
    bonusCredits: 2,
    adUnlockedBatches: 0,
    adUnlockedHd: false,
    adUnlockedWatermarkFree: false,
    subscriptionActive: false,
    subscriptionPlan: "NONE",
    batchesProcessed: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("app_config").doc("monetization").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("clips").get());
  await assertFails(unauthDb.collection("referral_codes").doc("VC-ALICE1").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, "VC-ALICE1", ts))
  );
});

test("Admin monetization config: verified admin write + mutual exclusion + spoof protection", async () => {
  const ts = new Date(Date.now() - 5000);
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();

  // Spoofed unverified admin email rejected
  await assertFails(
    spoofedAdminDb.collection("app_config").doc("monetization").set(validMonetizationPayload(ts, true))
  );

  // Regular user cannot write monetization config
  await assertFails(
    aliceDb.collection("app_config").doc("monetization").set(validMonetizationPayload(ts, true))
  );

  // Mutual exclusion violation (both true) rejected even for admin
  const invalidMutual = {
    ...validMonetizationPayload(ts, true),
    subscriptionEnabled: true,
  };
  await assertFails(
    adminDb.collection("app_config").doc("monetization").set(invalidMutual)
  );

  // Future timestamp rejected
  await assertFails(
    adminDb
      .collection("app_config")
      .doc("monetization")
      .set(validMonetizationPayload(new Date(Date.now() + 60000), true))
  );

  // Valid admin create succeeds
  await assertSucceeds(
    adminDb.collection("app_config").doc("monetization").set(validMonetizationPayload(ts, true))
  );

  // Regular signed-in user can read single doc, cannot list collection
  await assertSucceeds(aliceDb.collection("app_config").doc("monetization").get());
  await assertFails(aliceDb.collection("app_config").get());

  // Valid admin update toggling Ad-Supported OFF and Subscription ON succeeds
  await assertSucceeds(
    adminDb.collection("app_config").doc("monetization").update({
      adSupportedMonetization: false,
      subscriptionEnabled: true,
      activeAdNetwork: "ADSTERRA",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update with ghost field rejected
  await assertFails(
    adminDb.collection("app_config").doc("monetization").update({
      ghostField: "hack",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("User profile & clips: strict owner isolation, empty list support, and @DocumentId parity", async () => {
  const ts = new Date(Date.now() - 5000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, "VC-ALICE1", ts))
  );

  // Cross-user read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ bonusCredits: 999 }));

  // Owner cannot mutate immutable inviteCode or createdAt
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      inviteCode: "VC-HACKED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      adUnlockedHd: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Empty clips subcollection list query succeeds for owner, fails for Bob
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("clips").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("clips").get());

  // Valid clip create (no @DocumentId `id` field in map)
  const clipPayload = {
    userId: ALICE_UID,
    title: "Clip 1 • Viral Hook",
    sourceLabel: "Gallery Video",
    formatLabel: "Reels 9:16",
    aspectRatio: "9:16",
    durationMs: 15000,
    resolutionLabel: "1080p FHD",
    outputUriString: "content://media/external/video/media/101",
    displayPath: "Movies/VibeCut/VibeCut_clip_1.mp4",
    fileSizeMb: 4.2,
    discoveryMode: "VIRAL_CLIPS",
    cropMode: "FILL_CROP",
    watermarkApplied: false,
    createdAt: ts,
    updatedAt: ts,
  };
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").set(clipPayload)
  );

  // Reject payload containing redundant `id` key
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("clips")
      .doc("clip_2")
      .set({ ...clipPayload, id: "clip_2" })
  );

  // Owner can rename and delete own clip; Bob cannot
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").update({
      title: "Renamed Clip",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").delete()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("clips").doc("clip_1").delete()
  );
});

test("Referral codes & claims: cross-device referral tracking and self-referral guard", async () => {
  const ts = new Date(Date.now() - 5000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice creates her public invite code
  await assertSucceeds(
    aliceDb.collection("referral_codes").doc("VC-ALICE1").set({
      code: "VC-ALICE1",
      ownerUid: ALICE_UID,
      ownerName: "Alice",
      createdAt: ts,
    })
  );

  // Listing all referral codes is denied; direct lookup by exact code succeeds
  await assertFails(bobDb.collection("referral_codes").get());
  await assertSucceeds(bobDb.collection("referral_codes").doc("VC-ALICE1").get());

  // Self-referral denied
  await assertFails(
    aliceDb.collection("referral_claims").doc(ALICE_UID).set({
      claimantUid: ALICE_UID,
      claimantName: "Alice",
      inviterUid: ALICE_UID,
      code: "VC-ALICE1",
      creditsAwarded: 5,
      createdAt: ts,
    })
  );

  // Bob claims Alice's code from a new device
  await assertSucceeds(
    bobDb.collection("referral_claims").doc(BOB_UID).set({
      claimantUid: BOB_UID,
      claimantName: "Bob",
      inviterUid: ALICE_UID,
      code: "VC-ALICE1",
      creditsAwarded: 5,
      createdAt: ts,
    })
  );

  // Alice can query her confirmed referrals; unfiltered query fails
  await assertSucceeds(
    aliceDb.collection("referral_claims").where("inviterUid", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("referral_claims").get());
});
