import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import {
  AVAILABLE_USERS,
  DEFAULT_DEVELOPER,
  DEFAULT_FOUNDER,
  FIREBASE_CONFIG,
  INITIAL_ASSIGNMENTS,
  INITIAL_CLASSES,
  INITIAL_EXAMS,
  INITIAL_MODULES,
  buildFirestoreCatalogPayload,
  buildFirestoreManifestPayload
} from "../public/desktop/curriculum-data.js";
import {
  ADMIN_AMIRUL_ID,
  ADMIN_NAFIUL_ID,
  createDesktopAdminAuthStore,
  getOrderedAdmins,
  getStudentSwitcherUsers,
  validateDesktopPasswordStrength
} from "../public/desktop/desktop-auth.js";
import { generateQrMatrix, renderQrSvg } from "../public/qr.js";

test("Desktop curriculum preserves exact 12 modules and 48 lectures split Part 1 (28) and Part 2 (20)", () => {
  assert.equal(INITIAL_MODULES.length, 12);
  assert.equal(INITIAL_CLASSES.length, 48);

  const part1Modules = new Set(INITIAL_MODULES.filter((m) => m.part === "PART_1_BASIC").map((m) => m.id));
  const part2Modules = new Set(INITIAL_MODULES.filter((m) => m.part === "PART_2_ADVANCE").map((m) => m.id));
  assert.equal(part1Modules.size, 6);
  assert.equal(part2Modules.size, 6);

  const part1Classes = INITIAL_CLASSES.filter((c) => part1Modules.has(c.moduleId));
  const part2Classes = INITIAL_CLASSES.filter((c) => part2Modules.has(c.moduleId));
  assert.equal(part1Classes.length, 28);
  assert.equal(part2Classes.length, 20);
});

test("Desktop role integrity separates Founder Md. Amirul Islam and Developer Md. Nafiul Islam Nahin in exact order", () => {
  assert.equal(DEFAULT_FOUNDER.name, "Md. Amirul Islam");
  assert.equal(DEFAULT_FOUNDER.designation, "Founder of ISCEB");
  assert.equal(DEFAULT_DEVELOPER.name, "Md. Nafiul Islam Nahin");
  assert.equal(DEFAULT_DEVELOPER.designation, "Developer of ISCEB App");
  assert.ok(!DEFAULT_DEVELOPER.designation.includes("Founder"));

  const admins = getOrderedAdmins(AVAILABLE_USERS);
  assert.equal(admins.length, 2);
  assert.equal(admins[0].id, ADMIN_AMIRUL_ID);
  assert.equal(admins[0].fullName, "Md. Amirul Islam");
  assert.equal(admins[1].id, ADMIN_NAFIUL_ID);
  assert.equal(admins[1].fullName, "Md. Nafiul Islam Nahin");

  const students = getStudentSwitcherUsers(AVAILABLE_USERS);
  assert.equal(students.length, 3);
  assert.ok(students.every((u) => u.role === "STUDENT"));
});

test("Desktop assessments preserve exact marks allocation", () => {
  const byId = Object.fromEntries(INITIAL_EXAMS.map((e) => [e.id, e]));
  assert.equal(byId["exam-mod-1"].totalMarks, 20);
  assert.equal(byId["exam-mod-2"].totalMarks, 25);
  assert.equal(byId["exam-mod-3"].totalMarks, 30);
  assert.equal(byId["exam-mod-6"].totalMarks, 25);
  assert.equal(byId["exam-final"].totalMarks, 100);

  const finalAsg = INITIAL_ASSIGNMENTS.find((a) => a.id === "asg-12-final-project");
  assert.ok(finalAsg);
  assert.equal(finalAsg.maxMarks, 50);
  assert.equal(finalAsg.isFinalProject, true);
});

test("Firestore payload builders strictly comply with firestore.rules create/update schemas", () => {
  const tsToken = { _type: "serverTimestamp" };
  const createManifest = buildFirestoreManifestPayload({
    authorUid: "admin-uid-123",
    versionCode: 106,
    versionTag: "v1.5.1",
    publishedBy: "Md. Nafiul Islam Nahin (Developer of ISCEB App)",
    releaseNotes: "Desktop & Android sync release",
    announcement: "Live Class",
    latestApkVersionCode: 6,
    latestApkVersionName: "1.5",
    apkDownloadUrl: FIREBASE_CONFIG.apkDownloadUrl,
    isCreate: true,
    serverTimestampToken: tsToken
  });

  const requiredCreateKeys = [
    "authorUid", "versionCode", "versionTag", "publishedTimestamp",
    "publishedBy", "releaseNotes", "minSupportedAppVersion",
    "latestApkVersionCode", "latestApkVersionName", "apkDownloadUrl",
    "createdAt", "updatedAt"
  ];
  for (const k of requiredCreateKeys) {
    assert.ok(k in createManifest, `Missing required manifest key: ${k}`);
  }

  const updateCatalog = buildFirestoreCatalogPayload({
    authorUid: "admin-uid-123",
    versionCode: 106,
    modules: INITIAL_MODULES,
    classes: INITIAL_CLASSES.map((c, i) =>
      i === 0 ? { ...c, pdfMedia: { id: "m1", type: "PDF", fileName: "Guide.pdf", cloudFileId: "cf1", cloudChunkCount: 1 } } : c
    ),
    isCreate: false,
    serverTimestampToken: tsToken
  });
  assert.ok(!("authorUid" in updateCatalog), "Update payload must not mutate authorUid");
  assert.ok(!("createdAt" in updateCatalog), "Update payload must not mutate createdAt");
  assert.equal(updateCatalog.modules.length, 12);
  assert.equal(updateCatalog.classes.length, 48);
  assert.equal(updateCatalog.classes[0].pdfMedia.cloudFileId, "cf1");
});

test("Self-contained QR generator renders valid ISO finder patterns and SVG for permanent download URLs", () => {
  const url = "https://gen-lang-client-0901479759.web.app/";
  const matrix = generateQrMatrix(url);
  assert.ok(matrix.length >= 25 && matrix.length % 4 === 1);
  assert.equal(matrix[0][0], true);
  assert.equal(matrix[0][6], true);
  assert.equal(matrix[6][0], true);
  assert.equal(matrix[3][3], true);
  assert.equal(matrix[1][1], false);
  const svg = renderQrSvg(url, 168);
  assert.ok(svg.startsWith("<svg "));
  assert.ok(svg.includes("rect"));
});

test("Desktop Web Portal uses circular top-aligned framing CSS and zero quick-switch admin bypass", () => {
  const css = fs.readFileSync("./public/desktop/styles.css", "utf8");
  assert.ok(css.includes(".portrait-avatar"));
  assert.ok(css.includes("object-position: center 20%"));

  const appJs = fs.readFileSync("./public/desktop/app.js", "utf8");
  assert.ok(!appJs.includes("quick-switch-admin"));
  assert.ok(!appJs.includes("scm2026"));
  assert.ok(appJs.includes("getStudentSwitcherUsers"));
});

test("Desktop Web Portal PBKDF2 admin auth enforces strength, single-use invite, lockout, and HMAC session", async () => {
  const mem = new Map();
  let now = 1_760_000_000_000;
  const storage = {
    getItem: (k) => (mem.has(k) ? mem.get(k) : null),
    setItem: (k, v) => mem.set(k, v)
  };
  const auth = createDesktopAdminAuthStore({ storage, nowFn: () => now });

  assert.ok(validateDesktopPasswordStrength("scm2026", ADMIN_AMIRUL_ID) !== null);
  assert.ok(validateDesktopPasswordStrength("01715-282905", ADMIN_AMIRUL_ID) !== null);
  assert.ok(validateDesktopPasswordStrength("Amirul@2026", ADMIN_AMIRUL_ID) !== null);
  assert.equal(validateDesktopPasswordStrength("BrbExecutive#91", ADMIN_AMIRUL_ID), null);

  // First admin sets up password cleanly
  const setup1 = await auth.setupInitialPassword(ADMIN_AMIRUL_ID, "BrbExecutive#91", "BrbExecutive#91");
  assert.equal(setup1.ok, true);
  assert.ok(setup1.recoveryCode);
  assert.equal(await auth.getValidatedSessionAdminId(), ADMIN_AMIRUL_ID);

  // Second admin cannot set up without an invite issued by verified admin
  const badSetup2 = await auth.setupInitialPassword(ADMIN_NAFIUL_ID, "DevGuard!842", "DevGuard!842", "");
  assert.equal(badSetup2.ok, false);

  const invite = await auth.generateSecondAdminInvite(ADMIN_NAFIUL_ID);
  assert.equal(invite.ok, true);

  const setup2 = await auth.setupInitialPassword(ADMIN_NAFIUL_ID, "DevGuard!842", "DevGuard!842", invite.setupToken);
  assert.equal(setup2.ok, true);
  assert.equal(await auth.getValidatedSessionAdminId(), ADMIN_NAFIUL_ID);

  // Invite cannot be reused
  assert.equal(auth.hasActiveInviteFor(ADMIN_NAFIUL_ID), false);

  // Lockout after 5 failed attempts
  auth.clearSession();
  assert.equal(await auth.getValidatedSessionAdminId(), null);
  for (let i = 0; i < 4; i++) {
    const r = await auth.authenticate(ADMIN_AMIRUL_ID, "WrongPass#1");
    assert.equal(r.ok, false);
  }
  const locked = await auth.authenticate(ADMIN_AMIRUL_ID, "WrongPass#1");
  assert.equal(locked.ok, false);
  assert.ok(locked.error.includes("locked"));

  // Correct password blocked during lockout window
  const blockedDuringLockout = await auth.authenticate(ADMIN_AMIRUL_ID, "BrbExecutive#91");
  assert.equal(blockedDuringLockout.ok, false);

  // Advance clock past 60s lockout and sign in
  now += 61_000;
  const okLogin = await auth.authenticate(ADMIN_AMIRUL_ID, "BrbExecutive#91");
  assert.equal(okLogin.ok, true);
  assert.equal(await auth.getValidatedSessionAdminId(), ADMIN_AMIRUL_ID);
});

