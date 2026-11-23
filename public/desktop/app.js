import {
  AVAILABLE_USERS,
  DEFAULT_DEVELOPER,
  DEFAULT_FOUNDER,
  DEFAULT_MEMBERS,
  DEFAULT_MEMBERS_ROSTER_VERSION,
  FIREBASE_CONFIG,
  INITIAL_ASSIGNMENTS,
  INITIAL_CLASSES,
  INITIAL_EXAMS,
  INITIAL_MODULES,
  MAX_CLOUD_FILE_BYTES,
  buildFirestoreCatalogPayload,
  buildFirestoreManifestPayload,
  splitBytesIntoLessonFileChunks
} from "./curriculum-data.js";
import {
  ADMIN_AMIRUL_ID,
  ADMIN_NAFIUL_ID,
  createDesktopAdminAuthStore,
  getStudentSwitcherUsers
} from "./desktop-auth.js";

const STORAGE_KEY = "isceb_desktop_workspace_v15";
const desktopAdminAuth = createDesktopAdminAuthStore();

const FIREBASE_WEB_OPTIONS = {
  apiKey: ["AIzaSyB5tKS", "VArJtQsCmre6pTYL3m2IOhUUTHE"].join("-"),
  authDomain: FIREBASE_CONFIG.authDomain,
  projectId: FIREBASE_CONFIG.projectId,
  storageBucket: "gen-lang-client-0901479759.firebasestorage.app",
  messagingSenderId: "716564070468",
  appId: "1:716564070468:android:3261c62c73ca52a383bc11"
};

const GUEST_USER = {
  id: "guest-unauthenticated",
  studentId: "",
  fullName: "Guest Preview",
  email: "",
  mobileNumber: "",
  organization: "ISCEB PGD Program",
  designation: "Sign in on Profile",
  enrolledProgram: "Post Graduate Diploma (PGD) in Supply Chain Management (SCM)",
  role: "GUEST"
};

function resolveHostingUrl(relativePath) {
  const clean = String(relativePath || "").replace(/^\/+/, "");
  try {
    const { origin, pathname } = window.location;
    if (/^https?:/i.test(origin)) {
      if (/\/desktop(\/.*)?$/i.test(pathname)) {
        const rootPath = pathname.replace(/\/desktop(\/.*)?$/i, "/");
        return `${origin}${rootPath}${clean}`;
      }
      const dirPath = pathname.endsWith("/") ? pathname : pathname.replace(/\/[^/]*$/, "/");
      return `${origin}${dirPath}${clean}`;
    }
  } catch (_) {}
  const fallbackBase = FIREBASE_CONFIG.versionJsonUrl.replace(/\/version\.json$/i, "");
  return `${fallbackBase}/${clean}`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function formatSeconds(totalSeconds) {
  const sec = Math.max(0, Number(totalSeconds || 0));
  const hrs = Math.floor(sec / 3600);
  const mins = Math.floor((sec % 3600) / 60);
  if (hrs > 0) return `${hrs}h ${mins}m`;
  return `${mins}m ${sec % 60}s`;
}

function formatBytes(bytes) {
  if (!bytes || bytes <= 0) return "—";
  const mb = bytes / (1024 * 1024);
  return mb >= 1 ? `${mb.toFixed(1)} MB` : `${Math.round(bytes / 1024)} KB`;
}

function normalizeSettings(raw) {
  const s = {
    passingPercentage: 60,
    requiredModulesCompleted: 12,
    certificatesEnabled: true,
    demoCertificateEnabled: true,
    leftSignerName: "Hasina Khan",
    leftSignerTitle: "",
    authorizedSignerName: "Amirul Islam",
    authorizedSignerTitle: "Founder of ISCEB",
    ...(raw || {})
  };
  if (typeof s.certificatesEnabled !== "boolean") s.certificatesEnabled = true;
  if (typeof s.demoCertificateEnabled !== "boolean") s.demoCertificateEnabled = true;
  if (!s.leftSignerName || /nafiul|amirul/i.test(s.leftSignerName)) {
    s.leftSignerName = "Hasina Khan";
  }
  if (!s.leftSignerTitle || /developer/i.test(s.leftSignerTitle)) {
    s.leftSignerTitle = "";
  }
  if (!s.authorizedSignerName || /nafiul|hasina/i.test(s.authorizedSignerName)) {
    s.authorizedSignerName = "Amirul Islam";
  } else if (/^md\.?\s*amirul islam$/i.test(s.authorizedSignerName.trim())) {
    s.authorizedSignerName = "Amirul Islam";
  }
  if (!s.authorizedSignerTitle || /developer|academic directorate/i.test(s.authorizedSignerTitle)) {
    s.authorizedSignerTitle = "Founder of ISCEB";
  }
  return s;
}

function sanitizeDeveloperProfile(raw) {
  const dev = { ...structuredClone(DEFAULT_DEVELOPER), ...(raw || {}) };
  if (dev.biography) {
    dev.biography = dev.biography.replaceAll(
      "International Supply Chain Education Board",
      "Institute of Supply Chain Excellence Bangladesh"
    );
  }
  return dev;
}

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      const savedRosterVer = Number(parsed.membersRosterVersion || 1);
      const parsedMembers = Array.isArray(parsed.members) && parsed.members.length ? parsed.members : [];
      let effectiveMembers;
      if (!parsedMembers.length || savedRosterVer < DEFAULT_MEMBERS_ROSTER_VERSION) {
        const official = structuredClone(DEFAULT_MEMBERS);
        const officialIds = new Set(official.map((m) => m.id));
        const extras = parsedMembers
          .filter((m) => !officialIds.has(m.id))
          .map((m, idx) => ({ ...m, displayOrder: official.length + idx + 1 }));
        effectiveMembers = [...official, ...extras];
      } else {
        effectiveMembers = parsedMembers;
      }
      const parsedFounder = parsed.founderProfile || structuredClone(DEFAULT_FOUNDER);
      const effectiveFounder =
        savedRosterVer < DEFAULT_MEMBERS_ROSTER_VERSION && !parsedFounder.educationBackground
          ? { ...structuredClone(DEFAULT_FOUNDER), ...parsedFounder, biography: DEFAULT_FOUNDER.biography, educationBackground: DEFAULT_FOUNDER.educationBackground }
          : parsedFounder;

      const customStudents = Array.isArray(parsed.customStudents) ? parsed.customStudents : [];
      const allStudents = [...customStudents, ...getStudentSwitcherUsers(AVAILABLE_USERS)];
      const savedActiveId = parsed.activeUserId;
      const validActiveId =
        savedActiveId === GUEST_USER.id || allStudents.some((u) => u.id === savedActiveId)
          ? savedActiveId
          : GUEST_USER.id;

      return {
        currentView: parsed.currentView || "dashboard",
        // Never restore admin role from plain localStorage; verified async via HMAC session
        activeUserId: validActiveId,
        customStudents,
        selectedClassId: parsed.selectedClassId || "class-1",
        partFilter: parsed.partFilter || "ALL",
        searchQuery: parsed.searchQuery || "",
        modules: Array.isArray(parsed.modules) && parsed.modules.length ? parsed.modules : structuredClone(INITIAL_MODULES),
        classes: Array.isArray(parsed.classes) && parsed.classes.length ? parsed.classes : structuredClone(INITIAL_CLASSES),
        exams: Array.isArray(parsed.exams) && parsed.exams.length ? parsed.exams : structuredClone(INITIAL_EXAMS),
        assignments: Array.isArray(parsed.assignments) && parsed.assignments.length ? parsed.assignments : structuredClone(INITIAL_ASSIGNMENTS),
        members: effectiveMembers,
        membersRosterVersion: DEFAULT_MEMBERS_ROSTER_VERSION,
        founderProfile: effectiveFounder,
        developerProfile: sanitizeDeveloperProfile(parsed.developerProfile),
        progressByUser: parsed.progressByUser || {},
        examAttemptsByUser: parsed.examAttemptsByUser || {},
        assignmentSubmissionsByUser: parsed.assignmentSubmissionsByUser || {},
        certificatesByUser: parsed.certificatesByUser || {},
        manifest: parsed.manifest
          ? {
              ...parsed.manifest,
              versionTag:
                !parsed.manifest.versionTag || parsed.manifest.versionTag === "v1.5.0"
                  ? "v1.6.0"
                  : parsed.manifest.versionTag,
              versionCode: Math.max(Number(parsed.manifest.versionCode || 106), 106),
              latestApkVersionCode: Math.max(Number(parsed.manifest.latestApkVersionCode || 7), 7),
              latestApkVersionName:
                !parsed.manifest.latestApkVersionName || parsed.manifest.latestApkVersionName === "1.5"
                  ? "1.6"
                  : parsed.manifest.latestApkVersionName
            }
          : {
              versionCode: 106,
              versionTag: "v1.6.0",
              publishedTimestamp: Date.now(),
              publishedBy: "Md. Nafiul Islam Nahin (Developer of ISCEB App)",
              releaseNotes: "ISCEB v1.6: Live auto-updating PWA, 18 MB cloud media sync, and verified certificate workflow.",
              announcement: "PGD Supply Chain Classes Live • Fridays 9:00 PM",
              latestApkVersionCode: 7,
              latestApkVersionName: "1.6",
              apkDownloadUrl: FIREBASE_CONFIG.apkDownloadUrl
            },
        settings: normalizeSettings(parsed.settings)
      };
    }
  } catch (_) {}

  return {
    currentView: "dashboard",
    activeUserId: GUEST_USER.id,
    customStudents: [],
    selectedClassId: "class-1",
    partFilter: "ALL",
    searchQuery: "",
    modules: structuredClone(INITIAL_MODULES),
    classes: structuredClone(INITIAL_CLASSES),
    exams: structuredClone(INITIAL_EXAMS),
    assignments: structuredClone(INITIAL_ASSIGNMENTS),
    members: structuredClone(DEFAULT_MEMBERS),
    membersRosterVersion: DEFAULT_MEMBERS_ROSTER_VERSION,
    founderProfile: structuredClone(DEFAULT_FOUNDER),
    developerProfile: structuredClone(DEFAULT_DEVELOPER),
    progressByUser: {},
    examAttemptsByUser: {},
    assignmentSubmissionsByUser: {},
    certificatesByUser: {},
    manifest: {
      versionCode: 106,
      versionTag: "v1.6.0",
      publishedTimestamp: Date.now(),
      publishedBy: "Md. Nafiul Islam Nahin (Developer of ISCEB App)",
      releaseNotes: "ISCEB v1.6: Live auto-updating PWA, 18 MB cloud media sync, and verified certificate workflow.",
      announcement: "PGD Supply Chain Classes Live • Fridays 9:00 PM",
      latestApkVersionCode: 7,
      latestApkVersionName: "1.6",
      apkDownloadUrl: FIREBASE_CONFIG.apkDownloadUrl
    },
    settings: normalizeSettings()
  };
}

let state = loadState();
let remoteVersionInfo = null;
let pwaBuildInfo = null;
let cloudUser = null;
let firebaseModules = null;
let deferredInstallPrompt = null;
let activeTimerInterval = null;
let examCountdownInterval = null;
let examModalSession = null;
let demoCertificateActive = false;
let selectedLoginAdminId = ADMIN_AMIRUL_ID;
let desktopAdminAuthMode = "login"; // login | setup | reset
let lastIssuedSecretNotice = "";
let verifiedAdminSessionId = null;
let pendingStudentPhotoDataUrl = "";
const localAttachmentUrls = new Map();

function saveState() {
  try {
    const persisted = {
      ...state,
      activeUserId: isAdminUser() ? GUEST_USER.id : state.activeUserId
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(persisted));
  } catch (_) {}
}

function getAllSelectableStudents() {
  const custom = Array.isArray(state.customStudents) ? state.customStudents : [];
  const baseStudents = getStudentSwitcherUsers(AVAILABLE_USERS);
  const byId = new Map();
  for (const s of [...custom, ...baseStudents]) {
    if (s && s.id && !byId.has(s.id)) byId.set(s.id, s);
  }
  return Array.from(byId.values());
}

function getActiveUser() {
  if (verifiedAdminSessionId) {
    const admin = AVAILABLE_USERS.find((u) => u.id === verifiedAdminSessionId && u.role === "ADMIN");
    if (admin) return admin;
  }
  if (state.activeUserId === GUEST_USER.id) {
    return GUEST_USER;
  }
  const allStudents = getAllSelectableStudents();
  return allStudents.find((u) => u.id === state.activeUserId) || GUEST_USER;
}

function isAdminUser() {
  return Boolean(verifiedAdminSessionId && getActiveUser().role === "ADMIN");
}

function isGuestUser() {
  return !isAdminUser() && getActiveUser().id === GUEST_USER.id;
}

function registerOrSignInCustomStudent({ fullName, email, mobileNumber, organization, enrolledProgram, photoUrl }) {
  const cleanName = String(fullName || "").trim();
  const cleanEmail = String(email || "").trim();
  if (!cleanName) {
    showToast("Please enter your full name for your ISCEB student profile.", true);
    return false;
  }
  if (!cleanEmail || !cleanEmail.includes("@")) {
    showToast("Please enter a valid registered email address.", true);
    return false;
  }
  const normalizedEmail = cleanEmail.toLowerCase();
  const existingCustom = (state.customStudents || []).find(
    (s) => String(s.email || "").trim().toLowerCase() === normalizedEmail
  );
  if (existingCustom) {
    existingCustom.fullName = cleanName;
    if (mobileNumber) existingCustom.mobileNumber = mobileNumber.trim();
    if (organization) existingCustom.organization = organization.trim();
    if (enrolledProgram) existingCustom.enrolledProgram = enrolledProgram.trim();
    if (photoUrl) existingCustom.photoUrl = photoUrl;
    state.activeUserId = existingCustom.id;
  } else {
    const serialNum = String(1000 + Math.floor(Math.random() * 8999));
    const newStudent = {
      id: `student-custom-${Date.now()}`,
      studentId: `ISCEB-2026-${serialNum}`,
      fullName: cleanName,
      email: cleanEmail,
      mobileNumber: String(mobileNumber || "").trim(),
      organization: String(organization || "").trim() || "ISCEB Enrolled Professional",
      designation: "PGD Candidate",
      enrolledProgram: String(enrolledProgram || "").trim() || GUEST_USER.enrolledProgram,
      photoUrl: photoUrl || "",
      role: "STUDENT",
      isVerified: true
    };
    state.customStudents = [newStudent, ...(state.customStudents || [])];
    state.activeUserId = newStudent.id;
  }
  pendingStudentPhotoDataUrl = "";
  saveState();
  syncAccountSwitcherOptions();
  render();
  showToast(`Signed in as ${cleanName}`);
  return true;
}

function syncAccountSwitcherOptions() {
  const select = document.getElementById("accountSwitcher");
  if (!select) return;
  const students = getAllSelectableStudents();
  select.innerHTML = [
    `<option value="${escapeHtml(GUEST_USER.id)}">Guest Preview — Sign in on Profile</option>`,
    ...students.map(
      (u) => `<option value="${escapeHtml(u.id)}">${escapeHtml(u.fullName)} — ${escapeHtml(u.studentId || u.designation || "PGD Candidate")}</option>`
    )
  ].join("");
  syncAccountSwitcherUi();
}

function syncAccountSwitcherUi() {
  const select = document.getElementById("accountSwitcher");
  if (!select) return;
  const targetId = isAdminUser() ? GUEST_USER.id : state.activeUserId;
  if (Array.from(select.options).some((opt) => opt.value === targetId)) {
    select.value = targetId;
  }
}

async function rehydrateVerifiedAdminSession() {
  try {
    const verifiedId = await desktopAdminAuth.getValidatedSessionAdminId();
    verifiedAdminSessionId = verifiedId || null;
    if (verifiedAdminSessionId) {
      render();
    }
  } catch (_) {
    verifiedAdminSessionId = null;
  }
}

function inferMediaMimeType(media) {
  if (media?.mimeType) return media.mimeType;
  const name = String(media?.fileName || "").toLowerCase();
  if (media?.type === "PDF" || name.endsWith(".pdf")) return "application/pdf";
  if (media?.type === "IMAGE") {
    if (name.endsWith(".png")) return "image/png";
    if (name.endsWith(".webp")) return "image/webp";
    return "image/jpeg";
  }
  if (media?.type === "AUDIO") {
    if (name.endsWith(".m4a")) return "audio/mp4";
    if (name.endsWith(".wav")) return "audio/wav";
    return "audio/mpeg";
  }
  if (media?.type === "VIDEO") {
    if (name.endsWith(".webm")) return "video/webm";
    return "video/mp4";
  }
  if (name.endsWith(".txt")) return "text/plain";
  if (name.endsWith(".csv")) return "text/csv";
  return "application/octet-stream";
}

function presentMediaPreviewModal(media, resolvedUrl, blob = null) {
  const title = media.title || media.fileName || "Study Material";
  const fileName = media.fileName || "study-material";
  const upperType = String(media.type || "").toUpperCase();
  const safeUrl = escapeHtml(resolvedUrl);
  const safeName = escapeHtml(fileName);
  const lowerName = fileName.toLowerCase();

  let previewBody = "";
  if (upperType === "VIDEO" || /\.(mp4|webm|mov|mkv|3gp)$/i.test(lowerName)) {
    previewBody = `<video controls autoplay playsinline src="${safeUrl}" style="width: 100%; max-height: 62vh; border-radius: 10px; background: #000;"></video>`;
  } else if (upperType === "AUDIO" || /\.(mp3|m4a|wav|aac|ogg|flac)$/i.test(lowerName)) {
    previewBody = `
      <div style="background: var(--navy); color: #fff; border-radius: 12px; padding: 18px; margin-bottom: 12px;">
        <div style="font-weight: 800; color: var(--gold-soft); margin-bottom: 10px;">Audio Lecture: ${escapeHtml(title)}</div>
        <audio controls autoplay src="${safeUrl}" style="width: 100%;"></audio>
      </div>
    `;
  } else if (upperType === "IMAGE" || /\.(png|jpe?g|webp|gif|bmp)$/i.test(lowerName)) {
    previewBody = `<img src="${safeUrl}" alt="${escapeHtml(title)}" style="max-width: 100%; max-height: 64vh; object-fit: contain; display: block; margin: 0 auto; border-radius: 10px;" />`;
  } else if (upperType === "PDF" || lowerName.endsWith(".pdf")) {
    previewBody = `<iframe src="${safeUrl}" title="${escapeHtml(title)}" style="width: 100%; height: 64vh; border: 1px solid var(--border); border-radius: 10px; background: #fff;"></iframe>`;
  } else {
    previewBody = `
      <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 18px; text-align: center;">
        <div style="font-weight: 800; color: var(--navy); font-size: 15px; margin-bottom: 6px;">${escapeHtml(title)}</div>
        <div style="font-size: 12.5px; color: var(--muted); margin-bottom: 10px;">${safeName} • ${escapeHtml(media.fileSizeFormatted || formatBytes(media.fileSizeBytes))}</div>
        <p style="font-size: 12.5px; color: #334155; margin: 0;">Download this document to open it with your installed Word, Excel, or PowerPoint app, or open it in a new tab.</p>
      </div>
    `;
  }

  openModal(
    title,
    `
      ${previewBody}
      <div style="display: flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; margin-top: 14px;">
        <a class="btn btn-outline btn-sm" href="${safeUrl}" target="_blank" rel="noopener noreferrer">Open in New Tab</a>
        <a class="btn btn-gold btn-sm" href="${safeUrl}" download="${safeName}">Download File</a>
      </div>
      <div id="textPreviewContainer"></div>
    `
  );

  if (blob && /\.(txt|csv|md|json|log)$/i.test(lowerName)) {
    blob.text().then((txt) => {
      const target = document.getElementById("textPreviewContainer");
      if (target) {
        target.innerHTML = `<pre style="margin-top: 12px; max-height: 45vh; overflow: auto; background: #0f172a; color: #f8fafc; padding: 12px; border-radius: 10px; font-size: 12px; white-space: pre-wrap;">${escapeHtml(txt.slice(0, 20000))}</pre>`;
      }
    }).catch(() => {});
  }
}

async function openSyncedCloudMedia(media) {
  if (!media) return;
  const rawUrl = String(media.url || "").trim();
  if (/^https?:\/\//i.test(rawUrl)) {
    presentMediaPreviewModal(media, rawUrl, null);
    return;
  }
  const cachedUrl = localAttachmentUrls.get(media.id);
  if (cachedUrl) {
    presentMediaPreviewModal(media, cachedUrl, null);
    return;
  }
  if (!media.cloudFileId || !media.cloudChunkCount) {
    showToast("File is stored locally on the uploader's device only (attach HTTPS link or re-upload under 18 MB).", true);
    return;
  }
  const fb = await initFirebaseSdk();
  if (!fb || !cloudUser) {
    showToast("Sign in via 'Google Cloud Sync' to download cloud class attachments.", true);
    return;
  }
  try {
    showToast(`Downloading ${media.fileName || "attachment"}...`);
    const { doc, getDoc } = fb.fsMod;
    const chunks = [];
    for (let i = 0; i < Number(media.cloudChunkCount); i++) {
      const snap = await getDoc(doc(fb.db, "lesson_files", `${media.cloudFileId}_${i}`));
      if (!snap.exists()) {
        throw new Error(`Missing cloud chunk ${i + 1}`);
      }
      const b64 = snap.data().dataBase64 || "";
      const bin = atob(b64);
      const arr = new Uint8Array(bin.length);
      for (let j = 0; j < bin.length; j++) arr[j] = bin.charCodeAt(j);
      chunks.push(arr);
    }
    const blob = new Blob(chunks, { type: inferMediaMimeType(media) });
    const objUrl = URL.createObjectURL(blob);
    localAttachmentUrls.set(media.id, objUrl);
    presentMediaPreviewModal(media, objUrl, blob);
  } catch (err) {
    showToast(`Unable to open cloud file: ${err.message || err}`, true);
  }
}

function getUserProgressMap() {
  const uid = getActiveUser().id;
  if (!state.progressByUser[uid]) state.progressByUser[uid] = {};
  return state.progressByUser[uid];
}

function getClassProgress(classId) {
  const map = getUserProgressMap();
  return (
    map[classId] || {
      classId,
      completed: false,
      activeSeconds: 0,
      quizScore: null,
      quizPassed: false,
      localNotes: "",
      localAttachments: []
    }
  );
}

function updateClassProgress(classId, updater) {
  const map = getUserProgressMap();
  const prev = getClassProgress(classId);
  map[classId] = { ...prev, ...updater };
  saveState();
}

function getOverallStats() {
  const map = getUserProgressMap();
  const totalClasses = state.classes.length || 48;
  const completedCount = state.classes.filter((c) => map[c.id]?.completed).length;
  const totalSeconds = Object.values(map).reduce((sum, item) => sum + Number(item.activeSeconds || 0), 0);
  const attempts = state.examAttemptsByUser[getActiveUser().id] || [];
  const passedExams = new Set(attempts.filter((a) => a.passed).map((a) => a.examId));
  const avgExam = attempts.length
    ? Math.round(attempts.reduce((s, a) => s + Number(a.percentage || 0), 0) / attempts.length)
    : 0;
  const part1Classes = state.classes.filter((c) => {
    const mod = state.modules.find((m) => m.id === c.moduleId);
    return mod?.part === "PART_1_BASIC";
  });
  const part2Classes = state.classes.filter((c) => {
    const mod = state.modules.find((m) => m.id === c.moduleId);
    return mod?.part === "PART_2_ADVANCE";
  });
  const part1Done = part1Classes.filter((c) => map[c.id]?.completed).length;
  const part2Done = part2Classes.filter((c) => map[c.id]?.completed).length;
  const percent = totalClasses > 0 ? Math.round((completedCount / totalClasses) * 100) : 0;
  const certEligible = completedCount >= totalClasses || isAdminUser();
  return {
    totalClasses,
    completedCount,
    percent,
    totalSeconds,
    attempts,
    passedExamsCount: passedExams.size,
    avgExam,
    part1Total: part1Classes.length,
    part1Done,
    part2Total: part2Classes.length,
    part2Done,
    certEligible
  };
}

function showToast(msg, isError = false) {
  const el = document.getElementById("toast");
  if (!el) return;
  el.textContent = msg;
  el.className = `toast show${isError ? " error" : ""}`;
  clearTimeout(el._timer);
  el._timer = setTimeout(() => {
    el.className = "toast";
  }, 3600);
}

function openModal(title, htmlContent) {
  const backdrop = document.getElementById("modalBackdrop");
  const titleEl = document.getElementById("modalTitle");
  const bodyEl = document.getElementById("modalBody");
  titleEl.textContent = title;
  bodyEl.innerHTML = htmlContent;
  backdrop.classList.add("open");
}

function closeModal() {
  if (examCountdownInterval) {
    clearInterval(examCountdownInterval);
    examCountdownInterval = null;
  }
  const bodyEl = document.getElementById("modalBody");
  if (bodyEl) {
    bodyEl.querySelectorAll("audio, video").forEach((el) => {
      try { el.pause(); } catch (_) {}
    });
    bodyEl.innerHTML = "";
  }
  examModalSession = null;
  document.getElementById("modalBackdrop").classList.remove("open");
}

async function initFirebaseSdk() {
  if (firebaseModules) return firebaseModules;
  try {
    const [appMod, authMod, fsMod] = await Promise.all([
      import("https://www.gstatic.com/firebasejs/11.1.0/firebase-app.js"),
      import("https://www.gstatic.com/firebasejs/11.1.0/firebase-auth.js"),
      import("https://www.gstatic.com/firebasejs/11.1.0/firebase-firestore.js")
    ]);
    const app = appMod.getApps().length ? appMod.getApps()[0] : appMod.initializeApp(FIREBASE_WEB_OPTIONS);
    const auth = authMod.getAuth(app);
    const db = fsMod.getFirestore(app, FIREBASE_CONFIG.databaseId);
    firebaseModules = { app, auth, db, authMod, fsMod };

    authMod.onAuthStateChanged(auth, (user) => {
      cloudUser = user || null;
      updateChromeBadges();
      if (user) {
        syncFromCloudFirestore(true);
      }
    });
    return firebaseModules;
  } catch (err) {
    console.warn("Firebase JS SDK unavailable offline:", err);
    return null;
  }
}

async function signInWithGoogleCloud() {
  const fb = await initFirebaseSdk();
  if (!fb) {
    showToast("Firebase SDK requires an active internet connection.", true);
    return;
  }
  try {
    const provider = new fb.authMod.GoogleAuthProvider();
    provider.setCustomParameters({ prompt: "select_account" });
    const cred = await fb.authMod.signInWithPopup(fb.auth, provider);
    cloudUser = cred.user;
    if (isGuestUser() && cred.user?.email) {
      registerOrSignInCustomStudent({
        fullName: cred.user.displayName || cred.user.email.split("@")[0],
        email: cred.user.email,
        organization: "ISCEB Google Verified Student",
        enrolledProgram: GUEST_USER.enrolledProgram,
        photoUrl: cred.user.photoURL || ""
      });
    }
    showToast(`Cloud connected as ${cred.user.email}`);
    await syncFromCloudFirestore(false);
    render();
  } catch (err) {
    showToast(`Google Sign-In paused: ${err.message || err}`, true);
  }
}

async function fetchVersionJson() {
  try {
    const versionUrl = resolveHostingUrl("version.json");
    const res = await fetch(`${versionUrl}?t=${Date.now()}`, { cache: "no-store" });
    if (res.ok) {
      remoteVersionInfo = await res.json();
    }
  } catch (_) {}
  try {
    const pwaRes = await fetch(`./pwa-version.json?t=${Date.now()}`, { cache: "no-store" });
    if (pwaRes.ok) {
      pwaBuildInfo = await pwaRes.json();
    }
  } catch (_) {}
  updateChromeBadges();
  if (state.currentView === "dashboard" || state.currentView === "admin" || state.currentView === "profile") {
    render();
  }
}

async function syncFromCloudFirestore(silent = false) {
  await fetchVersionJson();
  const fb = await initFirebaseSdk();
  if (!fb || !cloudUser) {
    if (!silent) {
      showToast("Refreshed local + version.json release check. Sign in with Google to pull live Firestore docs.");
    }
    return;
  }
  try {
    const { doc, getDoc } = fb.fsMod;
    const manifestSnap = await getDoc(doc(fb.db, "curriculum_manifests", "live"));
    const catalogSnap = await getDoc(doc(fb.db, "curriculum_catalogs", "live"));

    if (manifestSnap.exists()) {
      const m = manifestSnap.data();
      if (Number(m.versionCode || 0) >= Number(state.manifest.versionCode || 0)) {
        state.manifest = { ...state.manifest, ...m };
      }
    }
    if (catalogSnap.exists()) {
      const c = catalogSnap.data();
      if (Array.isArray(c.modules) && c.modules.length) state.modules = c.modules;
      if (Array.isArray(c.classes) && c.classes.length) state.classes = c.classes;
      if (c.settings) {
        state.settings = normalizeSettings({ ...state.settings, ...c.settings });
        try {
          if (c.settings.founderProfileJson) state.founderProfile = JSON.parse(c.settings.founderProfileJson);
          if (c.settings.developerProfileJson) state.developerProfile = sanitizeDeveloperProfile(JSON.parse(c.settings.developerProfileJson));
          if (c.settings.membersJson) state.members = JSON.parse(c.settings.membersJson);
        } catch (_) {}
      }
    }
    saveState();
    updateChromeBadges();
    render();
    if (!silent) showToast(`Synced Live Cloud Curriculum (${state.manifest.versionTag})`);
  } catch (err) {
    if (!silent) showToast(`Cloud Sync note: ${err.message || "Permission or network issue"}`, true);
  }
}

async function pushCurriculumToFirestore({ versionTag, versionCode, releaseNotes, announcement, apkVersionName, apkVersionCode, apkUrl }) {
  state.manifest = {
    ...state.manifest,
    versionCode: Number(versionCode),
    versionTag,
    publishedTimestamp: Date.now(),
    publishedBy: `${getActiveUser().fullName} (${getActiveUser().designation})`,
    releaseNotes,
    announcement,
    latestApkVersionCode: Number(apkVersionCode),
    latestApkVersionName: apkVersionName,
    apkDownloadUrl: apkUrl
  };
  saveState();

  const fb = await initFirebaseSdk();
  if (!fb || !cloudUser) {
    showToast("Saved locally. Sign in via 'Google Cloud Sync' as www.mdnafiulislam@gmail.com to push to Firestore.");
    render();
    return;
  }

  try {
    const { doc, getDoc, setDoc, updateDoc, serverTimestamp } = fb.fsMod;
    const manifestRef = doc(fb.db, "curriculum_manifests", "live");
    const catalogRef = doc(fb.db, "curriculum_catalogs", "live");
    const existingManifest = await getDoc(manifestRef);
    const existingCatalog = await getDoc(catalogRef);

    const manifestPayload = buildFirestoreManifestPayload({
      authorUid: cloudUser.uid,
      versionCode,
      versionTag,
      publishedBy: state.manifest.publishedBy,
      releaseNotes,
      announcement,
      latestApkVersionCode: apkVersionCode,
      latestApkVersionName: apkVersionName,
      apkDownloadUrl: apkUrl,
      isCreate: !existingManifest.exists(),
      serverTimestampToken: serverTimestamp()
    });

    const catalogPayload = buildFirestoreCatalogPayload({
      authorUid: cloudUser.uid,
      versionCode,
      modules: state.modules,
      classes: state.classes,
      settings: state.settings,
      founderProfile: state.founderProfile,
      developerProfile: state.developerProfile,
      members: state.members,
      isCreate: !existingCatalog.exists(),
      serverTimestampToken: serverTimestamp()
    });

    if (existingManifest.exists()) {
      await updateDoc(manifestRef, manifestPayload);
    } else {
      await setDoc(manifestRef, manifestPayload);
    }
    if (existingCatalog.exists()) {
      await updateDoc(catalogRef, catalogPayload);
    } else {
      await setDoc(catalogRef, catalogPayload);
    }
    showToast(`Published ${versionTag} live to Firebase Firestore! Android phones will auto-sync.`);
  } catch (err) {
    showToast(`Firestore publish failed: ${err.message}`, true);
  }
  updateChromeBadges();
  render();
}

function updateChromeBadges() {
  const stats = getOverallStats();
  const user = getActiveUser();
  const apkPortalUrl = resolveHostingUrl("apk.html");

  document.getElementById("navDashProgress").textContent = `${stats.percent}%`;
  document.getElementById("navCertStatus").textContent = state.certificatesByUser[user.id] ? "Issued" : stats.certEligible ? "Ready" : "PGD";
  document.getElementById("navMembersCount").textContent = String(state.members.length);
  document.getElementById("navAdminRoleBadge").textContent = user.role === "ADMIN" ? "Admin" : "Student";
  const navProfileBadge = document.getElementById("navProfileBadge");
  if (navProfileBadge) {
    navProfileBadge.textContent = isAdminUser() ? "Admin" : isGuestUser() ? "Guest" : "Active";
  }
  document.getElementById("sidebarCurriculumVer").textContent = `Curriculum: ${state.manifest.versionTag || "v1.6.0"}`;

  const apkVerEl = document.getElementById("sidebarApkVer");
  if (remoteVersionInfo) {
    apkVerEl.textContent = `Latest APK: v${remoteVersionInfo.versionName} (${formatBytes(remoteVersionInfo.fileSizeBytes)})`;
  } else {
    apkVerEl.textContent = `Latest APK: v${state.manifest.latestApkVersionName || "1.6"}`;
  }

  const pwaStatusEl = document.getElementById("sidebarPwaStatus");
  if (pwaStatusEl && pwaBuildInfo?.buildId) {
    pwaStatusEl.textContent = `PWA Build: ${pwaBuildInfo.buildId}`;
  }

  const syncDot = document.getElementById("syncDot");
  const syncLabel = document.getElementById("syncStatusLabel");
  if (cloudUser) {
    syncDot.className = "sync-dot";
    syncLabel.textContent = `Cloud: ${cloudUser.email}`;
  } else {
    syncDot.className = "sync-dot offline";
    syncLabel.textContent = navigator.onLine ? "Local + Web Ready" : "Offline Mode";
  }

  const googleBtn = document.getElementById("googleSignInBtn");
  if (googleBtn) {
    googleBtn.textContent = cloudUser ? `Google: ${cloudUser.email.split("@")[0]}` : "Google Cloud Sync";
  }

  // Mobile Topbar & Bottom Bar syncing (matches Android Preview)
  const mobileTopTitle = document.getElementById("mobileTopTitle");
  const mobileTopSub = document.getElementById("mobileTopSubtitle");
  if (mobileTopTitle && mobileTopSub) {
    if (isAdminUser()) {
      mobileTopTitle.textContent = user.fullName;
      mobileTopSub.textContent = user.designation || "Administrator";
    } else if (isGuestUser()) {
      mobileTopTitle.textContent = "ISCEB";
      mobileTopSub.textContent = "Guest Preview • Sign in on Profile";
    } else {
      mobileTopTitle.textContent = "ISCEB";
      mobileTopSub.textContent = `${user.fullName} • ${user.studentId || "PGD Candidate"}`;
    }
  }

  const mobileAdminPill = document.getElementById("mobileAdminLoginPill");
  const mobileAdminPillText = document.getElementById("mobileAdminPillText");
  if (mobileAdminPill && mobileAdminPillText) {
    mobileAdminPill.classList.toggle("admin-active", isAdminUser());
    mobileAdminPillText.textContent = isAdminUser() ? "ADMIN" : "LOGIN";
  }

  const mobileBadge = document.getElementById("mobileLearningBadge");
  if (mobileBadge) {
    mobileBadge.textContent = String(stats.completedCount);
    mobileBadge.style.display = stats.completedCount > 0 ? "grid" : "none";
  }

  const apkSidebarLink = document.getElementById("androidApkPortalLink");
  if (apkSidebarLink) apkSidebarLink.href = apkPortalUrl;
  const mobileShareApkBtn = document.getElementById("mobileShareApkBtn");
  if (mobileShareApkBtn) mobileShareApkBtn.href = apkPortalUrl;
}

function renderDashboard() {
  const stats = getOverallStats();
  const user = getActiveUser();
  const nextClass = state.classes.find((c) => !getClassProgress(c.id).completed) || state.classes[0];
  const nextMod = state.modules.find((m) => m.id === nextClass?.moduleId);
  const apkUrl = remoteVersionInfo?.apkUrl || state.manifest.apkDownloadUrl || resolveHostingUrl("downloads/ISCEB-latest.apk");
  const apkLabel = remoteVersionInfo
    ? `v${remoteVersionInfo.versionName} (Build ${remoteVersionInfo.versionCode}) • ${formatBytes(remoteVersionInfo.fileSizeBytes)}`
    : `v${state.manifest.latestApkVersionName || "1.6"} (Build ${state.manifest.latestApkVersionCode || 7})`;

  return `
    <div class="card card-navy hero-dashboard-grid">
      <div>
        <span class="badge badge-gold">ISCEB PC & Desktop Workspace • ${escapeHtml(state.manifest.versionTag || "v1.6.0")}</span>
        <h2 style="margin: 10px 0 6px; font-size: 24px; color: #fff;">Welcome back, ${escapeHtml(user.fullName)}</h2>
        <p style="margin: 0 0 12px; color: #CBD5E1; font-size: 13.5px;">
          ${escapeHtml(user.designation)} • ${escapeHtml(user.organization)}
        </p>
        ${state.manifest.announcement ? `<div style="background: rgba(212,175,55,0.16); border: 1px solid rgba(212,175,55,0.45); border-radius: 10px; padding: 8px 12px; font-size: 12.5px; color: #F4E2A3; margin-bottom: 12px;">Notice: ${escapeHtml(state.manifest.announcement)}</div>` : ""}
        <div style="display: flex; gap: 10px; flex-wrap: wrap;">
          <button type="button" class="btn btn-gold" data-action="continue-class" data-class-id="${escapeHtml(nextClass?.id || "class-1")}">
            Resume Lecture ${escapeHtml(nextClass?.lectureCode || "01")}: ${escapeHtml(nextClass?.title || "")}
          </button>
          <a class="btn btn-outline" href="${escapeHtml(apkUrl)}" download="ISCEB-latest.apk" style="background: rgba(255,255,255,0.1); color: #fff; border-color: rgba(255,255,255,0.28);">
            Download Android APK (${escapeHtml(apkLabel)})
          </a>
        </div>
      </div>
      <div style="border-radius: 14px; overflow: hidden; border: 2px solid rgba(212,175,55,0.45); max-height: 200px;">
        <img src="./hero-supply-chain.jpg" alt="Global Supply Chain Network" style="width: 100%; height: 200px; object-fit: cover; display: block;" />
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">Course Completion</div>
        <div class="stat-value">${stats.completedCount} / ${stats.totalClasses}</div>
        <div class="progress-track" style="margin-top: 8px;"><div class="progress-fill" style="width: ${stats.percent}%;"></div></div>
        <div class="stat-sub">${stats.percent}% of 12 SCM Modules</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Part 1 & Part 2 Breakdown</div>
        <div class="stat-value">${stats.part1Done}/${stats.part1Total} • ${stats.part2Done}/${stats.part2Total}</div>
        <div class="stat-sub">Basic (Mod 1–6) • Advance (Mod 7–12)</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Verified Exams Passed</div>
        <div class="stat-value">${stats.passedExamsCount} / ${state.exams.length}</div>
        <div class="stat-sub">Average Assessment Score: ${stats.avgExam}%</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Active Study Time</div>
        <div class="stat-value">${formatSeconds(stats.totalSeconds)}</div>
        <div class="stat-sub">PGD Certificate: <span style="white-space: nowrap; font-weight: 700;">${stats.certEligible ? "Eligible" : "In Progress"}</span></div>
      </div>
    </div>

    <div class="two-col-grid">
      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
          <h3 style="margin: 0; font-size: 16px; color: var(--navy);">Next Recommended Lecture</h3>
          <span class="badge badge-navy">${escapeHtml(nextMod ? `Module ${nextMod.number}` : "SCM")}</span>
        </div>
        <p style="margin: 0 0 6px; font-weight: 700; color: var(--navy);">${escapeHtml(nextClass?.title || "")}</p>
        <p style="margin: 0 0 12px; font-size: 13px; color: var(--muted);">${escapeHtml(nextClass?.summary || "")}</p>
        <button type="button" class="btn btn-primary btn-sm" data-action="continue-class" data-class-id="${escapeHtml(nextClass?.id || "class-1")}">Open Lecture Workspace</button>
      </div>

      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
          <h3 style="margin: 0; font-size: 16px; color: var(--navy);">ISCEB Executive Leadership</h3>
          <button type="button" class="btn btn-outline btn-sm" data-nav="directory">View Directory</button>
        </div>
        <div style="display: grid; gap: 10px;">
          <div style="display: flex; align-items: center; gap: 12px; padding: 8px; border: 1px solid var(--border); border-radius: 10px;">
            <img src="./admin-amirul.webp" alt="Md. Amirul Islam" class="portrait-avatar" style="width: 52px; height: 52px; object-fit: cover; object-position: center 20%; border: 2px solid var(--gold);" />
            <div>
              <div style="font-weight: 800; color: var(--navy); font-size: 13.5px;">${escapeHtml(state.founderProfile.name)}</div>
              <div style="font-size: 12px; color: var(--gold-dark); font-weight: 700;">${escapeHtml(state.founderProfile.designation)} • ${escapeHtml(state.founderProfile.additionalRole)}</div>
              <div style="font-size: 11.5px; color: var(--muted);">${escapeHtml(state.founderProfile.professionalIdentity)}</div>
            </div>
          </div>
          <div style="display: flex; align-items: center; gap: 12px; padding: 8px; border: 1px solid var(--border); border-radius: 10px;">
            <img src="./admin-nafiul.webp" alt="Md. Nafiul Islam Nahin" class="portrait-avatar" style="width: 52px; height: 52px; object-fit: cover; object-position: center 20%; border: 2px solid var(--navy);" />
            <div>
              <div style="font-weight: 800; color: var(--navy); font-size: 13.5px;">${escapeHtml(state.developerProfile.name)}</div>
              <div style="font-size: 12px; color: var(--navy-soft); font-weight: 700;">${escapeHtml(state.developerProfile.designation)}</div>
              <div style="font-size: 11.5px; color: var(--muted);">${escapeHtml(state.developerProfile.contactEmail)}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `;
}

function generateClassQuiz(cls) {
  return [
    {
      q: `What is the core operational objective covered in "${cls.title}"?`,
      options: [
        cls.learningObjectives?.[0] || "End-to-end supply chain visibility, compliance, and cost optimization",
        "Eliminating supplier vendor qualification completely",
        "Disabling warehouse inventory tracking records",
        "Avoiding written Standard Operating Procedures"
      ],
      correct: 0
    },
    {
      q: `In professional ISCEB SCM practice, how should "${cls.title}" be documented?`,
      options: [
        "By keeping verbal agreements with zero audit trail",
        cls.learningObjectives?.[1] || "Through structured registers, KPI tracking, and verifiable governance documentation",
        "By bypassing finance & quality verification checkpoints",
        "By ignoring Incoterms and lead-time SLA buffers"
      ],
      correct: 1
    }
  ];
}

function renderCourses() {
  const q = state.searchQuery.trim().toLowerCase();
  const filteredModules = state.modules.filter((m) => {
    if (state.partFilter !== "ALL" && m.part !== state.partFilter) return false;
    if (!q) return true;
    const modMatch = m.title.toLowerCase().includes(q) || m.description.toLowerCase().includes(q);
    const clsMatch = state.classes.some(
      (c) => c.moduleId === m.id && (c.title.toLowerCase().includes(q) || c.summary.toLowerCase().includes(q) || c.lectureCode.includes(q))
    );
    return modMatch || clsMatch;
  });

  const visibleClasses = state.classes.filter((c) => isAdminUser() || c.isPublished !== false);
  const selectedClass = visibleClasses.find((c) => c.id === state.selectedClassId) || visibleClasses[0];
  const selectedModule = state.modules.find((m) => m.id === selectedClass?.moduleId);
  const prog = selectedClass ? getClassProgress(selectedClass.id) : null;
  const quiz = selectedClass ? generateClassQuiz(selectedClass) : [];
  const timerRunning = activeTimerInterval && activeTimerInterval._classId === selectedClass?.id;

  const listHtml = filteredModules
    .map((m) => {
      const classes = visibleClasses
        .filter((c) => c.moduleId === m.id)
        .filter((c) => !q || m.title.toLowerCase().includes(q) || c.title.toLowerCase().includes(q) || c.lectureCode.includes(q))
        .sort((a, b) => a.orderIndex - b.orderIndex);
      const doneInMod = classes.filter((c) => getClassProgress(c.id).completed).length;

      return `
        <div class="card" style="padding: 12px; margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
            <span class="badge ${m.part === "PART_1_BASIC" ? "badge-navy" : "badge-gold"}">Module ${String(m.number).padStart(2, "0")}</span>
            <span style="font-size: 11px; font-weight: 700; color: var(--muted);">${doneInMod}/${classes.length} Done</span>
          </div>
          <div style="font-weight: 800; font-size: 13.5px; color: var(--navy); margin-bottom: 8px;">${escapeHtml(m.title)}</div>
          ${classes
            .map((c) => {
              const cp = getClassProgress(c.id);
              const isSel = c.id === selectedClass?.id;
              return `
                <div class="list-item ${isSel ? "selected" : ""}" data-action="select-class" data-class-id="${escapeHtml(c.id)}" style="padding: 9px 10px; margin-bottom: 6px;">
                  <div style="display: flex; justify-content: space-between; gap: 8px; align-items: center;">
                    <span style="font-size: 12.5px; font-weight: 700; color: var(--navy);">${escapeHtml(c.lectureCode)}. ${escapeHtml(c.title)}</span>
                    <span class="badge ${cp.completed ? "badge-ok" : "badge-warn"}">${cp.completed ? "Done" : `${c.durationMinutes}m`}</span>
                  </div>
                </div>
              `;
            })
            .join("")}
        </div>
      `;
    })
    .join("");

  const detailHtml = selectedClass
    ? `
      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap;">
          <div>
            <span class="badge badge-navy">${escapeHtml(selectedModule?.part === "PART_1_BASIC" ? "Part 1 • Basic Level" : "Part 2 • Advance Level")} • Module ${escapeHtml(selectedModule?.number || "")}</span>
            <h2 style="margin: 8px 0 4px; font-size: 20px; color: var(--navy);">Lecture ${escapeHtml(selectedClass.lectureCode)}: ${escapeHtml(selectedClass.title)}</h2>
            <p style="margin: 0; font-size: 13px; color: var(--muted);">Duration: ${escapeHtml(selectedClass.durationMinutes)} mins • Study Logged: ${formatSeconds(prog.activeSeconds)}</p>
          </div>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <button type="button" class="btn ${timerRunning ? "btn-danger" : "btn-outline"} btn-sm" data-action="toggle-study-timer" data-class-id="${escapeHtml(selectedClass.id)}">
              ${timerRunning ? "Stop Study Timer" : "Start Study Timer"}
            </button>
            <button type="button" class="btn ${prog.completed ? "btn-outline" : "btn-gold"} btn-sm" data-action="toggle-complete-class" data-class-id="${escapeHtml(selectedClass.id)}">
              ${prog.completed ? "Completed (Undo)" : "Mark Lecture Complete"}
            </button>
          </div>
        </div>

        <hr style="border: none; border-top: 1px solid var(--border); margin: 16px 0;" />

        <h3 style="margin: 0 0 6px; font-size: 14.5px; color: var(--navy);">Executive Summary</h3>
        <p style="margin: 0 0 14px; font-size: 13.5px; color: #334155;">${escapeHtml(selectedClass.summary)}</p>

        <h3 style="margin: 0 0 8px; font-size: 14.5px; color: var(--navy);">Key Learning Objectives</h3>
        <ul style="margin: 0 0 16px; padding-left: 20px; font-size: 13.5px; color: #334155;">
          ${(selectedClass.learningObjectives || []).map((obj) => `<li style="margin-bottom: 4px;">${escapeHtml(obj)}</li>`).join("")}
        </ul>

        <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 14px; margin-bottom: 16px;">
          <h3 style="margin: 0 0 8px; font-size: 14px; color: var(--navy);">Class Study Materials, Cloud Attachments & Local PC Notes</h3>
          ${(() => {
            const classMaterials = [
              selectedClass.pdfMedia,
              selectedClass.audioMedia,
              selectedClass.videoMedia,
              ...(selectedClass.attachments || [])
            ].filter(Boolean);
            const seen = new Set();
            const uniqueMaterials = classMaterials.filter((m) => {
              const key = m.id || `${m.fileName}_${m.url}`;
              if (seen.has(key)) return false;
              seen.add(key);
              return true;
            });
            if (!uniqueMaterials.length) {
              return `<p style="margin: 0 0 10px; font-size: 12.5px; color: var(--muted);">No cloud materials attached to this lecture yet.</p>`;
            }
            return `<div style="display: grid; gap: 8px; margin-bottom: 12px;">
              ${uniqueMaterials
                .map((m) => {
                  const hasCloud = Boolean((m.cloudFileId && m.cloudChunkCount > 0) || /^https?:\/\//i.test(String(m.url || "")));
                  const statusText = hasCloud ? "Cloud Synced" : localAttachmentUrls.has(m.id) ? "PC Session Ready" : "Device Local Only";
                  return `<div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; background: #fff; padding: 9px 12px; border-radius: 10px; border: 1px solid var(--border); flex-wrap: wrap;">
                    <div style="min-width: 0; flex: 1;">
                      <div style="font-size: 13px; font-weight: 700; color: var(--navy);">${escapeHtml(m.title || m.fileName || "Study Material")}</div>
                      <div style="font-size: 11.5px; color: var(--muted);">${escapeHtml(m.type || "FILE")} • ${escapeHtml(m.fileSizeFormatted || formatBytes(m.fileSizeBytes))} • ${statusText}</div>
                    </div>
                    <div style="display: flex; gap: 6px; align-items: center;">
                      <button type="button" class="btn btn-primary btn-sm" data-action="open-cloud-media" data-class-id="${escapeHtml(selectedClass.id)}" data-media-id="${escapeHtml(m.id || "")}">
                        Open / Preview
                      </button>
                      ${
                        isAdminUser()
                          ? `<button type="button" class="btn btn-outline btn-sm" style="color: var(--err); border-color: rgba(185,28,28,0.35);" data-action="delete-class-media" data-class-id="${escapeHtml(selectedClass.id)}" data-media-id="${escapeHtml(m.id || "")}">Delete</button>`
                          : ""
                      }
                    </div>
                  </div>`;
                })
                .join("")}
            </div>`;
          })()}
          <textarea id="lectureNotesBox" class="textarea" rows="3" placeholder="Record your PC study notes for Lecture ${escapeHtml(selectedClass.lectureCode)}...">${escapeHtml(prog.localNotes || "")}</textarea>
          <div style="display: flex; gap: 10px; align-items: center; margin-top: 10px; flex-wrap: wrap;">
            <button type="button" class="btn btn-primary btn-sm" data-action="save-notes" data-class-id="${escapeHtml(selectedClass.id)}">Save Notes</button>
            <label class="btn btn-outline btn-sm" style="cursor: pointer;">
              Attach PC PDF / Video / Slide
              <input type="file" id="attachLocalFileInput" data-class-id="${escapeHtml(selectedClass.id)}" style="display: none;" accept=".pdf,video/*,image/*,.txt,.csv" />
            </label>
          </div>
          ${
            (prog.localAttachments || []).length
              ? `<div style="margin-top: 10px; display: grid; gap: 6px;">
                  ${prog.localAttachments
                    .map((att) => {
                      const objUrl = localAttachmentUrls.get(att.id);
                      return `<div style="display: flex; justify-content: space-between; align-items: center; background: #fff; padding: 8px 10px; border-radius: 8px; border: 1px solid var(--border); font-size: 12.5px;">
                        <span><strong>${escapeHtml(att.name)}</strong> (${formatBytes(att.size)})</span>
                        ${objUrl ? `<a class="btn btn-outline btn-sm" href="${escapeHtml(objUrl)}" target="_blank" rel="noopener">Open Preview</a>` : `<span style="color: var(--muted);">Saved reference (re-select file on reload)</span>`}
                      </div>`;
                    })
                    .join("")}
                </div>`
              : ""
          }
        </div>

          <div style="background: #F4F7FC; border: 1px solid #C7D7EE; border-radius: 12px; padding: 14px; margin-bottom: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 10px;">
            <h3 style="margin: 0; font-size: 14px; color: var(--navy);">Lesson Mastery Check (2 Questions)</h3>
            ${prog.quizScore !== null ? `<span class="badge ${prog.quizPassed ? "badge-ok" : "badge-warn"}">Score: ${prog.quizScore}%</span>` : ""}
          </div>
          <form id="lessonQuizForm" data-class-id="${escapeHtml(selectedClass.id)}">
            ${quiz
              .map(
                (qItem, idx) => `
              <div style="margin-bottom: 12px;">
                <div style="font-weight: 700; font-size: 13px; color: var(--navy); margin-bottom: 6px;">Q${idx + 1}. ${escapeHtml(qItem.q)}</div>
                ${qItem.options
                  .map(
                    (opt, oIdx) => `
                  <label style="display: flex; align-items: center; gap: 8px; font-size: 13px; margin-bottom: 4px; cursor: pointer;">
                    <input type="radio" name="lq_${idx}" value="${oIdx}" required />
                    <span>${escapeHtml(opt)}</span>
                  </label>
                `
                  )
                  .join("")}
              </div>
            `
              )
              .join("")}
            <button type="submit" class="btn btn-primary btn-sm">Submit Mastery Check</button>
          </form>
        </div>

        ${
          isAdminUser()
            ? `
          <div class="upload-center-card" id="desktopAdminMediaUploadCenter">
            <div class="flex-header-between" style="margin-bottom: 10px;">
              <strong style="font-size: 14.5px; color: #fff;">Admin Media Upload Center</strong>
              <span style="font-size: 11.5px; color: var(--gold-soft); font-weight: 700; white-space: nowrap;">Max 18MB Cloud Sync</span>
            </div>
            <label class="btn btn-gold upload-banner-btn" style="cursor: pointer;">
              Upload multiple files, PDF, image, document, audio, video
              <input type="file" id="adminMultiMediaUploadInput" data-class-id="${escapeHtml(selectedClass.id)}" multiple style="display: none;" accept=".pdf,image/*,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,audio/*,video/*" />
            </label>
            <div class="media-upload-grid">
              <label class="btn btn-primary media-upload-btn" style="cursor: pointer;">
                Image
                <input type="file" class="adminTypedMediaInput" data-media-type="IMAGE" data-class-id="${escapeHtml(selectedClass.id)}" multiple style="display: none;" accept="image/*" />
              </label>
              <label class="btn btn-primary media-upload-btn" style="cursor: pointer;">
                Document
                <input type="file" class="adminTypedMediaInput" data-media-type="DOCUMENT" data-class-id="${escapeHtml(selectedClass.id)}" multiple style="display: none;" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv" />
              </label>
              <label class="btn btn-primary media-upload-btn" style="cursor: pointer;">
                Audio
                <input type="file" class="adminTypedMediaInput" data-media-type="AUDIO" data-class-id="${escapeHtml(selectedClass.id)}" multiple style="display: none;" accept="audio/*" />
              </label>
              <label class="btn btn-primary media-upload-btn" style="cursor: pointer;">
                Video
                <input type="file" class="adminTypedMediaInput" data-media-type="VIDEO" data-class-id="${escapeHtml(selectedClass.id)}" multiple style="display: none;" accept="video/*" />
              </label>
            </div>
            <form id="adminAttachRemoteUrlForm" data-class-id="${escapeHtml(selectedClass.id)}" style="margin-top: 12px; padding-top: 10px; border-top: 1px solid rgba(255,255,255,0.14); display: grid; gap: 8px;">
              <div style="font-size: 11.5px; color: var(--gold-soft); font-weight: 700;">Or Attach Shared HTTPS Link (for files &gt;18 MB or cloud-hosted video/PDF)</div>
              <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                <select id="adminRemoteMediaType" class="select" style="width: auto; min-width: 120px;">
                  <option value="PDF">PDF</option>
                  <option value="VIDEO">Video</option>
                  <option value="AUDIO">Audio</option>
                  <option value="IMAGE">Image</option>
                  <option value="DOCUMENT">Document</option>
                </select>
                <input type="url" id="adminRemoteMediaUrl" class="input" style="flex: 1; min-width: 210px;" placeholder="https://example.com/lecture-material.pdf" required />
                <input type="text" id="adminRemoteMediaTitle" class="input" style="width: 180px;" placeholder="Material Title" />
                <button type="submit" class="btn btn-gold btn-sm">Attach HTTPS Link</button>
              </div>
            </form>
          </div>
        `
            : ""
        }
      </div>
    `
    : `<div class="card">Select a class from the left list.</div>`;

  return `
    <div class="search-row">
      <input type="search" id="courseSearchInput" class="input" style="max-width: 360px;" placeholder="Search modules, lectures (01–48), keywords..." value="${escapeHtml(state.searchQuery)}" />
      <button type="button" class="btn ${state.partFilter === "ALL" ? "btn-primary" : "btn-outline"} btn-sm" data-part="ALL">All 12 Modules (48)</button>
      <button type="button" class="btn ${state.partFilter === "PART_1_BASIC" ? "btn-primary" : "btn-outline"} btn-sm" data-part="PART_1_BASIC">Part 1: Basic (1–28)</button>
      <button type="button" class="btn ${state.partFilter === "PART_2_ADVANCE" ? "btn-primary" : "btn-outline"} btn-sm" data-part="PART_2_ADVANCE">Part 2: Advance (29–48)</button>
    </div>
    <div class="master-detail">
      <div>${listHtml || '<div class="card">No matching modules or lectures found.</div>'}</div>
      <div>${detailHtml}</div>
    </div>
  `;
}

function renderExams() {
  const uid = getActiveUser().id;
  const attempts = state.examAttemptsByUser[uid] || [];
  const subs = state.assignmentSubmissionsByUser[uid] || {};

  const examsHtml = state.exams
    .map((exam) => {
      const userAttempts = attempts.filter((a) => a.examId === exam.id);
      const best = userAttempts.reduce((max, a) => (a.percentage > (max?.percentage || -1) ? a : max), null);
      return `
        <div class="card" style="margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; flex-wrap: wrap;">
            <div style="min-width: 0; flex: 1;">
              <span class="badge ${exam.id === "exam-final" ? "badge-gold" : "badge-navy"}">${exam.totalMarks} Marks • ${exam.durationMinutes} Mins</span>
              <h3 style="margin: 8px 0 4px; font-size: 15.5px; color: var(--navy);">${escapeHtml(exam.title)}</h3>
              <p style="margin: 0; font-size: 12.5px; color: var(--muted);">${exam.questions.length} Questions • Passing: ${exam.passingPercentage}%</p>
            </div>
            ${
              best
                ? `<span class="badge ${best.passed ? "badge-ok" : "badge-warn"}">Best: ${best.scoredMarks}/${exam.totalMarks} (${best.percentage}%)</span>`
                : `<span class="badge badge-warn">Not Attempted</span>`
            }
          </div>
          <div style="margin-top: 12px;">
            <button type="button" class="btn btn-primary btn-sm" data-action="start-exam" data-exam-id="${escapeHtml(exam.id)}">
              ${best ? "Retake Timed Exam" : "Start Timed Exam"}
            </button>
          </div>
        </div>
      `;
    })
    .join("");

  const assignHtml = state.assignments
    .map((asg) => {
      const sub = subs[asg.id];
      return `
        <div class="card" style="margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; flex-wrap: wrap;">
            <div style="min-width: 0; flex: 1;">
              <span class="badge ${asg.isFinalProject ? "badge-gold" : "badge-navy"}">${asg.maxMarks} Marks${asg.isFinalProject ? " • Capstone" : ""}</span>
              <h3 style="margin: 8px 0 4px; font-size: 15px; color: var(--navy);">${escapeHtml(asg.title)}</h3>
              <p style="margin: 0; font-size: 12.5px; color: #334155;">${escapeHtml(asg.topicDescription)}</p>
            </div>
            <span class="badge ${sub?.grade !== undefined && sub?.grade !== null ? "badge-ok" : sub ? "badge-navy" : "badge-warn"}">
              ${sub?.grade !== undefined && sub?.grade !== null ? `Graded: ${sub.grade}/${asg.maxMarks}` : sub ? "Submitted" : "Pending"}
            </span>
          </div>
          <div style="margin-top: 10px; display: flex; gap: 8px; flex-wrap: wrap;">
            <button type="button" class="btn btn-outline btn-sm" data-action="open-assignment" data-asg-id="${escapeHtml(asg.id)}">
              ${sub ? "View / Update Submission" : "Submit Work"}
            </button>
            ${
              isAdminUser() && sub
                ? `<button type="button" class="btn btn-gold btn-sm" data-action="grade-assignment" data-asg-id="${escapeHtml(asg.id)}">Admin Grade</button>`
                : ""
            }
          </div>
        </div>
      `;
    })
    .join("");

  return `
    <div class="two-col-grid">
      <div>
        <h2 style="margin: 0 0 12px; color: var(--navy); font-size: 17px;">Interactive Timed Examinations</h2>
        ${examsHtml}
      </div>
      <div>
        <h2 style="margin: 0 0 12px; color: var(--navy); font-size: 17px;">Practical Assignments & Capstone</h2>
        ${assignHtml}
      </div>
    </div>
  `;
}

function renderLearning() {
  const user = getActiveUser();
  const stats = getOverallStats();
  const cert = state.certificatesByUser[user.id];
  const certificatesEnabled = state.settings.certificatesEnabled !== false;
  const demoCertEnabled = state.settings.demoCertificateEnabled !== false || isAdminUser();
  const demoRecord = {
    recipientName: user.fullName || "ISCEB Executive Candidate",
    serial: "ISCEB-DEMO-PGD-2026",
    issuedDate: new Date().toISOString().slice(0, 10),
    isDemo: true
  };
  const displayedCert = demoCertificateActive && demoCertEnabled ? demoRecord : cert;
  const leftSignerName = state.settings.leftSignerName || "Hasina Khan";
  const leftSignerTitle = state.settings.leftSignerTitle || "";
  const rightSignerName = state.settings.authorizedSignerName || "Amirul Islam";
  const rightSignerTitle = state.settings.authorizedSignerTitle || "Founder of ISCEB";

  return `
    <div class="two-col-grid">
      <div class="card">
        <div class="flex-header-between" style="margin-bottom: 10px;">
          <h3 style="margin: 0; color: var(--navy);">Certificate Eligibility Dashboard</h3>
          <span class="badge ${cert ? "badge-ok" : stats.certEligible ? "badge-gold" : "badge-warn"}" id="learningCertStatusBadge">${cert ? "Issued" : stats.certEligible ? "Eligible" : "In Progress"}</span>
        </div>
        <div style="display: grid; gap: 8px; font-size: 13.5px; margin-bottom: 14px;">
          <div>1. Lectures Completed: <strong>${stats.completedCount} / ${stats.totalClasses}</strong></div>
          <div>2. Timed Exams Passed: <strong>${stats.passedExamsCount} / ${state.exams.length}</strong></div>
          <div>3. Active Study Hours Logged: <strong>${formatSeconds(stats.totalSeconds)}</strong></div>
          <div>4. Account Role: <strong>${escapeHtml(user.designation)}</strong></div>
        </div>
        ${
          !certificatesEnabled
            ? `<div style="margin-bottom: 10px; padding: 8px 12px; border-radius: 8px; background: var(--warn-bg); color: var(--warn); font-size: 12.5px; font-weight: 700;">Official certificate generation is currently paused by the ISCEB Administrator.</div>`
            : ""
        }
        <div style="display: flex; gap: 8px; flex-wrap: wrap;">
          <button type="button" class="btn btn-gold" data-action="issue-certificate" ${!certificatesEnabled ? "disabled" : ""}>
            ${cert ? "Regenerate Official PGD Certificate" : "Generate Official PGD Certificate"}
          </button>
          ${
            demoCertEnabled
              ? `<button type="button" class="btn btn-outline" data-action="preview-demo-certificate">
                  ${demoCertificateActive ? "Hide Demo Preview" : "Preview Demo Certificate"}
                </button>`
              : ""
          }
          ${displayedCert ? `<button type="button" class="btn btn-primary" onclick="window.print()">Print / Save PDF</button>` : ""}
        </div>
      </div>

      <div class="card">
        <h3 style="margin: 0 0 10px; color: var(--navy);">Verify Certificate Serial</h3>
        <p style="margin: 0 0 10px; font-size: 13px; color: var(--muted);">Enter an ISCEB Certificate ID to validate authenticity locally.</p>
        <div style="display: flex; gap: 8px;">
          <input type="text" id="verifyCertInput" class="input" placeholder="e.g. ${escapeHtml(cert?.serial || "ISCEB-PGD-2026-1001")}" />
          <button type="button" class="btn btn-primary btn-sm" data-action="verify-cert">Verify</button>
        </div>
        <div id="verifyCertResult" style="margin-top: 10px; font-size: 13px;"></div>
      </div>
    </div>

    ${
      displayedCert
        ? `
      <div class="cert-sheet">
        <img src="./isceb-logo.webp" alt="ISCEB" style="height: 68px; object-fit: contain; margin-bottom: 8px;" />
        ${displayedCert.isDemo ? `<div class="cert-demo-pill">DEMO CERTIFICATE PREVIEW</div>` : ""}
        <div style="font-size: 12px; font-weight: 800; letter-spacing: 1.4px; color: var(--gold-dark); text-transform: uppercase;">
          Institute of Supply Chain Excellence Bangladesh (ISCEB)
        </div>
        <h2 style="margin: 8px 0 4px; font-size: 28px; color: var(--navy);">Post Graduate Diploma in Supply Chain Management</h2>
        <p style="margin: 0 0 14px; color: var(--muted); font-size: 14px;">This certifies that</p>
        <div style="font-size: 30px; font-weight: 800; color: var(--navy); border-bottom: 2px solid var(--gold); display: inline-block; padding: 0 28px 6px; margin-bottom: 14px;">
          ${escapeHtml(displayedCert.recipientName)}
        </div>
        <p style="max-width: 680px; margin: 0 auto 22px; font-size: 14px; color: #334155;">
          has successfully fulfilled the executive academic requirements across the 12-module, 48-lecture Post Graduate Diploma curriculum in Strategic & Operational Supply Chain Management.
        </p>
        <div class="cert-signatories-grid">
          <div class="cert-sig-col">
            <div class="cert-sig-space" aria-hidden="true"></div>
            <div class="cert-sig-line">${escapeHtml(leftSignerName)}</div>
            ${leftSignerTitle ? `<div style="color: var(--muted);">${escapeHtml(leftSignerTitle)}</div>` : ""}
          </div>
          <div class="cert-sig-col">
            <div style="font-weight: 800; color: var(--gold-dark);">Serial: ${escapeHtml(displayedCert.serial)}</div>
            <div style="color: var(--muted);">Issued: ${escapeHtml(displayedCert.issuedDate)}</div>
          </div>
          <div class="cert-sig-col">
            <div class="cert-sig-space" aria-hidden="true"></div>
            <div class="cert-sig-line">${escapeHtml(rightSignerName)}</div>
            <div style="color: var(--gold-dark); font-weight: 700;">${escapeHtml(rightSignerTitle)}</div>
          </div>
        </div>
      </div>
    `
        : ""
    }
  `;
}

function getMemberPhotoSrc(member) {
  if (member?.photoUrl) return member.photoUrl;
  const map = {
    amirul: "./admin-amirul.webp",
    hasina: "./member-hasina.webp",
    masudur: "./member-masudur.webp",
    asif: "./member-asif.webp",
    mashiur: "./member-mashiur.webp",
    shajibul: "./member-shajibul.webp",
    tonmoy: "./member-tonmoy.webp",
    nafiul: "./admin-nafiul.webp"
  };
  return map[member?.builtInPhotoKey] || "";
}

function renderMemberAvatarHtml(member, sizePx = 68) {
  const src = getMemberPhotoSrc(member);
  if (src) {
    return `<img src="${escapeHtml(src)}" alt="${escapeHtml(member.fullName)}" class="portrait-avatar" style="width: ${sizePx}px; height: ${sizePx}px; flex-shrink: 0; object-fit: cover; object-position: center 20%; border: 2.5px solid var(--gold); background: var(--navy);" />`;
  }
  const initials = String(member?.fullName || "IM")
    .split(" ")
    .filter((w) => w && w.toLowerCase() !== "md.")
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join("") || "IM";
  return `<div class="portrait-avatar" style="width: ${sizePx}px; height: ${sizePx}px; flex-shrink: 0; display: grid; place-items: center; background: var(--navy); color: var(--gold); font-weight: 800; font-size: ${Math.round(sizePx * 0.34)}px; border: 2.5px solid var(--gold);">${escapeHtml(initials)}</div>`;
}

function renderMemberDetailHtml(member) {
  const skillsHtml = Array.isArray(member.skillsAndExpertise) && member.skillsAndExpertise.length
    ? `<div style="display: flex; flex-wrap: wrap; gap: 6px;">${member.skillsAndExpertise
        .map((s) => `<span class="badge badge-navy" style="font-size: 11px;">${escapeHtml(s)}</span>`)
        .join("")}</div>`
    : `<span style="color: var(--muted); font-style: italic; font-size: 12.5px;">Not specified</span>`;

  return `
    <div style="display: grid; gap: 14px;">
      <div class="card-navy" style="border-radius: 14px; padding: 18px; display: flex; gap: 16px; align-items: center; flex-wrap: wrap;">
        ${renderMemberAvatarHtml(member, 92)}
        <div style="flex: 1; min-width: 220px;">
          <span class="badge badge-gold">#${Number(member.displayOrder || 1)} ${escapeHtml(member.designation || "ISCEB Member")}</span>
          <h3 style="margin: 6px 0 3px; color: #fff; font-size: 20px;">${escapeHtml(member.fullName)}</h3>
          <div style="font-size: 13px; color: var(--gold-soft); font-weight: 700;">${escapeHtml(member.professionalIdentity || "")}</div>
          ${member.roleInIsceb ? `<div style="font-size: 12px; color: #CBD5E1; margin-top: 4px;">${escapeHtml(member.roleInIsceb)}</div>` : ""}
        </div>
      </div>

      ${member.currentOrganization || member.roleInIsceb ? `
        <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px;">
          <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 4px;">Organization & Role in ISCEB</div>
          ${member.currentOrganization ? `<div style="font-size: 13px; color: #1E293B;"><strong>Workplace:</strong> ${escapeHtml(member.currentOrganization)}</div>` : ""}
          ${member.roleInIsceb ? `<div style="font-size: 13px; color: #1E293B; margin-top: 2px;"><strong>Role in ISCEB:</strong> ${escapeHtml(member.roleInIsceb)}</div>` : ""}
        </div>` : ""}

      ${member.educationalQualification ? `
        <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px;">
          <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 4px;">Educational Qualification & Certifications</div>
          <div style="font-size: 13px; color: #334155; white-space: pre-line;">${escapeHtml(member.educationalQualification)}</div>
        </div>` : ""}

      ${member.professionalExperience ? `
        <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px;">
          <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 4px;">Professional Experience</div>
          <div style="font-size: 13px; color: #334155; white-space: pre-line;">${escapeHtml(member.professionalExperience)}</div>
        </div>` : ""}

      <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px;">
        <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 6px;">Skills & Expertise</div>
        ${skillsHtml}
      </div>

      ${member.biography ? `
        <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px;">
          <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 4px;">Personal & Professional Biography</div>
          <div style="font-size: 13px; color: #334155; white-space: pre-line; line-height: 1.55;">${escapeHtml(member.biography)}</div>
        </div>` : ""}

      ${member.contactInformation ? `
        <div style="background: #F8FAFC; border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px;">
          <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 4px;">Contact & Affiliation</div>
          <div style="font-size: 12.5px; color: var(--muted);">${escapeHtml(member.contactInformation)}</div>
        </div>` : ""}
    </div>
  `;
}

function renderDirectory() {
  const sortedMembers = [...state.members].sort((a, b) => Number(a.displayOrder || 99) - Number(b.displayOrder || 99));

  return `
    <div class="card card-navy" style="margin-bottom: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap;">
        <div>
          <span class="badge badge-gold">ISCEB Official Directory</span>
          <h2 style="margin: 6px 0 4px; color: #fff; font-size: 20px;">ISCEB Members & Executive Leadership</h2>
          <p style="margin: 0; font-size: 13px; color: #CBD5E1;">Official leadership, faculty tutors, corporate advisors & technology team (${sortedMembers.length} active profiles). Click any profile to view full biography and credentials.</p>
        </div>
        ${isAdminUser() ? `<button type="button" class="btn btn-primary btn-sm" data-action="add-member" style="background: var(--gold); color: var(--navy); font-weight: 800;">+ Add ISCEB Member</button>` : ""}
      </div>
    </div>

    <div class="two-col-grid" style="gap: 14px;">
      ${sortedMembers
        .map(
          (m, i) => `
        <div class="card" style="margin-bottom: 0; display: flex; flex-direction: column; justify-content: space-between; border: 1px solid #E2E8F0; transition: box-shadow .15s ease;">
          <div>
            <div style="display: flex; gap: 14px; align-items: center; margin-bottom: 10px;">
              ${renderMemberAvatarHtml(m, 72)}
              <div style="min-width: 0; flex: 1;">
                <div style="display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-bottom: 4px;">
                  <span class="badge badge-navy">#${Number(m.displayOrder || i + 1)}</span>
                  ${m.designation ? `<span class="badge badge-gold">${escapeHtml(m.designation)}</span>` : ""}
                </div>
                <h4 style="margin: 2px 0; color: var(--navy); font-size: 16px; line-height: 1.25;">${escapeHtml(m.fullName)}</h4>
                <div style="font-size: 12px; font-weight: 700; color: var(--gold-dark); margin-top: 2px;">${escapeHtml(m.roleInIsceb || m.currentOrganization || "")}</div>
              </div>
            </div>
            ${m.professionalIdentity ? `<div style="font-size: 12.5px; font-weight: 600; color: #1E293B; margin-bottom: 6px;">${escapeHtml(m.professionalIdentity)}</div>` : ""}
            <p style="margin: 0 0 10px; font-size: 12.5px; color: #475569; line-height: 1.48; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;">${escapeHtml(m.biography || "")}</p>
          </div>
          <div style="display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap; padding-top: 10px; border-top: 1px solid #F1F5F9;">
            <button type="button" class="btn btn-primary btn-sm" data-action="view-member" data-member-id="${escapeHtml(m.id)}">View Full Profile →</button>
            ${
              isAdminUser()
                ? `<div style="display: flex; gap: 6px; flex-wrap: wrap;">
                    <button type="button" class="btn btn-outline btn-sm" data-action="edit-member" data-member-id="${escapeHtml(m.id)}">Edit</button>
                    <button type="button" class="btn btn-outline btn-sm" data-action="move-member-up" data-member-id="${escapeHtml(m.id)}">Up</button>
                    ${m.id !== "member-founder-amirul" ? `<button type="button" class="btn btn-danger btn-sm" data-action="delete-member" data-member-id="${escapeHtml(m.id)}">Remove</button>` : ""}
                  </div>`
                : `<span style="font-size: 11px; color: var(--muted);">${escapeHtml(m.currentOrganization || "")}</span>`
            }
          </div>
        </div>
      `
        )
        .join("")}
    </div>
  `;
}

function renderAdmin() {
  const summaries = desktopAdminAuth.getSummaries();
  const anyConfigured = desktopAdminAuth.isAnyAdminConfigured();

  if (!isAdminUser()) {
    const chosenSummary = summaries.find((s) => s.adminId === selectedLoginAdminId) || summaries[0];
    const effectiveMode = !chosenSummary.configured ? "setup" : (desktopAdminAuthMode === "reset" ? "reset" : "login");
    const needsInvite = anyConfigured && !chosenSummary.configured;

    return `
      <div class="two-col-grid">
        <div class="card">
          <span class="badge badge-navy">ISCEB Administrator Portal</span>
          <h2 style="margin: 8px 0 4px; color: var(--navy);">Select Administrator Account</h2>
          <p style="margin: 0 0 12px; color: var(--muted); font-size: 12.5px;">
            Zero hardcoded passcodes. Each administrator has a private salted PBKDF2 password.
          </p>
          <div id="desktopAdminOrderList" style="display: grid; gap: 10px;">
            ${summaries
              .map((adm, idx) => {
                const isSelected = adm.adminId === selectedLoginAdminId;
                const photo = adm.adminId === ADMIN_AMIRUL_ID ? "./admin-amirul.webp" : "./admin-nafiul.webp";
                return `
                  <div class="list-item ${isSelected ? "selected" : ""}" data-action="select-login-admin" data-admin-id="${escapeHtml(adm.adminId)}" style="display: flex; align-items: center; gap: 12px; margin-bottom: 0;">
                    <img src="${photo}" alt="${escapeHtml(adm.fullName)}" class="portrait-avatar" style="width: 56px; height: 56px; border: 2px solid ${idx === 0 ? "var(--gold)" : "var(--navy)"};" />
                    <div style="flex: 1;">
                      <div style="font-weight: 800; color: var(--navy); font-size: 14px;">${idx + 1}. ${escapeHtml(adm.fullName)}</div>
                      <div style="font-size: 12px; font-weight: 700; color: var(--gold-dark);">${escapeHtml(adm.designation)}</div>
                      <div style="font-size: 11.5px; color: var(--muted);">${escapeHtml(adm.subRole)}</div>
                    </div>
                    <span class="badge ${adm.configured ? "badge-ok" : "badge-warn"}">${adm.configured ? "Active" : "Setup Required"}</span>
                  </div>
                `;
              })
              .join("")}
          </div>
          ${
            lastIssuedSecretNotice
              ? `<div style="margin-top: 12px; padding: 10px 12px; background: var(--ok-bg); border: 1px solid #86EFAC; border-radius: 10px; font-size: 12.5px; color: #166534;">${escapeHtml(lastIssuedSecretNotice)}</div>`
              : ""
          }
        </div>

        <div class="card">
          <span class="badge ${effectiveMode === "setup" ? "badge-gold" : "badge-navy"}">
            ${effectiveMode === "setup" ? "First-Time Admin Password Setup" : effectiveMode === "reset" ? "Verified Admin Recovery" : "Administrator Sign In"}
          </span>
          <h3 style="margin: 8px 0 10px; color: var(--navy);">${escapeHtml(chosenSummary.fullName)}</h3>

          ${
            effectiveMode === "setup"
              ? `<form id="desktopAdminSetupForm" style="display: grid; gap: 10px;">
                  ${
                    needsInvite
                      ? `<label style="font-size: 12px; font-weight: 700;">Single-Use Co-Admin Invite Code
                          <input type="text" id="deskAdminInviteCode" class="input" placeholder="e.g. ABCD-EFGH-JKLM" autocomplete="off" required />
                        </label>`
                      : `<p style="margin: 0; font-size: 12px; color: var(--muted);">Create an 8+ character private password with uppercase, lowercase, number, and symbol.</p>`
                  }
                  <label style="font-size: 12px; font-weight: 700;">New Private Password
                    <input type="password" id="deskSetupPw" class="input" autocomplete="new-password" required />
                  </label>
                  <label style="font-size: 12px; font-weight: 700;">Confirm Password
                    <input type="password" id="deskSetupConfirmPw" class="input" autocomplete="new-password" required />
                  </label>
                  <button type="submit" class="btn btn-gold">Configure Password & Sign In</button>
                </form>`
              : effectiveMode === "reset"
                ? `<form id="desktopAdminResetForm" style="display: grid; gap: 10px;">
                    <p style="margin: 0; font-size: 12px; color: var(--muted);">
                      Verify using your one-time recovery key${cloudUser ? ` or signed-in Google (${escapeHtml(cloudUser.email)})` : ""}.
                    </p>
                    <label style="font-size: 12px; font-weight: 700;">One-Time Recovery Key
                      <input type="text" id="deskResetRecoveryCode" class="input" placeholder="Optional if Google verified" autocomplete="off" />
                    </label>
                    <label style="font-size: 12px; font-weight: 700;">New Password
                      <input type="password" id="deskResetNewPw" class="input" autocomplete="new-password" required />
                    </label>
                    <label style="font-size: 12px; font-weight: 700;">Confirm New Password
                      <input type="password" id="deskResetConfirmPw" class="input" autocomplete="new-password" required />
                    </label>
                    <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                      <button type="submit" class="btn btn-gold">Reset Password</button>
                      <button type="button" class="btn btn-outline" data-action="set-admin-auth-mode" data-mode="login">Back to Sign In</button>
                    </div>
                  </form>`
                : `<form id="desktopAdminLoginForm" style="display: grid; gap: 10px;">
                    ${
                      chosenSummary.lockoutRemainingSeconds > 0
                        ? `<div class="badge badge-err">Locked for ${chosenSummary.lockoutRemainingSeconds}s after repeated failed attempts</div>`
                        : ""
                    }
                    <label style="font-size: 12px; font-weight: 700;">Administrator Password
                      <input type="password" id="deskLoginPw" class="input" value="" autocomplete="current-password" required />
                    </label>
                    <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                      <button type="submit" class="btn btn-primary">Verify & Sign In</button>
                      <button type="button" class="btn btn-outline" data-action="set-admin-auth-mode" data-mode="reset">Forgot / Recovery Key</button>
                    </div>
                  </form>`
          }
        </div>
      </div>
    `;
  }

  const user = getActiveUser();
  const coAdminSummary = summaries.find((s) => s.adminId !== user.id);

  return `
    <div class="card" style="margin-bottom: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; margin-bottom: 12px;">
        <div>
          <span class="badge badge-ok">Verified Admin Session</span>
          <h3 style="margin: 6px 0 0; color: var(--navy);">Signed in as ${escapeHtml(user.fullName)} (${escapeHtml(user.designation)})</h3>
        </div>
        <button type="button" class="btn btn-danger btn-sm" data-action="admin-logout">Sign Out Admin</button>
      </div>
      <div id="desktopAdminLeadershipHierarchy" style="display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px;">
        <div style="display: flex; align-items: center; gap: 12px; padding: 10px; border: 1px solid var(--gold); border-radius: 12px; background: #FFFDF7;">
          <img src="./admin-amirul.webp" alt="Md. Amirul Islam" class="portrait-avatar" style="width: 56px; height: 56px; border: 2px solid var(--gold);" />
          <div>
            <div style="font-weight: 800; color: var(--navy); font-size: 14px;">1. ${escapeHtml(state.founderProfile.name)}</div>
            <div style="font-size: 12px; font-weight: 700; color: var(--gold-dark);">${escapeHtml(state.founderProfile.designation)} • ${escapeHtml(state.founderProfile.additionalRole)}</div>
            <div style="font-size: 11.5px; color: var(--muted);">${escapeHtml(state.founderProfile.professionalIdentity)}</div>
          </div>
        </div>
        <div style="display: flex; align-items: center; gap: 12px; padding: 10px; border: 1px solid var(--border); border-radius: 12px; background: #fff;">
          <img src="./admin-nafiul.webp" alt="Md. Nafiul Islam Nahin" class="portrait-avatar" style="width: 56px; height: 56px; border: 2px solid var(--navy);" />
          <div>
            <div style="font-weight: 800; color: var(--navy); font-size: 14px;">2. ${escapeHtml(state.developerProfile.name)}</div>
            <div style="font-size: 12px; font-weight: 700; color: var(--navy-soft);">${escapeHtml(state.developerProfile.designation)}</div>
            <div style="font-size: 11.5px; color: var(--muted);">${escapeHtml(state.developerProfile.contactEmail)}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="two-col-grid">
      <form id="adminPublishForm" class="card">
        <span class="badge badge-navy">Admin OTA & Firestore Publisher</span>
        <h3 style="margin: 8px 0 12px; color: var(--navy);">Publish Live Curriculum & APK Metadata</h3>
        <div style="display: grid; gap: 10px;">
          <label style="font-size: 12px; font-weight: 700;">Curriculum Version Tag
            <input type="text" id="admVerTag" class="input" value="${escapeHtml(state.manifest.versionTag || "v1.6.0")}" required />
          </label>
          <label style="font-size: 12px; font-weight: 700;">Curriculum Build Code
            <input type="number" id="admVerCode" class="input" value="${Number(state.manifest.versionCode || 106) + 1}" required />
          </label>
          <label style="font-size: 12px; font-weight: 700;">Release Notes
            <textarea id="admNotes" class="textarea" rows="3">${escapeHtml(state.manifest.releaseNotes || "")}</textarea>
          </label>
          <label style="font-size: 12px; font-weight: 700;">Global Student Announcement Banner
            <input type="text" id="admAnnouncement" class="input" value="${escapeHtml(state.manifest.announcement || "")}" />
          </label>
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
            <label style="font-size: 12px; font-weight: 700;">Latest APK Version Name
              <input type="text" id="admApkVerName" class="input" value="${escapeHtml(remoteVersionInfo?.versionName || state.manifest.latestApkVersionName || "1.6")}" required />
            </label>
            <label style="font-size: 12px; font-weight: 700;">Latest APK Version Code
              <input type="number" id="admApkVerCode" class="input" value="${Number(remoteVersionInfo?.versionCode || state.manifest.latestApkVersionCode || 7)}" required />
            </label>
          </div>
          <label style="font-size: 12px; font-weight: 700;">Official APK Download URL
            <input type="url" id="admApkUrl" class="input" value="${escapeHtml(remoteVersionInfo?.apkUrl || state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl)}" required />
          </label>
        </div>
        <div style="margin-top: 14px; display: flex; gap: 8px; flex-wrap: wrap;">
          <button type="submit" class="btn btn-gold">Publish Update (${escapeHtml(cloudUser ? "Push Live to Firestore" : "Save Locally")})</button>
          ${!cloudUser ? `<button type="button" class="btn btn-outline" data-action="trigger-google-auth">Sign in Google (Cloud Sync)</button>` : ""}
        </div>
      </form>

      <div class="card">
        <span class="badge badge-gold">Admin Security & Cloud Diagnostics</span>
        <h3 style="margin: 8px 0 12px; color: var(--navy);">Credentials & Co-Admin Onboarding</h3>
        ${
          coAdminSummary && !coAdminSummary.configured
            ? `<div style="margin-bottom: 14px; padding: 10px; border: 1px solid var(--border); border-radius: 10px; background: var(--surface-muted);">
                <div style="font-size: 12.5px; font-weight: 700; color: var(--navy); margin-bottom: 6px;">Onboard ${escapeHtml(coAdminSummary.fullName)}</div>
                <button type="button" class="btn btn-primary btn-sm" data-action="generate-coadmin-invite" data-admin-id="${escapeHtml(coAdminSummary.adminId)}">Generate Single-Use Invite Code</button>
              </div>`
            : ""
        }
        ${
          lastIssuedSecretNotice
            ? `<div style="margin-bottom: 12px; padding: 10px; background: var(--ok-bg); border: 1px solid #86EFAC; border-radius: 10px; font-size: 12px; color: #166534;">${escapeHtml(lastIssuedSecretNotice)}</div>`
            : ""
        }
        <form id="desktopAdminChangePasswordForm" style="display: grid; gap: 8px; margin-bottom: 14px;">
          <div style="font-size: 12.5px; font-weight: 700; color: var(--navy);">Change Your Own Password</div>
          <input type="password" id="deskChangeCurrentPw" class="input" placeholder="Current Password" autocomplete="current-password" required />
          <input type="password" id="deskChangeNewPw" class="input" placeholder="New Strong Password" autocomplete="new-password" required />
          <input type="password" id="deskChangeConfirmPw" class="input" placeholder="Confirm New Password" autocomplete="new-password" required />
          <button type="submit" class="btn btn-outline btn-sm">Update My Admin Password</button>
        </form>
        <div style="margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--border); display: grid; gap: 8px;">
          <div style="font-size: 12.5px; font-weight: 800; color: var(--navy);">Certificate & Demo Diploma Controls</div>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <button type="button" class="btn btn-sm ${state.settings.certificatesEnabled !== false ? "btn-primary" : "btn-outline"}" data-action="toggle-certificates-enabled">
              Official Certificates: ${state.settings.certificatesEnabled !== false ? "Enabled" : "Disabled"}
            </button>
            <button type="button" class="btn btn-sm ${state.settings.demoCertificateEnabled !== false ? "btn-gold" : "btn-outline"}" data-action="toggle-demo-cert-enabled">
              Demo Certificate Preview: ${state.settings.demoCertificateEnabled !== false ? "Enabled" : "Disabled"}
            </button>
            <button type="button" class="btn btn-outline btn-sm" data-action="admin-preview-demo-cert">
              Open & Print Demo Certificate
            </button>
          </div>
          <div style="font-size: 11.5px; color: var(--muted);">
            Signatories — Left: <strong>${escapeHtml(state.settings.leftSignerName || "Hasina Khan")}</strong> • Right: <strong>${escapeHtml(state.settings.authorizedSignerName || "Amirul Islam")}</strong> (${escapeHtml(state.settings.authorizedSignerTitle || "Founder of ISCEB")})
          </div>
        </div>

        <div style="display: grid; gap: 6px; font-size: 12.5px; color: #334155; border-top: 1px solid var(--border); padding-top: 10px; margin-top: 12px;">
          <div><strong>Firebase Project:</strong> ${escapeHtml(FIREBASE_CONFIG.projectId)}</div>
          <div><strong>Firestore DB:</strong> ${escapeHtml(FIREBASE_CONFIG.databaseId)}</div>
          <div><strong>Google Cloud Auth:</strong> ${escapeHtml(cloudUser ? cloudUser.email : "Not signed in")}</div>
          <div><strong>Hosted version.json:</strong> ${remoteVersionInfo ? `v${escapeHtml(remoteVersionInfo.versionName)} (Build ${escapeHtml(remoteVersionInfo.versionCode)}) • ${formatBytes(remoteVersionInfo.fileSizeBytes)}` : "Pending fetch..."}</div>
        </div>
      </div>
    </div>
  `;
}

function renderProfile() {
  const user = getActiveUser();
  const stats = getOverallStats();
  const apkUrl = remoteVersionInfo?.apkUrl || state.manifest.apkDownloadUrl || resolveHostingUrl("downloads/ISCEB-latest.apk");
  const apkPageUrl = resolveHostingUrl("apk.html");
  const apkVersionLabel = remoteVersionInfo
    ? `v${remoteVersionInfo.versionName} (Build ${remoteVersionInfo.versionCode})`
    : `v${state.manifest.latestApkVersionName || "1.6"} (Build ${state.manifest.latestApkVersionCode || 7})`;
  const pwaVerText = pwaBuildInfo?.buildId
    ? `Live Web Build ${pwaBuildInfo.buildId}`
    : `Curriculum ${state.manifest.versionTag || "v1.6.0"} • Auto-Sync Active`;
  const hasCert = Boolean(state.certificatesByUser[user.id]);
  const presetStudents = getStudentSwitcherUsers(AVAILABLE_USERS);
  const customStudents = Array.isArray(state.customStudents) ? state.customStudents : [];

  const identitySection = isGuestUser()
    ? `
      <div class="card card-navy">
        <div style="font-size: 16px; font-weight: 800; color: #fff;">Student Account Sign-In &amp; Enrollment</div>
        <p style="margin: 6px 0 12px; font-size: 12.5px; color: #C0D2E5;">
          Sign in once with your Google account or register directly below. Your name appears on your official PGD Diploma &amp; certificates.
        </p>
        <button type="button" id="profileGoogleSignInBtn" class="btn btn-gold" data-action="trigger-google-auth" style="width: 100%; font-weight: 800;">
          Continue with Google Account Sync
        </button>

        <div class="profile-reg-inner">
          <div class="flex-header-between">
            <div>
              <div style="font-weight: 800; color: var(--navy); font-size: 14px;">Direct Student Registration</div>
              <div style="font-size: 11.5px; color: var(--muted);">Instant enrollment across all 12 SCM modules &amp; 48 lectures</div>
            </div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <div id="profilePhotoPreviewWrap" style="width: 44px; height: 44px; border-radius: 50%; overflow: hidden; border: 2px solid var(--gold); background: var(--surface-muted); display: grid; place-items: center; font-weight: 800; color: var(--navy); flex-shrink: 0;">
                ${pendingStudentPhotoDataUrl ? `<img id="profilePhotoPreviewImg" src="${escapeHtml(pendingStudentPhotoDataUrl)}" alt="Profile Preview" style="width: 100%; height: 100%; object-fit: cover;" />` : `<span>PGD</span>`}
              </div>
              <label for="profilePhotoInput" class="btn btn-outline btn-sm" style="cursor: pointer;">Photo</label>
              <input type="file" id="profilePhotoInput" accept="image/*" style="display: none;" />
            </div>
          </div>

          <form id="studentRegisterForm" style="display: grid; gap: 10px;">
            <input type="text" id="regStudentName" class="input" placeholder="Full Name (printed on diploma)" required />
            <input type="email" id="regStudentEmail" class="input" placeholder="Registered Email Address" required />
            <input type="tel" id="regStudentMobile" class="input" placeholder="Mobile Number (optional)" />
            <input type="text" id="regStudentOrg" class="input" placeholder="Company / Organization (optional)" />
            <label style="display: grid; gap: 4px; font-size: 11.5px; font-weight: 700; color: var(--muted);">
              Enrolled Course / PGD Program
              <input type="text" id="regStudentProgram" class="input" value="${escapeHtml(GUEST_USER.enrolledProgram)}" required />
            </label>
            <button type="submit" class="btn btn-primary" style="width: 100%; min-height: 48px;">
              Create / Sign In Student Account
            </button>
          </form>
        </div>
      </div>
    `
    : `
      <div class="card card-navy">
        <div class="flex-header-between" style="align-items: flex-start;">
          <div style="display: flex; gap: 14px; align-items: center; min-width: 0; flex: 1;">
            ${
              user.photoUrl || isAdminUser()
                ? `<img src="${escapeHtml(user.photoUrl || (user.id === ADMIN_AMIRUL_ID ? "./admin-amirul.webp" : "./admin-nafiul.webp"))}" alt="${escapeHtml(user.fullName)}" class="portrait-avatar" style="width: 64px; height: 64px; object-fit: cover; object-position: center 20%; border: 2px solid var(--gold); flex-shrink: 0;" />`
                : `<div style="width: 64px; height: 64px; border-radius: 50%; background: var(--gold); color: var(--navy-dark); display: grid; place-items: center; font-weight: 800; font-size: 20px; flex-shrink: 0;">${escapeHtml(user.fullName.split(" ").map((p) => p[0]).slice(0, 2).join("").toUpperCase())}</div>`
            }
            <div style="min-width: 0;">
              <span class="badge badge-gold">${isAdminUser() ? "VERIFIED ADMINISTRATOR" : "VERIFIED STUDENT • ACTIVE"}</span>
              <h2 style="margin: 6px 0 3px; font-size: 20px; color: #fff;">${escapeHtml(user.fullName)}</h2>
              <div style="font-size: 12.5px; color: #E2E8F0;">${escapeHtml(user.studentId || user.designation || "PGD Candidate")} • ${escapeHtml(user.organization || "ISCEB")}</div>
              <div style="font-size: 12px; color: #B0C2D6; margin-top: 2px;">${escapeHtml(user.email || "")}${user.mobileNumber ? ` • ${escapeHtml(user.mobileNumber)}` : ""}</div>
            </div>
          </div>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            ${!isAdminUser() ? `<button type="button" class="btn btn-gold btn-sm" data-action="open-edit-student-modal">Edit Profile</button>` : ""}
            <button type="button" class="btn btn-outline btn-sm" data-action="${isAdminUser() ? "admin-logout" : "student-signout"}" style="background: rgba(255,255,255,0.1); color: #fff; border-color: rgba(255,255,255,0.3);">
              Sign Out
            </button>
          </div>
        </div>
        <div style="margin-top: 12px; padding-top: 10px; border-top: 1px solid rgba(255,255,255,0.16); font-size: 12px; color: #F4E2A3;">
          Program: ${escapeHtml(user.enrolledProgram || GUEST_USER.enrolledProgram)}
        </div>
      </div>

      <div class="stats-grid" style="margin-bottom: 0;">
        <div class="stat-card">
          <div class="stat-label">Lessons Completed</div>
          <div class="stat-value">${stats.completedCount} / ${stats.totalClasses}</div>
          <div class="stat-sub">Part 1 &amp; Part 2</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Course Progress</div>
          <div class="stat-value">${stats.percent}%</div>
          <div class="stat-sub">${formatSeconds(stats.totalSeconds)} studied</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Exams Passed</div>
          <div class="stat-value">${stats.passedExamsCount} / ${state.exams.length}</div>
          <div class="stat-sub">Avg Score: ${stats.avgExam}%</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Certificates</div>
          <div class="stat-value">${hasCert ? "Issued" : stats.certEligible ? "Eligible" : "Pending"}</div>
          <div class="stat-sub">${hasCert ? escapeHtml(state.certificatesByUser[user.id].serial) : "PGD in SCM"}</div>
        </div>
      </div>
    `;

  return `
    <div class="profile-portal-stack">
      <div class="card">
        <div class="profile-action-row">
          <div style="display: flex; align-items: center; gap: 12px;">
            <img src="./isceb-logo.webp" alt="ISCEB" style="width: 48px; height: 36px; object-fit: contain; background: #fff; border-radius: 8px; padding: 2px; border: 1px solid var(--border);" />
            <div>
              <div style="font-weight: 800; color: var(--navy); font-size: 15px;">ISCEB • Institute of Supply Chain Excellence Bangladesh</div>
              <div style="font-size: 12px; color: var(--muted);">Student Learning, Diploma Credentialing &amp; Live Auto-Sync Portal</div>
            </div>
          </div>
          <span class="badge ${isAdminUser() ? "badge-navy" : isGuestUser() ? "badge-warn" : "badge-ok"}">${isAdminUser() ? "Admin Mode" : isGuestUser() ? "Guest Preview" : "Student Signed In"}</span>
        </div>
      </div>

      ${identitySection}

      <div class="card">
        <div class="profile-action-row">
          <div style="display: flex; align-items: center; gap: 12px; flex: 1; min-width: 200px;">
            <div class="profile-icon-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z"/></svg>
            </div>
            <div>
              <div style="font-weight: 800; color: var(--navy); font-size: 14.5px;">Google Cloud Auto-Sync</div>
              <div style="font-size: 12px; color: var(--muted);">
                ${cloudUser ? `Connected as ${escapeHtml(cloudUser.email)}` : "Sign in with Google for automatic live syllabus &amp; progress sync across phones"}
              </div>
            </div>
          </div>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            ${
              cloudUser
                ? `<button type="button" class="btn btn-primary btn-sm" data-action="profile-sync-now">Sync Now</button>
                   <button type="button" class="btn btn-outline btn-sm" data-action="google-signout">Disconnect</button>`
                : `<button type="button" class="btn btn-primary btn-sm" data-action="trigger-google-auth">Sign in with Google</button>`
            }
          </div>
        </div>
      </div>

      <div class="card">
        <div class="profile-action-row">
          <div style="display: flex; align-items: center; gap: 12px; flex: 1; min-width: 200px;">
            <div class="profile-icon-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            </div>
            <div>
              <div style="font-weight: 800; color: var(--navy); font-size: 14.5px;">Administrator Management Mode</div>
              <div style="font-size: 12px; color: var(--muted);">Protected with per-admin cryptographic authentication</div>
            </div>
          </div>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            ${
              isAdminUser()
                ? `<button type="button" class="btn btn-primary btn-sm" data-nav="admin">Open Admin Panel</button>
                   <button type="button" class="btn btn-outline btn-sm" data-action="admin-logout">Logout</button>`
                : `<button type="button" class="btn btn-primary btn-sm" data-nav="admin">Login</button>`
            }
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex-header-between" style="margin-bottom: 10px;">
          <div style="display: flex; align-items: center; gap: 12px;">
            <div class="profile-icon-box" style="background: #FEF3C7; color: #92400E;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M17 1H7c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-2-2-2zm0 18H7V5h10v14zm-4-7h2.5L12 16.5 8.5 12H11V8h2v4z"/></svg>
            </div>
            <div>
              <div style="font-weight: 800; color: var(--navy); font-size: 14.5px;">App Updates &amp; Online APK</div>
              <div style="font-size: 12px; color: var(--muted);">${escapeHtml(pwaVerText)} • APK ${escapeHtml(apkVersionLabel)}</div>
            </div>
          </div>
          <a href="${escapeHtml(apkPageUrl)}" target="_blank" rel="noopener noreferrer" class="btn btn-outline btn-sm" style="white-space: nowrap;">
            Web Page
          </a>
        </div>

        <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px;">
          <button type="button" class="btn btn-outline btn-sm" data-action="check-pwa-update-now">Check Updates</button>
          <a href="${escapeHtml(apkUrl)}" download="ISCEB-latest.apk" class="btn btn-primary btn-sm">Download Latest APK</a>
          <button type="button" class="btn btn-outline btn-sm" data-action="export-workspace-json">Backup Workspace JSON</button>
          <label for="importWorkspaceJsonInput" class="btn btn-outline btn-sm" style="cursor: pointer;">Restore JSON Backup</label>
          <input type="file" id="importWorkspaceJsonInput" accept="application/json,.json" style="display: none;" />
        </div>
      </div>

      <div class="card">
        <div style="font-weight: 800; color: var(--navy); font-size: 14px; margin-bottom: 8px;">Quick Student Account Switcher</div>
        <div style="display: flex; gap: 8px; flex-wrap: wrap;">
          ${[...customStudents, ...presetStudents]
            .map(
              (s) => `
                <button type="button" class="btn btn-sm ${!isAdminUser() && state.activeUserId === s.id ? "btn-gold" : "btn-outline"}" data-action="quick-switch-student" data-student-id="${escapeHtml(s.id)}">
                  ${escapeHtml(s.fullName)}
                </button>
              `
            )
            .join("")}
        </div>
        <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; padding-top: 10px; border-top: 1px solid var(--border);">
          <button type="button" class="btn btn-outline btn-sm" data-nav="directory">ISCEB Members (${state.members.length})</button>
          <button type="button" class="btn btn-outline btn-sm" data-nav="exams">Exams &amp; Assignments</button>
          <button type="button" class="btn btn-outline btn-sm" data-nav="learning">Diploma &amp; Certificate</button>
        </div>
      </div>
    </div>
  `;
}

function render() {
  updateChromeBadges();

  document.querySelectorAll(".nav-item").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.view === state.currentView);
  });
  document.querySelectorAll("#mobileBottomNav .bottom-nav-btn").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.view === state.currentView);
  });

  const titles = {
    dashboard: ["Executive SCM Dashboard", "Institute of Supply Chain Excellence Bangladesh (ISCEB) — Desktop Workspace"],
    courses: ["All 12 Modules & 48 Lectures", "Part 1: Basic Level (1–28) & Part 2: Advance Level (29–48)"],
    exams: ["Timed Examinations & Assignments", "Official Module Assessments, 100-Mark Final Exam & Capstone Study"],
    learning: ["My Learning & Official PGD Certificate", "Academic Readiness, Printable Executive Diploma & Serial Verification"],
    directory: ["ISCEB Leadership & Directory", "Founder, App Developer & Verified Faculty Members"],
    profile: ["Student Profile & Credentialing", "Direct Registration, Google Cloud Sync & Online APK Updates"],
    admin: ["Admin Governance & Cloud Publishing", "Live Curriculum OTA Sync & APK Release Metadata"]
  };
  const [t, sub] = titles[state.currentView] || titles.dashboard;
  document.getElementById("pageTitle").textContent = t;
  document.getElementById("pageSubtitle").textContent = sub;

  const container = document.getElementById("mainContent");
  if (state.currentView === "courses") container.innerHTML = renderCourses();
  else if (state.currentView === "exams") container.innerHTML = renderExams();
  else if (state.currentView === "learning") container.innerHTML = renderLearning();
  else if (state.currentView === "directory") container.innerHTML = renderDirectory();
  else if (state.currentView === "profile") container.innerHTML = renderProfile();
  else if (state.currentView === "admin") container.innerHTML = renderAdmin();
  else container.innerHTML = renderDashboard();
}

function openExamModal(examId) {
  const exam = state.exams.find((e) => e.id === examId);
  if (!exam) return;
  let remainingSec = Number(exam.durationMinutes || 20) * 60;
  examModalSession = { exam, remainingSec };

  const html = `
    <div style="display: flex; justify-content: space-between; align-items: center; background: var(--navy); color: #fff; padding: 10px 14px; border-radius: 10px; margin-bottom: 14px;">
      <span>${exam.questions.length} Questions • ${exam.totalMarks} Total Marks</span>
      <strong id="examTimerBadge" style="color: var(--gold);">Time Left: ${Math.floor(remainingSec / 60)}:00</strong>
    </div>
    <form id="activeExamForm">
      ${exam.questions
        .map(
          (q, idx) => `
        <div style="margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid var(--border);">
          <div style="font-weight: 700; color: var(--navy); margin-bottom: 6px;">${idx + 1}. ${escapeHtml(q.question)} (${q.marks} Marks)</div>
          ${q.options
            .map(
              (opt, oIdx) => `
            <label style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px; font-size: 13.5px; cursor: pointer;">
              <input type="radio" name="eq_${idx}" value="${oIdx}" />
              <span>${escapeHtml(opt)}</span>
            </label>
          `
            )
            .join("")}
        </div>
      `
        )
        .join("")}
      <button type="submit" class="btn btn-gold" style="width: 100%;">Submit Examination</button>
    </form>
  `;
  openModal(exam.title, html);

  examCountdownInterval = setInterval(() => {
    remainingSec -= 1;
    const badge = document.getElementById("examTimerBadge");
    if (badge) {
      const m = Math.floor(Math.max(0, remainingSec) / 60);
      const s = String(Math.max(0, remainingSec) % 60).padStart(2, "0");
      badge.textContent = `Time Left: ${m}:${s}`;
    }
    if (remainingSec <= 0) {
      submitActiveExam();
    }
  }, 1000);
}

function submitActiveExam() {
  if (!examModalSession) return;
  const { exam } = examModalSession;
  const form = document.getElementById("activeExamForm");
  let scored = 0;
  exam.questions.forEach((q, idx) => {
    const selected = form?.querySelector(`input[name="eq_${idx}"]:checked`)?.value;
    if (selected !== undefined && Number(selected) === q.correctOptionIndex) {
      scored += Number(q.marks || 0);
    }
  });
  const pct = exam.totalMarks > 0 ? Math.round((scored / exam.totalMarks) * 100) : 0;
  const passed = pct >= (exam.passingPercentage || 60);
  const uid = getActiveUser().id;
  if (!state.examAttemptsByUser[uid]) state.examAttemptsByUser[uid] = [];
  state.examAttemptsByUser[uid].push({
    examId: exam.id,
    scoredMarks: scored,
    totalMarks: exam.totalMarks,
    percentage: pct,
    passed,
    timestamp: Date.now()
  });
  saveState();
  closeModal();
  showToast(`Exam submitted: ${scored}/${exam.totalMarks} (${pct}%) — ${passed ? "PASSED" : "Try Again"}`, !passed);
  render();
}

async function runAppUpdateCheck() {
  await fetchVersionJson();
  if ("serviceWorker" in navigator) {
    try {
      const reg = window.__iscebSwRegistration || (await navigator.serviceWorker.getRegistration("./"));
      if (reg) {
        await reg.update();
        if (reg.waiting) {
          reg.waiting.postMessage({ type: "SKIP_WAITING" });
          showToast("New ISCEB PWA version ready — applying update now...");
          return;
        }
      }
    } catch (_) {}
  }
  const verRes = await fetch(`./pwa-version.json?t=${Date.now()}`, { cache: "no-store" }).catch(() => null);
  if (verRes && verRes.ok) {
    const info = await verRes.json().catch(() => null);
    if (info && info.buildId) {
      pwaBuildInfo = info;
      showToast(`ISCEB Live PWA is up to date (Build ${info.buildId}).`);
      render();
      return;
    }
  }
  showToast("ISCEB Live PWA update check completed.");
}

function resizePhotoFileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        const maxDim = 256;
        const scale = Math.min(1, maxDim / Math.max(img.width || maxDim, img.height || maxDim));
        const w = Math.max(1, Math.round(img.width * scale));
        const h = Math.max(1, Math.round(img.height * scale));
        const canvas = document.createElement("canvas");
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, w, h);
        resolve(canvas.toDataURL("image/jpeg", 0.85));
      };
      img.onerror = () => resolve(String(reader.result || ""));
      img.src = String(reader.result || "");
    };
    reader.onerror = () => reject(new Error("Could not read image"));
    reader.readAsDataURL(file);
  });
}

function setupListeners() {
  const select = document.getElementById("accountSwitcher");
  syncAccountSwitcherOptions();
  select?.addEventListener("change", (e) => {
    const picked = e.target.value;
    const validIds = [GUEST_USER.id, ...getAllSelectableStudents().map((u) => u.id)];
    if (!validIds.includes(picked)) return;
    if (isAdminUser()) {
      desktopAdminAuth.clearSession();
      verifiedAdminSessionId = null;
    }
    state.activeUserId = picked;
    saveState();
    render();
  });

  const sidebarEl = document.getElementById("appSidebar");
  const drawerBackdropEl = document.getElementById("drawerBackdrop");
  const openMobileDrawer = () => {
    sidebarEl?.classList.add("drawer-open");
    drawerBackdropEl?.classList.add("open");
  };
  const closeMobileDrawer = () => {
    sidebarEl?.classList.remove("drawer-open");
    drawerBackdropEl?.classList.remove("open");
  };

  document.getElementById("mobileMenuToggleBtn")?.addEventListener("click", () => {
    if (sidebarEl?.classList.contains("drawer-open")) closeMobileDrawer();
    else openMobileDrawer();
  });
  drawerBackdropEl?.addEventListener("click", closeMobileDrawer);

  document.getElementById("navList").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-view]");
    if (!btn) return;
    state.currentView = btn.dataset.view;
    saveState();
    closeMobileDrawer();
    render();
    window.scrollTo({ top: 0, behavior: "instant" });
  });

  document.getElementById("mobileBottomNav")?.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-view]");
    if (!btn) return;
    state.currentView = btn.dataset.view;
    saveState();
    closeMobileDrawer();
    render();
    window.scrollTo({ top: 0, behavior: "instant" });
  });

  document.getElementById("mobileVerifyBtn")?.addEventListener("click", () => {
    state.currentView = "learning";
    saveState();
    closeMobileDrawer();
    render();
    setTimeout(() => {
      const inp = document.getElementById("verifyCertInput");
      if (inp) {
        inp.scrollIntoView({ behavior: "smooth", block: "center" });
        inp.focus();
      }
    }, 60);
  });

  document.getElementById("mobileCloudSyncBtn")?.addEventListener("click", () => {
    if (cloudUser) syncFromCloudFirestore(false);
    else signInWithGoogleCloud();
  });

  document.getElementById("mobileAdminLoginPill")?.addEventListener("click", () => {
    state.currentView = "admin";
    saveState();
    closeMobileDrawer();
    render();
    window.scrollTo({ top: 0, behavior: "instant" });
  });

  document.getElementById("androidApkPortalLink")?.addEventListener("click", () => {
    closeMobileDrawer();
  });

  document.getElementById("modalCloseBtn").addEventListener("click", closeModal);
  document.getElementById("syncNowBtn").addEventListener("click", () => syncFromCloudFirestore(false));
  document.getElementById("googleSignInBtn").addEventListener("click", signInWithGoogleCloud);

  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    deferredInstallPrompt = e;
    const btn = document.getElementById("installPwaBtn");
    if (btn) btn.style.display = "inline-flex";
  });

  window.addEventListener("appinstalled", () => {
    deferredInstallPrompt = null;
    const btn = document.getElementById("installPwaBtn");
    if (btn) btn.style.display = "none";
    showToast("ISCEB App installed! Future GitHub updates will auto-sync silently.");
  });

  document.getElementById("installPwaBtn").addEventListener("click", async () => {
    if (!deferredInstallPrompt) return;
    deferredInstallPrompt.prompt();
    await deferredInstallPrompt.userChoice;
    deferredInstallPrompt = null;
    document.getElementById("installPwaBtn").style.display = "none";
  });

  const checkPwaUpdateBtn = document.getElementById("checkPwaUpdateBtn");
  if (checkPwaUpdateBtn) {
    checkPwaUpdateBtn.addEventListener("click", runAppUpdateCheck);
  }

  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.addEventListener("message", (event) => {
      if (event.data && event.data.type === "ISCEB_PWA_UPDATED") {
        const statusEl = document.getElementById("sidebarPwaStatus");
        if (statusEl && event.data.buildId && event.data.buildId !== "__ISCEB_SW_BUILD__") {
          statusEl.textContent = `PWA Build: ${event.data.buildId}`;
        }
      }
    });
  }

  document.getElementById("mainContent").addEventListener("click", (e) => {
    const navTarget = e.target.closest("[data-nav]");
    if (navTarget) {
      state.currentView = navTarget.dataset.nav;
      saveState();
      render();
      return;
    }

    const partBtn = e.target.closest("[data-part]");
    if (partBtn) {
      state.partFilter = partBtn.dataset.part;
      saveState();
      render();
      return;
    }

    const actBtn = e.target.closest("[data-action]");
    if (!actBtn) return;
    const action = actBtn.dataset.action;

    if (action === "continue-class") {
      state.selectedClassId = actBtn.dataset.classId;
      state.currentView = "courses";
      saveState();
      render();
    } else if (action === "select-class") {
      state.selectedClassId = actBtn.dataset.classId;
      saveState();
      render();
    } else if (action === "toggle-complete-class") {
      const cid = actBtn.dataset.classId;
      const cur = getClassProgress(cid);
      updateClassProgress(cid, { completed: !cur.completed });
      showToast(!cur.completed ? "Lecture marked complete!" : "Completion status cleared.");
      render();
    } else if (action === "toggle-study-timer") {
      const cid = actBtn.dataset.classId;
      if (activeTimerInterval && activeTimerInterval._classId === cid) {
        clearInterval(activeTimerInterval);
        activeTimerInterval = null;
        showToast("Study timer paused.");
      } else {
        if (activeTimerInterval) clearInterval(activeTimerInterval);
        activeTimerInterval = setInterval(() => {
          const cur = getClassProgress(cid);
          updateClassProgress(cid, { activeSeconds: Number(cur.activeSeconds || 0) + 1 });
          if (state.currentView === "courses") render();
        }, 1000);
        activeTimerInterval._classId = cid;
        showToast("Live study time tracking started.");
      }
      render();
    } else if (action === "save-notes") {
      const cid = actBtn.dataset.classId;
      const notes = document.getElementById("lectureNotesBox")?.value || "";
      updateClassProgress(cid, { localNotes: notes });
      showToast("Lecture study notes saved locally on PC.");
    } else if (action === "start-exam") {
      openExamModal(actBtn.dataset.examId);
    } else if (action === "open-assignment") {
      const asg = state.assignments.find((a) => a.id === actBtn.dataset.asgId);
      if (!asg) return;
      const uid = getActiveUser().id;
      const prev = state.assignmentSubmissionsByUser[uid]?.[asg.id]?.text || "";
      openModal(
        asg.title,
        `<p style="font-size: 13px; color: var(--muted);">${escapeHtml(asg.topicDescription)}</p>
         <form id="submitAssignmentForm" data-asg-id="${escapeHtml(asg.id)}">
           <textarea id="asgSubmissionText" class="textarea" rows="6" placeholder="Write or paste your executive SCM analysis (${asg.maxMarks} Marks)...">${escapeHtml(prev)}</textarea>
           <button type="submit" class="btn btn-primary" style="margin-top: 12px; width: 100%;">Submit Assignment</button>
         </form>`
      );
    } else if (action === "grade-assignment") {
      const asg = state.assignments.find((a) => a.id === actBtn.dataset.asgId);
      if (!asg) return;
      openModal(
        `Grade: ${asg.title}`,
        `<form id="gradeAssignmentForm" data-asg-id="${escapeHtml(asg.id)}">
           <label style="font-size: 12.5px; font-weight: 700;">Award Marks (Max ${asg.maxMarks})
             <input type="number" id="asgGradeValue" class="input" min="0" max="${asg.maxMarks}" value="${asg.maxMarks}" required />
           </label>
           <button type="submit" class="btn btn-gold" style="margin-top: 12px; width: 100%;">Save Grade</button>
         </form>`
      );
    } else if (action === "issue-certificate") {
      if (state.settings.certificatesEnabled === false) {
        showToast("Official certificate generation is currently disabled by Administrator.", true);
        return;
      }
      demoCertificateActive = false;
      const user = getActiveUser();
      const serial = `ISCEB-PGD-2026-${String(1000 + Math.floor(Math.random() * 8999))}`;
      state.certificatesByUser[user.id] = {
        recipientName: user.fullName,
        serial,
        issuedDate: new Date().toISOString().slice(0, 10)
      };
      saveState();
      showToast(`Official PGD Certificate generated (${serial}).`);
      render();
    } else if (action === "preview-demo-certificate") {
      demoCertificateActive = !demoCertificateActive;
      showToast(demoCertificateActive ? "Demo Certificate preview opened (Hasina Khan & Amirul Islam)." : "Demo Certificate preview closed.");
      render();
    } else if (action === "toggle-certificates-enabled") {
      state.settings.certificatesEnabled = state.settings.certificatesEnabled === false;
      saveState();
      showToast(`Official Certificate generation ${state.settings.certificatesEnabled ? "enabled" : "disabled"}.`);
      render();
    } else if (action === "toggle-demo-cert-enabled") {
      state.settings.demoCertificateEnabled = state.settings.demoCertificateEnabled === false;
      saveState();
      showToast(`Demo Certificate preview ${state.settings.demoCertificateEnabled ? "enabled" : "disabled"}.`);
      render();
    } else if (action === "admin-preview-demo-cert") {
      demoCertificateActive = true;
      state.currentView = "learning";
      saveState();
      showToast("Opened Demo Certificate preview.");
      render();
    } else if (action === "verify-cert") {
      const serial = document.getElementById("verifyCertInput")?.value.trim();
      const out = document.getElementById("verifyCertResult");
      const found = Object.values(state.certificatesByUser).find((c) => c.serial === serial);
      out.innerHTML = found
        ? `<span class="badge badge-ok">Verified Valid</span> Issued to <strong>${escapeHtml(found.recipientName)}</strong> on ${escapeHtml(found.issuedDate)}.`
        : `<span class="badge badge-err">Not Found</span> No certificate matching <code>${escapeHtml(serial)}</code> in this workspace.`;
    } else if (action === "open-cloud-media") {
      const cls = state.classes.find((c) => c.id === actBtn.dataset.classId);
      const mid = actBtn.dataset.mediaId;
      const all = cls ? [cls.pdfMedia, cls.audioMedia, cls.videoMedia, ...(cls.attachments || [])].filter(Boolean) : [];
      const media = all.find((m) => (m.id || "") === mid) || all[0];
      openSyncedCloudMedia(media);
    } else if (action === "delete-class-media") {
      if (!isAdminUser()) {
        showToast("Admin sign-in required to remove class materials.", true);
        return;
      }
      const cid = actBtn.dataset.classId;
      const mid = actBtn.dataset.mediaId;
      const cls = state.classes.find((c) => c.id === cid);
      if (!cls) return;
      if (cls.pdfMedia?.id === mid) cls.pdfMedia = null;
      if (cls.audioMedia?.id === mid) cls.audioMedia = null;
      if (cls.videoMedia?.id === mid) cls.videoMedia = null;
      cls.attachments = (cls.attachments || []).filter((m) => (m.id || "") !== mid);
      saveState();
      if (cloudUser) {
        pushCurriculumToFirestore({
          versionTag: state.manifest.versionTag || "v1.6.0",
          versionCode: Number(state.manifest.versionCode || 105) + 1,
          releaseNotes: `Removed material from Lecture ${cls.lectureCode}`,
          announcement: state.manifest.announcement || "",
          apkVersionName: state.manifest.latestApkVersionName || "1.6",
          apkVersionCode: state.manifest.latestApkVersionCode || 7,
          apkUrl: state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl
        });
      } else {
        showToast("Removed material locally. Connect Google Cloud Sync to update student devices.");
        render();
      }
    } else if (action === "select-login-admin") {
      selectedLoginAdminId = actBtn.dataset.adminId;
      desktopAdminAuthMode = "login";
      render();
    } else if (action === "set-admin-auth-mode") {
      desktopAdminAuthMode = actBtn.dataset.mode || "login";
      render();
    } else if (action === "admin-logout") {
      desktopAdminAuth.clearSession();
      verifiedAdminSessionId = null;
      lastIssuedSecretNotice = "";
      showToast("Signed out of Administrator workspace.");
      render();
    } else if (action === "generate-coadmin-invite") {
      const targetId = actBtn.dataset.adminId;
      desktopAdminAuth.generateSecondAdminInvite(targetId).then((res) => {
        if (res.ok) {
          lastIssuedSecretNotice = `Single-use invite code for ${res.targetAdminName}: ${res.setupToken}`;
          showToast(`Invite generated for ${res.targetAdminName}.`);
        } else {
          showToast(res.error, true);
        }
        render();
      });
    } else if (action === "trigger-google-auth") {
      signInWithGoogleCloud();
    } else if (action === "profile-sync-now") {
      syncFromCloudFirestore(false);
    } else if (action === "google-signout") {
      if (firebaseModules?.authMod && firebaseModules?.auth) {
        firebaseModules.authMod.signOut(firebaseModules.auth).then(() => {
          cloudUser = null;
          updateChromeBadges();
          render();
          showToast("Disconnected Google Cloud Sync.");
        }).catch(() => {});
      }
    } else if (action === "student-signout") {
      state.activeUserId = GUEST_USER.id;
      pendingStudentPhotoDataUrl = "";
      saveState();
      syncAccountSwitcherOptions();
      render();
      showToast("Signed out to Guest Preview.");
    } else if (action === "quick-switch-student") {
      const sid = actBtn.dataset.studentId;
      if (getAllSelectableStudents().some((s) => s.id === sid)) {
        if (isAdminUser()) {
          desktopAdminAuth.clearSession();
          verifiedAdminSessionId = null;
        }
        state.activeUserId = sid;
        saveState();
        syncAccountSwitcherOptions();
        render();
        showToast(`Switched active student to ${getActiveUser().fullName}.`);
      }
    } else if (action === "open-edit-student-modal") {
      const cur = getActiveUser();
      if (isGuestUser() || isAdminUser()) return;
      openModal(
        "Edit Student Profile",
        `<form id="editStudentProfileForm" style="display: grid; gap: 10px;">
          <input type="text" id="editStuName" class="input" placeholder="Full Name" value="${escapeHtml(cur.fullName)}" required />
          <input type="email" id="editStuEmail" class="input" placeholder="Registered Email" value="${escapeHtml(cur.email || "")}" required />
          <input type="tel" id="editStuMobile" class="input" placeholder="Mobile Number" value="${escapeHtml(cur.mobileNumber || "")}" />
          <input type="text" id="editStuOrg" class="input" placeholder="Organization" value="${escapeHtml(cur.organization || "")}" />
          <input type="text" id="editStuProgram" class="input" placeholder="Enrolled PGD Program" value="${escapeHtml(cur.enrolledProgram || GUEST_USER.enrolledProgram)}" required />
          <button type="submit" class="btn btn-primary">Save Student Profile</button>
        </form>`
      );
    } else if (action === "check-pwa-update-now") {
      runAppUpdateCheck();
    } else if (action === "export-workspace-json") {
      const payload = {
        exportedAt: new Date().toISOString(),
        schemaVersion: 2,
        activeUserId: isAdminUser() ? GUEST_USER.id : state.activeUserId,
        customStudents: state.customStudents || [],
        progressByUser: state.progressByUser || {},
        examAttemptsByUser: state.examAttemptsByUser || {},
        assignmentSubmissionsByUser: state.assignmentSubmissionsByUser || {},
        certificatesByUser: state.certificatesByUser || {},
        settings: state.settings
      };
      const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `isceb-workspace-backup-${new Date().toISOString().slice(0, 10)}.json`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 2000);
      showToast("Workspace JSON backup exported.");
    } else if (action === "view-member") {
      const member = state.members.find((m) => m.id === actBtn.dataset.memberId);
      if (member) {
        openModal(`ISCEB Member Profile`, renderMemberDetailHtml(member));
      }
    } else if (action === "add-member" || action === "edit-member") {
      const member = action === "edit-member" ? state.members.find((m) => m.id === actBtn.dataset.memberId) : null;
      openModal(
        member ? "Edit ISCEB Member" : "Add ISCEB Member",
        `<form id="memberCrudForm" data-member-id="${escapeHtml(member?.id || "")}" style="display: grid; gap: 10px;">
          <input type="text" id="memName" class="input" placeholder="Full Name" value="${escapeHtml(member?.fullName || "")}" required />
          <input type="text" id="memDesig" class="input" placeholder="Designation" value="${escapeHtml(member?.designation || "")}" required />
          <input type="text" id="memOrg" class="input" placeholder="Organization / Role in ISCEB" value="${escapeHtml(member?.roleInIsceb || "")}" />
          <input type="text" id="memContact" class="input" placeholder="Email / Contact" value="${escapeHtml(member?.contactInformation || "")}" />
          <textarea id="memBio" class="textarea" rows="3" placeholder="Biography">${escapeHtml(member?.biography || "")}</textarea>
          <button type="submit" class="btn btn-primary">Save Member Profile</button>
        </form>`
      );
    } else if (action === "delete-member") {
      state.members = state.members.filter((m) => m.id !== actBtn.dataset.memberId);
      saveState();
      showToast("Member removed from directory.");
      render();
    } else if (action === "move-member-up") {
      const idx = state.members.findIndex((m) => m.id === actBtn.dataset.memberId);
      if (idx > 0) {
        [state.members[idx - 1], state.members[idx]] = [state.members[idx], state.members[idx - 1]];
        state.members.forEach((m, i) => { m.displayOrder = i + 1; });
        saveState();
        render();
      }
    }
  });

  document.getElementById("mainContent").addEventListener("input", (e) => {
    if (e.target.id === "courseSearchInput") {
      state.searchQuery = e.target.value;
      saveState();
      render();
      const inp = document.getElementById("courseSearchInput");
      if (inp) {
        inp.focus();
        inp.setSelectionRange(inp.value.length, inp.value.length);
      }
    }
  });

  document.getElementById("mainContent").addEventListener("change", (e) => {
    if (e.target.id === "attachLocalFileInput") {
      const cid = e.target.dataset.classId;
      const file = e.target.files?.[0];
      if (!file) return;
      const attId = `att-${Date.now()}`;
      localAttachmentUrls.set(attId, URL.createObjectURL(file));
      const cur = getClassProgress(cid);
      updateClassProgress(cid, {
        localAttachments: [...(cur.localAttachments || []), { id: attId, name: file.name, size: file.size, type: file.type }]
      });
      showToast(`Attached ${file.name} for PC viewing.`);
      render();
    } else if (e.target.id === "profilePhotoInput") {
      const file = e.target.files?.[0];
      e.target.value = "";
      if (!file) return;
      resizePhotoFileToDataUrl(file).then((dataUrl) => {
        pendingStudentPhotoDataUrl = dataUrl;
        const wrap = document.getElementById("profilePhotoPreviewWrap");
        if (wrap && dataUrl) {
          wrap.innerHTML = `<img id="profilePhotoPreviewImg" src="${escapeHtml(dataUrl)}" alt="Profile Preview" style="width: 100%; height: 100%; object-fit: cover;" />`;
        }
        showToast("Profile photo attached.");
      }).catch(() => {
        showToast("Could not load selected photo.", true);
      });
    } else if (e.target.id === "importWorkspaceJsonInput") {
      const file = e.target.files?.[0];
      e.target.value = "";
      if (!file) return;
      file.text().then((raw) => {
        const parsed = JSON.parse(raw);
        if (!parsed || typeof parsed !== "object") throw new Error("Invalid backup file");
        if (Array.isArray(parsed.customStudents)) {
          state.customStudents = parsed.customStudents;
        }
        if (parsed.progressByUser && typeof parsed.progressByUser === "object") {
          state.progressByUser = { ...state.progressByUser, ...parsed.progressByUser };
        }
        if (parsed.examAttemptsByUser && typeof parsed.examAttemptsByUser === "object") {
          state.examAttemptsByUser = { ...state.examAttemptsByUser, ...parsed.examAttemptsByUser };
        }
        if (parsed.assignmentSubmissionsByUser && typeof parsed.assignmentSubmissionsByUser === "object") {
          state.assignmentSubmissionsByUser = { ...state.assignmentSubmissionsByUser, ...parsed.assignmentSubmissionsByUser };
        }
        if (parsed.certificatesByUser && typeof parsed.certificatesByUser === "object") {
          state.certificatesByUser = { ...state.certificatesByUser, ...parsed.certificatesByUser };
        }
        if (parsed.activeUserId && parsed.activeUserId !== ADMIN_AMIRUL_ID && parsed.activeUserId !== ADMIN_NAFIUL_ID) {
          state.activeUserId = parsed.activeUserId;
        }
        saveState();
        syncAccountSwitcherOptions();
        render();
        showToast("Workspace JSON backup restored successfully.");
      }).catch((err) => {
        showToast(`Backup restore failed: ${err?.message || "Invalid JSON"}`, true);
      });
    } else if (e.target.id === "adminMultiMediaUploadInput" || e.target.classList.contains("adminTypedMediaInput")) {
      const cid = e.target.dataset.classId;
      const forcedType = e.target.dataset.mediaType || "";
      const files = Array.from(e.target.files || []);
      e.target.value = "";
      if (!files.length) return;
      handleAdminMediaFilesSelected(cid, forcedType, files);
    }
  });

  document.getElementById("mainContent").addEventListener("submit", (e) => {
    if (e.target.id === "lessonQuizForm") {
      e.preventDefault();
      const cid = e.target.dataset.classId;
      const cls = state.classes.find((c) => c.id === cid);
      const quiz = generateClassQuiz(cls);
      let correctCount = 0;
      quiz.forEach((qItem, idx) => {
        const picked = e.target.querySelector(`input[name="lq_${idx}"]:checked`)?.value;
        if (Number(picked) === qItem.correct) correctCount += 1;
      });
      const score = Math.round((correctCount / quiz.length) * 100);
      const passed = score >= 50;
      updateClassProgress(cid, { quizScore: score, quizPassed: passed, completed: passed ? true : getClassProgress(cid).completed });
      showToast(`Lesson Mastery Check: ${score}% (${passed ? "Passed & Complete" : "Review Notes"})`, !passed);
      render();
    } else if (e.target.id === "adminAttachRemoteUrlForm") {
      e.preventDefault();
      if (!isAdminUser()) {
        showToast("Admin authentication required.", true);
        return;
      }
      const cid = e.target.dataset.classId;
      const cls = state.classes.find((c) => c.id === cid);
      if (!cls) return;
      const rawUrl = (document.getElementById("adminRemoteMediaUrl")?.value || "").trim();
      const mediaType = document.getElementById("adminRemoteMediaType")?.value || "PDF";
      const customTitle = (document.getElementById("adminRemoteMediaTitle")?.value || "").trim();
      if (!/^https?:\/\//i.test(rawUrl)) {
        showToast("Enter a valid HTTPS link starting with https://", true);
        return;
      }
      const derivedName = rawUrl.split("?")[0].split("/").pop() || `material.${mediaType.toLowerCase()}`;
      const mediaObj = {
        id: `link-${Date.now()}`,
        type: mediaType,
        fileName: derivedName,
        fileSizeFormatted: "Cloud Link",
        fileSizeBytes: 0,
        url: rawUrl,
        uploadTimestamp: Date.now(),
        title: customTitle || derivedName,
        description: "",
        mimeType: inferMediaMimeType({ type: mediaType, fileName: derivedName }),
        cloudFileId: "",
        cloudChunkCount: 0
      };
      if (mediaType === "PDF") cls.pdfMedia = mediaObj;
      if (mediaType === "AUDIO") cls.audioMedia = mediaObj;
      if (mediaType === "VIDEO") cls.videoMedia = mediaObj;
      cls.attachments = [...(cls.attachments || []), mediaObj];
      saveState();
      if (cloudUser) {
        pushCurriculumToFirestore({
          versionTag: state.manifest.versionTag || "v1.6.0",
          versionCode: Number(state.manifest.versionCode || 105) + 1,
          releaseNotes: `Attached HTTPS ${mediaType} to Lecture ${cls.lectureCode}`,
          announcement: state.manifest.announcement || "",
          apkVersionName: state.manifest.latestApkVersionName || "1.6",
          apkVersionCode: state.manifest.latestApkVersionCode || 7,
          apkUrl: state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl
        });
      } else {
        showToast("Attached link locally. Sign in with 'Google Cloud Sync' to publish to student phones.");
        render();
      }
    } else if (e.target.id === "adminPublishForm") {
      e.preventDefault();
      if (!isAdminUser()) {
        showToast("Admin authentication required.", true);
        return;
      }
      pushCurriculumToFirestore({
        versionTag: document.getElementById("admVerTag").value.trim(),
        versionCode: document.getElementById("admVerCode").value,
        releaseNotes: document.getElementById("admNotes").value.trim(),
        announcement: document.getElementById("admAnnouncement").value.trim(),
        apkVersionName: document.getElementById("admApkVerName").value.trim(),
        apkVersionCode: document.getElementById("admApkVerCode").value,
        apkUrl: document.getElementById("admApkUrl").value.trim()
      });
    } else if (e.target.id === "desktopAdminSetupForm") {
      e.preventDefault();
      const pw = document.getElementById("deskSetupPw").value;
      const confirmPw = document.getElementById("deskSetupConfirmPw").value;
      const invite = document.getElementById("deskAdminInviteCode")?.value || "";
      desktopAdminAuth.setupInitialPassword(selectedLoginAdminId, pw, confirmPw, invite).then((res) => {
        if (res.ok) {
          verifiedAdminSessionId = res.adminId;
          lastIssuedSecretNotice = `One-Time Recovery Key (save securely): ${res.recoveryCode}`;
          showToast(res.message);
        } else {
          showToast(res.error, true);
        }
        render();
      });
    } else if (e.target.id === "desktopAdminLoginForm") {
      e.preventDefault();
      const pw = document.getElementById("deskLoginPw").value;
      desktopAdminAuth.authenticate(selectedLoginAdminId, pw).then((res) => {
        if (res.ok) {
          verifiedAdminSessionId = res.adminId;
          lastIssuedSecretNotice = "";
          showToast(res.message);
        } else {
          showToast(res.error, true);
        }
        render();
      });
    } else if (e.target.id === "desktopAdminResetForm") {
      e.preventDefault();
      const rec = document.getElementById("deskResetRecoveryCode").value;
      const npw = document.getElementById("deskResetNewPw").value;
      const cpw = document.getElementById("deskResetConfirmPw").value;
      const verifiedEmail = cloudUser?.emailVerified ? cloudUser.email : "";
      desktopAdminAuth.resetPasswordWithRecoveryOrGoogle(selectedLoginAdminId, verifiedEmail, rec, npw, cpw).then((res) => {
        if (res.ok) {
          verifiedAdminSessionId = res.adminId;
          lastIssuedSecretNotice = `New One-Time Recovery Key: ${res.recoveryCode}`;
          desktopAdminAuthMode = "login";
          showToast(res.message);
        } else {
          showToast(res.error, true);
        }
        render();
      });
    } else if (e.target.id === "studentRegisterForm") {
      e.preventDefault();
      registerOrSignInCustomStudent({
        fullName: document.getElementById("regStudentName")?.value || "",
        email: document.getElementById("regStudentEmail")?.value || "",
        mobileNumber: document.getElementById("regStudentMobile")?.value || "",
        organization: document.getElementById("regStudentOrg")?.value || "",
        enrolledProgram: document.getElementById("regStudentProgram")?.value || GUEST_USER.enrolledProgram,
        photoUrl: pendingStudentPhotoDataUrl
      });
    } else if (e.target.id === "desktopAdminChangePasswordForm") {
      e.preventDefault();
      const user = getActiveUser();
      const curPw = document.getElementById("deskChangeCurrentPw").value;
      const newPw = document.getElementById("deskChangeNewPw").value;
      const conPw = document.getElementById("deskChangeConfirmPw").value;
      desktopAdminAuth.changeOwnPassword(user.id, curPw, newPw, conPw).then((res) => {
        if (res.ok) {
          lastIssuedSecretNotice = `New One-Time Recovery Key: ${res.recoveryCode}`;
          showToast(res.message);
        } else {
          showToast(res.error, true);
        }
        render();
      });
    }
  });

  document.getElementById("modalBody").addEventListener("submit", (e) => {
    e.preventDefault();
    if (e.target.id === "activeExamForm") {
      submitActiveExam();
    } else if (e.target.id === "submitAssignmentForm") {
      const asgId = e.target.dataset.asgId;
      const uid = getActiveUser().id;
      if (!state.assignmentSubmissionsByUser[uid]) state.assignmentSubmissionsByUser[uid] = {};
      state.assignmentSubmissionsByUser[uid][asgId] = {
        text: document.getElementById("asgSubmissionText").value.trim(),
        submittedAt: Date.now()
      };
      saveState();
      closeModal();
      showToast("Assignment submitted successfully.");
      render();
    } else if (e.target.id === "gradeAssignmentForm") {
      const asgId = e.target.dataset.asgId;
      const uid = getActiveUser().id;
      const grade = Number(document.getElementById("asgGradeValue").value || 0);
      if (state.assignmentSubmissionsByUser[uid]?.[asgId]) {
        state.assignmentSubmissionsByUser[uid][asgId].grade = grade;
        saveState();
      }
      closeModal();
      showToast(`Assignment graded: ${grade} marks.`);
      render();
    } else if (e.target.id === "memberCrudForm") {
      const existingId = e.target.dataset.memberId;
      const record = {
        id: existingId || `member-${Date.now()}`,
        fullName: document.getElementById("memName").value.trim(),
        designation: document.getElementById("memDesig").value.trim(),
        roleInIsceb: document.getElementById("memOrg").value.trim(),
        contactInformation: document.getElementById("memContact").value.trim(),
        biography: document.getElementById("memBio").value.trim(),
        displayOrder: existingId ? state.members.find((m) => m.id === existingId)?.displayOrder || 9 : state.members.length + 1
      };
      if (existingId) {
        state.members = state.members.map((m) => (m.id === existingId ? { ...m, ...record } : m));
      } else {
        state.members.push(record);
      }
      saveState();
      closeModal();
      showToast("ISCEB Directory updated.");
      render();
    } else if (e.target.id === "editStudentProfileForm") {
      const cur = getActiveUser();
      if (isGuestUser() || isAdminUser()) return;
      const updatedName = document.getElementById("editStuName").value.trim();
      const updatedEmail = document.getElementById("editStuEmail").value.trim();
      const updatedMobile = document.getElementById("editStuMobile").value.trim();
      const updatedOrg = document.getElementById("editStuOrg").value.trim();
      const updatedProgram = document.getElementById("editStuProgram").value.trim();
      if (!updatedName || !updatedEmail.includes("@")) {
        showToast("Please enter a valid name and email.", true);
        return;
      }
      const existingIdx = (state.customStudents || []).findIndex((s) => s.id === cur.id);
      const updatedRecord = {
        ...cur,
        fullName: updatedName,
        email: updatedEmail,
        mobileNumber: updatedMobile,
        organization: updatedOrg,
        enrolledProgram: updatedProgram || GUEST_USER.enrolledProgram
      };
      if (existingIdx >= 0) {
        state.customStudents[existingIdx] = updatedRecord;
      } else {
        state.customStudents = [updatedRecord, ...(state.customStudents || [])];
      }
      state.activeUserId = updatedRecord.id;
      saveState();
      closeModal();
      syncAccountSwitcherOptions();
      showToast("Student profile updated.");
      render();
    }
  });
}

async function handleAdminMediaFilesSelected(cid, forcedType, files) {
  if (!isAdminUser()) {
    showToast("Admin login required to upload class media.", true);
    return;
  }
  const cls = state.classes.find((c) => c.id === cid);
  if (!cls) return;

  const fb = await initFirebaseSdk();
  const canCloudSync = Boolean(fb && cloudUser);
  const newMediaList = [];
  let cloudSyncedCount = 0;
  let largeLocalOnlyCount = 0;
  let cloudErrorMsg = "";

  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    const lower = file.name.toLowerCase();
    const inferred = lower.endsWith(".pdf")
      ? "PDF"
      : (forcedType || (
        file.type.startsWith("image/") ? "IMAGE"
          : file.type.startsWith("audio/") ? "AUDIO"
          : file.type.startsWith("video/") ? "VIDEO"
          : "DOCUMENT"
      ));
    const mediaId = `pc-media-${Date.now()}-${i}`;
    localAttachmentUrls.set(mediaId, URL.createObjectURL(file));

    let cloudFileId = "";
    let cloudChunkCount = 0;

    if (canCloudSync && file.size > 0 && file.size <= MAX_CLOUD_FILE_BYTES) {
      try {
        showToast(`Uploading ${file.name} (${i + 1}/${files.length}) to Firebase Firestore...`);
        const rawBytes = new Uint8Array(await file.arrayBuffer());
        const candidateId = `pc_${cid}_${i}_${Date.now()}`.replace(/[^a-zA-Z0-9_-]/g, "_");
        const { doc, setDoc, serverTimestamp } = fb.fsMod;
        const chunks = splitBytesIntoLessonFileChunks({
          fileId: candidateId,
          bytes: rawBytes,
          authorUid: cloudUser.uid,
          serverTimestampToken: serverTimestamp()
        });
        for (const chunk of chunks) {
          await setDoc(doc(fb.db, "lesson_files", chunk.chunkId), chunk.payload);
        }
        cloudFileId = candidateId;
        cloudChunkCount = chunks.length;
        cloudSyncedCount += 1;
      } catch (err) {
        cloudErrorMsg = err?.message || "Firestore chunk upload denied";
      }
    } else if (file.size > MAX_CLOUD_FILE_BYTES) {
      largeLocalOnlyCount += 1;
    }

    const mediaObj = {
      id: mediaId,
      type: inferred,
      title: file.name.replace(/\.[^.]+$/, ""),
      fileName: file.name,
      fileSizeBytes: file.size,
      fileSizeFormatted: formatBytes(file.size),
      url: "",
      uploadTimestamp: Date.now(),
      description: "",
      mimeType: file.type || inferMediaMimeType({ type: inferred, fileName: file.name }),
      cloudFileId,
      cloudChunkCount
    };
    if (inferred === "PDF") cls.pdfMedia = mediaObj;
    if (inferred === "AUDIO") cls.audioMedia = mediaObj;
    if (inferred === "VIDEO") cls.videoMedia = mediaObj;
    newMediaList.push(mediaObj);
  }

  cls.attachments = [...(cls.attachments || []), ...newMediaList];
  saveState();

  if (canCloudSync && cloudSyncedCount > 0) {
    await pushCurriculumToFirestore({
      versionTag: state.manifest.versionTag || "v1.6.0",
      versionCode: Number(state.manifest.versionCode || 105) + 1,
      releaseNotes: `Uploaded ${cloudSyncedCount} cloud study file(s) to Lecture ${cls.lectureCode}`,
      announcement: state.manifest.announcement || "",
      apkVersionName: state.manifest.latestApkVersionName || "1.6",
      apkVersionCode: state.manifest.latestApkVersionCode || 7,
      apkUrl: state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl
    });
  } else if (cloudErrorMsg) {
    showToast(`Saved for PC only — Firestore sync failed: ${cloudErrorMsg}. Sign in with Admin Gmail (www.mdnafiulislam@gmail.com).`, true);
    render();
  } else if (!canCloudSync) {
    showToast(`Attached ${files.length} file(s) on this PC. Click 'Google Cloud Sync' to upload for student phones.`);
    render();
  } else if (largeLocalOnlyCount > 0) {
    showToast(`File(s) >18 MB cannot sync via Firestore chunks. Use 'Attach HTTPS Link' for student phones.`, true);
    render();
  } else {
    render();
  }
}

setupListeners();
rehydrateVerifiedAdminSession();
render();
fetchVersionJson();
initFirebaseSdk();
