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
  buildFirestoreUserAccountPayload,
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
  assert.deepEqual(
    students.map((u) => u.fullName),
    ["Ayman Rahman", "Arham Hossain", "Zayan Ahmed"]
  );
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
      "Hasina Akter Khan",
      "Md. Amirul Islam",
      "MD. MASUDUR RAHMAN",
      "Shah Mohammad Asif Mahmud",
      "Mr. Md. Mashiur Rahman",
      "Shajibul Hasan",
      "Tonmoy Khan",
      "Md. Nafiul Islam Nahin"
    ]
  );
  assert.equal(DEFAULT_DEVELOPER.headline, "Developer | Supply Chain Management Enthusiast | Aspiring Corporate Professional");
  assert.equal(DEFAULT_DEVELOPER.personalPhilosophy, "Learn Continuously, Think Strategically, and Grow Professionally.");
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
  const url = "https://mdnafiulislam1234.github.io/isceb-app-live/";
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

test("PC/Web portal enforces official ISCEB full name, Hasina Akter Khan & Md. Amirul Islam certificate signatories, and Demo Preview", () => {
  const indexHtml = fs.readFileSync("./public/desktop/index.html", "utf8");
  const manifestJson = fs.readFileSync("./public/desktop/manifest.webmanifest", "utf8");
  const curriculumJs = fs.readFileSync("./public/desktop/curriculum-data.js", "utf8");
  const appJs = fs.readFileSync("./public/desktop/app.js", "utf8");
  const stylesCss = fs.readFileSync("./public/desktop/styles.css", "utf8");

  assert.ok(indexHtml.includes("user-scalable=no") && indexHtml.includes("maximum-scale=1.0"), "Mobile PWA viewport must lock pinch-zoom");
  assert.ok(stylesCss.includes("overflow-x: hidden") && stylesCss.includes("touch-action: pan-y"), "Mobile PWA must lock horizontal drag and overflow");

  for (const [name, content] of [
    ["index.html", indexHtml],
    ["manifest.webmanifest", manifestJson],
    ["curriculum-data.js", curriculumJs]
  ]) {
    assert.ok(content.includes("Institute of Supply Chain Excellence Bangladesh"), `${name} must include official ISCEB full name`);
    assert.ok(!content.includes("International Supply Chain Education Board"), `${name} must not contain old wrong full name`);
  }

  assert.ok(appJs.includes("Institute of Supply Chain Excellence Bangladesh (ISCEB)"));
  assert.ok(appJs.includes('leftSignerName: "Hasina Akter Khan"'));
  assert.ok(appJs.includes('leftSignerTitle: "Chairman & CEO"'));
  assert.ok(appJs.includes('authorizedSignerName: "Md. Amirul Islam"'));
  assert.ok(appJs.includes('authorizedSignerTitle: "Founder of ISCEB"'));
  assert.ok(appJs.includes('data-action="preview-demo-certificate"'));
  assert.ok(appJs.includes('data-action="toggle-certificates-enabled"'));
  assert.ok(appJs.includes('data-action="toggle-demo-cert-enabled"'));
  assert.ok(stylesCss.includes(".cert-signatories-grid"));
  assert.ok(stylesCss.includes("#FDF7F3") && stylesCss.includes("#F3DDD1"), "Certificate must use light warm reddish-cream parchment styling");
  assert.ok(!appJs.includes('#${Number(m.displayOrder'), "Member directory cards must not display #1/#2/#3 number badges");
  assert.ok(!appJs.includes('#${Number(member.displayOrder'), "Member detail modal must not display #1/#2/#3 number badges");

  const certStart = appJs.indexOf('<div class="cert-sheet">');
  const certEnd = appJs.indexOf("function getMemberPhotoSrc");
  assert.ok(certStart > 0 && certEnd > certStart, "Missing .cert-sheet template");
  const certSection = appJs.slice(certStart, certEnd);
  assert.ok(certSection.includes("Institute of Supply Chain Excellence Bangladesh (ISCEB)"));
  assert.ok(certSection.includes("leftSignerName"));
  assert.ok(certSection.includes("leftSignerTitle"));
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
  assert.ok(Array.isArray(manifest.icons) && manifest.icons.length >= 3);
  assert.ok(manifest.icons.some((ic) => ic.purpose === "any"), "Manifest must include dedicated purpose=any icon");
  assert.ok(manifest.icons.some((ic) => ic.purpose === "maskable"), "Manifest must include dedicated purpose=maskable icon");
  assert.ok(!manifest.icons.some((ic) => ic.purpose === "any maskable"), "Manifest must not combine 'any maskable' in one icon");
  for (const ic of manifest.icons) {
    assert.ok(ic.src.startsWith("./") && fs.existsSync(`./public/desktop/${ic.src.slice(2)}`), `Missing manifest icon on disk: ${ic.src}`);
  }

  const swJs = fs.readFileSync("./public/desktop/sw.js", "utf8");
  assert.ok(swJs.includes("__ISCEB_SW_BUILD__"));
  assert.ok(swJs.includes("skipWaiting()"));
  assert.ok(swJs.includes("clients.claim()"));
  assert.ok(swJs.includes('cache: "no-store"'));
  assert.ok(swJs.includes("ISCEB_PWA_UPDATED"));
  const shellMatch = swJs.match(/SHELL_ASSETS\s*=\s*\[([\s\S]*?)\]/);
  assert.ok(shellMatch, "SHELL_ASSETS array must exist in sw.js");
  for (const m of shellMatch[1].matchAll(/"\.\/([^"]+)"/g)) {
    const assetPath = `./public/desktop/${m[1]}`;
    assert.ok(fs.existsSync(assetPath), `Missing Service Worker SHELL_ASSETS file: ${assetPath}`);
  }

  const desktopHtml = fs.readFileSync("./public/desktop/index.html", "utf8");
  assert.ok(desktopHtml.includes('updateViaCache: "none"'));
  assert.ok(desktopHtml.includes("reg.update()"));
  assert.ok(desktopHtml.includes("controllerchange"));
  assert.ok(desktopHtml.includes("visibilitychange"));
  assert.ok(desktopHtml.includes("checkPwaUpdateBtn"));

  const appJs = fs.readFileSync("./public/desktop/app.js", "utf8");
  assert.ok(appJs.includes("isRunningStandalone"));
  assert.ok(appJs.includes("renderPwaInstallBanner"));
  assert.ok(appJs.includes('data-action="trigger-pwa-install"'));

  const workflow = fs.readFileSync("./.github/workflows/release-apk.yml", "utf8");
  assert.ok(workflow.includes("__ISCEB_SW_BUILD__"));
  assert.ok(workflow.includes("pwa-version.json"));
  assert.ok(workflow.includes("actions/deploy-pages@v4"));
});

test("Root index launches unified PWA workspace and apk.html preserves permanent Android APK portal", () => {
  assert.ok(fs.existsSync("./public/index.html"), "Missing public/index.html");
  assert.ok(fs.existsSync("./public/apk.html"), "Missing public/apk.html");

  const rootHtml = fs.readFileSync("./public/index.html", "utf8");
  assert.ok(rootHtml.includes("./desktop/"), "Root index must launch ./desktop/ unified PWA");
  assert.ok(rootHtml.includes("./apk.html"), "Root index must include ./apk.html fallback");

  const apkHtml = fs.readFileSync("./public/apk.html", "utf8");
  assert.ok(apkHtml.includes("ISCEB-latest.apk"), "apk.html must preserve direct APK download");
  assert.ok(apkHtml.includes("version.json"), "apk.html must read version.json metadata");

  const desktopHtml = fs.readFileSync("./public/desktop/index.html", "utf8");
  assert.ok(desktopHtml.includes("../apk.html"), "Desktop/PWA portal must link Android APK Portal to ../apk.html");

  const versionJson = JSON.parse(fs.readFileSync("./public/version.json", "utf8"));
  assert.ok(versionJson.desktopPortalUrl.startsWith("https://mdnafiulislam1234.github.io/isceb-app-live/desktop/"));
  assert.ok(versionJson.apkUrl.startsWith("https://mdnafiulislam1234.github.io/isceb-app-live/"));
  assert.equal(versionJson.downloadPageUrl, "https://mdnafiulislam1234.github.io/isceb-app-live/apk.html");
  assert.ok(FIREBASE_CONFIG.versionJsonUrl.startsWith("https://mdnafiulislam1234.github.io/isceb-app-live/"));
});

test("Mobile Web PWA mirrors Native Android 5-tab bottom bar, topbar, Student Profile & Registration, and non-404 apk.html routing", () => {
  const desktopHtml = fs.readFileSync("./public/desktop/index.html", "utf8");
  const desktopCss = fs.readFileSync("./public/desktop/styles.css", "utf8");
  const appJs = fs.readFileSync("./public/desktop/app.js", "utf8");
  const modelsKt = fs.readFileSync("./app/src/main/java/com/example/model/SupplyChainModels.kt", "utf8");
  const releaseSh = fs.readFileSync("./scripts/release_apk.sh", "utf8");

  assert.ok(desktopHtml.includes("/\\/desktop$/i.test(window.location.pathname)"), "Must redirect /desktop to /desktop/");
  assert.ok(desktopHtml.includes("mobile-topbar"));
  assert.ok(desktopHtml.includes("mobileMenuToggleBtn"));
  assert.ok(desktopHtml.includes("mobileAdminLoginPill"));
  assert.ok(desktopHtml.includes("mobile-bottom-nav"));
  for (const label of ["Home", "Modules", "Learning", "Members", "Profile"]) {
    assert.ok(desktopHtml.includes(`<span>${label}</span>`), `Missing mobile bottom tab: ${label}`);
  }

  assert.ok(desktopCss.includes(".mobile-bottom-nav"));
  assert.ok(desktopCss.includes(".mobile-topbar"));
  assert.ok(desktopCss.includes(".profile-portal-stack"));

  assert.ok(appJs.includes("function renderProfile()"));
  assert.ok(appJs.includes("Create / Sign In Student Account"));
  assert.ok(appJs.includes("Google Cloud Auto-Sync"));
  assert.ok(appJs.includes("Administrator Management Mode"));
  assert.ok(appJs.includes("App Updates &amp; Online APK") || appJs.includes("App Updates & Online APK"));
  assert.ok(appJs.includes('resolveHostingUrl("apk.html")'));

  assert.ok(appJs.includes("courseDetailOpen"), "Tapping a class must open dedicated lecture page");
  assert.ok(appJs.includes('data-action="back-to-modules"'), "Dedicated lecture page must include Back to Modules button");
  assert.ok(appJs.includes("lecture-hero-header") && desktopCss.includes(".lecture-hero-header"), "Lecture detail title must render full-width above action buttons");
  assert.ok(appJs.includes("renderPdfInlineIntoHost"), "Web/Mobile PWA must render PDFs inline on canvas");
  assert.ok(appJs.includes("saveMediaBlobToIdb") && appJs.includes("loadMediaBlobFromIdb"), "Uploaded files must persist in IndexedDB");
  assert.ok(appJs.includes('class="two-col-grid no-print"'), "Certificate print must exclude Eligibility and Verify cards");
  assert.ok(appJs.includes('class="cert-logo-wrap"'), "Certificate logo must use centered .cert-logo-wrap");
  assert.ok(appJs.includes("buildSinglePageLandscapeVectorPdf") && appJs.includes("exportLandscapeCertificatePdf"), "Must include pure Vector Landscape A4 PDF generator");
  assert.ok(appJs.includes("buildSinglePageLandscapePdfFromJpeg"), "Must retain fallback JPEG PDF builder");
  assert.ok(appJs.includes("/ShadingType 3") && appJs.includes("/Times-Bold"), "Certificate PDF must use true vector shading and fonts");
  assert.ok(appJs.includes("841.89") && appJs.includes("595.28"), "Generated certificate PDF must use Landscape A4 MediaBox");
  assert.ok(appJs.includes('data-action="download-certificate-pdf"'), "Must wire download-certificate-pdf action");
  assert.ok(appJs.includes('data-action="zoom-certificate-pdf"'), "Must wire zoom-certificate-pdf action");
  assert.ok(appJs.includes("mediaZoomInBtn") && desktopCss.includes("pinch-zoom"), "PDF & media modal must support up to 500% crisp zoom and pinch-zoom");
  assert.ok(desktopCss.includes(".cert-logo-wrap"), "Missing .cert-logo-wrap CSS");
  assert.ok(desktopCss.includes("mix-blend-mode: multiply;"), "Certificate logo must blend without white box");
  assert.ok(appJs.includes('globalCompositeOperation = "multiply"'), "PDF certificate logo must blend without white box");
  assert.ok(desktopCss.includes(".pdf-canvas-pages"), "Missing .pdf-canvas-pages CSS");

  const manifestJson = JSON.parse(fs.readFileSync("./public/desktop/manifest.webmanifest", "utf8"));
  assert.equal(manifestJson.background_color, "#FFFFFF", "PWA splash screen background_color must be white #FFFFFF");
  assert.equal(manifestJson.theme_color, "#FFFFFF", "PWA manifest theme_color must be white #FFFFFF during boot");
  assert.ok(desktopHtml.includes('id="appSplashOverlay"'), "Missing white animated #appSplashOverlay in desktop/index.html");
  assert.ok(desktopHtml.includes("Masterclass &amp; Executive Certification Portal"), "Splash overlay must display executive portal subtitle");
  assert.ok(desktopCss.includes(".splash-progress-fill") && desktopCss.includes("iscebSplashBarSweep"), "Splash overlay must include gold expanding loading bar");

  const rootIndexHtml = fs.readFileSync("./public/index.html", "utf8");
  assert.ok(!rootIndexHtml.includes("102B4C") && rootIndexHtml.includes("background: #FFFFFF"), "Root public/index.html must open with pure white theme");

  assert.ok(modelsKt.includes('"$HOSTING_BASE/apk.html"'), "Android DEFAULT_DOWNLOAD_PAGE_URL must point to apk.html");
  assert.ok(releaseSh.includes('f"{hosting_base}/apk.html"'), "release_apk.sh version.json generator must point downloadPageUrl to apk.html");
});

test("Desktop & Mobile Admin Assessment Studio builds custom MCQ exams, timed deadlines, Time Up lock, and Firestore sync", () => {
  const catalogWithAssessments = buildFirestoreCatalogPayload({
    authorUid: "admin-uid-123",
    versionCode: 120,
    modules: INITIAL_MODULES,
    classes: INITIAL_CLASSES,
    exams: [
      {
        ...INITIAL_EXAMS[0],
        openUntilMs: 1_900_000_000_000
      }
    ],
    assignments: [
      {
        ...INITIAL_ASSIGNMENTS[0],
        dueDateMs: 1_900_000_000_000
      }
    ],
    isCreate: false,
    serverTimestampToken: "TS"
  });
  assert.ok(typeof catalogWithAssessments.settings.examsJson === "string" && catalogWithAssessments.settings.examsJson.includes("1900000000000"));
  assert.ok(typeof catalogWithAssessments.settings.assignmentsJson === "string" && catalogWithAssessments.settings.assignmentsJson.includes("1900000000000"));

  const appJs = fs.readFileSync("./public/desktop/app.js", "utf8");
  assert.ok(appJs.includes("openAdminExamBuilderModal"), "Missing openAdminExamBuilderModal in app.js");
  assert.ok(appJs.includes("openAdminAssignmentBuilderModal"), "Missing openAdminAssignmentBuilderModal in app.js");
  assert.ok(appJs.includes("isAssignmentExpired") && appJs.includes("isExamExpired"), "Missing deadline expiration helpers in app.js");
  assert.ok(appJs.includes("isAutoSubmittedOnTimeout: Boolean(isAutoTimeout)"), "Timed exam auto-submit must record isAutoSubmittedOnTimeout");
  assert.ok(appJs.includes('data-action="admin-create-exam"') && appJs.includes('data-action="admin-create-assignment"'));
  assert.ok(appJs.includes("adminExamBuilderForm") && appJs.includes("adminAssignmentBuilderForm"));
  assert.ok(appJs.includes("c.settings.examsJson") && appJs.includes("c.settings.assignmentsJson"), "Cloud sync listener must deserialize examsJson & assignmentsJson");
});

test("Profile hero layout avoids vertical character breaking and Admin Roster tracks cloud & local registered students", () => {
  const createUserDoc = buildFirestoreUserAccountPayload({
    uid: "stu-uid-9837",
    studentId: "ISCEB-2026-9837",
    fullName: "Khondokar Jerin Anjum",
    email: "khondokarjerinanjum66@gmail.com",
    mobileNumber: "01886442994",
    photoUrl: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJ",
    isCreate: true,
    serverTimestampToken: "TS"
  });
  assert.equal(createUserDoc.uid, "stu-uid-9837");
  assert.equal(createUserDoc.role, "STUDENT");
  assert.equal(createUserDoc.accountStatus, "ACTIVE");
  assert.equal(createUserDoc.isVerified, false);
  assert.deepEqual(createUserDoc.assignedModuleIds, []);
  assert.equal(
    createUserDoc.photoUrl,
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJ",
    "Compact data:image/ avatars must be preserved for Firestore cloud sync"
  );
  assert.equal(createUserDoc.createdAt, "TS");
  assert.equal(createUserDoc.updatedAt, "TS");

  const oversizedPhotoDoc = buildFirestoreUserAccountPayload({
    uid: "stu-uid-9837",
    studentId: "ISCEB-2026-9837",
    fullName: "Khondokar Jerin Anjum",
    email: "khondokarjerinanjum66@gmail.com",
    mobileNumber: "01886442994",
    photoUrl: `data:image/jpeg;base64,${"A".repeat(96000)}`,
    isCreate: true,
    serverTimestampToken: "TS"
  });
  assert.ok(!("photoUrl" in oversizedPhotoDoc), "Oversized >95KB data URIs must be excluded to respect firestore.rules limit");

  const updateUserDoc = buildFirestoreUserAccountPayload({
    uid: "stu-uid-9837",
    studentId: "ISCEB-2026-9837",
    fullName: "Khondokar Jerin Anjum",
    email: "khondokarjerinanjum66@gmail.com",
    mobileNumber: "01886442994",
    photoUrl: "https://lh3.googleusercontent.com/a/avatar.jpg",
    isCreate: false,
    serverTimestampToken: "TS"
  });
  assert.deepEqual(Object.keys(updateUserDoc).sort(), [
    "designation", "enrolledProgram", "fullName", "mobileNumber", "organization", "photoUrl", "updatedAt"
  ]);
  assert.equal(updateUserDoc.photoUrl, "https://lh3.googleusercontent.com/a/avatar.jpg");

  const css = fs.readFileSync("./public/desktop/styles.css", "utf8");
  assert.ok(css.includes(".profile-hero-header") && css.includes(".profile-hero-name"), "Missing responsive profile hero CSS");
  assert.ok(css.includes("word-break: normal;"), "Profile name must keep whole words intact instead of vertical letter splits");

  const appJs = fs.readFileSync("./public/desktop/app.js", "utf8");
  assert.ok(appJs.includes('class="profile-hero-header"') && appJs.includes('class="profile-hero-name"'), "Profile card must use responsive hero header classes");
  assert.ok(appJs.includes("renderAdminRegisteredStudentsCard") && appJs.includes("fetchCloudRegisteredUsers"), "Admin view must include Registered Students & Cloud Directory");
  assert.ok(appJs.includes("syncStudentAccountToFirestore"), "Google Cloud Sync must persist student profiles into Firestore users collection");
  assert.ok(appJs.includes('id="signedInStudentPhotoInput"') && appJs.includes('id="editStuPhotoInput"'), "Signed-in students must be able to add or change photo after registration");
  assert.ok(appJs.includes('data-modal-action="clear-edit-student-photo"'), "Edit Student Profile modal must allow removing profile photo");
  assert.ok(appJs.includes('stu.photoUrl') && appJs.includes('<img src="${escapeHtml(stu.photoUrl)}"'), "Admin Registered Students card must render student photoUrl avatar when available");
});
