// ═════════════════════════════════════════════════════════════════════════════
// INTEGRATION NOTES  —  HoppeConnect
// Based on your actual AppwriteService.kt and NewReportActivity.java structure
// ═════════════════════════════════════════════════════════════════════════════


// ─────────────────────────────────────────────────────────────────────────────
// 1. AppwriteService.kt  — YOUR FILE IS ALREADY COMPLETE
//    All required constants are already declared:
//
//      const val DB_ID       = "69a559b30025d6fa1396"
//      const val COL_USERS   = "users"
//      const val COL_REPORTS = "approve_reports"
//      const val COL_ADMINS  = "admins"
//      const val COL_MSGS    = "messages"
//      const val COL_CHATS   = "chats"
//
//    NO CHANGES NEEDED to AppwriteService.kt
// ─────────────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────────────
// 2. AppwriteHelper.kt  — REPLACE with the full file already delivered
//    The three new methods added are:
//
//    a) findUserByField(db, collectionId, field, value)
//       4-param overload; used by LoginActivity to query the "admins" collection
//
//    b) findDocumentsByField(db, databaseId, collectionId, field, value)
//       Used by AdminApproveReportsFragment and AdminSpammedReportsFragment
//       to filter reports by status ("pending" / "approved" / "spammed")
//
//    c) listAllDocuments(db, databaseId, collectionId)
//       Used by AdminOverviewFragment to count total users and reports
//
//    Replace AppwriteHelper.kt with the delivered file.
// ─────────────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────────────
// 3. AndroidManifest.xml  — ADD the following
// ─────────────────────────────────────────────────────────────────────────────

// A) Inside <manifest>, before <application>:
//
//    <uses-permission android:name="android.permission.SEND_SMS" />
//    <uses-permission android:name="android.permission.RECEIVE_SMS" />

// B) Inside <application>, alongside your other <activity> entries:
//
//    <activity
//        android:name=".AdminDashboardActivity"
//        android:exported="false" />


// ─────────────────────────────────────────────────────────────────────────────
// 4. File placement — where each delivered file goes in your project
// ─────────────────────────────────────────────────────────────────────────────

// app/src/main/java/com/ritesh/hoppeconnect/
//   ├── LoginActivity.java                  replace existing
//   ├── RegisterActivity.java               replace existing
//   ├── ForgotPasswordActivity.java         replace existing
//   ├── SmsManagerHelper.java               NEW — add here
//   ├── AdminDashboardActivity.java         NEW — add here
//   └── admin/                              NEW package subfolder
//       ├── AdminOverviewFragment.java
//       ├── AdminApproveReportsFragment.java
//       ├── AdminSpammedReportsFragment.java
//       └── AdminProfileFragment.java

// AdminFragments.java was delivered as one combined reference file.
// Split it into 4 separate .java files inside the admin/ subfolder,
// one class per file, as listed above.

// app/src/main/res/
//   ├── layout/
//   │   ├── activity_admin_dashboard.xml    NEW
//   │   ├── fragment_admin_overview.xml     NEW
//   │   ├── fragment_admin_approve.xml      NEW
//   │   ├── fragment_admin_spammed.xml      NEW
//   │   └── fragment_admin_profile.xml      NEW
//   └── menu/
//       └── admin_bottom_nav_menu.xml       NEW

// admin_fragment_layouts.xml was delivered as one combined reference file.
// Split it into 4 separate XML files inside res/layout/ as listed above.


// ─────────────────────────────────────────────────────────────────────────────
// 5. Appwrite Console — collections needed
// ─────────────────────────────────────────────────────────────────────────────

// ── "admins" collection  (AppwriteService.COL_ADMINS = "admins") ─────────────
// Create under database ID "69a559b30025d6fa1396" with these attributes:
//
//   Field      Type     Required
//   ────────── ──────── ────────
//   username   String   yes
//   password   String   yes      SHA-256 hashed (salt$hex format)
//   name       String   no
//
// Insert one document manually for the super-admin:
//   {
//     "$id":      "admin_001",
//     "username": "Admin",
//     "password": "<hashed — see section 6>",
//     "name":     "Super Admin"
//   }
//
// Collection permissions: any:read is fine; writes restricted to server key only.

// ── "approve_reports" collection  (AppwriteService.COL_REPORTS = "approve_reports") ──
// This already exists (used by NewReportActivity). Ensure it has at minimum:
//
//   Field           Type      Required   Notes
//   ─────────────── ───────── ──────────────────────────────────────────────
//   title           String    yes
//   status          String    yes        "pending" | "approved" | "spammed"
//   userId          String    no         reporter's user ID
//   reportedBy      String    no         alias for userId if needed
//   photoUrls       String[]  no         array of storage view URLs
//   documentUrl     String    no
//   locationLat     String    no
//   locationLng     String    no
//
// The admin fragment tabs filter on the "status" field — see section 7.


// ─────────────────────────────────────────────────────────────────────────────
// 6. Generating the admin password hash (run once)
// ─────────────────────────────────────────────────────────────────────────────
// RegisterActivity.hashPassword() produces:  base64salt + "$" + sha256hex
// Use this standalone snippet to generate it without running the app:

/*
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

public class GenHash {
    public static void main(String[] args) throws Exception {
        String plain = "YourAdminPassword123!";   // change this
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        String salt = Base64.getEncoder().withoutPadding().encodeToString(saltBytes);
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        d.update(salt.getBytes());
        byte[] hb = d.digest(plain.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hb) hex.append(String.format(Locale.ROOT, "%02x", b));
        System.out.println(salt + "$" + hex);
    }
}
*/
// Paste the printed string into the "password" field of your admin document.


// ─────────────────────────────────────────────────────────────────────────────
// 7. NewReportActivity — status field alignment with admin tabs
// ─────────────────────────────────────────────────────────────────────────────
// NewReportActivity currently saves:   data.put("status", "active")
// The admin Approve tab filters for:   status == "pending"
//
// Option A (recommended): change the one line in NewReportActivity so every
// new report lands in the admin Approve queue automatically:
//
//   data.put("status", "pending");   // was "active"
//
// Option B: keep "active" and update AdminApproveReportsFragment to query
// for "active" instead of "pending":
//
//   AppwriteHelper.findDocumentsByField(
//       db, AppwriteService.DB_ID, AppwriteService.COL_REPORTS, "status", "active")
//
// Option A is cleaner — admin approves before the report goes public.


// ─────────────────────────────────────────────────────────────────────────────
// 8. Google Sign-In Error Code 10 fix
// ─────────────────────────────────────────────────────────────────────────────
// Error 10 = DEVELOPER_ERROR — SHA-1 fingerprint not registered.
//
// Step 1: Get your debug SHA-1
//   Android Studio terminal:  ./gradlew signingReport
//   Copy the SHA1 line under "Variant: debug"
//
// Step 2: Register it
//   Firebase Console → your project → Project Settings →
//   Android app (com.ritesh.hoppeconnect) → Add fingerprint → paste SHA-1
//
// Step 3: Re-download google-services.json
//   Firebase Console → Project Settings → Download google-services.json
//   Replace  app/google-services.json  with the new file
//
// Step 4: Clean and rebuild
//   Build → Clean Project  →  Build → Rebuild Project
//
// If not using Firebase (pure Google Cloud):
//   console.cloud.google.com → APIs & Services → Credentials →
//   Edit your Android OAuth 2.0 Client → add the SHA-1 there


// ─────────────────────────────────────────────────────────────────────────────
// 9. Bottom nav icons — add if missing
// ─────────────────────────────────────────────────────────────────────────────
// admin_bottom_nav_menu.xml references:
//   @drawable/ic_dashboard     @drawable/ic_check_circle
//   @drawable/ic_report        @drawable/ic_person
//
// Add them via:
//   Android Studio → res/drawable → right-click → New → Vector Asset →
//   Clip Art → search the name → Finish
// Name each file exactly as listed above.


// ─────────────────────────────────────────────────────────────────────────────
// 10. Redirect-loop root cause (summary)
// ─────────────────────────────────────────────────────────────────────────────
// The loop happened because:
//   RegisterActivity.finish() → back-stack popped to LoginActivity →
//   LoginActivity.onCreate() found KEY_UID in SharedPreferences →
//   immediately called routeLoggedInUser() before the screen was even visible.
//
// Fix: an "explicit_login" boolean extra is passed whenever an auth screen
// deliberately opens LoginActivity. Auto-redirect only fires when both:
//   (a) the "explicit_login" extra is absent, AND
//   (b) KEY_UID is already in SharedPreferences.
// A login screen opened intentionally by the user is therefore never
// auto-redirected away.