package com.example.auth

import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class AdminAccountStatus {
    PENDING_SETUP,
    ACTIVE
}

data class AdminAuthSummary(
    val adminId: String,
    val fullName: String,
    val designation: String,
    val additionalRole: String,
    val organization: String,
    val email: String,
    val mobileNumber: String,
    val status: AdminAccountStatus,
    val hasPendingSetupInvite: Boolean,
    val failedAttempts: Int,
    val lockoutRemainingSeconds: Long
)

sealed class AdminAuthResult {
    data class Authenticated(
        val adminId: String,
        val fullName: String,
        val message: String,
        val oneTimeRecoveryCode: String? = null
    ) : AdminAuthResult()

    data class SetupInviteGenerated(
        val targetAdminId: String,
        val targetAdminName: String,
        val setupToken: String,
        val expiresAtMillis: Long
    ) : AdminAuthResult()

    data class Success(
        val message: String,
        val oneTimeRecoveryCode: String? = null
    ) : AdminAuthResult()

    data class Failure(
        val errorMessage: String,
        val lockoutRemainingSeconds: Long = 0L
    ) : AdminAuthResult()
}

/**
 * Cryptographic per-admin credential store and session verifier.
 * Uses PBKDF2-HMAC-SHA256 (100,000 iterations) with per-account 128-bit random salts,
 * constant-time hash comparison, brute-force lockout, single-use second-admin invite tokens,
 * and authenticated session tokens verified on app startup.
 */
class AdminAuthManager(
    private val prefs: SharedPreferences,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {

    fun getOrderedAdminIds(): List<String> = listOf(ADMIN_AMIRUL_ID, ADMIN_NAFIUL_ID)

    fun isAnyAdminConfigured(): Boolean {
        return hasCredentials(ADMIN_AMIRUL_ID) || hasCredentials(ADMIN_NAFIUL_ID)
    }

    fun isAdminConfigured(adminId: String): Boolean = hasCredentials(adminId)

    fun hasActiveInviteFor(adminId: String): Boolean {
        val hash = prefs.getString(keyInviteHash(adminId), null)
        val expiresAt = prefs.getLong(keyInviteExpires(adminId), 0L)
        return !hash.isNullOrBlank() && expiresAt > timeProvider()
    }

    fun getAdminSummaries(): List<AdminAuthSummary> {
        val now = timeProvider()
        return listOf(
            AdminAuthSummary(
                adminId = ADMIN_AMIRUL_ID,
                fullName = "Md. Amirul Islam",
                designation = "Founder of ISCEB",
                additionalRole = "Lead Trainer of ISCEB",
                organization = "Head of Supply Chain Management at BRB Group",
                email = "amirul.islam@isceb.org",
                mobileNumber = "01715-282905",
                status = if (hasCredentials(ADMIN_AMIRUL_ID)) AdminAccountStatus.ACTIVE else AdminAccountStatus.PENDING_SETUP,
                hasPendingSetupInvite = hasActiveInviteFor(ADMIN_AMIRUL_ID),
                failedAttempts = prefs.getInt(keyFailedAttempts(ADMIN_AMIRUL_ID), 0),
                lockoutRemainingSeconds = remainingLockoutSeconds(ADMIN_AMIRUL_ID, now)
            ),
            AdminAuthSummary(
                adminId = ADMIN_NAFIUL_ID,
                fullName = "Md. Nafiul Islam Nahin",
                designation = "Developer of ISCEB App",
                additionalRole = "",
                organization = "ISCEB App Engineering",
                email = "www.mdnafiulislam@gmail.com",
                mobileNumber = "",
                status = if (hasCredentials(ADMIN_NAFIUL_ID)) AdminAccountStatus.ACTIVE else AdminAccountStatus.PENDING_SETUP,
                hasPendingSetupInvite = hasActiveInviteFor(ADMIN_NAFIUL_ID),
                failedAttempts = prefs.getInt(keyFailedAttempts(ADMIN_NAFIUL_ID), 0),
                lockoutRemainingSeconds = remainingLockoutSeconds(ADMIN_NAFIUL_ID, now)
            )
        )
    }

    /**
     * Completes first-time onboarding for an admin account:
     * - When NO admin exists on the device yet, either authorized admin can initialize their own strong password.
     * - Once any admin exists, setting up the second admin requires a valid single-use invite token
     *   issued by the already-authenticated administrator.
     */
    fun setupInitialPassword(
        adminId: String,
        newPassword: String,
        confirmPassword: String,
        setupInviteToken: String = ""
    ): AdminAuthResult {
        if (adminId !in getOrderedAdminIds()) {
            return AdminAuthResult.Failure("Unknown administrator account.")
        }
        if (hasCredentials(adminId)) {
            return AdminAuthResult.Failure("This administrator already has an active password configured.")
        }
        if (isAnyAdminConfigured()) {
            val trimmedInvite = setupInviteToken.trim()
            if (trimmedInvite.isBlank()) {
                return AdminAuthResult.Failure("An active admin must generate a single-use setup invite code first.")
            }
            val expectedHash = prefs.getString(keyInviteHash(adminId), null)
            val saltHex = prefs.getString(keyInviteSalt(adminId), null)
            val expiresAt = prefs.getLong(keyInviteExpires(adminId), 0L)
            if (expectedHash.isNullOrBlank() || saltHex.isNullOrBlank() || expiresAt <= timeProvider()) {
                clearInvite(adminId)
                return AdminAuthResult.Failure("Setup invite token is missing or expired. Generate a fresh invite from an active admin.")
            }
            if (!verifySecret(trimmedInvite, saltHex, expectedHash)) {
                return AdminAuthResult.Failure("Invalid single-use setup invite code.")
            }
        }
        if (newPassword != confirmPassword) {
            return AdminAuthResult.Failure("Passwords do not match.")
        }
        val policyError = validatePasswordStrength(newPassword, adminId)
        if (policyError != null) {
            return AdminAuthResult.Failure(policyError)
        }

        val recoveryCode = generateHumanFriendlyToken(12)
        storeCredentials(adminId, newPassword, recoveryCode)
        clearInvite(adminId)
        createSession(adminId)

        return AdminAuthResult.Authenticated(
            adminId = adminId,
            fullName = adminDisplayName(adminId),
            message = "Security password configured for ${adminDisplayName(adminId)}. Save your one-time recovery key in a safe place.",
            oneTimeRecoveryCode = recoveryCode
        )
    }

    /**
     * Generates a single-use cryptographic setup invite token for an unconfigured co-admin.
     * Must only be called while another admin has a verified active session.
     */
    fun generateSecondAdminSetupInvite(targetAdminId: String): AdminAuthResult {
        val currentSessionAdminId = getValidatedSessionAdminId()
            ?: return AdminAuthResult.Failure("Active admin session required to issue an invite token.")
        if (targetAdminId !in getOrderedAdminIds()) {
            return AdminAuthResult.Failure("Invalid administrator target.")
        }
        if (targetAdminId == currentSessionAdminId) {
            return AdminAuthResult.Failure("Choose the co-admin account that has not yet created a password.")
        }
        if (hasCredentials(targetAdminId)) {
            return AdminAuthResult.Failure("${adminDisplayName(targetAdminId)} already has a configured password.")
        }

        val token = generateHumanFriendlyToken(12)
        val salt = generateRandomBytes(SALT_BYTES)
        val hashHex = pbkdf2Hex(token.toCharArray(), salt)
        val expiresAt = timeProvider() + INVITE_TTL_MILLIS

        prefs.edit()
            .putString(keyInviteSalt(targetAdminId), bytesToHex(salt))
            .putString(keyInviteHash(targetAdminId), hashHex)
            .putLong(keyInviteExpires(targetAdminId), expiresAt)
            .apply()

        return AdminAuthResult.SetupInviteGenerated(
            targetAdminId = targetAdminId,
            targetAdminName = adminDisplayName(targetAdminId),
            setupToken = token,
            expiresAtMillis = expiresAt
        )
    }

    /**
     * Authenticates an administrator using their per-admin salted PBKDF2 password hash.
     */
    fun authenticate(adminId: String, password: String): AdminAuthResult {
        if (adminId !in getOrderedAdminIds()) {
            return AdminAuthResult.Failure("Invalid administrator selection.")
        }
        val now = timeProvider()
        val lockoutRemaining = remainingLockoutSeconds(adminId, now)
        if (lockoutRemaining > 0L) {
            return AdminAuthResult.Failure(
                errorMessage = "Too many failed attempts. Try again in ${lockoutRemaining}s.",
                lockoutRemainingSeconds = lockoutRemaining
            )
        }
        if (!hasCredentials(adminId)) {
            return AdminAuthResult.Failure("Admin password has not been set up yet. Complete First-Time Setup first.")
        }
        if (password.isEmpty()) {
            return AdminAuthResult.Failure("Enter your administrator password.")
        }

        val saltHex = prefs.getString(keySalt(adminId), null).orEmpty()
        val hashHex = prefs.getString(keyHash(adminId), null).orEmpty()

        return if (saltHex.isNotBlank() && hashHex.isNotBlank() && verifySecret(password, saltHex, hashHex)) {
            prefs.edit()
                .putInt(keyFailedAttempts(adminId), 0)
                .putLong(keyLockoutUntil(adminId), 0L)
                .apply()
            createSession(adminId)
            AdminAuthResult.Authenticated(
                adminId = adminId,
                fullName = adminDisplayName(adminId),
                message = "Authenticated as ${adminDisplayName(adminId)}."
            )
        } else {
            val attempts = prefs.getInt(keyFailedAttempts(adminId), 0) + 1
            val lockUntil = if (attempts >= MAX_FAILED_ATTEMPTS) now + LOCKOUT_DURATION_MILLIS else 0L
            prefs.edit()
                .putInt(keyFailedAttempts(adminId), if (lockUntil > 0L) 0 else attempts)
                .putLong(keyLockoutUntil(adminId), lockUntil)
                .apply()

            if (lockUntil > 0L) {
                val secs = LOCKOUT_DURATION_MILLIS / 1000L
                AdminAuthResult.Failure(
                    errorMessage = "Account temporarily locked for ${secs}s after $MAX_FAILED_ATTEMPTS failed login attempts.",
                    lockoutRemainingSeconds = secs
                )
            } else {
                val left = MAX_FAILED_ATTEMPTS - attempts
                AdminAuthResult.Failure("Incorrect password for ${adminDisplayName(adminId)}. $left attempt(s) remaining.")
            }
        }
    }

    /**
     * Allows a signed-in admin to rotate their own password after verifying the current password.
     */
    fun changeOwnPassword(
        adminId: String,
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): AdminAuthResult {
        val activeId = getValidatedSessionAdminId()
        if (activeId == null || activeId != adminId) {
            return AdminAuthResult.Failure("You can only change the password of your currently signed-in admin account.")
        }
        val saltHex = prefs.getString(keySalt(adminId), null).orEmpty()
        val hashHex = prefs.getString(keyHash(adminId), null).orEmpty()
        if (!verifySecret(currentPassword, saltHex, hashHex)) {
            return AdminAuthResult.Failure("Current password is incorrect.")
        }
        if (newPassword == currentPassword) {
            return AdminAuthResult.Failure("New password must differ from your current password.")
        }
        if (newPassword != confirmPassword) {
            return AdminAuthResult.Failure("New password and confirmation do not match.")
        }
        val policyError = validatePasswordStrength(newPassword, adminId)
        if (policyError != null) {
            return AdminAuthResult.Failure(policyError)
        }

        val nextRecovery = generateHumanFriendlyToken(12)
        storeCredentials(adminId, newPassword, nextRecovery)
        createSession(adminId)
        return AdminAuthResult.Success(
            message = "Password updated for ${adminDisplayName(adminId)}. Store your new one-time recovery key safely.",
            oneTimeRecoveryCode = nextRecovery
        )
    }

    /**
     * Resets an admin password ONLY when verified via either:
     * 1. Verified Firebase Google Sign-In matching that admin's registered official email, OR
     * 2. The one-time cryptographic recovery key issued during password creation.
     */
    fun resetPasswordWithVerification(
        adminId: String,
        verifiedGoogleEmail: String?,
        recoveryCode: String,
        newPassword: String,
        confirmPassword: String
    ): AdminAuthResult {
        if (!hasCredentials(adminId)) {
            return AdminAuthResult.Failure("This admin account has not completed initial setup yet.")
        }
        val officialEmail = when (adminId) {
            ADMIN_AMIRUL_ID -> "amirul.islam@isceb.org"
            ADMIN_NAFIUL_ID -> "www.mdnafiulislam@gmail.com"
            else -> return AdminAuthResult.Failure("Unknown admin account.")
        }

        val googleVerified = !verifiedGoogleEmail.isNullOrBlank() &&
            verifiedGoogleEmail.trim().equals(officialEmail, ignoreCase = true)

        val recoveryVerified = if (recoveryCode.isNotBlank()) {
            val recSalt = prefs.getString(keyRecoverySalt(adminId), null).orEmpty()
            val recHash = prefs.getString(keyRecoveryHash(adminId), null).orEmpty()
            recSalt.isNotBlank() && recHash.isNotBlank() && verifySecret(recoveryCode.trim(), recSalt, recHash)
        } else {
            false
        }

        if (!googleVerified && !recoveryVerified) {
            return AdminAuthResult.Failure(
                "Verification required: sign in with Google ($officialEmail) or enter your one-time recovery key."
            )
        }
        if (newPassword != confirmPassword) {
            return AdminAuthResult.Failure("New passwords do not match.")
        }
        val policyError = validatePasswordStrength(newPassword, adminId)
        if (policyError != null) {
            return AdminAuthResult.Failure(policyError)
        }

        val rotatedRecovery = generateHumanFriendlyToken(12)
        storeCredentials(adminId, newPassword, rotatedRecovery)
        prefs.edit()
            .putInt(keyFailedAttempts(adminId), 0)
            .putLong(keyLockoutUntil(adminId), 0L)
            .apply()
        createSession(adminId)

        return AdminAuthResult.Authenticated(
            adminId = adminId,
            fullName = adminDisplayName(adminId),
            message = "Password reset verified for ${adminDisplayName(adminId)}.",
            oneTimeRecoveryCode = rotatedRecovery
        )
    }

    /**
     * Validates the cryptographic admin session token before granting or restoring admin privileges.
     */
    fun getValidatedSessionAdminId(): String? {
        val sessionAdminId = prefs.getString(KEY_SESSION_ADMIN_ID, null) ?: return null
        val token = prefs.getString(KEY_SESSION_TOKEN, null) ?: return null
        val macHex = prefs.getString(KEY_SESSION_MAC, null) ?: return null
        val expiresAt = prefs.getLong(KEY_SESSION_EXPIRES_AT, 0L)

        if (sessionAdminId !in getOrderedAdminIds()) return null
        if (!hasCredentials(sessionAdminId)) return null
        if (expiresAt <= timeProvider()) {
            clearSession()
            return null
        }

        val deviceSecret = getOrCreateDeviceSessionKey()
        val expectedMac = computeSessionMac(deviceSecret, sessionAdminId, token, expiresAt)
        return if (constantTimeEqualsHex(expectedMac, macHex)) sessionAdminId else null
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_SESSION_ADMIN_ID)
            .remove(KEY_SESSION_TOKEN)
            .remove(KEY_SESSION_MAC)
            .remove(KEY_SESSION_EXPIRES_AT)
            .apply()
    }

    fun validatePasswordStrength(password: String, adminId: String): String? {
        if (password.length < 8) {
            return "Password must be at least 8 characters long."
        }
        if (password.length > 128) {
            return "Password must not exceed 128 characters."
        }
        val normalized = password.trim().lowercase(Locale.ROOT)
        val digitsOnly = password.replace("-", "").replace(" ", "")
        val blockedValues = setOf(
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
        )
        if (normalized in blockedValues || digitsOnly in blockedValues) {
            return "Choose a stronger private password — legacy passcodes, emails, names, and phone numbers are blocked."
        }
        if (adminId == ADMIN_AMIRUL_ID && normalized.contains("amirul")) {
            return "Password cannot contain your own name."
        }
        if (adminId == ADMIN_NAFIUL_ID && (normalized.contains("nafiul") || normalized.contains("nahin"))) {
            return "Password cannot contain your own name."
        }
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            return "Include uppercase, lowercase, a number, and a special character (e.g., !@#\$%)."
        }
        return null
    }

    private fun storeCredentials(adminId: String, password: String, recoveryCode: String) {
        val salt = generateRandomBytes(SALT_BYTES)
        val hashHex = pbkdf2Hex(password.toCharArray(), salt)

        val recoverySalt = generateRandomBytes(SALT_BYTES)
        val recoveryHashHex = pbkdf2Hex(recoveryCode.toCharArray(), recoverySalt)

        prefs.edit()
            .putString(keySalt(adminId), bytesToHex(salt))
            .putString(keyHash(adminId), hashHex)
            .putString(keyRecoverySalt(adminId), bytesToHex(recoverySalt))
            .putString(keyRecoveryHash(adminId), recoveryHashHex)
            .putInt(keyFailedAttempts(adminId), 0)
            .putLong(keyLockoutUntil(adminId), 0L)
            .apply()
    }

    private fun createSession(adminId: String) {
        val token = bytesToHex(generateRandomBytes(32))
        val expiresAt = timeProvider() + SESSION_TTL_MILLIS
        val secret = getOrCreateDeviceSessionKey()
        val macHex = computeSessionMac(secret, adminId, token, expiresAt)

        prefs.edit()
            .putString(KEY_SESSION_ADMIN_ID, adminId)
            .putString(KEY_SESSION_TOKEN, token)
            .putString(KEY_SESSION_MAC, macHex)
            .putLong(KEY_SESSION_EXPIRES_AT, expiresAt)
            .apply()
    }

    private fun getOrCreateDeviceSessionKey(): ByteArray {
        val existing = prefs.getString(KEY_DEVICE_SESSION_PEPPER, null)
        if (!existing.isNullOrBlank()) {
            return hexToBytes(existing)
        }
        val fresh = generateRandomBytes(32)
        prefs.edit().putString(KEY_DEVICE_SESSION_PEPPER, bytesToHex(fresh)).apply()
        return fresh
    }

    private fun computeSessionMac(secret: ByteArray, adminId: String, token: String, expiresAt: Long): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        val payload = "$adminId|$token|$expiresAt".toByteArray(Charsets.UTF_8)
        return bytesToHex(mac.doFinal(payload))
    }

    private fun hasCredentials(adminId: String): Boolean {
        return !prefs.getString(keySalt(adminId), null).isNullOrBlank() &&
            !prefs.getString(keyHash(adminId), null).isNullOrBlank()
    }

    private fun clearInvite(adminId: String) {
        prefs.edit()
            .remove(keyInviteSalt(adminId))
            .remove(keyInviteHash(adminId))
            .remove(keyInviteExpires(adminId))
            .apply()
    }

    private fun remainingLockoutSeconds(adminId: String, now: Long): Long {
        val lockUntil = prefs.getLong(keyLockoutUntil(adminId), 0L)
        return if (lockUntil > now) ((lockUntil - now) + 999L) / 1000L else 0L
    }

    private fun verifySecret(rawInput: String, saltHex: String, expectedHashHex: String): Boolean {
        val salt = hexToBytes(saltHex)
        val computed = pbkdf2Hex(rawInput.toCharArray(), salt)
        return constantTimeEqualsHex(computed, expectedHashHex)
    }

    private fun pbkdf2Hex(passwordChars: CharArray, salt: ByteArray): String {
        val bytes = try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            factory.generateSecret(spec).encoded
        } catch (_: Exception) {
            pbkdf2HmacSha256Pure(
                String(passwordChars).toByteArray(Charsets.UTF_8),
                salt,
                PBKDF2_ITERATIONS,
                KEY_LENGTH_BITS / 8
            )
        } finally {
            passwordChars.fill('\u0000')
        }
        return bytesToHex(bytes)
    }

    // RFC 2898 fallback using standard HmacSHA256 for Android API 24-25 compatibility
    private fun pbkdf2HmacSha256Pure(password: ByteArray, salt: ByteArray, iterations: Int, dkLen: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))
        val hLen = mac.macLength
        val blocks = (dkLen + hLen - 1) / hLen
        val out = ByteArray(blocks * hLen)
        for (i in 1..blocks) {
            val blockIndex = byteArrayOf(
                ((i shr 24) and 0xFF).toByte(),
                ((i shr 16) and 0xFF).toByte(),
                ((i shr 8) and 0xFF).toByte(),
                (i and 0xFF).toByte()
            )
            mac.reset()
            mac.update(salt)
            var u = mac.doFinal(blockIndex)
            val t = u.copyOf()
            for (c in 2..iterations) {
                mac.reset()
                u = mac.doFinal(u)
                for (j in t.indices) {
                    t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
                }
            }
            System.arraycopy(t, 0, out, (i - 1) * hLen, hLen)
        }
        return out.copyOf(dkLen)
    }

    private fun constantTimeEqualsHex(a: String, b: String): Boolean {
        if (a.length != b.length || a.isEmpty()) return false
        return MessageDigest.isEqual(a.toByteArray(Charsets.US_ASCII), b.toByteArray(Charsets.US_ASCII))
    }

    private fun generateRandomBytes(size: Int): ByteArray {
        return ByteArray(size).also { secureRandom.nextBytes(it) }
    }

    private fun generateHumanFriendlyToken(length: Int): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(length) {
            repeat(length) {
                if (it > 0 && it % 4 == 0) append('-')
                append(alphabet[secureRandom.nextInt(alphabet.length)])
            }
        }
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length / 2
        val result = ByteArray(len)
        for (i in 0 until len) {
            result[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }

    private fun adminDisplayName(adminId: String): String = when (adminId) {
        ADMIN_AMIRUL_ID -> "Md. Amirul Islam"
        ADMIN_NAFIUL_ID -> "Md. Nafiul Islam Nahin"
        else -> "Administrator"
    }

    private fun keySalt(adminId: String) = "admin_auth_salt_$adminId"
    private fun keyHash(adminId: String) = "admin_auth_hash_$adminId"
    private fun keyRecoverySalt(adminId: String) = "admin_auth_recovery_salt_$adminId"
    private fun keyRecoveryHash(adminId: String) = "admin_auth_recovery_hash_$adminId"
    private fun keyInviteSalt(adminId: String) = "admin_auth_invite_salt_$adminId"
    private fun keyInviteHash(adminId: String) = "admin_auth_invite_hash_$adminId"
    private fun keyInviteExpires(adminId: String) = "admin_auth_invite_expires_$adminId"
    private fun keyFailedAttempts(adminId: String) = "admin_auth_failed_$adminId"
    private fun keyLockoutUntil(adminId: String) = "admin_auth_lockout_$adminId"

    companion object {
        const val ADMIN_AMIRUL_ID = "admin-2"
        const val ADMIN_NAFIUL_ID = "admin-1"

        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MILLIS = 60_000L
        private const val INVITE_TTL_MILLIS = 24L * 60L * 60L * 1000L
        private const val SESSION_TTL_MILLIS = 7L * 24L * 60L * 60L * 1000L

        private const val KEY_SESSION_ADMIN_ID = "admin_session_admin_id"
        private const val KEY_SESSION_TOKEN = "admin_session_token"
        private const val KEY_SESSION_MAC = "admin_session_mac"
        private const val KEY_SESSION_EXPIRES_AT = "admin_session_expires_at"
        private const val KEY_DEVICE_SESSION_PEPPER = "admin_device_session_pepper"

        private val secureRandom = SecureRandom()
    }
}
