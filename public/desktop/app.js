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
  buildFirestoreCatalogPayload,
  buildFirestoreManifestPayload
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

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      return {
        currentView: parsed.currentView || "dashboard",
        // Never restore admin role from plain localStorage; verified async via HMAC session
        activeUserId:
          parsed.activeUserId && getStudentSwitcherUsers(AVAILABLE_USERS).some((u) => u.id === parsed.activeUserId)
            ? parsed.activeUserId
            : "student-1",
        selectedClassId: parsed.selectedClassId || "class-1",
        partFilter: parsed.partFilter || "ALL",
        searchQuery: parsed.searchQuery || "",
        modules: Array.isArray(parsed.modules) && parsed.modules.length ? parsed.modules : structuredClone(INITIAL_MODULES),
        classes: Array.isArray(parsed.classes) && parsed.classes.length ? parsed.classes : structuredClone(INITIAL_CLASSES),
        exams: Array.isArray(parsed.exams) && parsed.exams.length ? parsed.exams : structuredClone(INITIAL_EXAMS),
        assignments: Array.isArray(parsed.assignments) && parsed.assignments.length ? parsed.assignments : structuredClone(INITIAL_ASSIGNMENTS),
        members: Array.isArray(parsed.members) && parsed.members.length ? parsed.members : structuredClone(DEFAULT_MEMBERS),
        founderProfile: parsed.founderProfile || structuredClone(DEFAULT_FOUNDER),
        developerProfile: parsed.developerProfile || structuredClone(DEFAULT_DEVELOPER),
        progressByUser: parsed.progressByUser || {},
        examAttemptsByUser: parsed.examAttemptsByUser || {},
        assignmentSubmissionsByUser: parsed.assignmentSubmissionsByUser || {},
        certificatesByUser: parsed.certificatesByUser || {},
        manifest: parsed.manifest || {
          versionCode: 105,
          versionTag: "v1.5.0",
          publishedTimestamp: Date.now(),
          publishedBy: "Md. Nafiul Islam Nahin (Developer of ISCEB App)",
          releaseNotes: "ISCEB v1.5: Fast non-blocking startup, optimized APK size, adaptive desktop layout & PC Web Portal.",
          announcement: "PGD Supply Chain Classes Live • Fridays 9:00 PM",
          latestApkVersionCode: 6,
          latestApkVersionName: "1.5",
          apkDownloadUrl: FIREBASE_CONFIG.apkDownloadUrl
        },
        settings: parsed.settings || {
          passingPercentage: 60,
          requiredModulesCompleted: 12
        }
      };
    }
  } catch (_) {}

  return {
    currentView: "dashboard",
    activeUserId: "student-1",
    selectedClassId: "class-1",
    partFilter: "ALL",
    searchQuery: "",
    modules: structuredClone(INITIAL_MODULES),
    classes: structuredClone(INITIAL_CLASSES),
    exams: structuredClone(INITIAL_EXAMS),
    assignments: structuredClone(INITIAL_ASSIGNMENTS),
    members: structuredClone(DEFAULT_MEMBERS),
    founderProfile: structuredClone(DEFAULT_FOUNDER),
    developerProfile: structuredClone(DEFAULT_DEVELOPER),
    progressByUser: {},
    examAttemptsByUser: {},
    assignmentSubmissionsByUser: {},
    certificatesByUser: {},
    manifest: {
      versionCode: 105,
      versionTag: "v1.5.0",
      publishedTimestamp: Date.now(),
      publishedBy: "Md. Nafiul Islam Nahin (Developer of ISCEB App)",
      releaseNotes: "ISCEB v1.5: Fast non-blocking startup, optimized APK size, adaptive desktop layout & PC Web Portal.",
      announcement: "PGD Supply Chain Classes Live • Fridays 9:00 PM",
      latestApkVersionCode: 6,
      latestApkVersionName: "1.5",
      apkDownloadUrl: FIREBASE_CONFIG.apkDownloadUrl
    },
    settings: {
      passingPercentage: 60,
      requiredModulesCompleted: 12
    }
  };
}

let state = loadState();
let remoteVersionInfo = null;
let cloudUser = null;
let firebaseModules = null;
let deferredInstallPrompt = null;
let activeTimerInterval = null;
let examCountdownInterval = null;
let examModalSession = null;
let selectedLoginAdminId = ADMIN_AMIRUL_ID;
let desktopAdminAuthMode = "login"; // login | setup | reset
let lastIssuedSecretNotice = "";
let verifiedAdminSessionId = null;
const localAttachmentUrls = new Map();

function saveState() {
  try {
    const persisted = {
      ...state,
      activeUserId: isAdminUser() ? "student-1" : state.activeUserId
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(persisted));
  } catch (_) {}
}

function getActiveUser() {
  if (verifiedAdminSessionId) {
    const admin = AVAILABLE_USERS.find((u) => u.id === verifiedAdminSessionId && u.role === "ADMIN");
    if (admin) return admin;
  }
  const studentUsers = getStudentSwitcherUsers(AVAILABLE_USERS);
  return studentUsers.find((u) => u.id === state.activeUserId) || studentUsers[0] || AVAILABLE_USERS[0];
}

function isAdminUser() {
  return Boolean(verifiedAdminSessionId && getActiveUser().role === "ADMIN");
}

function syncAccountSwitcherUi() {
  const select = document.getElementById("accountSwitcher");
  if (!select) return;
  const studentUsers = getStudentSwitcherUsers(AVAILABLE_USERS);
  if (studentUsers.some((u) => u.id === state.activeUserId)) {
    select.value = state.activeUserId;
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

async function openSyncedCloudMedia(media) {
  if (!media) return;
  const rawUrl = String(media.url || "").trim();
  if (/^https?:\/\//i.test(rawUrl)) {
    window.open(rawUrl, "_blank", "noopener,noreferrer");
    return;
  }
  const cachedUrl = localAttachmentUrls.get(media.id);
  if (cachedUrl) {
    window.open(cachedUrl, "_blank", "noopener,noreferrer");
    return;
  }
  if (!media.cloudFileId || !media.cloudChunkCount) {
    showToast("File is stored locally on the uploader's device only (attach HTTPS link or re-upload under 3.5 MB).", true);
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
    const blob = new Blob(chunks, { type: media.mimeType || (media.type === "PDF" ? "application/pdf" : "application/octet-stream") });
    const objUrl = URL.createObjectURL(blob);
    localAttachmentUrls.set(media.id, objUrl);
    window.open(objUrl, "_blank", "noopener,noreferrer");
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
    showToast(`Cloud connected as ${cred.user.email}`);
    await syncFromCloudFirestore(false);
    render();
  } catch (err) {
    showToast(`Google Sign-In paused: ${err.message || err}`, true);
  }
}

async function fetchVersionJson() {
  try {
    const res = await fetch(`../version.json?t=${Date.now()}`, { cache: "no-store" });
    if (!res.ok) return;
    remoteVersionInfo = await res.json();
    updateChromeBadges();
    if (state.currentView === "dashboard" || state.currentView === "admin") {
      render();
    }
  } catch (_) {}
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
        state.settings = { ...state.settings, ...c.settings };
        try {
          if (c.settings.founderProfileJson) state.founderProfile = JSON.parse(c.settings.founderProfileJson);
          if (c.settings.developerProfileJson) state.developerProfile = JSON.parse(c.settings.developerProfileJson);
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

  document.getElementById("navDashProgress").textContent = `${stats.percent}%`;
  document.getElementById("navCertStatus").textContent = state.certificatesByUser[user.id] ? "Issued" : stats.certEligible ? "Ready" : "PGD";
  document.getElementById("navMembersCount").textContent = String(state.members.length);
  document.getElementById("navAdminRoleBadge").textContent = user.role === "ADMIN" ? "Admin" : "Student";
  document.getElementById("sidebarCurriculumVer").textContent = `Curriculum: ${state.manifest.versionTag || "v1.5.0"}`;

  const apkVerEl = document.getElementById("sidebarApkVer");
  if (remoteVersionInfo) {
    apkVerEl.textContent = `Latest APK: v${remoteVersionInfo.versionName} (${formatBytes(remoteVersionInfo.fileSizeBytes)})`;
  } else {
    apkVerEl.textContent = `Latest APK: v${state.manifest.latestApkVersionName || "1.5"}`;
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
}

function renderDashboard() {
  const stats = getOverallStats();
  const user = getActiveUser();
  const nextClass = state.classes.find((c) => !getClassProgress(c.id).completed) || state.classes[0];
  const nextMod = state.modules.find((m) => m.id === nextClass?.moduleId);
  const apkUrl = remoteVersionInfo?.apkUrl || state.manifest.apkDownloadUrl || FIREBASE_CONFIG.apkDownloadUrl;
  const apkLabel = remoteVersionInfo
    ? `v${remoteVersionInfo.versionName} (Build ${remoteVersionInfo.versionCode}) • ${formatBytes(remoteVersionInfo.fileSizeBytes)}`
    : `v${state.manifest.latestApkVersionName || "1.5"} (Build ${state.manifest.latestApkVersionCode || 6})`;

  return `
    <div class="card card-navy" style="display: grid; grid-template-columns: 1.5fr 1fr; gap: 18px; align-items: center;">
      <div>
        <span class="badge badge-gold">ISCEB PC & Desktop Workspace • ${escapeHtml(state.manifest.versionTag || "v1.5.0")}</span>
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
        <div class="stat-sub">PGD Certificate: ${stats.certEligible ? "Eligible" : "In Progress"}</div>
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
                  return `<div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; background: #fff; padding: 9px 12px; border-radius: 10px; border: 1px solid var(--border);">
                    <div>
                      <div style="font-size: 13px; font-weight: 700; color: var(--navy);">${escapeHtml(m.title || m.fileName || "Study Material")}</div>
                      <div style="font-size: 11.5px; color: var(--muted);">${escapeHtml(m.type || "FILE")} • ${escapeHtml(m.fileSizeFormatted || formatBytes(m.fileSizeBytes))} • ${hasCloud ? "Cloud Synced" : "Device Local"}</div>
                    </div>
                    <button type="button" class="btn btn-primary btn-sm" data-action="open-cloud-media" data-class-id="${escapeHtml(selectedClass.id)}" data-media-id="${escapeHtml(m.id || "")}">
                      Open / Preview
                    </button>
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

        <div style="background: #F4F7FC; border: 1px solid #C7D7EE; border-radius: 12px; padding: 14px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
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
          <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 10px;">
            <div>
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
          <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 10px;">
            <div>
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

  return `
    <div class="two-col-grid">
      <div class="card">
        <h3 style="margin: 0 0 10px; color: var(--navy);">PGD Certificate Readiness</h3>
        <div style="display: grid; gap: 8px; font-size: 13.5px; margin-bottom: 14px;">
          <div>1. Lectures Completed: <strong>${stats.completedCount} / ${stats.totalClasses}</strong></div>
          <div>2. Timed Exams Passed: <strong>${stats.passedExamsCount} / ${state.exams.length}</strong></div>
          <div>3. Active Study Hours Logged: <strong>${formatSeconds(stats.totalSeconds)}</strong></div>
          <div>4. Account Role: <strong>${escapeHtml(user.designation)}</strong></div>
        </div>
        <button type="button" class="btn btn-gold" data-action="issue-certificate">
          ${cert ? "Regenerate Official PGD Certificate" : "Generate Official PGD Certificate"}
        </button>
        ${cert ? `<button type="button" class="btn btn-primary" style="margin-left: 8px;" onclick="window.print()">Print / Save PDF</button>` : ""}
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
      cert
        ? `
      <div class="cert-sheet">
        <img src="./isceb-logo.webp" alt="ISCEB" style="height: 68px; object-fit: contain; margin-bottom: 8px;" />
        <div style="font-size: 12px; font-weight: 800; letter-spacing: 1.4px; color: var(--gold-dark); text-transform: uppercase;">
          International Supply Chain Education Board (ISCEB)
        </div>
        <h2 style="margin: 8px 0 4px; font-size: 28px; color: var(--navy);">Post Graduate Diploma in Supply Chain Management</h2>
        <p style="margin: 0 0 14px; color: var(--muted); font-size: 14px;">This certifies that</p>
        <div style="font-size: 30px; font-weight: 800; color: var(--navy); border-bottom: 2px solid var(--gold); display: inline-block; padding: 0 28px 6px; margin-bottom: 14px;">
          ${escapeHtml(cert.recipientName)}
        </div>
        <p style="max-width: 680px; margin: 0 auto 22px; font-size: 14px; color: #334155;">
          has successfully fulfilled the executive academic requirements across the 12-module, 48-lecture Post Graduate Diploma curriculum in Strategic & Operational Supply Chain Management.
        </p>
        <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-top: 26px; font-size: 12.5px;">
          <div>
            <div style="border-top: 1px solid #94A3B8; padding-top: 6px; font-weight: 800; color: var(--navy);">${escapeHtml(state.founderProfile.name)}</div>
            <div style="color: var(--muted);">${escapeHtml(state.founderProfile.designation)} • ${escapeHtml(state.founderProfile.additionalRole)}</div>
          </div>
          <div>
            <div style="font-weight: 800; color: var(--gold-dark);">Serial: ${escapeHtml(cert.serial)}</div>
            <div style="color: var(--muted);">Issued: ${escapeHtml(cert.issuedDate)}</div>
          </div>
          <div>
            <div style="border-top: 1px solid #94A3B8; padding-top: 6px; font-weight: 800; color: var(--navy);">${escapeHtml(state.developerProfile.name)}</div>
            <div style="color: var(--muted);">${escapeHtml(state.developerProfile.designation)}</div>
          </div>
        </div>
      </div>
    `
        : ""
    }
  `;
}

function renderDirectory() {
  const sortedMembers = [...state.members].sort((a, b) => Number(a.displayOrder || 99) - Number(b.displayOrder || 99));

  return `
    <div class="two-col-grid">
      <div class="card card-navy" style="display: flex; gap: 16px; align-items: center;">
        <img src="./admin-amirul.webp" alt="Md. Amirul Islam" class="portrait-avatar" style="width: 88px; height: 88px; object-fit: cover; object-position: center 20%; border: 3px solid var(--gold);" />
        <div>
          <span class="badge badge-gold">${escapeHtml(state.founderProfile.designation)}</span>
          <h3 style="margin: 6px 0 2px; color: #fff; font-size: 19px;">${escapeHtml(state.founderProfile.name)}</h3>
          <div style="font-size: 12.5px; color: var(--gold-soft); font-weight: 700;">${escapeHtml(state.founderProfile.additionalRole)} • ${escapeHtml(state.founderProfile.professionalIdentity)}</div>
          <p style="margin: 8px 0 4px; font-size: 12.5px; color: #CBD5E1;">${escapeHtml(state.founderProfile.biography)}</p>
          <div style="font-size: 12px; color: #E2E8F0;">Mobile: ${escapeHtml(state.founderProfile.mobileNumber)} • ${escapeHtml(state.founderProfile.email)}</div>
        </div>
      </div>

      <div class="card" style="display: flex; gap: 16px; align-items: center;">
        <img src="./admin-nafiul.webp" alt="Md. Nafiul Islam Nahin" class="portrait-avatar" style="width: 88px; height: 88px; object-fit: cover; object-position: center 20%; border: 3px solid var(--navy);" />
        <div>
          <span class="badge badge-navy">${escapeHtml(state.developerProfile.designation)}</span>
          <h3 style="margin: 6px 0 2px; color: var(--navy); font-size: 19px;">${escapeHtml(state.developerProfile.name)}</h3>
          <div style="font-size: 12.5px; color: var(--navy-soft); font-weight: 700;">Android, Web & Desktop Engineering</div>
          <p style="margin: 8px 0 4px; font-size: 12.5px; color: #334155;">${escapeHtml(state.developerProfile.biography)}</p>
          <div style="font-size: 12px; color: var(--muted);">Email: ${escapeHtml(state.developerProfile.contactEmail)} • ${escapeHtml(state.developerProfile.location)}</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <div>
          <h3 style="margin: 0; color: var(--navy);">ISCEB Members & Faculty Directory</h3>
          <p style="margin: 2px 0 0; font-size: 12.5px; color: var(--muted);">Verified leadership and board faculty (${sortedMembers.length} active profile${sortedMembers.length === 1 ? "" : "s"})</p>
        </div>
        ${isAdminUser() ? `<button type="button" class="btn btn-primary btn-sm" data-action="add-member">+ Add ISCEB Member</button>` : ""}
      </div>
      <div class="three-col-grid">
        ${sortedMembers
          .map(
            (m, i) => `
          <div class="card" style="margin-bottom: 0; background: var(--surface-muted);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
              <span class="badge badge-navy">#${i + 1} ${escapeHtml(m.designation)}</span>
            </div>
            <h4 style="margin: 4px 0; color: var(--navy); font-size: 15px;">${escapeHtml(m.fullName)}</h4>
            <div style="font-size: 12px; font-weight: 700; color: var(--gold-dark); margin-bottom: 6px;">${escapeHtml(m.roleInIsceb || m.currentOrganization || "")}</div>
            <p style="margin: 0 0 8px; font-size: 12.5px; color: #334155;">${escapeHtml(m.biography || "")}</p>
            <div style="font-size: 11.5px; color: var(--muted);">${escapeHtml(m.contactInformation || "")}</div>
            ${
              isAdminUser()
                ? `<div style="display: flex; gap: 6px; margin-top: 10px; flex-wrap: wrap;">
                    <button type="button" class="btn btn-outline btn-sm" data-action="edit-member" data-member-id="${escapeHtml(m.id)}">Edit</button>
                    <button type="button" class="btn btn-outline btn-sm" data-action="move-member-up" data-member-id="${escapeHtml(m.id)}">Up</button>
                    ${m.id !== "member-founder-amirul" ? `<button type="button" class="btn btn-danger btn-sm" data-action="delete-member" data-member-id="${escapeHtml(m.id)}">Remove</button>` : ""}
                  </div>`
                : ""
            }
          </div>
        `
          )
          .join("")}
      </div>
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
            <input type="text" id="admVerTag" class="input" value="${escapeHtml(state.manifest.versionTag || "v1.5.0")}" required />
          </label>
          <label style="font-size: 12px; font-weight: 700;">Curriculum Build Code
            <input type="number" id="admVerCode" class="input" value="${Number(state.manifest.versionCode || 105) + 1}" required />
          </label>
          <label style="font-size: 12px; font-weight: 700;">Release Notes
            <textarea id="admNotes" class="textarea" rows="3">${escapeHtml(state.manifest.releaseNotes || "")}</textarea>
          </label>
          <label style="font-size: 12px; font-weight: 700;">Global Student Announcement Banner
            <input type="text" id="admAnnouncement" class="input" value="${escapeHtml(state.manifest.announcement || "")}" />
          </label>
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
            <label style="font-size: 12px; font-weight: 700;">Latest APK Version Name
              <input type="text" id="admApkVerName" class="input" value="${escapeHtml(remoteVersionInfo?.versionName || state.manifest.latestApkVersionName || "1.5")}" required />
            </label>
            <label style="font-size: 12px; font-weight: 700;">Latest APK Version Code
              <input type="number" id="admApkVerCode" class="input" value="${Number(remoteVersionInfo?.versionCode || state.manifest.latestApkVersionCode || 6)}" required />
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
        <div style="display: grid; gap: 6px; font-size: 12.5px; color: #334155; border-top: 1px solid var(--border); padding-top: 10px;">
          <div><strong>Firebase Project:</strong> ${escapeHtml(FIREBASE_CONFIG.projectId)}</div>
          <div><strong>Firestore DB:</strong> ${escapeHtml(FIREBASE_CONFIG.databaseId)}</div>
          <div><strong>Google Cloud Auth:</strong> ${escapeHtml(cloudUser ? cloudUser.email : "Not signed in")}</div>
          <div><strong>Hosted version.json:</strong> ${remoteVersionInfo ? `v${escapeHtml(remoteVersionInfo.versionName)} (Build ${escapeHtml(remoteVersionInfo.versionCode)}) • ${formatBytes(remoteVersionInfo.fileSizeBytes)}` : "Pending fetch..."}</div>
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

  const titles = {
    dashboard: ["Executive SCM Dashboard", "International Supply Chain Education Board (ISCEB) — Desktop Workspace"],
    courses: ["All 12 Modules & 48 Lectures", "Part 1: Basic Level (1–28) & Part 2: Advance Level (29–48)"],
    exams: ["Timed Examinations & Assignments", "Official Module Assessments, 100-Mark Final Exam & Capstone Study"],
    learning: ["My Learning & Official PGD Certificate", "Academic Readiness, Printable Executive Diploma & Serial Verification"],
    directory: ["ISCEB Leadership & Directory", "Founder, App Developer & Verified Faculty Members"],
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

function setupListeners() {
  const select = document.getElementById("accountSwitcher");
  const studentUsers = getStudentSwitcherUsers(AVAILABLE_USERS);
  select.innerHTML = studentUsers.map(
    (u) => `<option value="${escapeHtml(u.id)}">${escapeHtml(u.fullName)} — ${escapeHtml(u.designation)}</option>`
  ).join("");
  syncAccountSwitcherUi();
  select.addEventListener("change", (e) => {
    const picked = e.target.value;
    if (!studentUsers.some((u) => u.id === picked)) return;
    if (isAdminUser()) {
      desktopAdminAuth.clearSession();
      verifiedAdminSessionId = null;
    }
    state.activeUserId = picked;
    saveState();
    render();
  });

  document.getElementById("navList").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-view]");
    if (!btn) return;
    state.currentView = btn.dataset.view;
    saveState();
    render();
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

  document.getElementById("installPwaBtn").addEventListener("click", async () => {
    if (!deferredInstallPrompt) return;
    deferredInstallPrompt.prompt();
    await deferredInstallPrompt.userChoice;
    deferredInstallPrompt = null;
    document.getElementById("installPwaBtn").style.display = "none";
  });

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
    }
  });
}

setupListeners();
rehydrateVerifiedAdminSession();
render();
fetchVersionJson();
initFirebaseSdk();
