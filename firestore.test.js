const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_nafiul_uid";
const ALICE_UID = "student_alice_uid";
const BOB_UID = "student_bob_uid";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const FIXED_PAST_TIME = new Date(1700000000000);
function nowPast() {
  return new Date(Date.now() - 1000);
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

function validManifestPayload(authorUid) {
  return {
    authorUid,
    versionCode: 102,
    versionTag: "v1.2.0",
    publishedTimestamp: 1763300000000,
    publishedBy: "Md. Nafiul Islam Nahin (Developer of ISCEB App)",
    releaseNotes: "Synced live modules and latest APK metadata",
    announcement: "ISCEB Live Auto-Sync Active",
    minSupportedAppVersion: 1,
    latestApkVersionCode: 2,
    latestApkVersionName: "1.1",
    apkDownloadUrl: "https://www.isceb.org/app/releases/latest",
    apkReleaseNotes: "Automatic update synchronization build",
    createdAt: FIXED_PAST_TIME,
    updatedAt: nowPast(),
  };
}

function validCatalogPayload(authorUid) {
  return {
    authorUid,
    versionCode: 102,
    modules: [{ id: "mod-1", number: 1, title: "Concept of SCM" }],
    classes: [{ id: "class-1", moduleId: "mod-1", orderIndex: 1, title: "Lecture 01" }],
    settings: { institutionName: "ISCEB", quizPassingScore: 70 },
    createdAt: FIXED_PAST_TIME,
    updatedAt: nowPast(),
  };
}

function validProgressPayload(userId, classId) {
  return {
    userId,
    classId,
    status: "IN_PROGRESS",
    activeLearningSeconds: 45,
    maxPlaybackPercent: 60.5,
    isPdfConfirmed: false,
    pdfReadingSeconds: 15,
    quizPassed: false,
    createdAt: FIXED_PAST_TIME,
    updatedAt: nowPast(),
  };
}

test("Unauthenticated user: denied reading or writing manifests, catalogs, and student progress", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("curriculum_manifests").doc("live").get());
  await assertFails(unauthDb.collection("curriculum_catalogs").doc("live").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("progress").doc("class-1").get());
  await assertFails(unauthDb.collection("curriculum_manifests").doc("live").set(validManifestPayload(ALICE_UID)));
});

test("Email spoofing attack: unverified admin email CANNOT publish live manifest", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "www.mdnafiulislam@gmail.com",
      email_verified: false,
    })
    .firestore();
  await assertFails(spoofDb.collection("curriculum_manifests").doc("live").set(validManifestPayload("spoof_uid")));
});

test("Student user: cannot escalate self to admin or overwrite live manifest/catalog", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true })
    .firestore();

  await assertFails(aliceDb.collection("admins").doc(ALICE_UID).set({ uid: ALICE_UID, email: "alice@test.com", role: "ADMIN" }));
  await assertFails(aliceDb.collection("curriculum_manifests").doc("live").set(validManifestPayload(ALICE_UID)));
  await assertFails(aliceDb.collection("curriculum_catalogs").doc("live").set(validCatalogPayload(ALICE_UID)));
});

test("Verified Admin: can publish and update live manifest & catalog; shadow update rejected", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "www.mdnafiulislam@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(adminDb.collection("curriculum_manifests").doc("live").set(validManifestPayload(ADMIN_UID)));
  await assertSucceeds(adminDb.collection("curriculum_catalogs").doc("live").set(validCatalogPayload(ADMIN_UID)));

  // Valid partial update preserving createdAt
  await assertSucceeds(
    adminDb.collection("curriculum_manifests").doc("live").update({
      versionCode: 103,
      versionTag: "v1.3.0",
      updatedAt: nowPast(),
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    adminDb.collection("curriculum_manifests").doc("live").update({
      versionCode: 104,
      ghostField: "hack",
      updatedAt: nowPast(),
    })
  );
});

test("Signed-in student: can get live manifest/catalog by doc ID, cannot blanket list collection", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "www.mdnafiulislam@gmail.com",
      email_verified: true,
    })
    .firestore();
  await assertSucceeds(adminDb.collection("curriculum_manifests").doc("live").set(validManifestPayload(ADMIN_UID)));
  await assertSucceeds(adminDb.collection("curriculum_catalogs").doc("live").set(validCatalogPayload(ADMIN_UID)));

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true })
    .firestore();
  await assertSucceeds(aliceDb.collection("curriculum_manifests").doc("live").get());
  await assertSucceeds(aliceDb.collection("curriculum_catalogs").doc("live").get());
  await assertFails(aliceDb.collection("curriculum_manifests").get());
});

test("Student progress isolation: owner can write/read/list own progress; cross-user access & immutable tampering fail", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@test.com", email_verified: true })
    .firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("class-1").set(validProgressPayload(ALICE_UID, "class-1"))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("class-1").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("progress").get());

  // Valid update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("class-1").update({
      status: "COMPLETED",
      activeLearningSeconds: 120,
      quizScore: 100,
      quizPassed: true,
      completionTimestamp: 1763300100000,
      updatedAt: nowPast(),
    })
  );

  // Immutable createdAt tamper must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("class-1").update({
      createdAt: nowPast(),
      updatedAt: nowPast(),
    })
  );

  // Cross-user read/list/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("progress").doc("class-1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("progress").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("progress").doc("class-2").set(validProgressPayload(BOB_UID, "class-2"))
  );
});

function validLessonFileChunkPayload(authorUid, fileId = "file-class-1-pdf", chunkIndex = 0, totalChunks = 1) {
  return {
    fileId,
    chunkIndex,
    totalChunks,
    dataBase64: "JVBERi0xLjQKJcTl8uXrp/Og0MTGCg==",
    authorUid,
    createdAt: FIXED_PAST_TIME,
    updatedAt: nowPast(),
  };
}

test("Lesson file chunks: verified Admin creates/updates; signed-in student gets by chunk ID only; unauth/student write/list denied", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "www.mdnafiulislam@gmail.com",
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true })
    .firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const chunkId = "file-class-1-pdf_0";

  // Admin creates valid chunk
  await assertSucceeds(
    adminDb.collection("lesson_files").doc(chunkId).set(validLessonFileChunkPayload(ADMIN_UID))
  );

  // Admin updates chunk payload preserving immutable createdAt & authorUid
  await assertSucceeds(
    adminDb.collection("lesson_files").doc(chunkId).update({
      totalChunks: 1,
      dataBase64: "JVBERi0xLjUKdXBkYXRlZA==",
      updatedAt: nowPast(),
    })
  );

  // Ghost field & invalid chunkIndex must fail
  await assertFails(
    adminDb.collection("lesson_files").doc("bad-chunk_0").set({
      ...validLessonFileChunkPayload(ADMIN_UID),
      chunkIndex: 2,
      totalChunks: 2,
    })
  );
  await assertFails(
    adminDb.collection("lesson_files").doc(chunkId).update({
      ghostField: "bad",
      updatedAt: nowPast(),
    })
  );

  // Signed-in student can get known chunk ID, cannot list, cannot write or delete
  await assertSucceeds(aliceDb.collection("lesson_files").doc(chunkId).get());
  await assertFails(aliceDb.collection("lesson_files").get());
  await assertFails(aliceDb.collection("lesson_files").doc("stu_0").set(validLessonFileChunkPayload(ALICE_UID)));
  await assertFails(aliceDb.collection("lesson_files").doc(chunkId).delete());

  // Unauthenticated access denied
  await assertFails(unauthDb.collection("lesson_files").doc(chunkId).get());
  await assertFails(unauthDb.collection("lesson_files").doc("unauth_0").set(validLessonFileChunkPayload("unauth")));
});

