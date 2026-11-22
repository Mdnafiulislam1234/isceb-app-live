import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import {
  AVAILABLE_USERS,
  DEFAULT_DEVELOPER,
  DEFAULT_FOUNDER,
  DEFAULT_MEMBERS,
  FIREBASE_CONFIG,
  INITIAL_ASSIGNMENTS,
  INITIAL_CLASSES,
  INITIAL_EXAMS,
  INITIAL_MODULES,
  MAX_CLOUD_FILE_BYTES,
  buildFirestoreCatalogPayload,
  buildFirestoreManifestPayload,
  splitBytesIntoLessonFileChunks
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

test("Desktop ISCEB Members directory enforces exact 1..8 order, portraits, and full bios", () => {
  assert.equal(DEFAULT_MEMBERS.length, 8);
  assert.deepEqual(
    DEFAULT_MEMBERS.map((m) => m.displayOrder),
    [1, 2, 3, 4, 5, 6, 7, 8]
  );
  assert.deepEqual(
    DEFAULT_MEMBERS.map((m) => m.fullName),
    [
      "Md. Amirul Islam",
      "Hasina Akter Khan",
      "MD. MASUDUR RAHMAN",
      "Shah Mohammad Asif Mahmud",
      "Mr. Md. Mashiur Rahman",
      "Shajibul Hasan",
      "Tonmoy Khan",
      "Developer Mohammad Nafiul Islam"
    ]
  );
  for (const member of DEFAULT_MEMBERS) {
    assert.ok(member.photoUrl.startsWith("./") && member.photoUrl.endsWith(".webp"));
    const diskPath = `./public/desktop/${member.photoUrl.slice(2)}`;
    assert.ok(fs.existsSync(diskPath), `Missing desktop member photo: ${diskPath}`);
    assert.ok(member.biography.length > 80, `Incomplete biography for ${member.fullName}`);
  }
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

test("Desktop Web Portal enforces single-line badges, untruncated upload banner, and parallel media upload options grid", () => {
  const css = fs.readFileSync("./public/desktop/styles.css", "utf8");
  assert.ok(css.includes("white-space: nowrap;"), "Badges and media upload buttons must disable line wrapping");
  assert.ok(css.includes(".upload-banner-btn"), "Missing .upload-banner-btn CSS");
  assert.ok(css.includes("overflow: visible;"), "Upload banner must never clip text");
  assert.ok(css.includes(".media-upload-grid"), "Missing .media-upload-grid CSS");
  assert.ok(css.includes(".media-upload-btn"), "Missing .media-upload-btn CSS");

  const appJs = fs.readFileSync("./public/desktop/app.js", "utf8");
  assert.ok(appJs.includes("Certificate Eligibility Dashboard"));
  assert.ok(appJs.includes("Upload multiple files, PDF, image, document, audio, video"));
  for (const label of ["Image", "Document", "Audio", "Video"]) {
    assert.ok(appJs.includes(label), `Missing media upload option: ${label}`);
  }
});

test("PC/Web portal enforces official ISCEB full name, Hasina Khan & Amirul Islam certificate signatories, and Demo Preview", () => {
  const indexHtml = fs.readFileSync("./public/desktop/index.html", "utf8");
  const manifestJson = fs.readFileSync("./public/desktop/manifest.webmanifest", "utf8");
  const curriculumJs = fs.readFileSync("./public/desktop/curriculum-data.js", "utf8");
  const appJs = fs.readFileSync("./public/desktop/app.js", "utf8");
  const stylesCss = fs.readFileSync("./public/desktop/styles.css", "utf8");

  for (const [name, content] of [
    ["index.html", indexHtml],
    ["manifest.webmanifest", manifestJson],
    ["curriculum-data.js", curriculumJs]
  ]) {
    assert.ok(content.includes("Institute of Supply Chain Excellence Bangladesh"), `${name} must include official ISCEB full name`);
    assert.ok(!content.includes("International Supply Chain Education Board"), `${name} must not contain old wrong full name`);
  }

  assert.ok(appJs.includes("Institute of Supply Chain Excellence Bangladesh (ISCEB)"));
  assert.ok(appJs.includes('leftSignerName: "Hasina Khan"'));
  assert.ok(appJs.includes('authorizedSignerName: "Amirul Islam"'));
  assert.ok(appJs.includes('authorizedSignerTitle: "Founder of ISCEB"'));
  assert.ok(appJs.includes('data-action="preview-demo-certificate"'));
  assert.ok(appJs.includes('data-action="toggle-certificates-enabled"'));
  assert.ok(appJs.includes('data-action="toggle-demo-cert-enabled"'));
  assert.ok(stylesCss.includes(".cert-signatories-grid"));

  const certStart = appJs.indexOf('<div class="cert-sheet">');
  const certEnd = appJs.indexOf("function getMemberPhotoSrc");
  assert.ok(certStart > 0 && certEnd > certStart, "Missing .cert-sheet template");
  const certSection = appJs.slice(certStart, certEnd);
  assert.ok(certSection.includes("Institute of Supply Chain Excellence Bangladesh (ISCEB)"));
  assert.ok(certSection.includes("leftSignerName"));
  assert.ok(certSection.includes("rightSignerName"));
  assert.ok(certSection.includes("rightSignerTitle"));
  assert.ok(!certSection.includes("developerProfile"), "Certificate template must never render developer Nafiul profile");
  assert.ok(!certSection.includes("Nafiul"), "Certificate template must never contain Nafiul");
});

test("Desktop chunked lesson_files helper complies with firestore.rules limits and strips ephemeral blob URLs", () => {
  assert.equal(MAX_CLOUD_FILE_BYTES, 18 * 1024 * 1024);

  const sampleBytes = new Uint8Array(850_000);
  for (let i = 0; i < sampleBytes.length; i++) sampleBytes[i] = i % 251;

  const chunks = splitBytesIntoLessonFileChunks({
    fileId: "pc_class-1_0_170000",
    bytes: sampleBytes,
    authorUid: "admin-uid-123",
    serverTimestampToken: "TS"
  });
  assert.equal(chunks.length, 3);
  assert.equal(chunks[0].chunkId, "pc_class-1_0_170000_0");
  assert.equal(chunks[0].payload.totalChunks, 3);
  assert.ok(chunks[0].payload.dataBase64.length <= 600_000);
  assert.equal(chunks[0].payload.authorUid, "admin-uid-123");

  const catalog = buildFirestoreCatalogPayload({
    authorUid: "admin-uid-123",
    versionCode: 110,
    modules: INITIAL_MODULES,
    classes: INITIAL_CLASSES.map((c, idx) =>
      idx === 0
        ? {
            ...c,
            pdfMedia: { id: "m1", type: "PDF", fileName: "Guide.pdf", url: "blob:https://example.com/123", cloudFileId: "cf1", cloudChunkCount: 1 },
            attachments: [{ id: "m1", type: "PDF", fileName: "Guide.pdf", url: "blob:https://example.com/123", cloudFileId: "cf1", cloudChunkCount: 1 }]
          }
        : c
    ),
    isCreate: false,
    serverTimestampToken: "TS"
  });
  assert.equal(catalog.classes[0].pdfMedia.url, "");
  assert.equal(catalog.classes[0].attachments[0].url, "");

  const appJs = fs.readFileSync("./public/desktop/app.js", "utf8");
  assert.ok(appJs.includes("presentMediaPreviewModal"));
  assert.ok(appJs.includes("handleAdminMediaFilesSelected"));
  assert.ok(appJs.includes("adminAttachRemoteUrlForm"));
  assert.ok(appJs.includes('data-action="delete-class-media"'));
});

test("GitHub Pages PWA setup configures permanent manifest, auto-updating Service Worker, and CI build stamping", () => {
  assert.ok(fs.existsSync("./public/.nojekyll"), "public/.nojekyll is required for GitHub Pages");

  const manifest = JSON.parse(fs.readFileSync("./public/desktop/manifest.webmanifest", "utf8"));
  assert.equal(manifest.start_url, "./");
  assert.equal(manifest.scope, "./");
  assert.equal(manifest.display, "standalone");
  assert.equal(manifest.orientation, "any");
  assert.equal(manifest.short_name, "ISCEB");
  assert.ok(Array.isArray(manifest.icons) && manifest.icons.length >= 2);

  const swJs = fs.readFileSync("./public/desktop/sw.js", "utf8");
  assert.ok(swJs.includes("__ISCEB_SW_BUILD__"));
  assert.ok(swJs.includes("skipWaiting()"));
  assert.ok(swJs.includes("clients.claim()"));
  assert.ok(swJs.includes('cache: "no-store"'));
  assert.ok(swJs.includes("ISCEB_PWA_UPDATED"));

  const desktopHtml = fs.readFileSync("./public/desktop/index.html", "utf8");
  assert.ok(desktopHtml.includes('updateViaCache: "none"'));
  assert.ok(desktopHtml.includes("reg.update()"));
  assert.ok(desktopHtml.includes("controllerchange"));
  assert.ok(desktopHtml.includes("visibilitychange"));
  assert.ok(desktopHtml.includes("checkPwaUpdateBtn"));

  const workflow = fs.readFileSync("./.github/workflows/release-apk.yml", "utf8");
  assert.ok(workflow.includes("__ISCEB_SW_BUILD__"));
  assert.ok(workflow.includes("pwa-version.json"));
  assert.ok(workflow.includes("actions/deploy-pages@v4"));
});


