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
  buildFirestoreUserAccountPayload,
  isCloudSyncedMedia,
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
    leftSignerName: "Hasina Akter Khan",
    leftSignerTitle: "Chairman & CEO",
    authorizedSignerName: "Md. Amirul Islam",
    authorizedSignerTitle: "Founder of ISCEB",
    ...(raw || {})
  };
  if (typeof s.certificatesEnabled !== "boolean") s.certificatesEnabled = true;
  if (typeof s.demoCertificateEnabled !== "boolean") s.demoCertificateEnabled = true;
  if (!s.leftSignerName || /nafiul|amirul/i.test(s.leftSignerName) || /^hasina khan$/i.test(s.leftSignerName.trim())) {
    s.leftSignerName = "Hasina Akter Khan";
  }
  if (!s.leftSignerTitle || /developer/i.test(s.leftSignerTitle)) {
    s.leftSignerTitle = "Chairman & CEO";
  }
  if (!s.authorizedSignerName || /nafiul|hasina/i.test(s.authorizedSignerName) || /^amirul islam$/i.test(s.authorizedSignerName.trim())) {
    s.authorizedSignerName = "Md. Amirul Islam";
  }
  if (!s.authorizedSignerTitle || /developer|academic directorate/i.test(s.authorizedSignerTitle)) {
    s.authorizedSignerTitle = "Founder of ISCEB";
  }
  return s;
}

function sanitizeDeveloperProfile(raw, forceUpgrade = false) {
  const defaults = structuredClone(DEFAULT_DEVELOPER);
  const dev = { ...defaults, ...(raw || {}) };
  if (dev.biography) {
    dev.biography = dev.biography.replaceAll(
      "International Supply Chain Education Board",
      "Institute of Supply Chain Excellence Bangladesh"
    );
  }
  const hasLegacyBio =
    !raw ||
    !raw.biography ||
    raw.biography.startsWith("App Developer for ISCEB") ||
    !raw.headline ||
    !raw.personalPhilosophy;
  if (forceUpgrade || hasLegacyBio) {
    return {
      ...defaults,
      contactEmail: dev.contactEmail || defaults.contactEmail,
      location: dev.location || defaults.location
    };
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
        developerProfile: sanitizeDeveloperProfile(
          parsed.developerProfile,
          savedRosterVer < DEFAULT_MEMBERS_ROSTER_VERSION
        ),
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
let cloudRegisteredUsers = [];
let cloudUsersLoadedAt = 0;
let cloudUsersAccessNote = "";
let adminStudentSearch = "";
let firebaseModules = null;
let firebaseInitPromise = null;
let liveCloudUnsubscribers = [];
let lastAutoResumeSyncMs = 0;
let isSyncingPendingMedia = false;
let deferredInstallPrompt = null;
let activeTimerInterval = null;
let examCountdownInterval = null;
let examModalSession = null;
let demoCertificateActive = false;
let courseDetailOpen = false;
let pendingMediaAfterSignIn = null;
let selectedLoginAdminId = ADMIN_AMIRUL_ID;
let desktopAdminAuthMode = "login"; // login | setup | reset
let lastIssuedSecretNotice = "";
let verifiedAdminSessionId = null;
let pendingStudentPhotoDataUrl = "";
let pendingEditStudentPhotoDataUrl = "";
let pendingEditStudentPhotoCleared = false;
const localAttachmentUrls = new Map();
const localAttachmentBlobs = new Map();
const cachedMediaIdbKeys = new Set();

const MEDIA_IDB_NAME = "isceb_media_blobs";
const MEDIA_IDB_STORE = "blobs";

function openMediaIdb() {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === "undefined") {
      reject(new Error("IndexedDB unavailable"));
      return;
    }
    const req = indexedDB.open(MEDIA_IDB_NAME, 1);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(MEDIA_IDB_STORE)) {
        db.createObjectStore(MEDIA_IDB_STORE, { keyPath: "key" });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error || new Error("IndexedDB open error"));
  });
}

async function saveMediaBlobToIdb(key, blob, meta = {}) {
  if (!key || !blob) return;
  localAttachmentBlobs.set(key, blob);
  cachedMediaIdbKeys.add(key);
  try {
    const db = await openMediaIdb();
    await new Promise((resolve, reject) => {
      const tx = db.transaction(MEDIA_IDB_STORE, "readwrite");
      tx.objectStore(MEDIA_IDB_STORE).put({
        key,
        blob,
        mimeType: blob.type || meta.mimeType || "application/octet-stream",
        fileName: meta.fileName || "",
        savedAt: Date.now()
      });
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  } catch (_) {}
}

async function loadMediaBlobFromIdb(key) {
  if (!key) return null;
  if (localAttachmentBlobs.has(key)) return localAttachmentBlobs.get(key);
  try {
    const db = await openMediaIdb();
    const record = await new Promise((resolve, reject) => {
      const tx = db.transaction(MEDIA_IDB_STORE, "readonly");
      const req = tx.objectStore(MEDIA_IDB_STORE).get(key);
      req.onsuccess = () => resolve(req.result || null);
      req.onerror = () => reject(req.error);
    });
    if (record?.blob) {
      localAttachmentBlobs.set(key, record.blob);
      return record.blob;
    }
  } catch (_) {}
  return null;
}

async function findMediaBlobForAttachmentInIdb(media) {
  if (!media) return null;
  const byId = await loadMediaBlobFromIdb(media.id);
  if (byId) return byId;
  if (media.cloudFileId) {
    const byCloudId = await loadMediaBlobFromIdb(media.cloudFileId);
    if (byCloudId) return byCloudId;
  }
  const targetName = String(media.fileName || "").trim();
  const targetSize = Number(media.fileSizeBytes || 0);
  if (!targetName) return null;
  try {
    const db = await openMediaIdb();
    const allRecords = await new Promise((resolve, reject) => {
      const tx = db.transaction(MEDIA_IDB_STORE, "readonly");
      const req = tx.objectStore(MEDIA_IDB_STORE).getAll();
      req.onsuccess = () => resolve(Array.isArray(req.result) ? req.result : []);
      req.onerror = () => reject(req.error);
    });
    const matched = allRecords.find(
      (rec) =>
        rec?.blob &&
        String(rec.fileName || "").trim() === targetName &&
        (!targetSize || Math.abs(Number(rec.blob.size || 0) - targetSize) <= 64)
    );
    if (matched?.blob) {
      if (media.id) localAttachmentBlobs.set(media.id, matched.blob);
      return matched.blob;
    }
  } catch (_) {}
  return null;
}

async function warmMediaIdbKeysCache() {
  try {
    const db = await openMediaIdb();
    const keys = await new Promise((resolve, reject) => {
      const tx = db.transaction(MEDIA_IDB_STORE, "readonly");
      const req = tx.objectStore(MEDIA_IDB_STORE).getAllKeys();
      req.onsuccess = () => resolve(Array.isArray(req.result) ? req.result : []);
      req.onerror = () => reject(req.error);
    });
    keys.forEach((k) => cachedMediaIdbKeys.add(String(k)));
  } catch (_) {}
}

function saveState() {
  try {
    const persisted = {
      ...state,
      activeUserId: isAdminUser() ? GUEST_USER.id : state.activeUserId
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(persisted));
  } catch (_) {}
}

function isRunningStandalone() {
  try {
    return (
      window.matchMedia?.("(display-mode: standalone)")?.matches === true ||
      window.navigator?.standalone === true
    );
  } catch (_) {
    return false;
  }
}

async function triggerPwaInstall() {
  if (deferredInstallPrompt) {
    try {
      deferredInstallPrompt.prompt();
      const choice = await deferredInstallPrompt.userChoice;
      deferredInstallPrompt = null;
      const btn = document.getElementById("installPwaBtn");
      if (btn) btn.style.display = "none";
      if (choice?.outcome === "accepted") {
        showToast("Installing ISCEB standalone app (no Chrome badge)...");
      }
      render();
      return;
    } catch (_) {}
  }

  openModal(
    "Install ISCEB App (No Chrome Badge)",
    `<div style="display: grid; gap: 10px; font-size: 13.5px; color: var(--text);">
      <div style="background: #EFF6FF; border: 1px solid #BFDBFE; border-radius: 10px; padding: 12px; color: #1E3A8A;">
        <strong>Why shortcuts sometimes show a Chrome logo:</strong><br/>
        Chrome has two different options under <strong>Add to Home screen</strong>: <em>Install</em> creates a standalone ISCEB app icon (no Chrome badge), while <em>Create shortcut</em> always adds a Chrome badge.
      </div>
      <ol style="margin: 0; padding-left: 18px; display: grid; gap: 6px;">
        <li>Open this exact link in <strong>Google Chrome</strong>: <code>https://mdnafiulislam1234.github.io/isceb-app-live/desktop/</code></li>
        <li>If an old Chrome-badged ISCEB shortcut exists on your Home Screen, delete it first.</li>
        <li>Tap Chrome menu <strong>&#8942;</strong> (top-right) &rarr; <strong>Add to Home screen</strong> &rarr; tap <strong>Install</strong> (NOT <em>Create shortcut</em>).</li>
      </ol>
      <div style="font-size: 12.5px; color: var(--muted);">
        Once installed this way, ISCEB opens full-screen without the Chrome bar and updates automatically whenever you push changes to GitHub.
      </div>
    </div>`
  );
}

function renderPwaInstallBanner() {
  if (isRunningStandalone()) return "";
  const promptReady = Boolean(deferredInstallPrompt);
  return `
    <div class="card" style="background: linear-gradient(135deg, #FFFDF5 0%, #FEF6DA 100%); border: 1.5px solid var(--gold); padding: 14px 16px;">
      <div class="profile-action-row">
        <div style="display: flex; align-items: center; gap: 12px; flex: 1 1 220px; min-width: 0;">
          <img src="./isceb-icon-192.png" alt="ISCEB App Icon" style="width: 44px; height: 44px; border-radius: 12px; border: 1px solid var(--gold); background: #fff; flex-shrink: 0;" />
          <div style="min-width: 0;">
            <div style="font-weight: 800; color: var(--navy); font-size: 14px;">
              ${promptReady ? "Install ISCEB Standalone App (No Chrome Badge)" : "Add ISCEB as Full-Screen App (Auto-Updates)"}
            </div>
            <div style="font-size: 12px; color: #5C4710; line-height: 1.4; margin-top: 2px;">
              ${
                promptReady
                  ? "One tap installs the clean ISCEB app icon directly on your Home Screen and auto-updates after every GitHub push."
                  : "In Chrome <strong>&#8942; &rarr; Add to Home screen</strong>, tap <strong>Install</strong> (never <em>Create shortcut</em>) for a clean icon with zero Chrome badge."
              }
            </div>
          </div>
        </div>
        <button type="button" class="btn ${promptReady ? "btn-gold" : "btn-primary"} btn-sm" data-action="trigger-pwa-install" style="white-space: nowrap;">
          ${promptReady ? "Install ISCEB App" : "How to Install"}
        </button>
      </div>
    </div>
  `;
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
  const createdOrUpdated = (state.customStudents || []).find((s) => s.id === state.activeUserId);
  if (createdOrUpdated && cloudUser) {
    syncStudentAccountToFirestore(createdOrUpdated);
  }
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

let pdfJsLibPromise = null;

async function getPdfJsLib() {
  if (pdfJsLibPromise) return pdfJsLibPromise;
  pdfJsLibPromise = (async () => {
    const pdfjsLib = await import("https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.8.69/pdf.min.mjs");
    if (pdfjsLib?.GlobalWorkerOptions) {
      pdfjsLib.GlobalWorkerOptions.workerSrc = "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.8.69/pdf.worker.min.mjs";
    }
    return pdfjsLib;
  })();
  return pdfJsLibPromise;
}

function renderPdfPageToCanvasCrisp(page, canvas, targetWidthCssPx, zoomRatio = 1) {
  const unscaled = page.getViewport({ scale: 1 });
  const baseScale = targetWidthCssPx / Math.max(unscaled.width, 1);
  const dpr = Math.min(3, Math.max(window.devicePixelRatio || 1, 1.5));
  const rawPixelScale = baseScale * dpr * Math.max(1, zoomRatio);
  // Keep total pixels safely within mobile browser canvas limits while staying ultra-sharp
  const maxDim = 4096;
  const maxScaleW = maxDim / Math.max(unscaled.width, 1);
  const maxScaleH = maxDim / Math.max(unscaled.height, 1);
  const renderScale = Math.max(1.8, Math.min(rawPixelScale, maxScaleW, maxScaleH));
  const viewport = page.getViewport({ scale: renderScale });
  canvas.width = Math.round(viewport.width);
  canvas.height = Math.round(viewport.height);
  const ctx = canvas.getContext("2d");
  return page.render({ canvasContext: ctx, viewport }).promise;
}

async function renderPdfInlineIntoHost(hostEl, resolvedUrl, blob, zoomControls = null) {
  if (!hostEl) return;
  try {
    const pdfjsLib = await getPdfJsLib();
    const bytes = blob
      ? new Uint8Array(await blob.arrayBuffer())
      : new Uint8Array(await (await fetch(resolvedUrl)).arrayBuffer());
    const pdf = await pdfjsLib.getDocument({ data: bytes }).promise;
    const stage = document.createElement("div");
    stage.className = "pdf-canvas-stage";
    hostEl.innerHTML = `<div style="font-size: 12px; font-weight: 700; color: var(--navy); padding: 2px 4px 8px;">PDF Document • ${pdf.numPages} Page(s) • Pinch or tap + to zoom</div>`;
    hostEl.appendChild(stage);

    const maxPages = Math.min(pdf.numPages, 40);
    const baseCssWidth = Math.max(hostEl.clientWidth - 24, 320);
    const pageRecords = [];
    let renderChain = Promise.resolve();

    for (let pageNum = 1; pageNum <= maxPages; pageNum++) {
      const page = await pdf.getPage(pageNum);
      const canvas = document.createElement("canvas");
      stage.appendChild(canvas);
      pageRecords.push({ page, canvas });
      await renderPdfPageToCanvasCrisp(page, canvas, baseCssWidth, 1.35);
    }

    if (zoomControls) {
      let currentZoom = 100;
      const applyZoom = (nextZoom) => {
        currentZoom = Math.max(100, Math.min(500, nextZoom));
        stage.style.width = `${currentZoom}%`;
        if (zoomControls.labelEl) zoomControls.labelEl.textContent = `${currentZoom}%`;
        const zoomFactor = currentZoom / 100;
        renderChain = renderChain.then(async () => {
          for (const rec of pageRecords) {
            if (!document.body.contains(rec.canvas)) return;
            await renderPdfPageToCanvasCrisp(rec.page, rec.canvas, baseCssWidth, Math.max(1.35, zoomFactor));
          }
        }).catch(() => {});
      };
      zoomControls.onZoomChange = applyZoom;
      applyZoom(100);
    }

    if (pdf.numPages > maxPages) {
      const note = document.createElement("div");
      note.style.cssText = "font-size: 12px; color: var(--muted); text-align: center; padding: 6px;";
      note.textContent = `Showing first ${maxPages} of ${pdf.numPages} pages. Tap 'Download File' below for full document.`;
      hostEl.appendChild(note);
    }
  } catch (_) {
    hostEl.innerHTML = `
      <iframe src="${escapeHtml(resolvedUrl)}" title="PDF Preview" style="width: 100%; height: 52vh; border: 1px solid var(--border); border-radius: 8px; background: #fff;"></iframe>
      <div style="font-size: 12px; color: var(--muted); text-align: center; padding: 6px 4px;">
        If your mobile browser shows a blank frame, tap <strong>Download File</strong> or <strong>Open in New Tab</strong> below to view in your PDF reader.
      </div>
    `;
  }
}

function presentMediaPreviewModal(media, resolvedUrl, blob = null) {
  const title = media.title || media.fileName || "Study Material";
  const fileName = media.fileName || "study-material";
  const upperType = String(media.type || "").toUpperCase();
  const safeUrl = escapeHtml(resolvedUrl);
  const safeName = escapeHtml(fileName);
  const lowerName = fileName.toLowerCase();
  const isPdf = upperType === "PDF" || lowerName.endsWith(".pdf");
  const isImage = upperType === "IMAGE" || /\.(png|jpe?g|webp|gif|bmp)$/i.test(lowerName);

  const zoomToolbarHtml = (isPdf || isImage)
    ? `
      <div class="media-zoom-toolbar">
        <span style="font-size: 12px; font-weight: 700; color: var(--navy);">HD Zoom (Up to 500%)</span>
        <div class="media-zoom-controls">
          <button type="button" class="btn btn-outline btn-sm" id="mediaZoomOutBtn" aria-label="Zoom out">−</button>
          <span id="mediaZoomLabel" style="min-width: 46px; text-align: center; font-size: 12.5px; font-weight: 800; color: var(--navy);">100%</span>
          <button type="button" class="btn btn-outline btn-sm" id="mediaZoomInBtn" aria-label="Zoom in">+</button>
          <button type="button" class="btn btn-outline btn-sm" id="mediaZoomResetBtn">Fit</button>
        </div>
      </div>
    `
    : "";

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
  } else if (isImage) {
    previewBody = `
      ${zoomToolbarHtml}
      <div class="pdf-canvas-pages">
        <div id="imageZoomStage" class="pdf-canvas-stage">
          <img src="${safeUrl}" alt="${escapeHtml(title)}" style="width: 100%; height: auto; display: block; border-radius: 8px;" />
        </div>
      </div>
    `;
  } else if (isPdf) {
    previewBody = `
      ${zoomToolbarHtml}
      <div id="pdfCanvasHost" class="pdf-canvas-pages">
        <div style="text-align: center; padding: 24px 12px; font-size: 13px; font-weight: 700; color: var(--navy);">
          Rendering Crisp Vector PDF...
        </div>
      </div>
    `;
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

  const zoomOutBtn = document.getElementById("mediaZoomOutBtn");
  const zoomInBtn = document.getElementById("mediaZoomInBtn");
  const zoomResetBtn = document.getElementById("mediaZoomResetBtn");
  const zoomLabelEl = document.getElementById("mediaZoomLabel");

  if (isPdf) {
    const host = document.getElementById("pdfCanvasHost");
    const zoomController = { labelEl: zoomLabelEl, onZoomChange: null };
    let currentZoom = 100;
    const setPdfZoom = (next) => {
      currentZoom = Math.max(100, Math.min(500, next));
      if (zoomController.onZoomChange) zoomController.onZoomChange(currentZoom);
    };
    if (zoomInBtn) zoomInBtn.addEventListener("click", () => setPdfZoom(currentZoom + 75));
    if (zoomOutBtn) zoomOutBtn.addEventListener("click", () => setPdfZoom(currentZoom - 75));
    if (zoomResetBtn) zoomResetBtn.addEventListener("click", () => setPdfZoom(100));
    renderPdfInlineIntoHost(host, resolvedUrl, blob, zoomController);
  } else if (isImage) {
    const imgStage = document.getElementById("imageZoomStage");
    let currentZoom = 100;
    const setImgZoom = (next) => {
      currentZoom = Math.max(100, Math.min(500, next));
      if (imgStage) imgStage.style.width = `${currentZoom}%`;
      if (zoomLabelEl) zoomLabelEl.textContent = `${currentZoom}%`;
    };
    if (zoomInBtn) zoomInBtn.addEventListener("click", () => setImgZoom(currentZoom + 75));
    if (zoomOutBtn) zoomOutBtn.addEventListener("click", () => setImgZoom(currentZoom - 75));
    if (zoomResetBtn) zoomResetBtn.addEventListener("click", () => setImgZoom(100));
  }

  if (blob && /\.(txt|csv|md|json|log)$/i.test(lowerName)) {
    blob.text().then((txt) => {
      const target = document.getElementById("textPreviewContainer");
      if (target) {
        target.innerHTML = `<pre style="margin-top: 12px; max-height: 45vh; overflow: auto; background: #0f172a; color: #f8fafc; padding: 12px; border-radius: 10px; font-size: 12px; white-space: pre-wrap;">${escapeHtml(txt.slice(0, 20000))}</pre>`;
      }
    }).catch(() => {});
  }
}

async function uploadChunksWithRetry(fb, fileId, rawBytes, label = "file") {
  const { doc, setDoc, serverTimestamp } = fb.fsMod;
  const chunks = splitBytesIntoLessonFileChunks({
    fileId,
    bytes: rawBytes,
    authorUid: cloudUser.uid,
    serverTimestampToken: serverTimestamp()
  });
  for (let cIdx = 0; cIdx < chunks.length; cIdx++) {
    const chunk = chunks[cIdx];
    let lastErr = null;
    for (let attempt = 0; attempt < 3; attempt++) {
      try {
        if (chunks.length > 1 && (cIdx === 0 || (cIdx + 1) % 3 === 0 || cIdx === chunks.length - 1)) {
          showToast(`Syncing ${label} (${cIdx + 1}/${chunks.length} parts)...`);
        }
        await setDoc(doc(fb.db, "lesson_files", chunk.chunkId), chunk.payload);
        lastErr = null;
        break;
      } catch (err) {
        lastErr = err;
        const msg = String(err?.code || err?.message || "").toLowerCase();
        if (msg.includes("permission")) break;
        await new Promise((r) => setTimeout(r, 400 * (attempt + 1)));
      }
    }
    if (lastErr) throw lastErr;
  }
  return chunks.length;
}

async function downloadChunksInParallelWithRetry(fb, cloudFileId, chunkCount) {
  const { doc, getDoc } = fb.fsMod;
  const total = Number(chunkCount || 0);
  const results = new Array(total);
  const batchSize = 4;
  for (let start = 0; start < total; start += batchSize) {
    const end = Math.min(total, start + batchSize);
    const batchPromises = [];
    for (let i = start; i < end; i++) {
      batchPromises.push(
        (async (idx) => {
          let lastErr = null;
          for (let attempt = 0; attempt < 2; attempt++) {
            try {
              const snap = await getDoc(doc(fb.db, "lesson_files", `${cloudFileId}_${idx}`));
              if (!snap.exists()) {
                throw new Error(`Missing cloud chunk ${idx + 1}`);
              }
              const b64 = snap.data().dataBase64 || "";
              const bin = atob(b64);
              const arr = new Uint8Array(bin.length);
              for (let j = 0; j < bin.length; j++) arr[j] = bin.charCodeAt(j);
              results[idx] = arr;
              return;
            } catch (err) {
              lastErr = err;
              await new Promise((r) => setTimeout(r, 350 * (attempt + 1)));
            }
          }
          throw lastErr || new Error(`Failed loading chunk ${idx + 1}`);
        })(i)
      );
    }
    await Promise.all(batchPromises);
  }
  return results;
}

async function openSyncedCloudMedia(media) {
  if (!media) return;
  const rawUrl = String(media.url || "").trim();
  if (/^https?:\/\//i.test(rawUrl)) {
    presentMediaPreviewModal(media, rawUrl, null);
    return;
  }

  const cachedBlob = await findMediaBlobForAttachmentInIdb(media);
  if (cachedBlob) {
    let cachedUrl = localAttachmentUrls.get(media.id);
    if (!cachedUrl) {
      cachedUrl = URL.createObjectURL(cachedBlob);
      localAttachmentUrls.set(media.id, cachedUrl);
    }
    presentMediaPreviewModal(media, cachedUrl, cachedBlob);
    return;
  }

  const cachedUrl = localAttachmentUrls.get(media.id);
  if (cachedUrl) {
    presentMediaPreviewModal(media, cachedUrl, null);
    return;
  }

  if (!media.cloudFileId || !media.cloudChunkCount) {
    const adminRepairHtml = isAdminUser()
      ? `<div style="display: grid; gap: 8px; padding-top: 6px; border-top: 1px solid var(--border);">
          <label class="btn btn-gold btn-sm" style="cursor: pointer; text-align: center;">
            Select File &amp; Sync to Cloud Now
            <input type="file" id="repairBrokenMediaInput" data-media-id="${escapeHtml(media.id || "")}" data-media-title="${escapeHtml(media.title || media.fileName || "")}" style="display: none;" accept=".pdf,image/*,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,audio/*,video/*" />
          </label>
          <button type="button" class="btn btn-outline btn-sm" style="color: var(--err); border-color: rgba(185,28,28,0.35);" data-modal-action="remove-unsynced-media" data-media-id="${escapeHtml(media.id || "")}">
            Remove Broken Entry from Catalog
          </button>
        </div>`
      : "";
    openModal(
      media.title || media.fileName || "File Not Synced Yet",
      `<div style="display: grid; gap: 12px; font-size: 13.5px;">
        <div style="background: #FEF3C7; border: 1px solid #FDE68A; border-radius: 10px; padding: 12px; color: #92400E;">
          <strong>This file has not been synced to Firebase Cloud yet.</strong><br/>
          It was uploaded on the Admin's original device before Google Cloud Sync finished uploading its chunks.
        </div>
        <p style="margin: 0; color: var(--muted);">
          Ask the Admin to open <strong>Google Cloud Sync</strong> on the device where they uploaded it (or re-upload this file under 18 MB / attach an HTTPS link) so it syncs automatically to all student phones.
        </p>
        ${adminRepairHtml}
      </div>`
    );
    return;
  }

  const fb = await initFirebaseSdk();
  if (!fb || !cloudUser) {
    pendingMediaAfterSignIn = media;
    openModal(
      `Open ${media.fileName || "Study Material"}`,
      `<div style="display: grid; gap: 12px; font-size: 13.5px;">
        <p style="margin: 0; color: var(--text);">
          Sign in with your Google account once to securely download cloud-synced lecture materials (<strong>${escapeHtml(media.fileName || media.title || "PDF")}</strong>) from ISCEB Cloud.
        </p>
        <button type="button" class="btn btn-gold" data-action="signin-and-open-pending-media">
          Sign in with Google &amp; Open File
        </button>
      </div>`
    );
    return;
  }

  try {
    showToast(`Downloading ${media.fileName || "attachment"}...`);
    const chunks = await downloadChunksInParallelWithRetry(fb, media.cloudFileId, media.cloudChunkCount);
    const blob = new Blob(chunks, { type: inferMediaMimeType(media) });
    if (media.id) await saveMediaBlobToIdb(media.id, blob, { fileName: media.fileName, mimeType: blob.type });
    await saveMediaBlobToIdb(media.cloudFileId, blob, { fileName: media.fileName, mimeType: blob.type });
    const objUrl = URL.createObjectURL(blob);
    localAttachmentUrls.set(media.id || media.cloudFileId, objUrl);
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

function stopLiveCloudListeners() {
  while (liveCloudUnsubscribers.length) {
    const unsub = liveCloudUnsubscribers.pop();
    try {
      if (typeof unsub === "function") unsub();
    } catch (_) {}
  }
}

function startLiveCloudListeners(fb) {
  stopLiveCloudListeners();
  if (!fb || !fb.fsMod?.onSnapshot || !cloudUser) return;
  try {
    const { doc, onSnapshot } = fb.fsMod;
    const unsubManifest = onSnapshot(
      doc(fb.db, "curriculum_manifests", "live"),
      (snap) => {
        if (!snap.exists()) return;
        const m = snap.data();
        if (Number(m.versionCode || 0) >= Number(state.manifest.versionCode || 0)) {
          state.manifest = { ...state.manifest, ...m };
          saveState();
          updateChromeBadges();
        }
      },
      () => {}
    );
    const unsubCatalog = onSnapshot(
      doc(fb.db, "curriculum_catalogs", "live"),
      (snap) => {
        if (!snap.exists()) return;
        applyRemoteCatalogData(snap.data());
        saveState();
        updateChromeBadges();
        render();
      },
      () => {}
    );
    liveCloudUnsubscribers.push(unsubManifest, unsubCatalog);
  } catch (_) {}
}

async function initFirebaseSdk() {
  if (firebaseModules) {
    if (firebaseModules.auth?.authStateReady) {
      try {
        await firebaseModules.auth.authStateReady();
        cloudUser = firebaseModules.auth.currentUser || cloudUser;
      } catch (_) {}
    }
    return firebaseModules;
  }
  if (firebaseInitPromise) return firebaseInitPromise;

  firebaseInitPromise = (async () => {
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

      authMod.getRedirectResult(auth).catch(() => {});

      authMod.onAuthStateChanged(auth, async (user) => {
        cloudUser = user || null;
        if (!user) {
          stopLiveCloudListeners();
          cloudRegisteredUsers = [];
          cloudUsersLoadedAt = 0;
          cloudUsersAccessNote = "";
        }
        updateChromeBadges();
        if (user) {
          syncStudentAccountToFirestore(getActiveUser()).catch(() => {});
          if (isAdminUser()) {
            await syncAllPendingLocalMediaToCloud({ silentNoOp: true }).catch(() => {});
          }
          await syncFromCloudFirestore(true);
          startLiveCloudListeners(firebaseModules);
          if (pendingMediaAfterSignIn) {
            const mediaToOpen = pendingMediaAfterSignIn;
            pendingMediaAfterSignIn = null;
            openSyncedCloudMedia(mediaToOpen);
          }
        }
      });

      if (typeof auth.authStateReady === "function") {
        await auth.authStateReady();
        cloudUser = auth.currentUser || cloudUser;
        updateChromeBadges();
      }
      return firebaseModules;
    } catch (err) {
      console.warn("Firebase JS SDK unavailable offline:", err);
      firebaseInitPromise = null;
      return null;
    }
  })();

  return firebaseInitPromise;
}

async function syncStudentAccountToFirestore(studentProfile, options = {}) {
  const { allowEmptyPhoto = false } = options || {};
  const fb = await initFirebaseSdk();
  if (!fb || !cloudUser) return false;
  if (!studentProfile || studentProfile.id === GUEST_USER.id || studentProfile.role === "ADMIN" || isAdminUser()) {
    return false;
  }
  const authEmail = String(cloudUser.email || "").trim().toLowerCase();
  if (authEmail === "www.mdnafiulislam@gmail.com" || authEmail === "amirul.islam@isceb.org") {
    return false;
  }
  try {
    const { doc, getDoc, setDoc, updateDoc, serverTimestamp } = fb.fsMod;
    const ref = doc(fb.db, "users", cloudUser.uid);
    const snap = await getDoc(ref);
    const isCreate = !snap.exists();
    const existingRemotePhoto = snap.exists() ? String(snap.data()?.photoUrl || "") : "";
    const resolvedPhoto = allowEmptyPhoto
      ? ""
      : (studentProfile.photoUrl || existingRemotePhoto || cloudUser.photoURL || "");
    if (!allowEmptyPhoto && !studentProfile.photoUrl && existingRemotePhoto) {
      const idx = (state.customStudents || []).findIndex((s) => s.id === studentProfile.id);
      if (idx >= 0) {
        state.customStudents[idx] = { ...state.customStudents[idx], photoUrl: existingRemotePhoto };
        saveState();
      }
    }
    const payload = buildFirestoreUserAccountPayload({
      uid: cloudUser.uid,
      studentId: studentProfile.studentId || `ISCEB-2026-${cloudUser.uid.slice(0, 6).toUpperCase()}`,
      fullName: studentProfile.fullName || cloudUser.displayName || "ISCEB Learner",
      email: studentProfile.email || cloudUser.email,
      photoUrl: resolvedPhoto,
      organization: studentProfile.organization || "",
      designation: studentProfile.designation || "PGD Candidate",
      mobileNumber: studentProfile.mobileNumber || "",
      enrolledProgram: studentProfile.enrolledProgram || GUEST_USER.enrolledProgram,
      isCreate,
      serverTimestampToken: serverTimestamp()
    });
    if (isCreate) {
      await setDoc(ref, payload);
    } else {
      await updateDoc(ref, payload);
    }
    return true;
  } catch (err) {
    console.warn("Cloud student profile sync skipped:", err?.message || err);
    return false;
  }
}

async function fetchCloudRegisteredUsers(showResultToast = false) {
  const fb = await initFirebaseSdk();
  if (!fb || !cloudUser) {
    cloudRegisteredUsers = [];
    cloudUsersAccessNote = "Sign in with an authorized Admin Google account to view cloud-registered students.";
    if (showResultToast) showToast("Sign in with Google first to load cloud-registered students.", true);
    return [];
  }
  try {
    const { collection, getDocs } = fb.fsMod;
    const snap = await getDocs(collection(fb.db, "users"));
    const rows = snap.docs.map((d) => {
      const data = d.data() || {};
      return {
        uid: data.uid || d.id,
        studentId: data.studentId || "",
        fullName: data.fullName || "ISCEB Learner",
        email: data.email || "",
        mobileNumber: data.mobileNumber || "",
        organization: data.organization || "",
        designation: data.designation || "PGD Candidate",
        enrolledProgram: data.enrolledProgram || GUEST_USER.enrolledProgram,
        role: data.role || "STUDENT",
        accountStatus: data.accountStatus || "ACTIVE",
        isVerified: Boolean(data.isVerified),
        photoUrl: data.photoUrl || "",
        source: "CLOUD"
      };
    });
    rows.sort((a, b) => String(a.fullName || "").localeCompare(String(b.fullName || "")));
    cloudRegisteredUsers = rows;
    cloudUsersLoadedAt = Date.now();
    cloudUsersAccessNote = "";
    if (showResultToast) {
      const studentCount = rows.filter((r) => r.role === "STUDENT").length;
      showToast(`Loaded ${studentCount} registered student(s) from Firestore.`);
    }
    render();
    return rows;
  } catch (err) {
    cloudUsersAccessNote = "Admin Google account (www.mdnafiulislam@gmail.com or amirul.islam@isceb.org) required to list all students.";
    if (showResultToast) showToast(cloudUsersAccessNote, true);
    return [];
  }
}

async function signInWithGoogleCloud() {
  const fb = await initFirebaseSdk();
  if (!fb) {
    showToast("Firebase SDK requires an active internet connection.", true);
    return;
  }
  const provider = new fb.authMod.GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  try {
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
    } else if (!isGuestUser() && !isAdminUser()) {
      await syncStudentAccountToFirestore(getActiveUser());
    }
    showToast(`Cloud connected as ${cred.user.email}`);
    await syncFromCloudFirestore(false);
    if (pendingMediaAfterSignIn) {
      const mediaToOpen = pendingMediaAfterSignIn;
      pendingMediaAfterSignIn = null;
      openSyncedCloudMedia(mediaToOpen);
    }
    render();
  } catch (err) {
    const code = String(err?.code || "");
    if (code.includes("popup-blocked") || code.includes("operation-not-supported")) {
      try {
        showToast("Redirecting to Google Sign-In...");
        await fb.authMod.signInWithRedirect(fb.auth, provider);
        return;
      } catch (redirErr) {
        showToast(`Google Sign-In failed: ${redirErr.message || redirErr}`, true);
        return;
      }
    }
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

function mergeRemoteClassesSafely(remoteClasses) {
  if (!Array.isArray(remoteClasses) || !remoteClasses.length) return;
  const localById = new Map((state.classes || []).map((cls) => [cls.id, cls]));
  state.classes = remoteClasses.map((remoteCls) => {
    const localCls = localById.get(remoteCls.id);
    if (!localCls) return remoteCls;
    const remoteList = Array.isArray(remoteCls.attachments) ? remoteCls.attachments : [];
    const localPendingUnsynced = isAdminUser()
      ? (localCls.attachments || []).filter((lm) => {
          if (isCloudSyncedMedia(lm)) return false;
          if (!localAttachmentBlobs.has(lm.id) && !cachedMediaIdbKeys.has(lm.id)) return false;
          return !remoteList.some((rm) => rm.id === lm.id && isCloudSyncedMedia(rm));
        })
      : [];
    if (!localPendingUnsynced.length) return remoteCls;
    const mergedAttachments = [...remoteList, ...localPendingUnsynced];
    return {
      ...remoteCls,
      attachments: mergedAttachments,
      pdfMedia: remoteCls.pdfMedia && isCloudSyncedMedia(remoteCls.pdfMedia) ? remoteCls.pdfMedia : localCls.pdfMedia || remoteCls.pdfMedia,
      audioMedia: remoteCls.audioMedia && isCloudSyncedMedia(remoteCls.audioMedia) ? remoteCls.audioMedia : localCls.audioMedia || remoteCls.audioMedia,
      videoMedia: remoteCls.videoMedia && isCloudSyncedMedia(remoteCls.videoMedia) ? remoteCls.videoMedia : localCls.videoMedia || remoteCls.videoMedia
    };
  });
}

function applyRemoteCatalogData(c) {
  if (!c || typeof c !== "object") return;
  if (Array.isArray(c.modules) && c.modules.length) state.modules = c.modules;
  if (Array.isArray(c.classes) && c.classes.length) mergeRemoteClassesSafely(c.classes);
  if (c.settings) {
    state.settings = normalizeSettings({ ...state.settings, ...c.settings });
    try {
      if (c.settings.founderProfileJson) state.founderProfile = JSON.parse(c.settings.founderProfileJson);
      if (c.settings.developerProfileJson) state.developerProfile = sanitizeDeveloperProfile(JSON.parse(c.settings.developerProfileJson));
      if (c.settings.examsJson) {
        const remoteExams = JSON.parse(c.settings.examsJson);
        if (Array.isArray(remoteExams) && remoteExams.length) {
          state.exams = remoteExams;
        }
      }
      if (c.settings.assignmentsJson) {
        const remoteAssignments = JSON.parse(c.settings.assignmentsJson);
        if (Array.isArray(remoteAssignments) && remoteAssignments.length) {
          state.assignments = remoteAssignments;
        }
      }
      if (c.settings.membersJson) {
        const remoteMembers = JSON.parse(c.settings.membersJson);
        if (Array.isArray(remoteMembers) && remoteMembers.length) {
          const amirul = remoteMembers.find((m) => m.id === "member-founder-amirul");
          const hasina = remoteMembers.find((m) => m.id === "member-hasina-khan");
          if (
            Number(c.versionCode || 0) <= 106 &&
            amirul &&
            hasina &&
            Number(amirul.displayOrder) === 1 &&
            Number(hasina.displayOrder) === 2
          ) {
            hasina.displayOrder = 1;
            amirul.displayOrder = 2;
          }
          remoteMembers.sort((a, b) => Number(a.displayOrder || 99) - Number(b.displayOrder || 99));
          state.members = remoteMembers;
        }
      }
    } catch (_) {}
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
    await warmMediaIdbKeysCache();
    if (isAdminUser()) {
      await syncAllPendingLocalMediaToCloud({ silentNoOp: true }).catch(() => {});
    }
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
      applyRemoteCatalogData(catalogSnap.data());
    }
    saveState();
    if (isAdminUser() || ["www.mdnafiulislam@gmail.com", "amirul.islam@isceb.org"].includes(String(cloudUser.email || "").toLowerCase())) {
      await fetchCloudRegisteredUsers(false);
    }
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
    return false;
  }

  try {
    if (isAdminUser()) {
      await syncAllPendingLocalMediaToCloud({ skipPublish: true, silentNoOp: true }).catch(() => {});
    }
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
      exams: state.exams,
      assignments: state.assignments,
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
    showToast(`Published ${versionTag} live to Firebase Firestore! All devices will auto-sync.`);
    updateChromeBadges();
    render();
    return true;
  } catch (err) {
    showToast(`Firestore publish failed: ${err.message}`, true);
    updateChromeBadges();
    render();
    return false;
  }
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
    ${renderPwaInstallBanner()}
    <div class="card card-navy hero-dashboard-grid">
      <div style="min-width: 0;">
        <span class="badge badge-gold">ISCEB Mobile & Web App • ${escapeHtml(state.manifest.versionTag || "v1.6.0")}</span>
        <h2 style="margin: 10px 0 6px; font-size: 22px; color: #fff; line-height: 1.25;">Welcome back, ${escapeHtml(user.fullName)}</h2>
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
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; gap: 8px; flex-wrap: wrap;">
          <h3 style="margin: 0; font-size: 16px; color: var(--navy);">Next Recommended Lecture</h3>
          <span class="badge badge-navy">${escapeHtml(nextMod ? `Module ${nextMod.number}` : "SCM")}</span>
        </div>
        <p style="margin: 0 0 6px; font-weight: 700; color: var(--navy);">${escapeHtml(nextClass?.title || "")}</p>
        <p style="margin: 0 0 12px; font-size: 13px; color: var(--muted);">${escapeHtml(nextClass?.summary || "")}</p>
        <button type="button" class="btn btn-primary btn-sm" data-action="continue-class" data-class-id="${escapeHtml(nextClass?.id || "class-1")}">Open Lecture Workspace</button>
      </div>

      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; gap: 8px; flex-wrap: wrap;">
          <h3 style="margin: 0; font-size: 16px; color: var(--navy);">ISCEB Executive Leadership</h3>
          <button type="button" class="btn btn-outline btn-sm" data-nav="directory">View Directory</button>
        </div>
        <div style="display: grid; gap: 10px; min-width: 0;">
          <div style="display: flex; align-items: center; gap: 12px; padding: 8px; border: 1px solid var(--border); border-radius: 10px; min-width: 0;">
            <img src="./admin-amirul.webp" alt="Md. Amirul Islam" class="portrait-avatar" style="width: 52px; height: 52px; object-fit: cover; object-position: center 20%; border: 2px solid var(--gold);" />
            <div style="min-width: 0; flex: 1;">
              <div style="font-weight: 800; color: var(--navy); font-size: 13.5px;">${escapeHtml(state.founderProfile.name)}</div>
              <div style="font-size: 12px; color: var(--gold-dark); font-weight: 700;">${escapeHtml(state.founderProfile.designation)} • ${escapeHtml(state.founderProfile.additionalRole)}</div>
              <div style="font-size: 11.5px; color: var(--muted);">${escapeHtml(state.founderProfile.professionalIdentity)}</div>
            </div>
          </div>
          <div style="display: flex; align-items: center; gap: 12px; padding: 8px; border: 1px solid var(--border); border-radius: 10px; cursor: pointer; min-width: 0;" data-action="view-member" data-member-id="member-developer-nafiul">
            <img src="./admin-nafiul.webp" alt="Md. Nafiul Islam Nahin" class="portrait-avatar" style="width: 52px; height: 52px; object-fit: cover; object-position: center 20%; border: 2px solid var(--navy);" />
            <div style="min-width: 0; flex: 1;">
              <div style="font-weight: 800; color: var(--navy); font-size: 13.5px;">${escapeHtml(state.developerProfile.name)}</div>
              <div style="font-size: 12px; color: var(--navy-soft); font-weight: 700;">${escapeHtml(state.developerProfile.designation)}</div>
              <div style="font-size: 11px; color: var(--muted); line-height: 1.35;">${escapeHtml(state.developerProfile.headline || state.developerProfile.contactEmail)}</div>
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

function getUniqueClassMaterials(cls) {
  if (!cls) return [];
  const classMaterials = [
    cls.pdfMedia,
    cls.audioMedia,
    cls.videoMedia,
    ...(cls.attachments || [])
  ].filter(Boolean);
  const seen = new Set();
  return classMaterials.filter((m) => {
    const key = m.id || `${m.fileName}_${m.url}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
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

  const visibleClasses = state.classes
    .filter((c) => isAdminUser() || c.isPublished !== false)
    .slice()
    .sort((a, b) => a.orderIndex - b.orderIndex);
  const selectedClass = visibleClasses.find((c) => c.id === state.selectedClassId) || visibleClasses[0];
  const selectedIdx = visibleClasses.findIndex((c) => c.id === selectedClass?.id);
  const prevClass = selectedIdx > 0 ? visibleClasses[selectedIdx - 1] : null;
  const nextClass = selectedIdx >= 0 && selectedIdx < visibleClasses.length - 1 ? visibleClasses[selectedIdx + 1] : null;
  const selectedModule = state.modules.find((m) => m.id === selectedClass?.moduleId);
  const prog = selectedClass ? getClassProgress(selectedClass.id) : null;
  const quiz = selectedClass ? generateClassQuiz(selectedClass) : [];
  const timerRunning = activeTimerInterval && activeTimerInterval._classId === selectedClass?.id;

  if (courseDetailOpen && selectedClass) {
    const uniqueMaterials = getUniqueClassMaterials(selectedClass);
    const unsyncedAdminLocalCount = uniqueMaterials.filter(
      (m) => !/^https?:\/\//i.test(String(m.url || "")) && (!m.cloudFileId || !m.cloudChunkCount)
    ).length;

    return `
      <div class="lecture-detail-topbar">
        <button type="button" class="btn btn-outline btn-sm" data-action="back-to-modules">
          &larr; All Modules &amp; Classes
        </button>
        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
          ${
            prevClass
              ? `<button type="button" class="btn btn-outline btn-sm" data-action="select-class" data-class-id="${escapeHtml(prevClass.id)}">&lsaquo; Prev (${escapeHtml(prevClass.lectureCode)})</button>`
              : ""
          }
          ${
            nextClass
              ? `<button type="button" class="btn btn-outline btn-sm" data-action="select-class" data-class-id="${escapeHtml(nextClass.id)}">Next (${escapeHtml(nextClass.lectureCode)}) &rsaquo;</button>`
              : ""
          }
        </div>
      </div>

      <div class="card">
        <div class="lecture-hero-header">
          <div class="lecture-hero-meta">
            <span class="badge badge-navy lecture-hero-badge">${escapeHtml(selectedModule?.part === "PART_1_BASIC" ? "Part 1 • Basic Level" : "Part 2 • Advance Level")} • Module ${escapeHtml(selectedModule?.number || "")}</span>
            <h2 class="lecture-hero-title">Lecture ${escapeHtml(selectedClass.lectureCode)}: ${escapeHtml(selectedClass.title)}</h2>
            <p style="margin: 0; font-size: 13px; color: var(--muted);">Duration: ${escapeHtml(selectedClass.durationMinutes)} mins • Study Logged: ${formatSeconds(prog.activeSeconds)}</p>
          </div>
          <div class="lecture-hero-actions">
            <button type="button" class="btn ${timerRunning ? "btn-danger" : "btn-outline"} btn-sm" data-action="toggle-study-timer" data-class-id="${escapeHtml(selectedClass.id)}">
              ${timerRunning ? "Stop Study Timer" : "Start Study Timer"}
            </button>
            <button type="button" class="btn ${prog.completed ? "btn-outline" : "btn-gold"} btn-sm" data-action="toggle-complete-class" data-class-id="${escapeHtml(selectedClass.id)}">
              ${prog.completed ? "Completed (Undo)" : "Mark Lecture Complete"}
            </button>
          </div>
        </div>

        <hr style="border: none; border-top: 1px solid var(--border); margin: 16px 0;" />

        <div style="background: var(--surface-muted); border: 1.5px solid #CBD5E1; border-radius: 12px; padding: 14px; margin-bottom: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 8px;">
            <h3 style="margin: 0; font-size: 14.5px; color: var(--navy);">Class Study Materials, PDF, Video &amp; Audio</h3>
            <span class="badge ${uniqueMaterials.length ? "badge-ok" : "badge-warn"}">${uniqueMaterials.length} File(s)</span>
          </div>
          ${
            !uniqueMaterials.length
              ? `<p style="margin: 0 0 10px; font-size: 13px; color: var(--muted);">No PDF, video, or audio materials attached to this lecture yet.</p>`
              : `<div style="display: grid; gap: 8px; margin-bottom: 12px;">
                  ${uniqueMaterials
                    .map((m) => {
                      const hasCloud = isCloudSyncedMedia(m);
                      const hasLocal = localAttachmentUrls.has(m.id) || localAttachmentBlobs.has(m.id);
                      const statusText = hasCloud ? "Cloud Synced" : hasLocal ? "Pending Cloud Sync" : "Needs Re-upload & Sync";
                      const typeUpper = String(m.type || "FILE").toUpperCase();
                      const actionLabel =
                        typeUpper === "PDF"
                          ? "Open PDF"
                          : typeUpper === "VIDEO"
                          ? "Play Video"
                          : typeUpper === "AUDIO"
                          ? "Listen Audio"
                          : "Open / Preview";
                      return `<div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; background: #fff; padding: 10px 12px; border-radius: 10px; border: 1px solid var(--border); flex-wrap: wrap;">
                        <div style="min-width: 0; flex: 1;">
                          <div style="font-size: 13.5px; font-weight: 700; color: var(--navy);">${escapeHtml(m.title || m.fileName || "Study Material")}</div>
                          <div style="font-size: 11.5px; color: ${hasCloud ? "var(--muted)" : "#B45309"}; font-weight: ${hasCloud ? "400" : "700"};">${escapeHtml(typeUpper)} • ${escapeHtml(m.fileSizeFormatted || formatBytes(m.fileSizeBytes))} • ${statusText}</div>
                        </div>
                        <div style="display: flex; gap: 6px; align-items: center; flex-wrap: wrap;">
                          <button type="button" class="btn btn-gold btn-sm" data-action="open-cloud-media" data-class-id="${escapeHtml(selectedClass.id)}" data-media-id="${escapeHtml(m.id || "")}">
                            ${actionLabel}
                          </button>
                          ${
                            isAdminUser() && !hasCloud
                              ? `<label class="btn btn-primary btn-sm" style="cursor: pointer;">
                                  Re-upload &amp; Sync
                                  <input type="file" class="repairInlineMediaInput" data-class-id="${escapeHtml(selectedClass.id)}" data-media-id="${escapeHtml(m.id || "")}" style="display: none;" accept=".pdf,image/*,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,audio/*,video/*" />
                                </label>`
                              : ""
                          }
                          ${
                            isAdminUser()
                              ? `<button type="button" class="btn btn-outline btn-sm" style="color: var(--err); border-color: rgba(185,28,28,0.35);" data-action="delete-class-media" data-class-id="${escapeHtml(selectedClass.id)}" data-media-id="${escapeHtml(m.id || "")}">Delete</button>`
                              : ""
                          }
                        </div>
                      </div>`;
                    })
                    .join("")}
                </div>`
          }
          <textarea id="lectureNotesBox" class="textarea" rows="3" placeholder="Record your study notes for Lecture ${escapeHtml(selectedClass.lectureCode)}...">${escapeHtml(prog.localNotes || "")}</textarea>
          <div style="display: flex; gap: 10px; align-items: center; margin-top: 10px; flex-wrap: wrap;">
            <button type="button" class="btn btn-primary btn-sm" data-action="save-notes" data-class-id="${escapeHtml(selectedClass.id)}">Save Notes</button>
            <label class="btn btn-outline btn-sm" style="cursor: pointer;">
              Attach Personal PDF / Video / Slide
              <input type="file" id="attachLocalFileInput" data-class-id="${escapeHtml(selectedClass.id)}" style="display: none;" accept=".pdf,video/*,image/*,.txt,.csv" />
            </label>
          </div>
          ${
            (prog.localAttachments || []).length
              ? `<div style="margin-top: 10px; display: grid; gap: 6px;">
                  ${prog.localAttachments
                    .map((att) => {
                      return `<div style="display: flex; justify-content: space-between; align-items: center; gap: 8px; background: #fff; padding: 8px 10px; border-radius: 8px; border: 1px solid var(--border); font-size: 12.5px;">
                        <span><strong>${escapeHtml(att.name)}</strong> (${formatBytes(att.size)})</span>
                        <button type="button" class="btn btn-outline btn-sm" data-action="open-personal-attachment" data-class-id="${escapeHtml(selectedClass.id)}" data-att-id="${escapeHtml(att.id)}">Open File</button>
                      </div>`;
                    })
                    .join("")}
                </div>`
              : ""
          }
        </div>

        ${
          isAdminUser()
            ? `
          <div class="upload-center-card" id="desktopAdminMediaUploadCenter" style="margin-bottom: 16px;">
            <div class="flex-header-between" style="margin-bottom: 10px;">
              <strong style="font-size: 14.5px; color: #fff;">Admin Media Upload Center</strong>
              <span style="font-size: 11.5px; color: var(--gold-soft); font-weight: 700; white-space: nowrap;">Max 18MB Cloud Sync</span>
            </div>
            ${
              !cloudUser
                ? `<div style="background: rgba(245, 158, 11, 0.2); border: 1px solid rgba(251, 191, 36, 0.55); border-radius: 10px; padding: 10px 12px; margin-bottom: 10px; display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap;">
                    <span style="font-size: 12px; color: #FEF3C7;">Sign in with <strong>Google Cloud Sync</strong> before uploading so students on other phones can open your PDFs/videos.</span>
                    <button type="button" class="btn btn-gold btn-sm" data-action="trigger-google-auth">Connect Google Cloud</button>
                  </div>`
                : unsyncedAdminLocalCount > 0
                ? `<div style="background: rgba(212, 175, 55, 0.16); border: 1px solid rgba(212, 175, 55, 0.5); border-radius: 10px; padding: 10px 12px; margin-bottom: 10px; display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap;">
                    <span style="font-size: 12px; color: #F4E2A3;">${unsyncedAdminLocalCount} local file(s) need cloud sync for students.</span>
                    <button type="button" class="btn btn-gold btn-sm" data-action="sync-local-media-to-cloud" data-class-id="${escapeHtml(selectedClass.id)}">Sync Local Files to Cloud</button>
                  </div>`
                : ""
            }
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
                <select id="adminRemoteMediaType" class="select" style="width: auto; min-width: min(120px, 100%);">
                  <option value="PDF">PDF</option>
                  <option value="VIDEO">Video</option>
                  <option value="AUDIO">Audio</option>
                  <option value="IMAGE">Image</option>
                  <option value="DOCUMENT">Document</option>
                </select>
                <input type="url" id="adminRemoteMediaUrl" class="input" style="flex: 1 1 200px; min-width: 0;" placeholder="https://example.com/lecture-material.pdf" required />
                <input type="text" id="adminRemoteMediaTitle" class="input" style="flex: 1 1 160px; min-width: 0;" placeholder="Material Title" />
                <button type="submit" class="btn btn-gold btn-sm">Attach HTTPS Link</button>
              </div>
            </form>
          </div>
        `
            : ""
        }

        <h3 style="margin: 0 0 6px; font-size: 14.5px; color: var(--navy);">Executive Summary</h3>
        <p style="margin: 0 0 14px; font-size: 13.5px; color: #334155;">${escapeHtml(selectedClass.summary)}</p>

        <h3 style="margin: 0 0 8px; font-size: 14.5px; color: var(--navy);">Key Learning Objectives</h3>
        <ul style="margin: 0 0 16px; padding-left: 20px; font-size: 13.5px; color: #334155;">
          ${(selectedClass.learningObjectives || []).map((obj) => `<li style="margin-bottom: 4px;">${escapeHtml(obj)}</li>`).join("")}
        </ul>

        <div style="background: #F4F7FC; border: 1px solid #C7D7EE; border-radius: 12px; padding: 14px;">
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
      </div>
    `;
  }

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
              const matCount = getUniqueClassMaterials(c).length;
              return `
                <div class="list-item" data-action="select-class" data-class-id="${escapeHtml(c.id)}" style="padding: 10px 11px; margin-bottom: 6px;">
                  <div style="display: flex; justify-content: space-between; gap: 8px; align-items: center;">
                    <span style="font-size: 13px; font-weight: 700; color: var(--navy); min-width: 0; flex: 1;">${escapeHtml(c.lectureCode)}. ${escapeHtml(c.title)}</span>
                    <div style="display: flex; align-items: center; gap: 6px; flex-shrink: 0;">
                      ${matCount > 0 ? `<span class="badge badge-navy">${matCount} File${matCount > 1 ? "s" : ""}</span>` : ""}
                      <span class="badge ${cp.completed ? "badge-ok" : "badge-warn"}">${cp.completed ? "Done" : `${c.durationMinutes}m`}</span>
                      <span style="color: var(--muted); font-weight: 800; font-size: 16px; line-height: 1;">&rsaquo;</span>
                    </div>
                  </div>
                </div>
              `;
            })
            .join("")}
        </div>
      `;
    })
    .join("");

  return `
    <div class="search-row">
      <input type="search" id="courseSearchInput" class="input" style="max-width: 360px;" placeholder="Search modules, lectures (01–48), keywords..." value="${escapeHtml(state.searchQuery)}" />
      <button type="button" class="btn ${state.partFilter === "ALL" ? "btn-primary" : "btn-outline"} btn-sm" data-part="ALL">All 12 Modules (48)</button>
      <button type="button" class="btn ${state.partFilter === "PART_1_BASIC" ? "btn-primary" : "btn-outline"} btn-sm" data-part="PART_1_BASIC">Part 1: Basic (1–28)</button>
      <button type="button" class="btn ${state.partFilter === "PART_2_ADVANCE" ? "btn-primary" : "btn-outline"} btn-sm" data-part="PART_2_ADVANCE">Part 2: Advance (29–48)</button>
    </div>
    <div>${listHtml || '<div class="card">No matching modules or lectures found.</div>'}</div>
  `;
}

function formatDeadlineLabel(ms) {
  if (!ms || Number(ms) <= 0) return "No Deadline";
  try {
    return new Date(Number(ms)).toLocaleString(undefined, {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
  } catch (_) {
    return new Date(Number(ms)).toISOString().slice(0, 16).replace("T", " ");
  }
}

function toDateTimeLocalValue(ms) {
  if (!ms || Number(ms) <= 0) return "";
  const d = new Date(Number(ms));
  if (Number.isNaN(d.getTime())) return "";
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function isAssignmentExpired(asg, nowMs = Date.now()) {
  return Boolean(asg && asg.dueDateMs && Number(nowMs) > Number(asg.dueDateMs));
}

function isExamExpired(exam, nowMs = Date.now()) {
  return Boolean(exam && exam.openUntilMs && Number(nowMs) > Number(exam.openUntilMs));
}

function formatRemainingDeadlineBadge(targetMs, nowMs = Date.now()) {
  if (!targetMs) return "";
  const diffSec = Math.floor((Number(targetMs) - Number(nowMs)) / 1000);
  if (diffSec <= 0) return "Time Up • Closed";
  const days = Math.floor(diffSec / 86400);
  const hours = Math.floor((diffSec % 86400) / 3600);
  const mins = Math.floor((diffSec % 3600) / 60);
  if (days > 0) return `${days}d ${hours}h left`;
  if (hours > 0) return `${hours}h ${mins}m left`;
  return `${Math.max(1, mins)}m left`;
}

async function syncAssessmentsToCloudIfConnected(changeSummary) {
  saveState();
  if (cloudUser) {
    await pushCurriculumToFirestore({
      versionTag: state.manifest.versionTag || "v1.6.0",
      versionCode: Number(state.manifest.versionCode || 106) + 1,
      releaseNotes: changeSummary || "Updated exams & assignments",
      announcement: state.manifest.announcement || "",
      apkVersionName: state.manifest.latestApkVersionName || "1.6",
      apkVersionCode: state.manifest.latestApkVersionCode || 7,
      apkUrl: state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl
    });
  } else {
    showToast(`${changeSummary} (Saved locally. Connect Google Cloud Sync to push to all students.)`);
    render();
  }
}

function buildExamQuestionBuilderRowHtml(q = {}, idx = 0) {
  const opts = Array.isArray(q.options) && q.options.length >= 4
    ? q.options
    : ["", "", "", ""];
  const correctIdx = Number(q.correctOptionIndex || 0);
  const marksVal = Number(q.marks || 5);
  return `
    <div class="exam-q-builder-row" data-q-index="${idx}" style="border: 1.5px solid #CBD5E1; border-radius: 12px; padding: 12px; background: #F8FAFC; display: grid; gap: 8px;">
      <div style="display: flex; justify-content: space-between; align-items: center; gap: 8px;">
        <strong class="exam-q-row-label" style="color: var(--navy); font-size: 13px;">Question ${idx + 1}</strong>
        <div style="display: flex; gap: 8px; align-items: center;">
          <label style="font-size: 11.5px; font-weight: 700; display: flex; align-items: center; gap: 4px;">Marks
            <input type="number" class="input eq-marks-input" min="1" max="50" value="${marksVal}" style="width: 66px; padding: 4px 8px;" required />
          </label>
          <button type="button" class="btn btn-outline btn-sm" data-modal-action="builder-remove-question" style="color: var(--err); border-color: rgba(185,28,28,0.35);">Remove</button>
        </div>
      </div>
      <textarea class="textarea eq-question-input" rows="2" placeholder="Write MCQ question prompt..." required>${escapeHtml(q.question || "")}</textarea>
      <div style="display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px;">
        ${[0, 1, 2, 3]
          .map(
            (oIdx) => `
          <input type="text" class="input eq-opt-input" data-opt-idx="${oIdx}" placeholder="Option ${String.fromCharCode(65 + oIdx)}" value="${escapeHtml(opts[oIdx] || "")}" required />
        `
          )
          .join("")}
      </div>
      <div style="display: grid; grid-template-columns: auto 1fr; gap: 8px; align-items: center;">
        <label style="font-size: 12px; font-weight: 700; display: flex; align-items: center; gap: 6px;">Correct Option:
          <select class="select eq-correct-select" style="width: auto; padding: 5px 10px;">
            ${[0, 1, 2, 3]
              .map((oIdx) => `<option value="${oIdx}" ${correctIdx === oIdx ? "selected" : ""}>Option ${String.fromCharCode(65 + oIdx)}</option>`)
              .join("")}
          </select>
        </label>
        <input type="text" class="input eq-exp-input" placeholder="Answer explanation (shown after submit, optional)" value="${escapeHtml(q.explanation || "")}" />
      </div>
    </div>
  `;
}

function openAdminExamBuilderModal(examId = "") {
  if (!isAdminUser()) {
    showToast("Admin sign-in required to create or edit exams.", true);
    return;
  }
  const existing = examId ? state.exams.find((e) => e.id === examId) : null;
  const questions = existing?.questions?.length
    ? existing.questions
    : [
        {
          id: `q-${Date.now()}-1`,
          question: "",
          options: ["", "", "", ""],
          correctOptionIndex: 0,
          marks: 5,
          explanation: ""
        }
      ];

  const moduleOptions = state.modules
    .slice()
    .sort((a, b) => Number(a.number) - Number(b.number))
    .map(
      (m) => `<option value="${escapeHtml(m.id)}" ${(existing?.moduleId || "mod-1") === m.id ? "selected" : ""}>Module ${m.number}: ${escapeHtml(m.title)}</option>`
    )
    .join("");

  const html = `
    <form id="adminExamBuilderForm" data-exam-id="${escapeHtml(existing?.id || "")}" style="display: grid; gap: 10px;">
      <label style="font-size: 12px; font-weight: 700;">Module
        <select id="aebModuleId" class="select">${moduleOptions}</select>
      </label>
      <label style="font-size: 12px; font-weight: 700;">Exam Title
        <input type="text" id="aebTitle" class="input" placeholder="e.g. Module 4 Timed Assessment" value="${escapeHtml(existing?.title || "")}" required />
      </label>
      <div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px;">
        <label style="font-size: 12px; font-weight: 700;">Duration (Mins)
          <input type="number" id="aebDuration" class="input" min="1" max="180" value="${Number(existing?.durationMinutes || 20)}" required />
        </label>
        <label style="font-size: 12px; font-weight: 700;">Passing (%)
          <input type="number" id="aebPassing" class="input" min="20" max="100" value="${Number(existing?.passingPercentage || 60)}" required />
        </label>
        <label style="font-size: 12px; font-weight: 700;">Max Attempts
          <input type="number" id="aebMaxAttempts" class="input" min="1" max="50" value="${Number(existing?.maxAttempts || 3)}" required />
        </label>
      </div>
      <div style="background: #FFFBEB; border: 1px solid #FCD34D; border-radius: 10px; padding: 10px; display: grid; gap: 6px;">
        <label style="font-size: 12px; font-weight: 700; color: var(--navy);">Closing Deadline / Time-Up Cutoff (Optional)
          <input type="datetime-local" id="aebOpenUntil" class="input" value="${toDateTimeLocalValue(existing?.openUntilMs)}" style="margin-top: 4px;" />
        </label>
        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
          <button type="button" class="btn btn-outline btn-sm" data-modal-action="builder-set-deadline-preset" data-target-input="aebOpenUntil" data-hours="0">No Cutoff</button>
          <button type="button" class="btn btn-outline btn-sm" data-modal-action="builder-set-deadline-preset" data-target-input="aebOpenUntil" data-hours="1">+1 Hour</button>
          <button type="button" class="btn btn-outline btn-sm" data-modal-action="builder-set-deadline-preset" data-target-input="aebOpenUntil" data-hours="24">+24 Hours</button>
          <button type="button" class="btn btn-outline btn-sm" data-modal-action="builder-set-deadline-preset" data-target-input="aebOpenUntil" data-hours="72">+3 Days</button>
        </div>
        <div style="font-size: 11px; color: #78350F;">Countdown auto-submits when Exam Duration reaches 00:00, and new starts lock after the Closing Deadline.</div>
      </div>

      <div style="display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-top: 4px;">
        <strong style="color: var(--navy); font-size: 13.5px;">MCQ Questions Builder</strong>
        <button type="button" class="btn btn-primary btn-sm" data-modal-action="builder-add-question">+ Add Question</button>
      </div>
      <div id="examQuestionRowsHost" style="display: grid; gap: 10px;">
        ${questions.map((q, idx) => buildExamQuestionBuilderRowHtml(q, idx)).join("")}
      </div>
      <button type="submit" class="btn btn-gold" style="width: 100%; margin-top: 6px;">
        Save Exam & Questions ${cloudUser ? "(+ Publish Cloud)" : "(Local + Publish Ready)"}
      </button>
    </form>
  `;
  openModal(existing ? `Edit Exam: ${existing.title}` : "Create Timed Exam & MCQ Questions", html);
}

function openAdminAssignmentBuilderModal(asgId = "") {
  if (!isAdminUser()) {
    showToast("Admin sign-in required to create or edit assignments.", true);
    return;
  }
  const existing = asgId ? state.assignments.find((a) => a.id === asgId) : null;
  const moduleOptions = state.modules
    .slice()
    .sort((a, b) => Number(a.number) - Number(b.number))
    .map(
      (m) => `<option value="${escapeHtml(m.id)}" ${(existing?.moduleId || "mod-1") === m.id ? "selected" : ""}>Module ${m.number}: ${escapeHtml(m.title)}</option>`
    )
    .join("");

  const html = `
    <form id="adminAssignmentBuilderForm" data-asg-id="${escapeHtml(existing?.id || "")}" style="display: grid; gap: 10px;">
      <label style="font-size: 12px; font-weight: 700;">Module
        <select id="aabModuleId" class="select">${moduleOptions}</select>
      </label>
      <label style="font-size: 12px; font-weight: 700;">Assignment Title
        <input type="text" id="aabTitle" class="input" placeholder="e.g. Module 3 LC Discrepancy Case Assignment" value="${escapeHtml(existing?.title || "")}" required />
      </label>
      <label style="font-size: 12px; font-weight: 700;">Assignment Topic / Case Prompt
        <textarea id="aabTopic" class="textarea" rows="3" placeholder="Explain the case scenario and deliverables..." required>${escapeHtml(existing?.topicDescription || "")}</textarea>
      </label>
      <label style="font-size: 12px; font-weight: 700;">Submission Instructions (Optional)
        <textarea id="aabInstructions" class="textarea" rows="2" placeholder="Formatting, word limit, or upload rules...">${escapeHtml(existing?.instructions || "")}</textarea>
      </label>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; align-items: center;">
        <label style="font-size: 12px; font-weight: 700;">Max Marks
          <input type="number" id="aabMarks" class="input" min="5" max="200" value="${Number(existing?.maxMarks || 20)}" required />
        </label>
        <label style="display: flex; align-items: center; gap: 8px; font-size: 12.5px; font-weight: 700; margin-top: 16px;">
          <input type="checkbox" id="aabFinalProject" ${existing?.isFinalProject ? "checked" : ""} />
          <span>Final Capstone Project</span>
        </label>
      </div>
      <label style="font-size: 12px; font-weight: 700;">Reference Brief / Question Sheet URL (Optional HTTPS Link)
        <input type="url" id="aabRefUrl" class="input" placeholder="https://example.com/assignment-brief.pdf" value="${escapeHtml(existing?.referenceUrl || "")}" />
      </label>
      <div style="background: #FFFBEB; border: 1px solid #FCD34D; border-radius: 10px; padding: 10px; display: grid; gap: 6px;">
        <label style="font-size: 12px; font-weight: 700; color: var(--navy);">Submission Deadline / Time-Up Cutoff (Optional)
          <input type="datetime-local" id="aabDueDate" class="input" value="${toDateTimeLocalValue(existing?.dueDateMs)}" style="margin-top: 4px;" />
        </label>
        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
          <button type="button" class="btn btn-outline btn-sm" data-modal-action="builder-set-deadline-preset" data-target-input="aabDueDate" data-hours="0">No Deadline</button>
          <button type="button" class="btn btn-outline btn-sm" data-modal-action="builder-set-deadline-preset" data-target-input="aabDueDate" data-hours="1">+1 Hour</button>
          <button type="button" class="btn btn-outline btn-sm" data-modal-action="builder-set-deadline-preset" data-target-input="aabDueDate" data-hours="24">+24 Hours</button>
          <button type="button" class="btn btn-outline btn-sm" data-modal-action="builder-set-deadline-preset" data-target-input="aabDueDate" data-hours="72">+3 Days</button>
          <button type="button" class="btn btn-outline btn-sm" data-modal-action="builder-set-deadline-preset" data-target-input="aabDueDate" data-hours="168">+7 Days</button>
        </div>
        <div style="font-size: 11px; color: #78350F;">After this date &amp; time, students see "Time Up • Closed" and cannot submit new answers.</div>
      </div>
      <button type="submit" class="btn btn-gold" style="width: 100%; margin-top: 6px;">
        Save Assignment ${cloudUser ? "(+ Publish Cloud)" : "(Local + Publish Ready)"}
      </button>
    </form>
  `;
  openModal(existing ? `Edit Assignment: ${existing.title}` : "Create Practical Assignment & Deadline", html);
}

function renderExams() {
  const uid = getActiveUser().id;
  const attempts = state.examAttemptsByUser[uid] || [];
  const subs = state.assignmentSubmissionsByUser[uid] || {};
  const adminLogged = isAdminUser();

  const adminBar = adminLogged
    ? `
      <div class="card" style="margin-bottom: 16px; background: linear-gradient(135deg, #0D2240 0%, #15345C 100%); color: #fff; border: 1.5px solid var(--gold);">
        <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap;">
          <div>
            <span class="badge badge-gold">Admin Assessment &amp; Question Studio</span>
            <h3 style="margin: 6px 0 4px; color: #fff; font-size: 16px;">Create Timed MCQ Exams &amp; Deadlined Assignments</h3>
            <p style="margin: 0; font-size: 12.5px; color: #D7E3F3;">
              Questions &amp; tasks auto-lock when Time Up arrives. Connect Google Cloud Sync to push live to all students.
            </p>
          </div>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <button type="button" class="btn btn-gold btn-sm" data-action="admin-create-exam">+ New Exam &amp; Questions</button>
            <button type="button" class="btn btn-outline btn-sm" style="background: rgba(255,255,255,0.12); color: #fff; border-color: rgba(255,255,255,0.35);" data-action="admin-create-assignment">+ New Assignment</button>
            <button type="button" class="btn btn-outline btn-sm" style="background: rgba(212,175,55,0.18); color: var(--gold-soft); border-color: var(--gold);" data-action="admin-publish-assessments-now">
              ${cloudUser ? "Publish Live to Cloud" : "Connect Cloud &amp; Publish"}
            </button>
          </div>
        </div>
      </div>
    `
    : "";

  const examsHtml = state.exams
    .map((exam) => {
      const userAttempts = attempts.filter((a) => a.examId === exam.id);
      const best = userAttempts.reduce((max, a) => (a.percentage > (max?.percentage || -1) ? a : max), null);
      const expired = isExamExpired(exam);
      const windowBadge = exam.openUntilMs
        ? `<span class="badge ${expired ? "badge-err" : "badge-warn"}">${expired ? "Time Up • Closed" : `Closes ${formatDeadlineLabel(exam.openUntilMs)} (${formatRemainingDeadlineBadge(exam.openUntilMs)})`}</span>`
        : `<span class="badge badge-ok">Always Open • ${exam.durationMinutes}m Timer</span>`;

      return `
        <div class="card" style="margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; flex-wrap: wrap;">
            <div style="min-width: 0; flex: 1;">
              <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                <span class="badge ${exam.id === "exam-final" ? "badge-gold" : "badge-navy"}">${exam.totalMarks} Marks • ${exam.durationMinutes} Mins</span>
                ${windowBadge}
              </div>
              <h3 style="margin: 8px 0 4px; font-size: 15.5px; color: var(--navy);">${escapeHtml(exam.title)}</h3>
              <p style="margin: 0; font-size: 12.5px; color: var(--muted);">${(exam.questions || []).length} Questions • Passing: ${exam.passingPercentage}% • Auto-submits at 00:00</p>
            </div>
            ${
              best
                ? `<span class="badge ${best.passed ? "badge-ok" : "badge-warn"}">Best: ${best.scoredMarks}/${exam.totalMarks} (${best.percentage}%)</span>`
                : `<span class="badge badge-warn">Not Attempted</span>`
            }
          </div>
          <div style="margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap;">
            <button type="button" class="btn btn-primary btn-sm" data-action="start-exam" data-exam-id="${escapeHtml(exam.id)}" ${expired && !adminLogged ? "disabled" : ""}>
              ${expired ? "Exam Closed (Time Up)" : best ? "Retake Timed Exam" : "Start Timed Exam"}
            </button>
            ${
              adminLogged
                ? `
                  <button type="button" class="btn btn-outline btn-sm" data-action="admin-edit-exam" data-exam-id="${escapeHtml(exam.id)}">Edit Questions / Timer</button>
                  <button type="button" class="btn btn-outline btn-sm" style="color: var(--err); border-color: rgba(185,28,28,0.35);" data-action="admin-delete-exam" data-exam-id="${escapeHtml(exam.id)}">Delete</button>
                `
                : ""
            }
          </div>
        </div>
      `;
    })
    .join("");

  const assignHtml = state.assignments
    .map((asg) => {
      const sub = subs[asg.id];
      const expired = isAssignmentExpired(asg);
      const deadlineChip = asg.dueDateMs
        ? `<span class="badge ${expired ? "badge-err" : "badge-warn"}">${expired ? "Time Up • Closed" : `Due ${formatDeadlineLabel(asg.dueDateMs)} (${formatRemainingDeadlineBadge(asg.dueDateMs)})`}</span>`
        : "";
      return `
        <div class="card" style="margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; flex-wrap: wrap;">
            <div style="min-width: 0; flex: 1;">
              <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                <span class="badge ${asg.isFinalProject ? "badge-gold" : "badge-navy"}">${asg.maxMarks} Marks${asg.isFinalProject ? " • Capstone" : ""}</span>
                ${deadlineChip}
              </div>
              <h3 style="margin: 8px 0 4px; font-size: 15px; color: var(--navy);">${escapeHtml(asg.title)}</h3>
              <p style="margin: 0; font-size: 12.5px; color: #334155;">${escapeHtml(asg.topicDescription)}</p>
              ${
                asg.referenceUrl
                  ? `<div style="margin-top: 6px; font-size: 12px;"><a href="${escapeHtml(asg.referenceUrl)}" target="_blank" rel="noopener noreferrer" style="color: var(--navy); font-weight: 700;">Open Reference Brief Link &rsaquo;</a></div>`
                  : ""
              }
            </div>
            <span class="badge ${sub?.grade !== undefined && sub?.grade !== null ? "badge-ok" : sub ? "badge-navy" : expired ? "badge-err" : "badge-warn"}">
              ${sub?.grade !== undefined && sub?.grade !== null ? `Graded: ${sub.grade}/${asg.maxMarks}` : sub ? "Submitted" : expired ? "Closed" : "Pending"}
            </span>
          </div>
          <div style="margin-top: 10px; display: flex; gap: 8px; flex-wrap: wrap;">
            <button type="button" class="btn btn-outline btn-sm" data-action="open-assignment" data-asg-id="${escapeHtml(asg.id)}">
              ${expired ? (sub ? "View Submission (Closed)" : "View Prompt (Closed)") : sub ? "View / Update Submission" : "Submit Work"}
            </button>
            ${
              adminLogged
                ? `
                  ${sub ? `<button type="button" class="btn btn-gold btn-sm" data-action="grade-assignment" data-asg-id="${escapeHtml(asg.id)}">Admin Grade</button>` : ""}
                  <button type="button" class="btn btn-outline btn-sm" data-action="admin-edit-assignment" data-asg-id="${escapeHtml(asg.id)}">Edit / Deadline</button>
                  <button type="button" class="btn btn-outline btn-sm" style="color: var(--err); border-color: rgba(185,28,28,0.35);" data-action="admin-delete-assignment" data-asg-id="${escapeHtml(asg.id)}">Delete</button>
                `
                : ""
            }
          </div>
        </div>
      `;
    })
    .join("");

  return `
    ${adminBar}
    <div class="two-col-grid">
      <div>
        <h2 style="margin: 0 0 12px; color: var(--navy); font-size: 17px;">Interactive Timed Examinations (${state.exams.length})</h2>
        ${examsHtml || '<div class="card">No exams configured yet.</div>'}
      </div>
      <div>
        <h2 style="margin: 0 0 12px; color: var(--navy); font-size: 17px;">Practical Assignments & Capstone (${state.assignments.length})</h2>
        ${assignHtml || '<div class="card">No assignments configured yet.</div>'}
      </div>
    </div>
  `;
}

function loadImageAsset(src) {
  return new Promise((resolve) => {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => resolve(img);
    img.onerror = () => resolve(null);
    img.src = src;
  });
}

function wrapCanvasTextLines(ctx, text, maxWidth) {
  const words = String(text || "").trim().split(/\s+/).filter(Boolean);
  const lines = [];
  let line = "";
  for (const w of words) {
    const candidate = line ? `${line} ${w}` : w;
    if (ctx.measureText(candidate).width <= maxWidth) {
      line = candidate;
    } else {
      if (line) lines.push(line);
      line = w;
    }
  }
  if (line) lines.push(line);
  return lines;
}

function escapePdfTextString(str) {
  return String(str || "")
    .replace(/[\u2018\u2019]/g, "'")
    .replace(/[\u201C\u201D]/g, '"')
    .replace(/[\u2013\u2014\u2022]/g, "-")
    .replace(/[^\x20-\x7E]/g, " ")
    .replace(/\\/g, "\\\\")
    .replace(/\(/g, "\\(")
    .replace(/\)/g, "\\)");
}

function measurePdfTextWidthPt(text, fontKey, fontSize, canvasCtx = null) {
  const clean = String(text || "");
  if (!clean) return 0;
  if (canvasCtx) {
    const fontFamilyMap = {
      F1: `${fontSize}px "Times New Roman", Times, Georgia, serif`,
      F2: `bold ${fontSize}px "Times New Roman", Times, Georgia, serif`,
      F3: `italic ${fontSize}px "Times New Roman", Times, Georgia, serif`,
      F4: `${fontSize}px Helvetica, Arial, sans-serif`,
      F5: `bold ${fontSize}px Helvetica, Arial, sans-serif`
    };
    canvasCtx.font = fontFamilyMap[fontKey] || `${fontSize}px serif`;
    return canvasCtx.measureText(clean).width * 0.96;
  }
  const isBold = fontKey === "F2" || fontKey === "F5";
  const isSans = fontKey === "F4" || fontKey === "F5";
  let units = 0;
  for (const ch of clean) {
    if (ch === " ") units += isSans ? 0.28 : 0.25;
    else if ("iljI!.,:;'|".includes(ch)) units += 0.28;
    else if ("frt()[]/-".includes(ch)) units += 0.36;
    else if ("mwMW@%".includes(ch)) units += isBold ? 0.86 : 0.8;
    else if (ch >= "A" && ch <= "Z") units += isBold ? 0.69 : 0.65;
    else units += isBold ? 0.53 : 0.49;
  }
  return units * fontSize;
}

function wrapPdfVectorLines(text, maxWidthPt, fontKey, fontSize, canvasCtx = null) {
  const words = String(text || "").trim().split(/\s+/).filter(Boolean);
  const lines = [];
  let line = "";
  for (const w of words) {
    const candidate = line ? `${line} ${w}` : w;
    if (measurePdfTextWidthPt(candidate, fontKey, fontSize, canvasCtx) <= maxWidthPt) {
      line = candidate;
    } else {
      if (line) lines.push(line);
      line = w;
    }
  }
  if (line) lines.push(line);
  return lines;
}

function pdfCirclePathOps(cx, cy, r) {
  const k = r * 0.55228475;
  return [
    `${(cx + r).toFixed(2)} ${cy.toFixed(2)} m`,
    `${(cx + r).toFixed(2)} ${(cy + k).toFixed(2)} ${(cx + k).toFixed(2)} ${(cy + r).toFixed(2)} ${cx.toFixed(2)} ${(cy + r).toFixed(2)} c`,
    `${(cx - k).toFixed(2)} ${(cy + r).toFixed(2)} ${(cx - r).toFixed(2)} ${(cy + k).toFixed(2)} ${(cx - r).toFixed(2)} ${cy.toFixed(2)} c`,
    `${(cx - r).toFixed(2)} ${(cy - k).toFixed(2)} ${(cx - k).toFixed(2)} ${(cy - r).toFixed(2)} ${cx.toFixed(2)} ${(cy - r).toFixed(2)} c`,
    `${(cx + k).toFixed(2)} ${(cy - r).toFixed(2)} ${(cx + r).toFixed(2)} ${(cy - k).toFixed(2)} ${(cx + r).toFixed(2)} ${cy.toFixed(2)} c`
  ].join("\n");
}

function pdfRoundRectPathOps(x, y, w, h, r) {
  const rr = Math.min(r, w / 2, h / 2);
  const k = rr * 0.55228475;
  return [
    `${(x + rr).toFixed(2)} ${y.toFixed(2)} m`,
    `${(x + w - rr).toFixed(2)} ${y.toFixed(2)} l`,
    `${(x + w - rr + k).toFixed(2)} ${y.toFixed(2)} ${(x + w).toFixed(2)} ${(y + rr - k).toFixed(2)} ${(x + w).toFixed(2)} ${(y + rr).toFixed(2)} c`,
    `${(x + w).toFixed(2)} ${(y + h - rr).toFixed(2)} l`,
    `${(x + w).toFixed(2)} ${(y + h - rr + k).toFixed(2)} ${(x + w - rr + k).toFixed(2)} ${(y + h).toFixed(2)} ${(x + w - rr).toFixed(2)} ${(y + h).toFixed(2)} c`,
    `${(x + rr).toFixed(2)} ${(y + h).toFixed(2)} l`,
    `${(x + rr - k).toFixed(2)} ${(y + h).toFixed(2)} ${x.toFixed(2)} ${(y + h - rr + k).toFixed(2)} ${x.toFixed(2)} ${(y + h - rr).toFixed(2)} c`,
    `${x.toFixed(2)} ${(y + rr).toFixed(2)} l`,
    `${x.toFixed(2)} ${(y + rr - k).toFixed(2)} ${(x + rr - k).toFixed(2)} ${y.toFixed(2)} ${(x + rr).toFixed(2)} ${y.toFixed(2)} c h`
  ].join("\n");
}

function buildSinglePageLandscapeVectorPdf({
  certRecord,
  signerConfig,
  logoJpegBytes = null,
  logoPxW = 0,
  logoPxH = 0,
  measureCtx = null
}) {
  const enc = new TextEncoder();
  const pageW = 841.89;
  const pageH = 595.28;
  const cx = pageW / 2;
  const cy = pageH / 2;
  const pdfY = (topY) => pageH - topY;
  const ops = [];

  const drawCenteredText = (fontKey, sizePt, topBaselineY, text, rgbStr) => {
    const clean = String(text || "");
    const w = measurePdfTextWidthPt(clean, fontKey, sizePt, measureCtx);
    const x = cx - w / 2;
    const y = pdfY(topBaselineY);
    ops.push(`BT /${fontKey} ${sizePt.toFixed(2)} Tf ${rgbStr} rg 1 0 0 1 ${x.toFixed(2)} ${y.toFixed(2)} Tm (${escapePdfTextString(clean)}) Tj ET`);
  };

  const drawCenteredAtX = (xCenter, fontKey, sizePt, topBaselineY, text, rgbStr) => {
    const clean = String(text || "");
    const w = measurePdfTextWidthPt(clean, fontKey, sizePt, measureCtx);
    const x = xCenter - w / 2;
    const y = pdfY(topBaselineY);
    ops.push(`BT /${fontKey} ${sizePt.toFixed(2)} Tf ${rgbStr} rg 1 0 0 1 ${x.toFixed(2)} ${y.toFixed(2)} Tm (${escapePdfTextString(clean)}) Tj ET`);
  };

  // 1. Warm reddish-cream base fill + smooth vector radial parchment shading
  ops.push("0.984 0.941 0.914 rg 0 0 841.89 595.28 re f");
  ops.push("q 0 0 841.89 595.28 re W n /Sh0 sh Q");

  // 2. Crisp Vector Guilloche Circles, Spokes & Center Watermark
  ops.push("q /GSwm gs 0.875 0.757 0.698 RG 0.875 0.757 0.698 rg");
  for (const [r, lw, dash] of [[142, 1.2, "[] 0"], [125, 0.9, "[5 5] 0"], [98, 1.1, "[] 0"], [71, 0.9, "[] 0"]]) {
    ops.push(`${lw} w ${dash} d\n${pdfCirclePathOps(cx, cy, r)}\nS`);
  }
  ops.push("[] 0 d 0.7 w");
  for (let deg = 0; deg < 360; deg += 15) {
    const rad = (deg * Math.PI) / 180;
    const rIn = deg % 30 === 0 ? 58 : 84;
    const rOut = 136;
    ops.push(
      `${(cx + Math.cos(rad) * rIn).toFixed(2)} ${(cy + Math.sin(rad) * rIn).toFixed(2)} m ` +
      `${(cx + Math.cos(rad) * rOut).toFixed(2)} ${(cy + Math.sin(rad) * rOut).toFixed(2)} l S`
    );
  }
  const wmW = measurePdfTextWidthPt("I S C E B", "F2", 44, measureCtx);
  ops.push(`BT /F2 44 Tf 0.875 0.757 0.698 rg 1 0 0 1 ${(cx - wmW / 2).toFixed(2)} ${(cy - 14).toFixed(2)} Tm (I S C E B) Tj ET`);
  ops.push("Q");

  // 3. Full-Frame Double Gold, Inner Navy & Fine Gold Vector Borders + Corner Diamonds
  ops.push("0.831 0.686 0.216 RG 4.8 w 14.40 14.40 813.09 566.48 re S");
  ops.push("0.027 0.102 0.200 RG 1.9 w 22.00 22.00 797.89 551.28 re S");
  ops.push("0.651 0.486 0.000 RG 0.9 w 30.70 30.70 780.49 533.88 re S");
  ops.push("0.831 0.686 0.216 rg");
  for (const [dx, dy] of [[30.7, 30.7], [pageW - 30.7, 30.7], [30.7, pageH - 30.7], [pageW - 30.7, pageH - 30.7]]) {
    const d = 4.6;
    ops.push(`${dx.toFixed(2)} ${(dy + d).toFixed(2)} m ${(dx + d).toFixed(2)} ${dy.toFixed(2)} l ${dx.toFixed(2)} ${(dy - d).toFixed(2)} l ${(dx - d).toFixed(2)} ${dy.toFixed(2)} l h f`);
  }

  // 4. Centered Official ISCEB Logo (high-res pre-blended onto parchment)
  let topCursor = 46;
  if (logoJpegBytes && logoPxW > 0 && logoPxH > 0) {
    const logoPtH = 62;
    const logoPtW = (logoPxW / logoPxH) * logoPtH;
    const logoX = cx - logoPtW / 2;
    const logoY = pdfY(topCursor + logoPtH);
    ops.push(`q ${logoPtW.toFixed(2)} 0 0 ${logoPtH.toFixed(2)} ${logoX.toFixed(2)} ${logoY.toFixed(2)} cm /Im0 Do Q`);
    topCursor += logoPtH + 14;
  } else {
    topCursor += 20;
  }

  // Optional Demo Pill
  if (certRecord.isDemo) {
    const pillW = 162;
    const pillH = 19;
    const px = cx - pillW / 2;
    const pyBottom = pdfY(topCursor + pillH);
    ops.push("0.957 0.886 0.639 rg 0.831 0.686 0.216 RG 1.1 w");
    ops.push(`${pdfRoundRectPathOps(px, pyBottom, pillW, pillH, pillH / 2)}\nB`);
    drawCenteredText("F5", 8.6, topCursor + 12.6, "DEMO CERTIFICATE PREVIEW", "0.027 0.102 0.200");
    topCursor += pillH + 16;
  } else {
    topCursor += 6;
  }

  // 5. Crisp Vector Institution Header, Subtitle & Diamond Divider
  drawCenteredText("F2", 11.6, topCursor, "INSTITUTE OF SUPPLY CHAIN EXCELLENCE BANGLADESH (ISCEB)", "0.027 0.102 0.200");
  topCursor += 15.5;
  drawCenteredText("F5", 9.6, topCursor, "POST GRADUATE DIPLOMA (PGD) IN SUPPLY CHAIN MANAGEMENT", "0.651 0.486 0.000");
  topCursor += 13;

  const divY = pdfY(topCursor);
  ops.push("0.831 0.686 0.216 RG 1.2 w");
  ops.push(`${(cx - 165).toFixed(2)} ${divY.toFixed(2)} m ${(cx - 12).toFixed(2)} ${divY.toFixed(2)} l ${(cx + 12).toFixed(2)} ${divY.toFixed(2)} m ${(cx + 165).toFixed(2)} ${divY.toFixed(2)} l S`);
  ops.push(`0.831 0.686 0.216 rg ${cx.toFixed(2)} ${(divY + 4.2).toFixed(2)} m ${(cx + 4.2).toFixed(2)} ${divY.toFixed(2)} l ${cx.toFixed(2)} ${(divY - 4.2).toFixed(2)} l ${(cx - 4.2).toFixed(2)} ${divY.toFixed(2)} l h f`);
  topCursor += 31;

  // 6. Crisp Vector Main Diploma Title
  const titleLines = wrapPdfVectorLines("POST GRADUATE DIPLOMA (PGD) IN SUPPLY CHAIN MANAGEMENT (SCM)", 690, "F2", 20, measureCtx);
  for (const tl of titleLines) {
    drawCenteredText("F2", 20, topCursor, tl, "0.027 0.102 0.200");
    topCursor += 25;
  }
  topCursor += 6;

  // 7. Conferral & Recipient Name + Vector Gold Underline
  drawCenteredText("F3", 12.6, topCursor, "This authoritative academic credential is officially conferred upon", "0.361 0.294 0.271");
  topCursor += 35;

  const recipientStr = String(certRecord.recipientName || "ISCEB Candidate");
  drawCenteredText("F2", 26, topCursor, recipientStr, "0.027 0.102 0.200");
  const namePtW = measurePdfTextWidthPt(recipientStr, "F2", 26, measureCtx);
  const underlineW = Math.max(205, Math.min(560, namePtW + 44));
  topCursor += 9;
  const ulY = pdfY(topCursor);
  ops.push(`0.831 0.686 0.216 RG 2.0 w ${(cx - underlineW / 2).toFixed(2)} ${ulY.toFixed(2)} m ${(cx + underlineW / 2).toFixed(2)} ${ulY.toFixed(2)} l S`);
  topCursor += 28;

  // 8. Recognition Narrative
  const narrative = "in recognition of successfully completing all 12 Specialization Modules and 48 Executive Lessons, Part 1: Basic (Foundation Level) & Part 2: Advance (Strategic & Analytical Level), Module Examinations, Capstone Project, and Final Comprehensive Assessment.";
  const narrativeLines = wrapPdfVectorLines(narrative, 660, "F1", 11.4, measureCtx);
  for (const nl of narrativeLines) {
    drawCenteredText("F1", 11.4, topCursor, nl, "0.243 0.200 0.184");
    topCursor += 17;
  }

  // 9. Symmetrical 3-Column Vector Signatories
  const sigTopY = pageH - 89;
  const leftX = 175;
  const rightX = pageW - 175;
  const sigPdfY = pdfY(sigTopY);
  ops.push("0.549 0.478 0.447 RG 1.0 w");
  ops.push(`${(leftX - 88).toFixed(2)} ${sigPdfY.toFixed(2)} m ${(leftX + 88).toFixed(2)} ${sigPdfY.toFixed(2)} l S`);
  ops.push(`${(rightX - 88).toFixed(2)} ${sigPdfY.toFixed(2)} m ${(rightX + 88).toFixed(2)} ${sigPdfY.toFixed(2)} l S`);

  drawCenteredAtX(leftX, "F2", 12, sigTopY + 16, signerConfig.leftSignerName, "0.027 0.102 0.200");
  drawCenteredAtX(rightX, "F2", 12, sigTopY + 16, signerConfig.rightSignerName, "0.027 0.102 0.200");
  if (signerConfig.leftSignerTitle) {
    drawCenteredAtX(leftX, "F5", 10, sigTopY + 31, signerConfig.leftSignerTitle, "0.651 0.486 0.000");
  }
  drawCenteredAtX(rightX, "F5", 10, sigTopY + 31, signerConfig.rightSignerTitle, "0.651 0.486 0.000");

  drawCenteredAtX(cx, "F5", 10.6, sigTopY + 13, `Serial: ${certRecord.serial}`, "0.651 0.486 0.000");
  drawCenteredAtX(cx, "F4", 9.6, sigTopY + 28, `Issued: ${certRecord.issuedDate}`, "0.353 0.431 0.522");

  const streamStr = ops.join("\n") + "\n";
  const hasLogo = Boolean(logoJpegBytes && logoPxW > 0 && logoPxH > 0);
  const chunks = [];
  let offset = 0;
  const pushBytes = (u8) => {
    chunks.push(u8);
    offset += u8.length;
  };
  const pushText = (s) => pushBytes(enc.encode(s));

  pushText("%PDF-1.4\n");
  pushBytes(new Uint8Array([0x25, 0xe2, 0xe3, 0xcf, 0xd3, 0x0a]));
  const offsets = [0];
  const addObj = (body) => {
    const id = offsets.length;
    offsets.push(offset);
    pushText(`${id} 0 obj\n${body}\nendobj\n`);
    return id;
  };

  addObj("<< /Type /Catalog /Pages 2 0 R >>");
  addObj("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
  const xObjDict = hasLogo ? " /XObject << /Im0 12 0 R >>" : "";
  addObj(
    `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${pageW.toFixed(2)} ${pageH.toFixed(2)}] ` +
      `/Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R /F4 7 0 R /F5 8 0 R >> ` +
      `/Shading << /Sh0 9 0 R >> /ExtGState << /GSwm 10 0 R >>${xObjDict} >> /Contents 11 0 R >>`
  );
  addObj("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Roman /Encoding /WinAnsiEncoding >>");
  addObj("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Bold /Encoding /WinAnsiEncoding >>");
  addObj("<< /Type /Font /Subtype /Type1 /BaseFont /Times-Italic /Encoding /WinAnsiEncoding >>");
  addObj("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
  addObj("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
  addObj(
    "<< /ShadingType 3 /ColorSpace /DeviceRGB /Coords [420.95 310.0 15 420.95 297.64 520] /Extend [true true] " +
      "/Function << /FunctionType 3 /Domain [0 1] /Functions [ " +
      "<< /FunctionType 2 /Domain [0 1] /C0 [0.9922 0.9686 0.9529] /C1 [0.9843 0.9412 0.9137] /N 1 >> " +
      "<< /FunctionType 2 /Domain [0 1] /C0 [0.9843 0.9412 0.9137] /C1 [0.9529 0.8667 0.8196] /N 1 >> " +
      "] /Bounds [0.65] /Encode [0 1 0 1] >> >>"
  );
  addObj("<< /Type /ExtGState /ca 0.34 /CA 0.34 >>");
  const streamBytes = enc.encode(streamStr);
  offsets.push(offset);
  pushText(`11 0 obj\n<< /Length ${streamBytes.length} >>\nstream\n`);
  pushBytes(streamBytes);
  pushText("\nendstream\nendobj\n");

  if (hasLogo) {
    offsets.push(offset);
    pushText(
      `12 0 obj\n<< /Type /XObject /Subtype /Image /Width ${logoPxW} /Height ${logoPxH} ` +
        `/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${logoJpegBytes.length} >>\nstream\n`
    );
    pushBytes(logoJpegBytes);
    pushText("\nendstream\nendobj\n");
  }

  const xrefStart = offset;
  pushText(`xref\n0 ${offsets.length}\n0000000000 65535 f \n`);
  for (let i = 1; i < offsets.length; i++) {
    pushText(`${String(offsets[i]).padStart(10, "0")} 00000 n \n`);
  }
  pushText(`trailer\n<< /Size ${offsets.length} /Root 1 0 R >>\nstartxref\n${xrefStart}\n%%EOF\n`);

  const out = new Uint8Array(offset);
  let pos = 0;
  for (const c of chunks) {
    out.set(c, pos);
    pos += c.length;
  }
  return out;
}

function buildSinglePageLandscapePdfFromJpeg(jpegBytes, pxWidth, pxHeight) {
  const enc = new TextEncoder();
  const pageW = 841.89;
  const pageH = 595.28;
  const contentStreamStr = `q ${pageW.toFixed(2)} 0 0 ${pageH.toFixed(2)} 0 0 cm /Im0 Do Q\n`;

  const chunks = [];
  let offset = 0;
  const pushBytes = (u8) => {
    chunks.push(u8);
    offset += u8.length;
  };
  const pushText = (str) => pushBytes(enc.encode(str));

  pushText("%PDF-1.4\n%\xFF\xFF\xFF\xFF\n");
  const offsets = [0];

  offsets.push(offset);
  pushText("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

  offsets.push(offset);
  pushText("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

  offsets.push(offset);
  pushText(
    `3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${pageW.toFixed(2)} ${pageH.toFixed(2)}] /Resources << /XObject << /Im0 4 0 R >> >> /Contents 5 0 R >>\nendobj\n`
  );

  offsets.push(offset);
  pushText(
    `4 0 obj\n<< /Type /XObject /Subtype /Image /Width ${pxWidth} /Height ${pxHeight} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpegBytes.length} >>\nstream\n`
  );
  pushBytes(jpegBytes);
  pushText("\nendstream\nendobj\n");

  offsets.push(offset);
  pushText(`5 0 obj\n<< /Length ${contentStreamStr.length} >>\nstream\n${contentStreamStr}endstream\nendobj\n`);

  const xrefStart = offset;
  pushText("xref\n0 6\n0000000000 65535 f \n");
  for (let i = 1; i <= 5; i++) {
    pushText(`${String(offsets[i]).padStart(10, "0")} 00000 n \n`);
  }
  pushText(`trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n${xrefStart}\n%%EOF\n`);

  const out = new Uint8Array(offset);
  let pos = 0;
  for (const c of chunks) {
    out.set(c, pos);
    pos += c.length;
  }
  return out;
}

async function exportLandscapeCertificatePdf(certRecord, signerConfig, { previewOnly = false } = {}) {
  if (!certRecord) return;
  showToast(previewOnly ? "Opening crisp HD Certificate viewer..." : "Preparing crisp Vector Landscape Certificate PDF...");

  const measureCanvas = document.createElement("canvas");
  const measureCtx = measureCanvas.getContext("2d");

  let logoJpegBytes = null;
  let logoPxW = 0;
  let logoPxH = 0;
  const logoImg = await loadImageAsset("./isceb-logo.webp");
  if (logoImg && logoImg.width > 0 && logoImg.height > 0) {
    logoPxW = logoImg.naturalWidth || logoImg.width;
    logoPxH = logoImg.naturalHeight || logoImg.height;
    const logoCanvas = document.createElement("canvas");
    logoCanvas.width = logoPxW;
    logoCanvas.height = logoPxH;
    const lctx = logoCanvas.getContext("2d");
    lctx.fillStyle = "#FDF7F3";
    lctx.fillRect(0, 0, logoPxW, logoPxH);
    lctx.globalCompositeOperation = "multiply";
    lctx.drawImage(logoImg, 0, 0, logoPxW, logoPxH);
    const logoBlob = await new Promise((resolve) => logoCanvas.toBlob(resolve, "image/jpeg", 0.98));
    if (logoBlob) {
      logoJpegBytes = new Uint8Array(await logoBlob.arrayBuffer());
    }
  }

  const pdfBytes = buildSinglePageLandscapeVectorPdf({
    certRecord,
    signerConfig,
    logoJpegBytes,
    logoPxW,
    logoPxH,
    measureCtx
  });
  const pdfBlob = new Blob([pdfBytes], { type: "application/pdf" });
  const safeSerial = String(certRecord.serial || "ISCEB-PGD").replace(/[^A-Za-z0-9_-]/g, "_");
  const fileName = `ISCEB-Certificate-${safeSerial}.pdf`;
  const pdfUrl = URL.createObjectURL(pdfBlob);

  if (!previewOnly) {
    const dlLink = document.createElement("a");
    dlLink.href = pdfUrl;
    dlLink.download = fileName;
    document.body.appendChild(dlLink);
    dlLink.click();
    dlLink.remove();
  }

  presentMediaPreviewModal(
    {
      id: `cert-pdf-${safeSerial}`,
      type: "PDF",
      title: `ISCEB Official Landscape Certificate (${certRecord.serial})`,
      fileName,
      fileSizeBytes: pdfBlob.size,
      fileSizeFormatted: formatBytes(pdfBlob.size)
    },
    pdfUrl,
    pdfBlob
  );
  showToast(previewOnly ? "Zoom in up to 500% — crisp Vector PDF ready." : "Crisp Vector Landscape Certificate PDF ready.");
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
  const leftSignerName = state.settings.leftSignerName || "Hasina Akter Khan";
  const leftSignerTitle = state.settings.leftSignerTitle || "Chairman & CEO";
  const rightSignerName = state.settings.authorizedSignerName || "Md. Amirul Islam";
  const rightSignerTitle = state.settings.authorizedSignerTitle || "Founder of ISCEB";

  return `
    <div class="two-col-grid no-print">
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
          ${
            displayedCert
              ? `<button type="button" class="btn btn-primary" data-action="download-certificate-pdf">Save Landscape PDF</button>
                 <button type="button" class="btn btn-outline" data-action="zoom-certificate-pdf">Zoom / View HD</button>
                 <button type="button" class="btn btn-outline" onclick="window.print()">Print</button>`
              : ""
          }
        </div>
      </div>

      <div class="card">
        <h3 style="margin: 0 0 10px; color: var(--navy);">Verify Certificate Serial</h3>
        <p style="margin: 0 0 10px; font-size: 13px; color: var(--muted);">Enter an ISCEB Certificate ID to validate authenticity locally.</p>
        <div style="display: flex; gap: 8px; align-items: center;">
          <input type="text" id="verifyCertInput" class="input" style="min-width: 0; flex: 1;" placeholder="e.g. ${escapeHtml(cert?.serial || "ISCEB-PGD-2026-1001")}" />
          <button type="button" class="btn btn-primary btn-sm" style="white-space: nowrap; flex-shrink: 0;" data-action="verify-cert">Verify</button>
        </div>
        <div id="verifyCertResult" style="margin-top: 10px; font-size: 13px;"></div>
      </div>
    </div>

    ${
      displayedCert
        ? `
      <div class="cert-sheet">
        <div class="cert-sheet-inner">
          <div class="cert-logo-wrap">
            <img src="./isceb-logo.webp" alt="ISCEB" />
          </div>
          ${displayedCert.isDemo ? `<div class="cert-demo-pill">DEMO CERTIFICATE PREVIEW</div>` : ""}
          <div class="cert-institution">
            Institute of Supply Chain Excellence Bangladesh (ISCEB)
          </div>
          <div class="cert-subtitle">Post Graduate Diploma (PGD) in Supply Chain Management</div>
          <h2 class="cert-title">POST GRADUATE DIPLOMA (PGD) IN SUPPLY CHAIN MANAGEMENT (SCM)</h2>
          <p class="cert-confer">This authoritative academic credential is officially conferred upon</p>
          <div class="cert-recipient">
            ${escapeHtml(displayedCert.recipientName)}
          </div>
          <p class="cert-narrative">
            in recognition of successfully completing all 12 Specialization Modules and 48 Executive Lessons, Part 1: Basic (Foundation Level) &amp; Part 2: Advance (Strategic &amp; Analytical Level), Module Examinations, Capstone Project, and Final Comprehensive Assessment.
          </p>
          <div class="cert-signatories-grid">
            <div class="cert-sig-col">
              <div class="cert-sig-space" aria-hidden="true"></div>
              <div class="cert-sig-line">${escapeHtml(leftSignerName)}</div>
              ${leftSignerTitle ? `<div class="cert-sig-title">${escapeHtml(leftSignerTitle)}</div>` : ""}
            </div>
            <div class="cert-sig-col">
              <div style="font-weight: 800; color: var(--gold-dark);">Serial: ${escapeHtml(displayedCert.serial)}</div>
              <div style="color: var(--muted);">Issued: ${escapeHtml(displayedCert.issuedDate)}</div>
            </div>
            <div class="cert-sig-col">
              <div class="cert-sig-space" aria-hidden="true"></div>
              <div class="cert-sig-line">${escapeHtml(rightSignerName)}</div>
              <div class="cert-sig-title">${escapeHtml(rightSignerTitle)}</div>
            </div>
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
  const devExtra = member.id === "member-developer-nafiul" ? state.developerProfile : null;

  return `
    <div style="display: grid; gap: 14px;">
      <div class="card-navy" style="border-radius: 14px; padding: 18px; display: flex; gap: 16px; align-items: center; flex-wrap: wrap;">
        ${renderMemberAvatarHtml(member, 92)}
        <div style="flex: 1; min-width: 220px;">
          <span class="badge badge-gold">${escapeHtml(member.designation || "ISCEB Member")}</span>
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
          <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 4px;">Education</div>
          <div style="font-size: 13px; color: #334155; white-space: pre-line;">${escapeHtml(member.educationalQualification)}</div>
        </div>` : ""}

      ${member.professionalExperience ? `
        <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px;">
          <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 4px;">Professional Experience</div>
          <div style="font-size: 13px; color: #334155; white-space: pre-line;">${escapeHtml(member.professionalExperience)}</div>
        </div>` : ""}

      <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px;">
        <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 6px;">Core Competencies & Skills</div>
        ${skillsHtml}
      </div>

      ${member.biography ? `
        <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px;">
          <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 4px;">About Me & Biography</div>
          <div style="font-size: 13px; color: #334155; white-space: pre-line; line-height: 1.55;">${escapeHtml(member.biography)}</div>
        </div>` : ""}

      ${devExtra ? `
        ${devExtra.mission || devExtra.vision ? `
          <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px; display: grid; gap: 6px;">
            <div style="font-weight: 800; color: var(--navy); font-size: 13px;">Mission &amp; Vision</div>
            ${devExtra.mission ? `<div style="font-size: 13px; color: #334155;"><strong>Mission:</strong> ${escapeHtml(devExtra.mission)}</div>` : ""}
            ${devExtra.vision ? `<div style="font-size: 13px; color: #334155;"><strong>Vision:</strong> ${escapeHtml(devExtra.vision)}</div>` : ""}
          </div>` : ""}
        ${devExtra.projectInformation ? `
          <div style="background: var(--surface-muted); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px;">
            <div style="font-weight: 800; color: var(--navy); font-size: 13px; margin-bottom: 4px;">About This Project (ISCEB App)</div>
            <div style="font-size: 13px; color: #334155; line-height: 1.5;">${escapeHtml(devExtra.projectInformation)}</div>
          </div>` : ""}
        ${devExtra.personalPhilosophy || devExtra.personalInterests ? `
          <div style="background: #FDF7F3; border: 1px solid #EBD3C5; border-radius: 12px; padding: 12px 14px; display: grid; gap: 6px;">
            <div style="font-weight: 800; color: var(--navy); font-size: 13px;">Personal Philosophy &amp; Interests</div>
            ${devExtra.personalPhilosophy ? `<div style="font-size: 13px; color: #1E293B; font-style: italic;">“${escapeHtml(devExtra.personalPhilosophy)}”</div>` : ""}
            ${devExtra.personalInterests ? `<div style="font-size: 12.5px; color: #475569;"><strong>Interests:</strong> ${escapeHtml(devExtra.personalInterests)}</div>` : ""}
          </div>` : ""}
      ` : ""}

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
          (m) => `
        <div class="card" style="margin-bottom: 0; display: flex; flex-direction: column; justify-content: space-between; border: 1px solid #E2E8F0; transition: box-shadow .15s ease;">
          <div>
            <div style="display: flex; gap: 14px; align-items: center; margin-bottom: 10px;">
              ${renderMemberAvatarHtml(m, 72)}
              <div style="min-width: 0; flex: 1;">
                ${m.designation ? `<div style="display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-bottom: 4px;"><span class="badge badge-gold">${escapeHtml(m.designation)}</span></div>` : ""}
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
            Signatories — Left: <strong>${escapeHtml(state.settings.leftSignerName || "Hasina Akter Khan")}</strong> (${escapeHtml(state.settings.leftSignerTitle || "Chairman & CEO")}) • Right: <strong>${escapeHtml(state.settings.authorizedSignerName || "Md. Amirul Islam")}</strong> (${escapeHtml(state.settings.authorizedSignerTitle || "Founder of ISCEB")})
          </div>
        </div>

        <div style="margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--border); display: grid; gap: 8px;">
          <div style="font-size: 12.5px; font-weight: 800; color: var(--navy);">Exams, MCQ Questions &amp; Deadlined Assignments</div>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <button type="button" class="btn btn-primary btn-sm" data-action="admin-create-exam">+ Create Exam &amp; Questions</button>
            <button type="button" class="btn btn-gold btn-sm" data-action="admin-create-assignment">+ Create Assignment &amp; Deadline</button>
            <button type="button" class="btn btn-outline btn-sm" data-nav="exams">Manage All (${state.exams.length} Exams / ${state.assignments.length} Tasks)</button>
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

    ${renderAdminRegisteredStudentsCard()}
  `;
}

function getMergedRegisteredStudentsForAdmin() {
  const byKey = new Map();
  for (const row of cloudRegisteredUsers || []) {
    const key = String(row.email || row.uid || row.studentId).trim().toLowerCase();
    if (key) byKey.set(key, { ...row, source: "CLOUD" });
  }
  for (const loc of state.customStudents || []) {
    const key = String(loc.email || loc.id || loc.studentId).trim().toLowerCase();
    if (!key) continue;
    if (!byKey.has(key)) {
      byKey.set(key, { ...loc, source: "LOCAL" });
    }
  }
  return Array.from(byKey.values());
}

function renderAdminRegisteredStudentsCard() {
  const allAccounts = getMergedRegisteredStudentsForAdmin();
  const studentAccounts = allAccounts.filter((u) => (u.role || "STUDENT") === "STUDENT");
  const cloudCount = studentAccounts.filter((u) => u.source === "CLOUD").length;
  const localCount = studentAccounts.filter((u) => u.source === "LOCAL").length;
  const q = String(adminStudentSearch || "").trim().toLowerCase();
  const filtered = q
    ? allAccounts.filter((u) =>
        `${u.fullName || ""} ${u.studentId || ""} ${u.email || ""} ${u.mobileNumber || ""} ${u.organization || ""}`
          .toLowerCase()
          .includes(q)
      )
    : allAccounts;

  return `
    <div class="card" id="adminRegisteredStudentsCard" style="margin-top: 16px;">
      <div class="flex-header-between" style="margin-bottom: 12px;">
        <div>
          <span class="badge badge-navy">Student Enrollment Roster</span>
          <h3 style="margin: 6px 0 2px; color: var(--navy);">Registered Students &amp; Cloud Directory</h3>
          <div style="font-size: 12px; color: var(--muted);">
            Students who sign in with Google Cloud Sync appear automatically across both Android &amp; Web Admin rosters.
          </div>
        </div>
        <div style="display: flex; gap: 8px; flex-wrap: wrap;">
          <button type="button" class="btn btn-primary btn-sm" data-action="refresh-cloud-users">Refresh Cloud Roster</button>
          ${!cloudUser ? `<button type="button" class="btn btn-gold btn-sm" data-action="trigger-google-auth">Sign in Google (Cloud Sync)</button>` : ""}
        </div>
      </div>

      <div class="stats-grid" style="margin-bottom: 12px;">
        <div class="stat-card">
          <div class="stat-label">Registered Students</div>
          <div class="stat-value">${studentAccounts.length}</div>
          <div class="stat-sub">Total Enrolled</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Cloud Synced</div>
          <div class="stat-value">${cloudCount}</div>
          <div class="stat-sub">All Devices (Firestore)</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Local Browser Only</div>
          <div class="stat-value">${localCount}</div>
          <div class="stat-sub">Pending Google Sync</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">All Directory Users</div>
          <div class="stat-value">${allAccounts.length}</div>
          <div class="stat-sub">${cloudUsersLoadedAt ? "Live Synced" : "Local Snapshot"}</div>
        </div>
      </div>

      ${
        cloudUsersAccessNote
          ? `<div style="margin-bottom: 12px; padding: 10px 12px; border-radius: 10px; background: var(--warn-bg); color: var(--warn); font-size: 12px; font-weight: 600;">${escapeHtml(cloudUsersAccessNote)}</div>`
          : ""
      }

      <div style="margin-bottom: 12px;">
        <input type="search" id="adminStudentSearchInput" class="input" placeholder="Search registered students by name, Student ID, email, or phone..." value="${escapeHtml(adminStudentSearch)}" />
      </div>

      ${
        filtered.length === 0
          ? `<div style="padding: 16px; border: 1px dashed var(--border); border-radius: 12px; color: var(--muted); font-size: 13px; text-align: center;">
              ${allAccounts.length === 0 ? "No student registrations synced yet. Once a student signs in with Google Cloud Auto-Sync, their full profile appears here automatically." : "No students match your search filter."}
            </div>`
          : `<div class="registered-student-grid">
              ${filtered
                .map((stu) => {
                  const initials = String(stu.fullName || "S")
                    .split(" ")
                    .filter(Boolean)
                    .map((p) => p[0])
                    .slice(0, 2)
                    .join("")
                    .toUpperCase();
                  return `
                    <div class="registered-student-card">
                      <div class="flex-header-between" style="align-items: flex-start;">
                        <div style="display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1 1 220px;">
                          ${
                            stu.photoUrl
                              ? `<img src="${escapeHtml(stu.photoUrl)}" alt="${escapeHtml(stu.fullName)}" class="portrait-avatar" style="width: 46px; height: 46px; border: 2px solid var(--gold); flex-shrink: 0; object-fit: cover; object-position: center 20%;" />`
                              : `<div style="width: 46px; height: 46px; border-radius: 50%; background: var(--navy); color: var(--gold); display: grid; place-items: center; font-weight: 800; font-size: 14px; flex-shrink: 0;">${escapeHtml(initials || "S")}</div>`
                          }
                          <div style="min-width: 0; flex: 1;">
                            <div style="font-weight: 800; color: var(--navy); font-size: 14.5px; word-break: normal; overflow-wrap: break-word;">${escapeHtml(stu.fullName)}</div>
                            <div style="font-size: 12px; color: var(--gold-dark); font-weight: 700;">${escapeHtml(stu.studentId || "ID Pending")} • ${escapeHtml(stu.organization || "ISCEB Enrolled")}</div>
                          </div>
                        </div>
                        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                          <span class="badge ${stu.source === "CLOUD" ? "badge-ok" : "badge-warn"}">${stu.source === "CLOUD" ? "Cloud Synced" : "Local Device"}</span>
                          <span class="badge ${stu.isVerified ? "badge-gold" : "badge-navy"}">${escapeHtml(stu.role || "STUDENT")} • ${stu.isVerified ? "Verified" : escapeHtml(stu.accountStatus || "ACTIVE")}</span>
                        </div>
                      </div>
                      <div style="font-size: 12px; color: #334155; overflow-wrap: anywhere;">
                        <strong>Email:</strong> ${escapeHtml(stu.email || "—")}${stu.mobileNumber ? ` &nbsp;•&nbsp; <strong>Mobile:</strong> ${escapeHtml(stu.mobileNumber)}` : ""}
                      </div>
                      <div style="font-size: 11.5px; color: var(--muted);">
                        <strong>Program:</strong> ${escapeHtml(stu.enrolledProgram || GUEST_USER.enrolledProgram)}
                      </div>
                    </div>
                  `;
                })
                .join("")}
            </div>`
      }
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
        <div class="profile-hero-header">
          <div class="profile-hero-identity">
            ${
              isAdminUser()
                ? `<img src="${escapeHtml(user.photoUrl || (user.id === ADMIN_AMIRUL_ID ? "./admin-amirul.webp" : "./admin-nafiul.webp"))}" alt="${escapeHtml(user.fullName)}" class="portrait-avatar" style="width: 60px; height: 60px; object-fit: cover; object-position: center 20%; border: 2px solid var(--gold); flex-shrink: 0;" />`
                : `<label for="signedInStudentPhotoInput" title="${user.photoUrl ? "Change Profile Photo" : "Add Profile Photo"}" style="cursor: pointer; position: relative; flex-shrink: 0; display: inline-block;">
                    ${
                      user.photoUrl
                        ? `<img src="${escapeHtml(user.photoUrl)}" alt="${escapeHtml(user.fullName)}" class="portrait-avatar" style="width: 60px; height: 60px; object-fit: cover; object-position: center 20%; border: 2px solid var(--gold); display: block;" />`
                        : `<div style="width: 60px; height: 60px; border-radius: 50%; background: var(--gold); color: var(--navy-dark); display: grid; place-items: center; font-weight: 800; font-size: 19px;">${escapeHtml(user.fullName.split(" ").filter(Boolean).map((p) => p[0]).slice(0, 2).join("").toUpperCase())}</div>`
                    }
                    <span style="position: absolute; right: -2px; bottom: -2px; width: 22px; height: 22px; border-radius: 50%; background: var(--navy-dark); color: var(--gold); border: 1.5px solid var(--gold); display: grid; place-items: center; font-size: 11px; font-weight: 800; line-height: 1;" aria-hidden="true">+</span>
                  </label>
                  <input type="file" id="signedInStudentPhotoInput" accept="image/*" style="display: none;" />`
            }
            <div class="profile-hero-meta">
              <span class="badge badge-gold">${isAdminUser() ? "VERIFIED ADMINISTRATOR" : "VERIFIED STUDENT • ACTIVE"}</span>
              <h2 class="profile-hero-name">${escapeHtml(user.fullName)}</h2>
              <div class="profile-hero-line" style="color: #E2E8F0;">${escapeHtml(user.studentId || user.designation || "PGD Candidate")} • ${escapeHtml(user.organization || "ISCEB")}</div>
              <div class="profile-hero-line" style="font-size: 12px; color: #B0C2D6; margin-top: 2px;">${escapeHtml(user.email || "")}${user.mobileNumber ? ` • ${escapeHtml(user.mobileNumber)}` : ""}</div>
            </div>
          </div>
          <div class="profile-hero-actions">
            ${!isAdminUser() ? `<label for="signedInStudentPhotoInput" class="btn btn-outline btn-sm" style="cursor: pointer; background: rgba(212,175,55,0.16); color: #F4E2A3; border-color: rgba(212,175,55,0.45);">${user.photoUrl ? "Change Photo" : "Add Photo"}</label>` : ""}
            ${!isAdminUser() ? `<button type="button" class="btn btn-gold btn-sm" data-action="open-edit-student-modal">Edit Profile</button>` : ""}
            <button type="button" class="btn btn-outline btn-sm" data-action="${isAdminUser() ? "admin-logout" : "student-signout"}" style="background: rgba(255,255,255,0.1); color: #fff; border-color: rgba(255,255,255,0.3);">
              Sign Out
            </button>
          </div>
        </div>
        <div style="margin-top: 12px; padding-top: 10px; border-top: 1px solid rgba(255,255,255,0.16); font-size: 12px; color: #F4E2A3;">
          Program: ${escapeHtml(user.enrolledProgram || GUEST_USER.enrolledProgram)}
        </div>
        ${
          !isAdminUser() && !cloudUser
            ? `<div style="margin-top: 8px; padding: 8px 10px; border-radius: 8px; background: rgba(255,255,255,0.08); font-size: 11.5px; color: #CBD5E1;">
                Saved on this device. Connect <strong>Google Cloud Auto-Sync</strong> below so your profile &amp; progress sync to Admin roster.
              </div>`
            : ""
        }
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
          <div style="display: flex; align-items: center; gap: 12px; flex: 1 1 200px; min-width: 0;">
            <div class="profile-icon-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z"/></svg>
            </div>
            <div style="min-width: 0;">
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
          <div style="display: flex; align-items: center; gap: 12px; flex: 1 1 200px; min-width: 0;">
            <div class="profile-icon-box">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            </div>
            <div style="min-width: 0;">
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
          ${
            !isRunningStandalone()
              ? `<button type="button" class="btn btn-gold btn-sm" data-action="trigger-pwa-install">${deferredInstallPrompt ? "Install App (No Badge)" : "Install Guide"}</button>`
              : ""
          }
          <button type="button" class="btn btn-outline btn-sm" data-action="check-pwa-update-now">Auto-Update / Check Latest</button>
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
  if (isExamExpired(exam) && !isAdminUser()) {
    showToast(`Examination window closed on ${formatDeadlineLabel(exam.openUntilMs)} (Time Up).`, true);
    return;
  }
  const durationSec = Math.max(60, Number(exam.durationMinutes || 20) * 60);
  const cutoffSec = exam.openUntilMs
    ? Math.max(10, Math.floor((Number(exam.openUntilMs) - Date.now()) / 1000))
    : durationSec;
  let remainingSec = Math.min(durationSec, cutoffSec);
  examModalSession = { exam, remainingSec };

  const m0 = Math.floor(remainingSec / 60);
  const s0 = String(remainingSec % 60).padStart(2, "0");

  const html = `
    <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; background: var(--navy); color: #fff; padding: 10px 14px; border-radius: 10px; margin-bottom: 14px;">
      <span>${(exam.questions || []).length} Questions • ${exam.totalMarks} Total Marks • Auto-submits at 00:00</span>
      <strong id="examTimerBadge" style="color: ${remainingSec <= 60 ? "#FCA5A5" : "var(--gold)"}; font-family: monospace;">Time Left: ${m0}:${s0}</strong>
    </div>
    <form id="activeExamForm">
      ${(exam.questions || [])
        .map(
          (q, idx) => `
        <div style="margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid var(--border);">
          <div style="font-weight: 700; color: var(--navy); margin-bottom: 6px;">${idx + 1}. ${escapeHtml(q.question)} (${q.marks} Marks)</div>
          ${(q.options || [])
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
      if (remainingSec <= 60) {
        badge.style.color = "#FCA5A5";
      }
    }
    if (remainingSec <= 0) {
      submitActiveExam(true);
    }
  }, 1000);
}

function submitActiveExam(isAutoTimeout = false) {
  if (!examModalSession) return;
  const { exam } = examModalSession;
  const form = document.getElementById("activeExamForm");
  let scored = 0;
  (exam.questions || []).forEach((q, idx) => {
    const selected = form?.querySelector(`input[name="eq_${idx}"]:checked`)?.value;
    if (selected !== undefined && Number(selected) === Number(q.correctOptionIndex)) {
      scored += Number(q.marks || 0);
    }
  });
  const totalMarks = Number(exam.totalMarks || 0) || (exam.questions || []).reduce((s, q) => s + Number(q.marks || 0), 0) || 1;
  const pct = totalMarks > 0 ? Math.round((scored / totalMarks) * 100) : 0;
  const passed = pct >= (exam.passingPercentage || 60);
  const uid = getActiveUser().id;
  if (!state.examAttemptsByUser[uid]) state.examAttemptsByUser[uid] = [];
  state.examAttemptsByUser[uid].push({
    examId: exam.id,
    scoredMarks: scored,
    totalMarks,
    percentage: pct,
    passed,
    isAutoSubmittedOnTimeout: Boolean(isAutoTimeout),
    timestamp: Date.now()
  });
  saveState();
  closeModal();
  const prefix = isAutoTimeout ? "Time Up! Exam auto-submitted" : "Exam submitted";
  showToast(`${prefix}: ${scored}/${totalMarks} (${pct}%) — ${passed ? "PASSED" : "Try Again"}`, !passed);
  render();
}

async function runAppUpdateCheck() {
  showToast("Checking GitHub Pages for latest ISCEB app update...");
  await fetchVersionJson();
  let swTriggeredUpdate = false;
  if ("serviceWorker" in navigator) {
    try {
      const reg = window.__iscebSwRegistration || (await navigator.serviceWorker.getRegistration("./"));
      if (reg) {
        await reg.update();
        if (reg.waiting) {
          swTriggeredUpdate = true;
          reg.waiting.postMessage({ type: "SKIP_WAITING" });
          showToast("New ISCEB update ready — applying update now...");
          return;
        }
        if (reg.installing) {
          swTriggeredUpdate = true;
          showToast("Downloading latest ISCEB update...");
          await new Promise((resolve) => {
            const worker = reg.installing;
            const timer = setTimeout(resolve, 6000);
            worker.addEventListener("statechange", () => {
              if (worker.state === "installed" || worker.state === "redundant") {
                clearTimeout(timer);
                resolve();
              }
            });
          });
          if (reg.waiting) {
            reg.waiting.postMessage({ type: "SKIP_WAITING" });
            showToast("Applying new ISCEB update...");
            return;
          }
        }
      }
    } catch (_) {}
  }

  const verRes = await fetch(`./pwa-version.json?t=${Date.now()}`, { cache: "no-store" }).catch(() => null);
  if (verRes && verRes.ok) {
    const info = await verRes.json().catch(() => null);
    if (info && info.buildId) {
      const prevBuild = localStorage.getItem("isceb_pwa_build_id");
      pwaBuildInfo = info;
      localStorage.setItem("isceb_pwa_build_id", info.buildId);
      if ((prevBuild && prevBuild !== info.buildId) || swTriggeredUpdate) {
        if ("caches" in window) {
          try {
            const keys = await caches.keys();
            await Promise.all(keys.filter((k) => k.startsWith("isceb-")).map((k) => caches.delete(k)));
          } catch (_) {}
        }
        showToast(`Applying ISCEB Build ${info.buildId}...`);
        setTimeout(() => window.location.reload(), 350);
        return;
      }
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
        const maxDim = 200;
        const scale = Math.min(1, maxDim / Math.max(img.width || maxDim, img.height || maxDim));
        const w = Math.max(1, Math.round(img.width * scale));
        const h = Math.max(1, Math.round(img.height * scale));
        const canvas = document.createElement("canvas");
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, w, h);
        let out = canvas.toDataURL("image/jpeg", 0.8);
        for (const q of [0.65, 0.5, 0.35]) {
          if (out.length <= 75000) break;
          out = canvas.toDataURL("image/jpeg", q);
        }
        resolve(out);
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
    if (btn.dataset.view === "courses") courseDetailOpen = false;
    saveState();
    closeMobileDrawer();
    render();
    window.scrollTo({ top: 0, behavior: "instant" });
  });

  document.getElementById("mobileBottomNav")?.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-view]");
    if (!btn) return;
    state.currentView = btn.dataset.view;
    if (btn.dataset.view === "courses") courseDetailOpen = false;
    saveState();
    closeMobileDrawer();
    render();
    window.scrollTo({ top: 0, behavior: "instant" });
  });

  window.addEventListener("popstate", () => {
    if (state.currentView === "courses" && courseDetailOpen) {
      courseDetailOpen = false;
      render();
      window.scrollTo({ top: 0, behavior: "instant" });
    }
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

  const triggerResumeSync = () => {
    const now = Date.now();
    if (now - lastAutoResumeSyncMs < 8000) return;
    lastAutoResumeSyncMs = now;
    if (cloudUser) {
      syncFromCloudFirestore(true);
    }
  };
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") triggerResumeSync();
  });
  window.addEventListener("online", triggerResumeSync);

  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    deferredInstallPrompt = e;
    const btn = document.getElementById("installPwaBtn");
    if (btn) btn.style.display = "inline-flex";
    render();
  });

  window.addEventListener("appinstalled", () => {
    deferredInstallPrompt = null;
    const btn = document.getElementById("installPwaBtn");
    if (btn) btn.style.display = "none";
    showToast("ISCEB App installed! Future GitHub updates will auto-sync silently.");
    render();
  });

  document.getElementById("installPwaBtn").addEventListener("click", triggerPwaInstall);

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
      courseDetailOpen = true;
      try { window.history.pushState({ iscebClass: state.selectedClassId }, ""); } catch (_) {}
      saveState();
      render();
      window.scrollTo({ top: 0, behavior: "instant" });
    } else if (action === "select-class") {
      state.selectedClassId = actBtn.dataset.classId;
      courseDetailOpen = true;
      try { window.history.pushState({ iscebClass: state.selectedClassId }, ""); } catch (_) {}
      saveState();
      render();
      window.scrollTo({ top: 0, behavior: "instant" });
    } else if (action === "back-to-modules") {
      courseDetailOpen = false;
      render();
      window.scrollTo({ top: 0, behavior: "instant" });
    } else if (action === "open-personal-attachment") {
      const cid = actBtn.dataset.classId;
      const attId = actBtn.dataset.attId;
      const att = (getClassProgress(cid).localAttachments || []).find((a) => a.id === attId);
      if (att) {
        openSyncedCloudMedia({
          id: att.id,
          title: att.name,
          fileName: att.name,
          fileSizeBytes: att.size,
          fileSizeFormatted: formatBytes(att.size),
          type: String(att.name || "").toLowerCase().endsWith(".pdf") ? "PDF" : "DOCUMENT"
        });
      }
    } else if (action === "signin-and-open-pending-media") {
      closeModal();
      signInWithGoogleCloud();
    } else if (action === "sync-local-media-to-cloud") {
      syncPendingLocalClassMediaToCloud(actBtn.dataset.classId);
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
    } else if (action === "admin-create-exam") {
      openAdminExamBuilderModal("");
    } else if (action === "admin-edit-exam") {
      openAdminExamBuilderModal(actBtn.dataset.examId);
    } else if (action === "admin-delete-exam") {
      if (!isAdminUser()) return;
      const exId = actBtn.dataset.examId;
      const target = state.exams.find((x) => x.id === exId);
      state.exams = state.exams.filter((x) => x.id !== exId);
      syncAssessmentsToCloudIfConnected(`Deleted exam: ${target?.title || exId}`);
    } else if (action === "admin-create-assignment") {
      openAdminAssignmentBuilderModal("");
    } else if (action === "admin-edit-assignment") {
      openAdminAssignmentBuilderModal(actBtn.dataset.asgId);
    } else if (action === "admin-delete-assignment") {
      if (!isAdminUser()) return;
      const asgId = actBtn.dataset.asgId;
      const target = state.assignments.find((x) => x.id === asgId);
      state.assignments = state.assignments.filter((x) => x.id !== asgId);
      syncAssessmentsToCloudIfConnected(`Deleted assignment: ${target?.title || asgId}`);
    } else if (action === "admin-publish-assessments-now") {
      if (!isAdminUser()) return;
      if (!cloudUser) {
        signInWithGoogleCloud();
      } else {
        pushCurriculumToFirestore({
          versionTag: state.manifest.versionTag || "v1.6.0",
          versionCode: Number(state.manifest.versionCode || 106) + 1,
          releaseNotes: "Published live exams, MCQ questions & deadlined assignments",
          announcement: state.manifest.announcement || "",
          apkVersionName: state.manifest.latestApkVersionName || "1.6",
          apkVersionCode: state.manifest.latestApkVersionCode || 7,
          apkUrl: state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl
        });
      }
    } else if (action === "open-assignment") {
      const asg = state.assignments.find((a) => a.id === actBtn.dataset.asgId);
      if (!asg) return;
      const uid = getActiveUser().id;
      const prev = state.assignmentSubmissionsByUser[uid]?.[asg.id]?.text || "";
      const expired = isAssignmentExpired(asg);
      const deadlineBanner = asg.dueDateMs
        ? `<div style="margin-bottom: 10px; padding: 8px 12px; border-radius: 8px; font-size: 12.5px; font-weight: 700; background: ${expired ? "#FEE2E2" : "#FFFBEB"}; color: ${expired ? "#991B1B" : "#92400E"}; border: 1px solid ${expired ? "#FCA5A5" : "#FCD34D"};">
            ${expired ? `Time Up • Submissions closed on ${escapeHtml(formatDeadlineLabel(asg.dueDateMs))}` : `Submission Deadline: ${escapeHtml(formatDeadlineLabel(asg.dueDateMs))} (${escapeHtml(formatRemainingDeadlineBadge(asg.dueDateMs))})`}
          </div>`
        : "";
      openModal(
        asg.title,
        `${deadlineBanner}
         <p style="font-size: 13px; color: var(--navy); font-weight: 600;">${escapeHtml(asg.topicDescription)}</p>
         ${asg.instructions ? `<p style="font-size: 12px; color: var(--muted);">${escapeHtml(asg.instructions)}</p>` : ""}
         ${asg.referenceUrl ? `<p style="font-size: 12px;"><a href="${escapeHtml(asg.referenceUrl)}" target="_blank" rel="noopener noreferrer" style="color: var(--navy); font-weight: 700;">Download / Open Reference Sheet &rsaquo;</a></p>` : ""}
         <form id="submitAssignmentForm" data-asg-id="${escapeHtml(asg.id)}">
           <textarea id="asgSubmissionText" class="textarea" rows="6" placeholder="Write or paste your executive SCM analysis (${asg.maxMarks} Marks)..." ${expired ? "disabled" : ""}>${escapeHtml(prev)}</textarea>
           <button type="submit" class="btn btn-primary" style="margin-top: 12px; width: 100%;" ${expired ? "disabled" : ""}>
             ${expired ? "Submissions Closed (Time Up)" : "Submit Assignment"}
           </button>
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
      showToast(demoCertificateActive ? "Demo Certificate preview opened (Hasina Akter Khan & Md. Amirul Islam)." : "Demo Certificate preview closed.");
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
    } else if (action === "download-certificate-pdf" || action === "zoom-certificate-pdf") {
      const activeUser = getActiveUser();
      const issuedCert = state.certificatesByUser[activeUser.id];
      const demoCertAllowed = state.settings.demoCertificateEnabled !== false || isAdminUser();
      const certToExport =
        demoCertificateActive && demoCertAllowed
          ? {
              recipientName: activeUser.fullName || "ISCEB Executive Candidate",
              serial: "ISCEB-DEMO-PGD-2026",
              issuedDate: new Date().toISOString().slice(0, 10),
              isDemo: true
            }
          : issuedCert;
      if (!certToExport) {
        showToast("Generate your PGD Certificate or open Demo Preview first.", true);
        return;
      }
      exportLandscapeCertificatePdf(
        certToExport,
        {
          leftSignerName: state.settings.leftSignerName || "Hasina Akter Khan",
          leftSignerTitle: state.settings.leftSignerTitle || "Chairman & CEO",
          rightSignerName: state.settings.authorizedSignerName || "Md. Amirul Islam",
          rightSignerTitle: state.settings.authorizedSignerTitle || "Founder of ISCEB"
        },
        { previewOnly: action === "zoom-certificate-pdf" }
      );
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
    } else if (action === "refresh-cloud-users") {
      fetchCloudRegisteredUsers(true);
    } else if (action === "profile-sync-now") {
      syncStudentAccountToFirestore(getActiveUser()).finally(() => syncFromCloudFirestore(false));
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
      pendingEditStudentPhotoDataUrl = "";
      pendingEditStudentPhotoCleared = false;
      const initials = cur.fullName.split(" ").filter(Boolean).map((p) => p[0]).slice(0, 2).join("").toUpperCase() || "PGD";
      openModal(
        "Edit Student Profile",
        `<form id="editStudentProfileForm" style="display: grid; gap: 10px;">
          <div style="display: flex; align-items: center; gap: 12px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface-muted);">
            <div id="editStuPhotoPreviewWrap" data-initials="${escapeHtml(initials)}" style="width: 56px; height: 56px; border-radius: 50%; overflow: hidden; border: 2px solid var(--gold); background: var(--navy); color: var(--gold); display: grid; place-items: center; font-weight: 800; font-size: 18px; flex-shrink: 0;">
              ${cur.photoUrl ? `<img src="${escapeHtml(cur.photoUrl)}" alt="${escapeHtml(cur.fullName)}" style="width: 100%; height: 100%; object-fit: cover;" />` : `<span>${escapeHtml(initials)}</span>`}
            </div>
            <div style="min-width: 0; flex: 1;">
              <div style="font-size: 12.5px; font-weight: 700; color: var(--navy); margin-bottom: 6px;">Profile Photo</div>
              <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                <label for="editStuPhotoInput" class="btn btn-outline btn-sm" style="cursor: pointer;">${cur.photoUrl ? "Change Photo" : "Add Photo"}</label>
                <input type="file" id="editStuPhotoInput" accept="image/*" style="display: none;" />
                ${cur.photoUrl ? `<button type="button" class="btn btn-outline btn-sm" data-modal-action="clear-edit-student-photo">Remove Photo</button>` : ""}
              </div>
            </div>
          </div>
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
    } else if (action === "trigger-pwa-install") {
      triggerPwaInstall();
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
    } else if (e.target.id === "adminStudentSearchInput") {
      adminStudentSearch = e.target.value;
      render();
      const inp = document.getElementById("adminStudentSearchInput");
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
      e.target.value = "";
      if (!file) return;
      const attId = `att-${Date.now()}`;
      localAttachmentUrls.set(attId, URL.createObjectURL(file));
      saveMediaBlobToIdb(attId, file, { fileName: file.name, mimeType: file.type }).then(() => {
        const cur = getClassProgress(cid);
        updateClassProgress(cid, {
          localAttachments: [...(cur.localAttachments || []), { id: attId, name: file.name, size: file.size, type: file.type }]
        });
        showToast(`Attached ${file.name} and saved offline.`);
        render();
      });
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
    } else if (e.target.id === "signedInStudentPhotoInput") {
      const file = e.target.files?.[0];
      e.target.value = "";
      if (!file) return;
      const cur = getActiveUser();
      if (isGuestUser() || isAdminUser()) return;
      resizePhotoFileToDataUrl(file).then((dataUrl) => {
        if (!dataUrl) {
          showToast("Could not load selected photo.", true);
          return;
        }
        const existingIdx = (state.customStudents || []).findIndex((s) => s.id === cur.id);
        const updatedRecord = { ...cur, photoUrl: dataUrl };
        if (existingIdx >= 0) {
          state.customStudents[existingIdx] = updatedRecord;
        } else {
          state.customStudents = [updatedRecord, ...(state.customStudents || [])];
        }
        state.activeUserId = updatedRecord.id;
        saveState();
        syncAccountSwitcherOptions();
        showToast("Profile photo updated.");
        render();
        if (cloudUser) {
          syncStudentAccountToFirestore(updatedRecord);
        }
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
    } else if (e.target.classList.contains("repairInlineMediaInput")) {
      const cid = e.target.dataset.classId;
      const mid = e.target.dataset.mediaId;
      const file = e.target.files?.[0];
      e.target.value = "";
      if (!file) return;
      repairSingleBrokenMedia(mid, file, cid);
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
          if (cloudUser) {
            fetchCloudRegisteredUsers(false);
          }
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

  document.getElementById("modalBody").addEventListener("click", (e) => {
    const mAct = e.target.closest("[data-modal-action]");
    if (!mAct) return;
    const modalAction = mAct.dataset.modalAction;
    if (modalAction === "builder-add-question") {
      const host = document.getElementById("examQuestionRowsHost");
      if (!host) return;
      const idx = host.querySelectorAll(".exam-q-builder-row").length;
      const wrapper = document.createElement("div");
      wrapper.innerHTML = buildExamQuestionBuilderRowHtml({}, idx);
      host.appendChild(wrapper.firstElementChild);
    } else if (modalAction === "builder-remove-question") {
      const host = document.getElementById("examQuestionRowsHost");
      const rows = host ? host.querySelectorAll(".exam-q-builder-row") : [];
      if (rows.length <= 1) {
        showToast("An exam requires at least 1 MCQ question.", true);
        return;
      }
      mAct.closest(".exam-q-builder-row")?.remove();
      host.querySelectorAll(".exam-q-builder-row").forEach((row, i) => {
        row.dataset.qIndex = String(i);
        const lbl = row.querySelector(".exam-q-row-label");
        if (lbl) lbl.textContent = `Question ${i + 1}`;
      });
    } else if (modalAction === "builder-set-deadline-preset") {
      const inputId = mAct.dataset.targetInput;
      const hours = Number(mAct.dataset.hours || 0);
      const inp = document.getElementById(inputId);
      if (!inp) return;
      inp.value = hours > 0 ? toDateTimeLocalValue(Date.now() + hours * 3600 * 1000) : "";
    } else if (modalAction === "clear-edit-student-photo") {
      pendingEditStudentPhotoDataUrl = "";
      pendingEditStudentPhotoCleared = true;
      const wrap = document.getElementById("editStuPhotoPreviewWrap");
      if (wrap) {
        const initials = wrap.dataset.initials || "PGD";
        wrap.innerHTML = `<span>${escapeHtml(initials)}</span>`;
      }
      showToast("Photo cleared — tap Save Student Profile to apply.");
    } else if (modalAction === "connect-google-and-sync-pending") {
      closeModal();
      signInWithGoogleCloud();
    } else if (modalAction === "remove-unsynced-media") {
      if (!isAdminUser()) return;
      const mid = mAct.dataset.mediaId;
      state.classes.forEach((c) => {
        if (c.pdfMedia?.id === mid) c.pdfMedia = null;
        if (c.audioMedia?.id === mid) c.audioMedia = null;
        if (c.videoMedia?.id === mid) c.videoMedia = null;
        if (Array.isArray(c.attachments)) {
          c.attachments = c.attachments.filter((m) => (m.id || "") !== mid);
        }
      });
      saveState();
      closeModal();
      if (cloudUser) {
        pushCurriculumToFirestore({
          versionTag: state.manifest.versionTag || "v1.6.0",
          versionCode: Number(state.manifest.versionCode || 106) + 1,
          releaseNotes: "Removed unsynced attachment from catalog",
          announcement: state.manifest.announcement || "",
          apkVersionName: state.manifest.latestApkVersionName || "1.6",
          apkVersionCode: state.manifest.latestApkVersionCode || 7,
          apkUrl: state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl
        });
      } else {
        showToast("Removed locally.");
        render();
      }
    }
  });

  document.getElementById("modalBody").addEventListener("change", (e) => {
    if (e.target.id === "repairBrokenMediaInput") {
      const file = e.target.files?.[0];
      const mid = e.target.dataset.mediaId || "";
      e.target.value = "";
      if (!file) return;
      closeModal();
      repairSingleBrokenMedia(mid, file);
    } else if (e.target.id === "editStuPhotoInput") {
      const file = e.target.files?.[0];
      e.target.value = "";
      if (!file) return;
      resizePhotoFileToDataUrl(file).then((dataUrl) => {
        if (!dataUrl) {
          showToast("Could not load selected photo.", true);
          return;
        }
        pendingEditStudentPhotoDataUrl = dataUrl;
        pendingEditStudentPhotoCleared = false;
        const wrap = document.getElementById("editStuPhotoPreviewWrap");
        if (wrap) {
          wrap.innerHTML = `<img src="${escapeHtml(dataUrl)}" alt="Profile Preview" style="width: 100%; height: 100%; object-fit: cover;" />`;
        }
        showToast("Photo selected — tap Save Student Profile.");
      }).catch(() => {
        showToast("Could not load selected photo.", true);
      });
    }
  });

  document.getElementById("modalBody").addEventListener("submit", (e) => {
    e.preventDefault();
    if (e.target.id === "activeExamForm") {
      submitActiveExam(false);
    } else if (e.target.id === "adminExamBuilderForm") {
      if (!isAdminUser()) return;
      const existingId = e.target.dataset.examId;
      const moduleId = document.getElementById("aebModuleId")?.value || "mod-1";
      const title = document.getElementById("aebTitle")?.value.trim() || "";
      const durationMinutes = Math.max(1, Math.min(180, Number(document.getElementById("aebDuration")?.value || 20)));
      const passingPercentage = Math.max(20, Math.min(100, Number(document.getElementById("aebPassing")?.value || 60)));
      const maxAttempts = Math.max(1, Math.min(50, Number(document.getElementById("aebMaxAttempts")?.value || 3)));
      const openUntilRaw = document.getElementById("aebOpenUntil")?.value || "";
      const openUntilMs = openUntilRaw ? new Date(openUntilRaw).getTime() : null;

      const qRows = Array.from(document.querySelectorAll("#examQuestionRowsHost .exam-q-builder-row"));
      const questions = qRows.map((row, idx) => {
        const question = row.querySelector(".eq-question-input")?.value.trim() || `Question ${idx + 1}`;
        const options = Array.from(row.querySelectorAll(".eq-opt-input")).map((inp) => inp.value.trim());
        const correctOptionIndex = Number(row.querySelector(".eq-correct-select")?.value || 0);
        const marks = Math.max(1, Number(row.querySelector(".eq-marks-input")?.value || 5));
        const explanation = row.querySelector(".eq-exp-input")?.value.trim() || "";
        return {
          id: `q-${Date.now()}-${idx}`,
          question,
          options,
          correctOptionIndex,
          marks,
          explanation
        };
      });

      if (!title || !questions.length || questions.some((q) => q.options.some((o) => !o))) {
        showToast("Fill in the exam title and all 4 options for every MCQ question.", true);
        return;
      }

      const totalMarks = questions.reduce((sum, q) => sum + Number(q.marks || 0), 0);
      const prev = existingId ? state.exams.find((x) => x.id === existingId) : null;
      const savedExam = {
        ...(prev || {}),
        id: existingId || `exam-${moduleId}-${Date.now()}`,
        moduleId,
        title,
        totalMarks,
        durationMinutes,
        passingPercentage,
        maxAttempts,
        openUntilMs: Number.isFinite(openUntilMs) && openUntilMs > 0 ? openUntilMs : null,
        questions
      };

      if (existingId) {
        state.exams = state.exams.map((x) => (x.id === existingId ? savedExam : x));
      } else {
        state.exams = [...state.exams, savedExam];
      }
      closeModal();
      syncAssessmentsToCloudIfConnected(`Saved exam '${savedExam.title}' (${questions.length} MCQ, ${durationMinutes}m)`);
    } else if (e.target.id === "adminAssignmentBuilderForm") {
      if (!isAdminUser()) return;
      const existingId = e.target.dataset.asgId;
      const moduleId = document.getElementById("aabModuleId")?.value || "mod-1";
      const title = document.getElementById("aabTitle")?.value.trim() || "";
      const topicDescription = document.getElementById("aabTopic")?.value.trim() || "";
      const instructions = document.getElementById("aabInstructions")?.value.trim() || "";
      const maxMarks = Math.max(5, Math.min(200, Number(document.getElementById("aabMarks")?.value || 20)));
      const isFinalProject = Boolean(document.getElementById("aabFinalProject")?.checked);
      const referenceUrl = document.getElementById("aabRefUrl")?.value.trim() || "";
      const dueRaw = document.getElementById("aabDueDate")?.value || "";
      const dueDateMs = dueRaw ? new Date(dueRaw).getTime() : null;

      if (!title || !topicDescription) {
        showToast("Enter assignment title and topic description.", true);
        return;
      }

      const prev = existingId ? state.assignments.find((a) => a.id === existingId) : null;
      const savedAsg = {
        ...(prev || {}),
        id: existingId || `asg-${moduleId}-${Date.now()}`,
        moduleId,
        title,
        topicDescription,
        instructions: instructions || "Submit concise supply chain analysis report.",
        maxMarks,
        isFinalProject,
        referenceUrl: /^https?:\/\//i.test(referenceUrl) ? referenceUrl : "",
        dueDateMs: Number.isFinite(dueDateMs) && dueDateMs > 0 ? dueDateMs : null
      };

      if (existingId) {
        state.assignments = state.assignments.map((a) => (a.id === existingId ? savedAsg : a));
      } else {
        state.assignments = [...state.assignments, savedAsg];
      }
      closeModal();
      syncAssessmentsToCloudIfConnected(`Saved assignment '${savedAsg.title}'`);
    } else if (e.target.id === "submitAssignmentForm") {
      const asgId = e.target.dataset.asgId;
      const targetAsg = state.assignments.find((a) => a.id === asgId);
      if (isAssignmentExpired(targetAsg)) {
        closeModal();
        showToast("Submission deadline expired (Time Up).", true);
        render();
        return;
      }
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
      const nextPhotoUrl = pendingEditStudentPhotoCleared
        ? ""
        : (pendingEditStudentPhotoDataUrl || cur.photoUrl || "");
      const updatedRecord = {
        ...cur,
        fullName: updatedName,
        email: updatedEmail,
        mobileNumber: updatedMobile,
        organization: updatedOrg,
        enrolledProgram: updatedProgram || GUEST_USER.enrolledProgram,
        photoUrl: nextPhotoUrl
      };
      pendingEditStudentPhotoDataUrl = "";
      pendingEditStudentPhotoCleared = false;
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
      if (cloudUser) {
        syncStudentAccountToFirestore(updatedRecord, { allowEmptyPhoto: !updatedRecord.photoUrl });
      }
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
    await saveMediaBlobToIdb(mediaId, file, { fileName: file.name, mimeType: file.type });

    let cloudFileId = "";
    let cloudChunkCount = 0;

    if (canCloudSync && file.size > 0 && file.size <= MAX_CLOUD_FILE_BYTES) {
      try {
        showToast(`Uploading ${file.name} (${i + 1}/${files.length}) to Firebase Cloud...`);
        const rawBytes = new Uint8Array(await file.arrayBuffer());
        const candidateId = `pc_${cid}_${i}_${Date.now()}`.replace(/[^a-zA-Z0-9_-]/g, "_");
        cloudChunkCount = await uploadChunksWithRetry(fb, candidateId, rawBytes, file.name);
        cloudFileId = candidateId;
        await saveMediaBlobToIdb(candidateId, file, { fileName: file.name, mimeType: file.type });
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
    const published = await pushCurriculumToFirestore({
      versionTag: state.manifest.versionTag || "v1.6.0",
      versionCode: Number(state.manifest.versionCode || 106) + 1,
      releaseNotes: `Uploaded ${cloudSyncedCount} cloud study file(s) to Lecture ${cls.lectureCode}`,
      announcement: state.manifest.announcement || "",
      apkVersionName: state.manifest.latestApkVersionName || "1.6",
      apkVersionCode: state.manifest.latestApkVersionCode || 7,
      apkUrl: state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl
    });
    if (published) {
      showToast(`Uploaded & synced ${cloudSyncedCount} file(s)! All student phones can open them now.`);
    }
  } else if (cloudErrorMsg) {
    render();
    openModal(
      "Google Admin Sync Required",
      `<div style="display: grid; gap: 12px; font-size: 13.5px;">
        <div style="background: #FEE2E2; border: 1px solid #FCA5A5; border-radius: 10px; padding: 12px; color: #991B1B;">
          <strong>Cloud upload permission denied (${escapeHtml(cloudErrorMsg)}).</strong><br/>
          Connected Google account: <strong>${escapeHtml(cloudUser?.email || "None")}</strong>
        </div>
        <p style="margin: 0; color: var(--muted);">
          Your file is saved safely on this device. Connect <strong>Google Cloud Sync</strong> with your Admin email (<strong>www.mdnafiulislam@gmail.com</strong> or <strong>amirul.islam@isceb.org</strong>) so it uploads to every student's phone automatically.
        </p>
        <button type="button" class="btn btn-gold" data-modal-action="connect-google-and-sync-pending">
          Connect Admin Google Account &amp; Sync Now
        </button>
      </div>`
    );
  } else if (!canCloudSync) {
    render();
    openModal(
      "Connect Google Cloud to Push to Students",
      `<div style="display: grid; gap: 12px; font-size: 13.5px;">
        <div style="background: #FEF3C7; border: 1px solid #FDE68A; border-radius: 10px; padding: 12px; color: #92400E;">
          <strong>Saved on this device only.</strong><br/>
          Because Google Cloud Sync is not connected yet, student phones cannot open this file until you sign in with Admin Gmail.
        </div>
        <button type="button" class="btn btn-gold" data-modal-action="connect-google-and-sync-pending">
          Sign in with Google &amp; Upload to Students Now
        </button>
      </div>`
    );
  } else if (largeLocalOnlyCount > 0) {
    showToast(`File(s) >18 MB cannot sync via Firestore chunks. Use 'Attach HTTPS Link' for student phones.`, true);
    render();
  } else {
    render();
  }
}

async function syncAllPendingLocalMediaToCloud(options = {}) {
  const { targetClassId = "", skipPublish = false, silentNoOp = false } = options || {};
  if (!isAdminUser() || isSyncingPendingMedia) return 0;
  const fb = await initFirebaseSdk();
  if (!fb || !cloudUser) {
    if (!silentNoOp) await signInWithGoogleCloud();
    return 0;
  }

  isSyncingPendingMedia = true;
  let synced = 0;
  let lastSyncedClassCode = "";
  try {
    const classesToScan = targetClassId
      ? state.classes.filter((c) => c.id === targetClassId)
      : state.classes;

    for (const cls of classesToScan) {
      const targets = getUniqueClassMaterials(cls).filter((m) => !isCloudSyncedMedia(m));
      for (let i = 0; i < targets.length; i++) {
        const m = targets[i];
        const blob = await findMediaBlobForAttachmentInIdb(m);
        if (!blob || blob.size === 0 || blob.size > MAX_CLOUD_FILE_BYTES) continue;
        try {
          const rawBytes = new Uint8Array(await blob.arrayBuffer());
          const candidateId = `pc_${cls.id}_${i}_${Date.now()}`.replace(/[^a-zA-Z0-9_-]/g, "_");
          const chunkCount = await uploadChunksWithRetry(fb, candidateId, rawBytes, m.fileName || m.title || "file");
          const patchMedia = (item) => {
            if (!item || item.id !== m.id) return;
            item.cloudFileId = candidateId;
            item.cloudChunkCount = chunkCount;
            item.fileSizeBytes = blob.size;
            item.fileSizeFormatted = formatBytes(blob.size);
          };
          patchMedia(cls.pdfMedia);
          patchMedia(cls.audioMedia);
          patchMedia(cls.videoMedia);
          (cls.attachments || []).forEach(patchMedia);
          if (m.id) await saveMediaBlobToIdb(m.id, blob, { fileName: m.fileName, mimeType: blob.type });
          await saveMediaBlobToIdb(candidateId, blob, { fileName: m.fileName, mimeType: blob.type });
          synced += 1;
          lastSyncedClassCode = cls.lectureCode || cls.id;
        } catch (err) {
          if (!silentNoOp) showToast(`Cloud upload error: ${err?.message || err}`, true);
        }
      }
    }
    if (synced > 0) {
      saveState();
      if (!skipPublish) {
        await pushCurriculumToFirestore({
          versionTag: state.manifest.versionTag || "v1.6.0",
          versionCode: Number(state.manifest.versionCode || 106) + 1,
          releaseNotes: `Auto-synced ${synced} pending study file(s)${lastSyncedClassCode ? ` (Lecture ${lastSyncedClassCode})` : ""}`,
          announcement: state.manifest.announcement || "",
          apkVersionName: state.manifest.latestApkVersionName || "1.6",
          apkVersionCode: state.manifest.latestApkVersionCode || 7,
          apkUrl: state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl
        });
      }
    } else if (!silentNoOp) {
      showToast("No cached local files <= 18 MB found on this device. Tap 'Re-upload & Sync' on any missing item.", true);
      render();
    }
  } finally {
    isSyncingPendingMedia = false;
  }
  return synced;
}

async function syncPendingLocalClassMediaToCloud(cid) {
  return syncAllPendingLocalMediaToCloud({ targetClassId: cid, skipPublish: false, silentNoOp: false });
}

async function repairSingleBrokenMedia(mediaId, file, explicitClassId = "") {
  if (!isAdminUser() || !file) return;
  const fb = await initFirebaseSdk();
  if (!fb || !cloudUser) {
    showToast("Sign in with Admin Google Cloud Sync first before repairing a cloud file.", true);
    return;
  }
  if (file.size === 0 || file.size > MAX_CLOUD_FILE_BYTES) {
    showToast("File must be non-empty and <= 18 MB for direct Firestore sync (or use HTTPS link).", true);
    return;
  }

  let targetCls = explicitClassId ? state.classes.find((c) => c.id === explicitClassId) : null;
  if (!targetCls) {
    targetCls = state.classes.find((c) =>
      getUniqueClassMaterials(c).some((m) => (m.id || "") === mediaId)
    );
  }
  if (!targetCls) {
    showToast("Could not locate target lecture for this file.", true);
    return;
  }

  try {
    showToast(`Uploading & repairing ${file.name} in Firebase Cloud...`);
    const rawBytes = new Uint8Array(await file.arrayBuffer());
    const candidateId = `pc_${targetCls.id}_rep_${Date.now()}`.replace(/[^a-zA-Z0-9_-]/g, "_");
    const chunkCount = await uploadChunksWithRetry(fb, candidateId, rawBytes, file.name);
    const localBlobUrl = URL.createObjectURL(file);
    const applyPatch = (item) => {
      if (!item || (mediaId && item.id !== mediaId)) return;
      item.fileName = file.name;
      item.fileSizeBytes = file.size;
      item.fileSizeFormatted = formatBytes(file.size);
      item.mimeType = file.type || inferMediaMimeType({ type: item.type, fileName: file.name });
      item.url = "";
      item.uploadTimestamp = Date.now();
      item.cloudFileId = candidateId;
      item.cloudChunkCount = chunkCount;
    };
    applyPatch(targetCls.pdfMedia);
    applyPatch(targetCls.audioMedia);
    applyPatch(targetCls.videoMedia);
    (targetCls.attachments || []).forEach(applyPatch);

    if (mediaId) {
      localAttachmentUrls.set(mediaId, localBlobUrl);
      await saveMediaBlobToIdb(mediaId, file, { fileName: file.name, mimeType: file.type });
    }
    await saveMediaBlobToIdb(candidateId, file, { fileName: file.name, mimeType: file.type });
    saveState();

    await pushCurriculumToFirestore({
      versionTag: state.manifest.versionTag || "v1.6.0",
      versionCode: Number(state.manifest.versionCode || 106) + 1,
      releaseNotes: `Synced ${file.name} to Lecture ${targetCls.lectureCode}`,
      announcement: state.manifest.announcement || "",
      apkVersionName: state.manifest.latestApkVersionName || "1.6",
      apkVersionCode: state.manifest.latestApkVersionCode || 7,
      apkUrl: state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl
    });
    showToast(`${file.name} is now synced to Cloud and ready on all student phones!`);
  } catch (err) {
    showToast(`Repair upload failed: ${err?.message || err}. Sign in as Admin Gmail (www.mdnafiulislam@gmail.com).`, true);
  }
}

setupListeners();
rehydrateVerifiedAdminSession();
render();
fetchVersionJson();
initFirebaseSdk();
