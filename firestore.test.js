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
const TEACHER_UID = "teacher_karim_uid";
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
    startTimeMs: 1763290000000,
    lastActiveTimeMs: 1763300000000,
    activeLearningSeconds: 45,
    idleExcludedSeconds: 12,
    maxPlaybackPercent: 60.5,
    lastPlaybackPositionMs: 18500,
    isPdfConfirmed: false,
    pdfReadingSeconds: 15,
    lastPdfPage: 2,
    totalPdfPages: 5,
    visitedPdfPages: [0, 1, 2],
    quizPassed: false,
    createdAt: FIXED_PAST_TIME,
    updatedAt: nowPast(),
  };
}

function validStudentAccountPayload(uid, studentCode, fullName, email) {
  return {
    uid,
    studentId: studentCode,
    fullName,
    email,
    photoUrl: "",
    role: "STUDENT",
    organization: "Dhaka Port Operations",
    designation: "PGD Candidate",
    mobileNumber: "+8801700000000",
    enrolledProgram: "Post Graduate Diploma (PGD) in SCM",
    accountStatus: "ACTIVE",
    isVerified: false,
    assignedModuleIds: [],
    createdAt: FIXED_PAST_TIME,
    updatedAt: nowPast(),
  };
}

function validTeacherAccountPayload(uid, email, assignedModuleIds) {
  return {
    uid,
    studentId: "ISCEB-FAC-2026-01",
    fullName: "Prof. Karim Uddin",
    email,
    photoUrl: "",
    role: "TEACHER",
    organization: "ISCEB Faculty",
    designation: "Senior Lecturer",
    mobileNumber: "+8801711111111",
    enrolledProgram: "Post Graduate Diploma (PGD) in SCM",
    accountStatus: "ACTIVE",
    isVerified: true,
    assignedModuleIds,
    createdAt: FIXED_PAST_TIME,
    updatedAt: nowPast(),
  };
}

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

test("Unauthenticated user: denied reading or writing manifests, catalogs, accounts, and progress", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("curriculum_manifests").doc("live").get());
  await assertFails(unauthDb.collection("curriculum_catalogs").doc("live").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("progress").doc("class-1").get());
  await assertFails(unauthDb.collection("curriculum_manifests").doc("live").set(validManifestPayload(ALICE_UID)));
});

test("Email spoofing attack: unverified admin email CANNOT publish live manifest or grant teacher role", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "www.mdnafiulislam@gmail.com",
      email_verified: false,
    })
    .firestore();
  await assertFails(spoofDb.collection("curriculum_manifests").doc("live").set(validManifestPayload("spoof_uid")));
  await assertFails(
    spoofDb.collection("users").doc(TEACHER_UID).set(validTeacherAccountPayload(TEACHER_UID, "karim@isceb.org", ["mod-1"]))
  );
});

test("Student user: can create & update own safe profile fields; CANNOT self-promote role or alter studentId/verification", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@test.com", email_verified: true })
    .firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validStudentAccountPayload(ALICE_UID, "ISCEB-PGD-2026-1001", "Alice Rahman", "alice@test.com"))
  );

  // Self-updating safe profile fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fullName: "Alice Rahman MBA",
      organization: "Chattogram Freight",
      updatedAt: nowPast(),
    })
  );

  // Self-promoting to TEACHER or ADMIN or changing studentId/isVerified fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "TEACHER",
      assignedModuleIds: ["mod-1"],
      updatedAt: nowPast(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: nowPast(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set(validTeacherAccountPayload(BOB_UID, "bob@test.com", ["mod-1"]))
  );

  // Cross-student profile read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ fullName: "Hacked", updatedAt: nowPast() }));

  // Admins collection escalation & catalog overwrite denied
  await assertFails(aliceDb.collection("admins").doc(ALICE_UID).set({ uid: ALICE_UID, email: "alice@test.com", role: "ADMIN" }));
  await assertFails(aliceDb.collection("curriculum_manifests").doc("live").set(validManifestPayload(ALICE_UID)));
});

test("Verified Admin: publishes live manifest & catalog, creates Teacher with assigned modules, and logs audit trail", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "www.mdnafiulislam@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(adminDb.collection("curriculum_manifests").doc("live").set(validManifestPayload(ADMIN_UID)));
  await assertSucceeds(adminDb.collection("curriculum_catalogs").doc("live").set(validCatalogPayload(ADMIN_UID)));
  await assertSucceeds(
    adminDb.collection("users").doc(TEACHER_UID).set(validTeacherAccountPayload(TEACHER_UID, "karim@isceb.org", ["mod-1", "mod-2"]))
  );
  await assertSucceeds(
    adminDb.collection("audit_logs").doc("log-1").set({
      logId: "log-1",
      actorUid: ADMIN_UID,
      actorName: "Md. Nafiul Islam Nahin",
      actorRole: "ADMIN",
      actionType: "ASSIGN_TEACHER_MODULES",
      targetId: TEACHER_UID,
      summary: "Assigned mod-1 and mod-2 to Prof. Karim Uddin",
      timestampMs: 1763300050000,
      createdAt: nowPast(),
    })
  );

  // Immutable audit log cannot be updated or deleted
  await assertFails(adminDb.collection("audit_logs").doc("log-1").update({ summary: "Altered" }));
  await assertFails(adminDb.collection("audit_logs").doc("log-1").delete());
});

test("Teacher RBAC scoping: Teacher can create/grade in assigned modules (mod-1) ONLY; denied on unassigned module (mod-6) & admin manifests", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "www.mdnafiulislam@gmail.com",
      email_verified: true,
    })
    .firestore();
  const teacherDb = testEnv
    .authenticatedContext(TEACHER_UID, { email: "karim@isceb.org", email_verified: true })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true })
    .firestore();

  await assertSucceeds(
    adminDb.collection("users").doc(TEACHER_UID).set(validTeacherAccountPayload(TEACHER_UID, "karim@isceb.org", ["mod-1"]))
  );

  const validAsg = {
    id: "asg-mod1-custom",
    moduleId: "mod-1",
    title: "Forecast Error Case Study",
    topicDescription: "MAD and MAPE analysis",
    instructions: "Submit PDF report",
    maxMarks: 20,
    isFinalProject: false,
    isRequired: true,
    allowFileAttachment: true,
    createdByUid: TEACHER_UID,
    createdByName: "Prof. Karim Uddin",
    createdAt: FIXED_PAST_TIME,
    updatedAt: nowPast(),
  };

  // Teacher creates assignment in assigned mod-1 -> succeeds
  await assertSucceeds(teacherDb.collection("assignments").doc("asg-mod1-custom").set(validAsg));

  // Teacher tries creating assignment in unassigned mod-6 -> denied!
  await assertFails(
    teacherDb.collection("assignments").doc("asg-mod6-unauth").set({
      ...validAsg,
      id: "asg-mod6-unauth",
      moduleId: "mod-6",
    })
  );

  // Teacher tries publishing global manifest or altering global admin settings -> denied!
  await assertFails(teacherDb.collection("curriculum_manifests").doc("live").set(validManifestPayload(TEACHER_UID)));

  // Student submits assignment in mod-1
  const validSubmission = {
    submissionId: "sub-1",
    assignmentId: "asg-mod1-custom",
    moduleId: "mod-1",
    studentId: ALICE_UID,
    studentName: "Alice Rahman",
    submissionText: "Forecast MAD = 12.4 units.",
    attachedFileName: "mad.pdf",
    submissionTimestamp: 1763300100000,
    marksAwarded: null,
    feedback: null,
    isGraded: false,
    status: "SUBMITTED",
    gradedByUid: null,
    gradedByName: null,
    gradedTimestamp: null,
    createdAt: FIXED_PAST_TIME,
    updatedAt: nowPast(),
  };
  await assertSucceeds(aliceDb.collection("submissions").doc("sub-1").set(validSubmission));

  // Student CANNOT self-grade or alter marks
  await assertFails(
    aliceDb.collection("submissions").doc("sub-1").update({
      marksAwarded: 20,
      isGraded: true,
      status: "GRADED",
      updatedAt: nowPast(),
    })
  );

  // Assigned Teacher grades submission -> succeeds
  await assertSucceeds(
    teacherDb.collection("submissions").doc("sub-1").update({
      marksAwarded: 18,
      feedback: "Solid forecast breakdown",
      isGraded: true,
      status: "GRADED",
      gradedByUid: TEACHER_UID,
      gradedByName: "Prof. Karim Uddin",
      gradedTimestamp: 1763300200000,
      updatedAt: nowPast(),
    })
  );

  // Teacher CANNOT retroactively tamper with student's submission text while grading
  await assertFails(
    teacherDb.collection("submissions").doc("sub-1").update({
      submissionText: "Replaced text",
      updatedAt: nowPast(),
    })
  );

  // Teacher CANNOT grade submissions for unassigned modules (mod-6)
  await assertSucceeds(
    aliceDb.collection("submissions").doc("sub-mod6").set({
      ...validSubmission,
      submissionId: "sub-mod6",
      assignmentId: "asg-6",
      moduleId: "mod-6",
    })
  );
  await assertFails(
    teacherDb.collection("submissions").doc("sub-mod6").update({
      marksAwarded: 15,
      isGraded: true,
      status: "GRADED",
      updatedAt: nowPast(),
    })
  );
});

test("Certificates registry: owner issues valid cert; cannot issue for another student or revoke/tamper; only Admin revokes/reinstates", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "www.mdnafiulislam@gmail.com",
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@test.com", email_verified: true })
    .firestore();

  const certId = "ISCEB-SCM-202611-A1B2C3";
  const validCert = {
    certificateId: certId,
    studentId: ALICE_UID,
    studentCode: "ISCEB-PGD-2026-1001",
    studentName: "Alice Rahman",
    courseTitle: "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
    issueDate: "November 19, 2026",
    issueTimestamp: 1763300300000,
    institutionName: "ISCEB",
    officialWebsite: "www.isceb.org",
    authorizedSigner: "Md. Amirul Islam, Founder of ISCEB",
    status: "VALID",
    revocationReason: null,
    revocationTimestamp: null,
    completedClassesCount: 48,
    totalRequiredClasses: 48,
    verificationUrl: `https://www.isceb.org/verify/${certId}`,
    createdAt: FIXED_PAST_TIME,
    updatedAt: nowPast(),
  };

  // Alice issues her earned certificate
  await assertSucceeds(aliceDb.collection("certificates").doc(certId).set(validCert));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("certificates").doc(certId).set(validCert));

  // Bob CANNOT issue certificate under Alice's UID or modify Alice's certificate
  await assertFails(bobDb.collection("certificates").doc("ISCEB-SCM-FAKE").set({ ...validCert, certificateId: "ISCEB-SCM-FAKE" }));
  await assertFails(aliceDb.collection("certificates").doc(certId).update({ studentName: "Renamed", updatedAt: nowPast() }));

  // Any signed-in verifier can look up by ID
  await assertSucceeds(bobDb.collection("certificates").doc(certId).get());

  // Only Admin can revoke/reinstate certificate
  await assertSucceeds(
    adminDb.collection("certificates").doc(certId).update({
      status: "REVOKED",
      revocationReason: "Audit discrepancy",
      revocationTimestamp: 1763300400000,
      updatedAt: nowPast(),
    })
  );
  await assertFails(aliceDb.collection("certificates").doc(certId).update({ status: "VALID", updatedAt: nowPast() }));
});

test("Student progress isolation: owner writes PDF/playback progress; cross-user access & immutable tampering fail", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, { email: "bob@test.com", email_verified: true })
    .firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("class-1").set(validProgressPayload(ALICE_UID, "class-1"))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("progress").doc("class-1").update({
      status: "COMPLETED",
      activeLearningSeconds: 120,
      pdfReadingSeconds: 60,
      lastPdfPage: 4,
      visitedPdfPages: [0, 1, 2, 3, 4],
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

test("Lesson file chunks: Admin & active Teacher upload chunks; student read by ID only; unauth/student write/list denied", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "www.mdnafiulislam@gmail.com",
      email_verified: true,
    })
    .firestore();
  const teacherDb = testEnv
    .authenticatedContext(TEACHER_UID, { email: "karim@isceb.org", email_verified: true })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@test.com", email_verified: true })
    .firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertSucceeds(
    adminDb.collection("users").doc(TEACHER_UID).set(validTeacherAccountPayload(TEACHER_UID, "karim@isceb.org", ["mod-1"]))
  );

  const chunkId = "file-class-1-pdf_0";
  await assertSucceeds(adminDb.collection("lesson_files").doc(chunkId).set(validLessonFileChunkPayload(ADMIN_UID)));
  await assertSucceeds(teacherDb.collection("lesson_files").doc("tchunk_0").set(validLessonFileChunkPayload(TEACHER_UID, "tfile-1")));

  await assertSucceeds(aliceDb.collection("lesson_files").doc(chunkId).get());
  await assertFails(aliceDb.collection("lesson_files").get());
  await assertFails(aliceDb.collection("lesson_files").doc("stu_0").set(validLessonFileChunkPayload(ALICE_UID)));
  await assertFails(aliceDb.collection("lesson_files").doc(chunkId).delete());
  await assertFails(unauthDb.collection("lesson_files").doc(chunkId).get());
});
