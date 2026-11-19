/**
 * Desktop Web Portal Cryptographic Per-Admin Authentication & Role Filter Utilities.
 * Zero hardcoded or prefilled passwords; enforces separate PBKDF2-SHA256 credentials,
 * brute-force lockout, single-use second-admin invite codes, and student-only account switching.
 */

export const ADMIN_AMIRUL_ID = "admin-2";
export const ADMIN_NAFIUL_ID = "admin-1";
export const ORDERED_ADMIN_IDS = [ADMIN_AMIRUL_ID, ADMIN_NAFIUL_ID];
export const DESKTOP_AUTH_STORAGE_KEY = "isceb_desktop_admin_auth_v1";

const PBKDF2_ITERATIONS = 100000;
const MAX_FAILED_ATTEMPTS = 5;
const LOCKOUT_MS = 60 * 1000;
const INVITE_TTL_MS = 24 * 60 * 60 * 1000;
const SESSION_TTL_MS = 7 * 24 * 60 * 60 * 1000;

export function getStudentSwitcherUsers(users) {
  return (users || []).filter((u) => u && u.role === "STUDENT");
}

export function getOrderedAdmins(users) {
  const map = new Map((users || []).filter((u) => u && u.role === "ADMIN").map((u) => [u.id, u]));
  return ORDERED_ADMIN_IDS.map((id) => map.get(id)).filter(Boolean);
}

export function validateDesktopPasswordStrength(password, adminId) {
  const pw = String(password || "");
  if (pw.length < 8) return "Password must be at least 8 characters long.";
  if (pw.length > 128) return "Password must not exceed 128 characters.";

  const normalized = pw.trim().toLowerCase();
  const digitsOnly = normalized.replace(/[-\s]/g, "");
  const blocked = new Set([
    "scm2026",
    "admin",
    "admin123",
    "password",
    "password123",
    "12345678",
    "isceb",
    "isceb2026",
    "amirul",
    "nafiul",
    "nahin",
    "01715282905",
    "amirul.islam@isceb.org",
    "www.mdnafiulislam@gmail.com"
  ]);
  if (blocked.has(normalized) || blocked.has(digitsOnly)) {
    return "Choose a stronger private password — legacy passcodes, emails, names, and phone numbers are blocked.";
  }
  if (adminId === ADMIN_AMIRUL_ID && normalized.includes("amirul")) {
    return "Password cannot contain your own name.";
  }
  if (adminId === ADMIN_NAFIUL_ID && (normalized.includes("nafiul") || normalized.includes("nahin"))) {
    return "Password cannot contain your own name.";
  }
  if (!/[A-Z]/.test(pw) || !/[a-z]/.test(pw) || !/[0-9]/.test(pw) || !/[^A-Za-z0-9]/.test(pw)) {
    return "Include uppercase, lowercase, a number, and a special character (e.g. !@#$%).";
  }
  return null;
}

function getCryptoApi() {
  const c = globalThis.crypto;
  if (!c || !c.subtle) {
    throw new Error("WebCrypto API (HTTPS or localhost) is required for admin authentication.");
  }
  return c;
}

function randomHex(byteCount) {
  const bytes = new Uint8Array(byteCount);
  getCryptoApi().getRandomValues(bytes);
  return Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
}

function randomHumanToken(length = 12) {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = new Uint8Array(length);
  getCryptoApi().getRandomValues(bytes);
  let out = "";
  for (let i = 0; i < length; i++) {
    if (i > 0 && i % 4 === 0) out += "-";
    out += alphabet[bytes[i] % alphabet.length];
  }
  return out;
}

function constantTimeEqualHex(a, b) {
  if (!a || !b || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) {
    diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return diff === 0;
}

async function pbkdf2Hex(secret, saltHex) {
  const cryptoApi = getCryptoApi();
  const enc = new TextEncoder();
  const saltBytes = new Uint8Array(saltHex.match(/.{1,2}/g).map((h) => parseInt(h, 16)));
  const keyMaterial = await cryptoApi.subtle.importKey(
    "raw",
    enc.encode(secret),
    { name: "PBKDF2" },
    false,
    ["deriveBits"]
  );
  const derivedBits = await cryptoApi.subtle.deriveBits(
    {
      name: "PBKDF2",
      salt: saltBytes,
      iterations: PBKDF2_ITERATIONS,
      hash: "SHA-256"
    },
    keyMaterial,
    256
  );
  return Array.from(new Uint8Array(derivedBits), (b) => b.toString(16).padStart(2, "0")).join("");
}

async function hmacSha256Hex(secretHex, payloadText) {
  const cryptoApi = getCryptoApi();
  const keyBytes = new Uint8Array(secretHex.match(/.{1,2}/g).map((h) => parseInt(h, 16)));
  const key = await cryptoApi.subtle.importKey(
    "raw",
    keyBytes,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const sig = await cryptoApi.subtle.sign("HMAC", key, new TextEncoder().encode(payloadText));
  return Array.from(new Uint8Array(sig), (b) => b.toString(16).padStart(2, "0")).join("");
}

function adminDisplayName(adminId) {
  if (adminId === ADMIN_AMIRUL_ID) return "Md. Amirul Islam";
  if (adminId === ADMIN_NAFIUL_ID) return "Md. Nafiul Islam Nahin";
  return "Administrator";
}

function officialAdminEmail(adminId) {
  if (adminId === ADMIN_AMIRUL_ID) return "amirul.islam@isceb.org";
  if (adminId === ADMIN_NAFIUL_ID) return "www.mdnafiulislam@gmail.com";
  return "";
}

export function createDesktopAdminAuthStore({ storage = globalThis.localStorage, nowFn = () => Date.now() } = {}) {
  function readStore() {
    try {
      const raw = storage?.getItem?.(DESKTOP_AUTH_STORAGE_KEY);
      if (raw) return JSON.parse(raw);
    } catch (_) {}
    return { pepper: "", admins: {}, session: null };
  }

  function writeStore(data) {
    try {
      storage?.setItem?.(DESKTOP_AUTH_STORAGE_KEY, JSON.stringify(data));
    } catch (_) {}
  }

  function ensureAdminRecord(store, adminId) {
    if (!store.admins[adminId]) {
      store.admins[adminId] = {
        saltHex: "",
        hashHex: "",
        recoverySaltHex: "",
        recoveryHashHex: "",
        inviteSaltHex: "",
        inviteHashHex: "",
        inviteExpiresAt: 0,
        failedAttempts: 0,
        lockoutUntil: 0
      };
    }
    return store.admins[adminId];
  }

  function isAdminConfigured(adminId) {
    const store = readStore();
    const rec = store.admins?.[adminId];
    return Boolean(rec && rec.saltHex && rec.hashHex);
  }

  function isAnyAdminConfigured() {
    return ORDERED_ADMIN_IDS.some((id) => isAdminConfigured(id));
  }

  function hasActiveInviteFor(adminId) {
    const store = readStore();
    const rec = store.admins?.[adminId];
    return Boolean(rec && rec.inviteHashHex && rec.inviteSaltHex && Number(rec.inviteExpiresAt || 0) > nowFn());
  }

  function getSummaries() {
    const store = readStore();
    const now = nowFn();
    return ORDERED_ADMIN_IDS.map((id) => {
      const rec = store.admins?.[id] || {};
      const lockUntil = Number(rec.lockoutUntil || 0);
      return {
        adminId: id,
        fullName: adminDisplayName(id),
        designation: id === ADMIN_AMIRUL_ID ? "Founder of ISCEB" : "Developer of ISCEB App",
        subRole: id === ADMIN_AMIRUL_ID ? "Lead Trainer of ISCEB • Head of SCM at BRB Group" : officialAdminEmail(id),
        configured: Boolean(rec.saltHex && rec.hashHex),
        hasActiveInvite: Boolean(rec.inviteHashHex && Number(rec.inviteExpiresAt || 0) > now),
        lockoutRemainingSeconds: lockUntil > now ? Math.ceil((lockUntil - now) / 1000) : 0
      };
    });
  }

  async function issueSession(store, adminId) {
    if (!store.pepper) store.pepper = randomHex(32);
    const token = randomHex(32);
    const expiresAt = nowFn() + SESSION_TTL_MS;
    const macHex = await hmacSha256Hex(store.pepper, `${adminId}|${token}|${expiresAt}`);
    store.session = { adminId, token, expiresAt, macHex };
    writeStore(store);
  }

  async function getValidatedSessionAdminId() {
    const store = readStore();
    const s = store.session;
    if (!s || !s.adminId || !s.token || !s.macHex || !store.pepper) return null;
    if (!ORDERED_ADMIN_IDS.includes(s.adminId) || !isAdminConfigured(s.adminId)) return null;
    if (Number(s.expiresAt || 0) <= nowFn()) {
      clearSession();
      return null;
    }
    const expected = await hmacSha256Hex(store.pepper, `${s.adminId}|${s.token}|${s.expiresAt}`);
    return constantTimeEqualHex(expected, s.macHex) ? s.adminId : null;
  }

  function clearSession() {
    const store = readStore();
    store.session = null;
    writeStore(store);
  }

  async function setupInitialPassword(adminId, newPassword, confirmPassword, inviteToken = "") {
    if (!ORDERED_ADMIN_IDS.includes(adminId)) {
      return { ok: false, error: "Unknown administrator account." };
    }
    if (isAdminConfigured(adminId)) {
      return { ok: false, error: "This administrator already has an active password configured." };
    }
    const store = readStore();
    const rec = ensureAdminRecord(store, adminId);

    if (isAnyAdminConfigured()) {
      const trimmedInvite = String(inviteToken || "").trim();
      if (!trimmedInvite) {
        return { ok: false, error: "An active admin must generate a single-use setup invite code first." };
      }
      if (!rec.inviteHashHex || !rec.inviteSaltHex || Number(rec.inviteExpiresAt || 0) <= nowFn()) {
        rec.inviteHashHex = "";
        rec.inviteSaltHex = "";
        rec.inviteExpiresAt = 0;
        writeStore(store);
        return { ok: false, error: "Setup invite token is missing or expired. Ask an active admin to generate one." };
      }
      const computedInvite = await pbkdf2Hex(trimmedInvite, rec.inviteSaltHex);
      if (!constantTimeEqualHex(computedInvite, rec.inviteHashHex)) {
        return { ok: false, error: "Invalid single-use setup invite code." };
      }
    }

    if (newPassword !== confirmPassword) {
      return { ok: false, error: "Passwords do not match." };
    }
    const policyErr = validateDesktopPasswordStrength(newPassword, adminId);
    if (policyErr) return { ok: false, error: policyErr };

    const recoveryCode = randomHumanToken(12);
    rec.saltHex = randomHex(16);
    rec.hashHex = await pbkdf2Hex(newPassword, rec.saltHex);
    rec.recoverySaltHex = randomHex(16);
    rec.recoveryHashHex = await pbkdf2Hex(recoveryCode, rec.recoverySaltHex);
    rec.inviteSaltHex = "";
    rec.inviteHashHex = "";
    rec.inviteExpiresAt = 0;
    rec.failedAttempts = 0;
    rec.lockoutUntil = 0;

    await issueSession(store, adminId);
    return {
      ok: true,
      adminId,
      fullName: adminDisplayName(adminId),
      recoveryCode,
      message: `Password configured for ${adminDisplayName(adminId)}. Save your one-time recovery key.`
    };
  }

  async function authenticate(adminId, password) {
    if (!ORDERED_ADMIN_IDS.includes(adminId)) {
      return { ok: false, error: "Invalid administrator selection." };
    }
    const store = readStore();
    const rec = ensureAdminRecord(store, adminId);
    const now = nowFn();
    if (Number(rec.lockoutUntil || 0) > now) {
      const secs = Math.ceil((rec.lockoutUntil - now) / 1000);
      return { ok: false, error: `Too many failed attempts. Try again in ${secs}s.` };
    }
    if (!rec.saltHex || !rec.hashHex) {
      return { ok: false, error: "Admin password not configured yet. Complete First-Time Setup first." };
    }
    if (!password) {
      return { ok: false, error: "Enter your administrator password." };
    }
    const computed = await pbkdf2Hex(password, rec.saltHex);
    if (constantTimeEqualHex(computed, rec.hashHex)) {
      rec.failedAttempts = 0;
      rec.lockoutUntil = 0;
      await issueSession(store, adminId);
      return { ok: true, adminId, fullName: adminDisplayName(adminId), message: `Signed in as ${adminDisplayName(adminId)}.` };
    }
    rec.failedAttempts = Number(rec.failedAttempts || 0) + 1;
    if (rec.failedAttempts >= MAX_FAILED_ATTEMPTS) {
      rec.failedAttempts = 0;
      rec.lockoutUntil = now + LOCKOUT_MS;
      writeStore(store);
      return { ok: false, error: `Account locked for 60s after ${MAX_FAILED_ATTEMPTS} failed login attempts.` };
    }
    writeStore(store);
    return { ok: false, error: `Incorrect password. ${MAX_FAILED_ATTEMPTS - rec.failedAttempts} attempt(s) remaining.` };
  }

  async function generateSecondAdminInvite(targetAdminId) {
    const activeAdminId = await getValidatedSessionAdminId();
    if (!activeAdminId) return { ok: false, error: "Active verified admin session required." };
    if (!ORDERED_ADMIN_IDS.includes(targetAdminId) || targetAdminId === activeAdminId) {
      return { ok: false, error: "Select the co-admin account." };
    }
    if (isAdminConfigured(targetAdminId)) {
      return { ok: false, error: `${adminDisplayName(targetAdminId)} already has a password configured.` };
    }
    const store = readStore();
    const rec = ensureAdminRecord(store, targetAdminId);
    const token = randomHumanToken(12);
    rec.inviteSaltHex = randomHex(16);
    rec.inviteHashHex = await pbkdf2Hex(token, rec.inviteSaltHex);
    rec.inviteExpiresAt = nowFn() + INVITE_TTL_MS;
    writeStore(store);
    return { ok: true, targetAdminId, targetAdminName: adminDisplayName(targetAdminId), setupToken: token };
  }

  async function changeOwnPassword(adminId, currentPassword, newPassword, confirmPassword) {
    const activeAdminId = await getValidatedSessionAdminId();
    if (!activeAdminId || activeAdminId !== adminId) {
      return { ok: false, error: "Only the currently signed-in admin can change their password." };
    }
    const store = readStore();
    const rec = ensureAdminRecord(store, adminId);
    const computedCurrent = await pbkdf2Hex(currentPassword || "", rec.saltHex || "00");
    if (!rec.hashHex || !constantTimeEqualHex(computedCurrent, rec.hashHex)) {
      return { ok: false, error: "Current password is incorrect." };
    }
    if (newPassword === currentPassword) {
      return { ok: false, error: "New password must differ from current password." };
    }
    if (newPassword !== confirmPassword) {
      return { ok: false, error: "New passwords do not match." };
    }
    const policyErr = validateDesktopPasswordStrength(newPassword, adminId);
    if (policyErr) return { ok: false, error: policyErr };

    const nextRecovery = randomHumanToken(12);
    rec.saltHex = randomHex(16);
    rec.hashHex = await pbkdf2Hex(newPassword, rec.saltHex);
    rec.recoverySaltHex = randomHex(16);
    rec.recoveryHashHex = await pbkdf2Hex(nextRecovery, rec.recoverySaltHex);
    await issueSession(store, adminId);
    return { ok: true, recoveryCode: nextRecovery, message: `Password updated for ${adminDisplayName(adminId)}.` };
  }

  async function resetPasswordWithRecoveryOrGoogle(adminId, verifiedGoogleEmail, recoveryCode, newPassword, confirmPassword) {
    if (!isAdminConfigured(adminId)) {
      return { ok: false, error: "Admin account has not been set up yet." };
    }
    const store = readStore();
    const rec = ensureAdminRecord(store, adminId);
    const expectedEmail = officialAdminEmail(adminId);
    const googleOk = Boolean(verifiedGoogleEmail && expectedEmail && verifiedGoogleEmail.trim().toLowerCase() === expectedEmail.toLowerCase());
    let recoveryOk = false;
    if (recoveryCode && rec.recoverySaltHex && rec.recoveryHashHex) {
      const compRec = await pbkdf2Hex(recoveryCode.trim(), rec.recoverySaltHex);
      recoveryOk = constantTimeEqualHex(compRec, rec.recoveryHashHex);
    }
    if (!googleOk && !recoveryOk) {
      return { ok: false, error: `Verification required: sign in with Google (${expectedEmail}) or enter your one-time recovery key.` };
    }
    if (newPassword !== confirmPassword) {
      return { ok: false, error: "New passwords do not match." };
    }
    const policyErr = validateDesktopPasswordStrength(newPassword, adminId);
    if (policyErr) return { ok: false, error: policyErr };

    const rotatedRecovery = randomHumanToken(12);
    rec.saltHex = randomHex(16);
    rec.hashHex = await pbkdf2Hex(newPassword, rec.saltHex);
    rec.recoverySaltHex = randomHex(16);
    rec.recoveryHashHex = await pbkdf2Hex(rotatedRecovery, rec.recoverySaltHex);
    rec.failedAttempts = 0;
    rec.lockoutUntil = 0;
    await issueSession(store, adminId);
    return { ok: true, adminId, fullName: adminDisplayName(adminId), recoveryCode: rotatedRecovery, message: "Password reset complete." };
  }

  return {
    isAdminConfigured,
    isAnyAdminConfigured,
    hasActiveInviteFor,
    getSummaries,
    getValidatedSessionAdminId,
    clearSession,
    setupInitialPassword,
    authenticate,
    generateSecondAdminInvite,
    changeOwnPassword,
    resetPasswordWithRecoveryOrGoogle
  };
}
