# Supply Chain Masterclass (SCM)

**Supply Chain Masterclass** is an executive-grade, mobile-first learning application built for supply chain practitioners, logistics executives, trade finance specialists, and students. It features a complete 12-module specialization curriculum comprising 48 foundational lessons, interactive multimedia viewers, rigorous student progress tracking, authoritative completion rules, on-demand landscape PDF certificate generation, and an institutional certificate verification registry.

---

## 🎨 Theme & Visual Design
- **Primary Navy:** `#071A33`
- **Secondary Navy:** `#102B4C`
- **Metallic Gold:** `#D4AF37`
- **Soft Gold:** `#F4E2A3`
- **Light Background:** `#F5F7FA`
- **Pure White Cards:** `#FFFFFF`

---

## 🚀 Key Upgrades & New Features

### 1. Multi-User Student Progress Tracking
- **Isolated Progress Per Student:** Progress (start times, active learning seconds, last active timestamps, quiz scores, and completions) is tracked and persisted separately for each user profile.
- **Student Profile Switcher:** Easily switch between verified test students (`Sarah Jenkins`, `Alex Rivera`, `David Chen`) and `Director Marcus Vance` (Admin) in the Profile tab.
- **Persistence Across Sessions:** Progress is immediately saved to local persistent storage (`student_progress_${studentId}`) and restored upon login/logout or device switching.

### 2. Authoritative Completion Rules
- **Active Learning Time Tracking:** Active study seconds accumulate strictly while audio or video is actively playing or while PDF notes are being studied. Paused playback or backgrounding freezes active time.
- **Configurable Thresholds:**
  - Minimum active learning time per class (default 60 seconds).
  - Minimum audio/video playback percentage (default 80%).
  - Minimum PDF reading time (default 45 seconds) with explicit reading confirmation button.
  - Mandatory Class Mastery Quiz (default 70% passing score).
- **Authoritative Verification:** A class cannot be marked complete simply by opening a file or clicking a button. All required criteria (active time, playback %, reading confirmation, and quiz pass) must be met before an automatic green checkmark and persistent timestamp are awarded.

### 3. Automatic Checkmarks & State Indicators
- **Visual State Badges:**
  - `COMPLETED`: Verified green checkmark with recorded timestamp.
  - `IN PROGRESS`: Amber badge showing accumulated active study seconds.
  - `PENDING`: Gray badge for unstarted classes.
  - `LOCKED`: Administrative locked status.
- **Duplicate Prevention:** Authoritative verification guarantees idempotency; repeated attempts cannot corrupt timestamps or create duplicate completion entries.

### 4. Course Completion & Certification Dashboard
- Course progress dashboard across all 12 modules and 48 classes.
- Admin can configure which modules are required for diploma eligibility (default: all 12 modules).
- Authoritative recalculation of eligibility based on stored records.

### 5. On-Demand Landscape PDF Certificate Generation
- **Visual Design:** High-resolution vector PDF certificate rendered in landscape A4 format matching the brand palette (dark navy borders, inner metallic gold borders, ornate diamond flourishes, official gold seal, student profile name, issue date, unique ID, and authorized signer).
- **Native Android Engine:** Generated on-demand using Android `android.graphics.pdf.PdfDocument` with `FileProvider` integration for instant preview, viewing in external PDF viewers, printing, and sharing.
- **Configurable Credentials:** Admin can configure institution name, accreditation statement, authorized signer name, title, and diploma title.

### 6. Institutional Certificate Verification Registry
- **Unique Identifier:** Each certificate receives a unique, non-guessable hash ID (e.g. `SCM-2026-CERT-XXXX-XXXX`).
- **Verification Page:** Anyone can enter a Certificate ID to verify:
  - Status: `VALID` or `REVOKED`.
  - Recipient verified name.
  - Course title and completion record (48 of 48 classes).
  - Issuing date, institution, and authorized signer.
- **Revocation Governance:** Administrators can revoke compromised or disputed certificates with an auditable reason, or reinstate them as valid.
- **Accreditation Disclaimer:** The app never claims unverified accreditation unless explicitly configured by the institution.

### 7. Governance & Administrative Settings
- Passcode: `scm2026` (or quick demo login button).
- Controls for:
  - Minimum active learning time slider.
  - Minimum playback percentage slider.
  - Mandatory quiz requirement toggle and passing threshold.
  - Required modules selection checkboxes.
  - Institution name, signer name, and title.
  - **Fast-Track Testing Tool:** Allows testing certificate generation immediately by fast-tracking the active student to 100% completion and quiz pass.

---

## 📦 12-Module Specialization Curriculum (48 Classes)

1. **Module 1: Supply Chain Fundamentals** (Classes 1–4)
2. **Module 2: Procurement and Sourcing** (Classes 5–8)
3. **Module 3: Inventory Management** (Classes 9–12)
4. **Module 4: Warehousing Management** (Classes 13–16)
5. **Module 5: Transportation and Logistics** (Classes 17–20)
6. **Module 6: Import and Export Operations** (Classes 21–24)
7. **Module 7: International Trade Documentation** (Classes 25–28)
8. **Module 8: Letter of Credit and Trade Finance** (Classes 29–32)
9. **Module 9: Customs and C&F Operations** (Classes 33–36)
10. **Module 10: Demand Planning and Forecasting** (Classes 37–40)
11. **Module 11: ERP, Digital Supply Chain and Analytics** (Classes 41–44)
12. **Module 12: Supply Chain Strategy and Risk Management** (Classes 45–48)

---

## ☁️ Production Firebase Cloud Configuration

### 1. Cloud Firestore Schema
- `/users/{userId}/class_progress/{classId}`:
  - `status`: String (`NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`)
  - `activeLearningSeconds`: Number
  - `maxPlaybackPercent`: Number
  - `isPdfConfirmed`: Boolean
  - `quizPassed`: Boolean
  - `completionTimestamp`: Timestamp
- `/certificates/{certificateId}`:
  - `certificateId`: String (Unique)
  - `studentId`: String
  - `studentName`: String
  - `issueDate`: String
  - `status`: String (`VALID`, `REVOKED`)
  - `revocationReason`: String

### 2. Firebase Cloud Storage Security Rules
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /courses/{moduleId}/{classId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null 
                    && request.auth.token.admin == true 
                    && request.resource.size < 50 * 1024 * 1024;
      allow delete: if request.auth != null && request.auth.token.admin == true;
    }
  }
}
```

---

## 🧪 Automated Unit Test Suite (Robolectric)
Execute the complete test suite:
```bash
gradle :app:testDebugUnitTest
```
Covering:
- Initial 12 modules and 48 classes integrity.
- Incomplete class vs completed status.
- Active learning time accumulation and paused playback exclusion.
- Authoritative completion rules enforcement (time + playback/reading + quiz).
- Invariant completion timestamps on repeated attempts.
- Multi-user progress isolation across profile switches.
- Certificate eligibility validation and rejection of unauthorized requests.
- Landscape certificate generation, non-guessable IDs, and file output.
- Certificate verification for Valid, Revoked, and Not Found states.
- Administrative revocation and reinstatement.
