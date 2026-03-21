package com.ritesh.hoppeconnect

import android.content.Context
import android.util.Log
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.models.User
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.runBlocking

object AppwriteService {

    private const val TAG = "AppwriteService"

    const val USERS_BUCKET_ID  = "gvjgvjgvjgvjvg"
    const val ENDPOINT         = "https://cloud.appwrite.io/v1"
    const val PROJECT_ID       = "hoppeconnect"
    const val DB_ID            = "69a559b30025d6fa1396"
    const val COL_USERS        = "users"
    const val COL_REPORTS      = "reports"
    const val COL_CHATS        = "chats"
    const val COL_ADMINS       = "admins"
    const val COL_MSGS         = "messages"
    const val CHAT_BUCKET_ID   = "chat_media"

    // Admin email — always treated as admin, skips email verification
    const val ADMIN_EMAIL = "riteshshinde472@gmail.com"

    @JvmField val APPWRITE_ENDPOINT   = ENDPOINT
    @JvmField val APPWRITE_PROJECT_ID = PROJECT_ID

    private var client    : Client?    = null
    private var _account  : Account?   = null
    private var _databases: Databases? = null
    private var _storage  : Storage?   = null

    @JvmStatic
    fun init(context: Context) {
        if (client == null) {
            Log.d(TAG, "Initializing Appwrite — endpoint=$ENDPOINT  project=$PROJECT_ID")
            client = Client(context)
                .setEndpoint(ENDPOINT)
                .setProject(PROJECT_ID)
            _account   = Account(client!!)
            _databases = Databases(client!!)
            _storage   = Storage(client!!)
            Log.d(TAG, "Appwrite client ready")
        }
    }

    @JvmStatic fun getAccount()  : Account   = _account   ?: error("Call init() first")
    @JvmStatic fun getDatabases(): Databases = _databases ?: error("Call init() first")
    @JvmStatic fun getStorage()  : Storage   = _storage   ?: error("Call init() first")

    @JvmStatic
    fun isAdminEmail(email: String): Boolean =
        email.trim().lowercase() == ADMIN_EMAIL.lowercase()

    @JvmStatic
    fun isLoggedIn(): Boolean {
        return try {
            runBlocking { _account?.get() ?: throw IllegalStateException("Call init() first") }
            true
        } catch (e: Exception) { false }
    }

    @JvmStatic
    fun getCurrentUserOrNull(): User<Map<String, Any>>? {
        return try { runBlocking { _account?.get() } } catch (e: Exception) { null }
    }

    /**
     * Creates Appwrite auth account + session, then sends verification email.
     *
     * One-time Appwrite Console setup:
     *   1. Console → your project → Auth → Settings
     *   2. Enable "Email / Password" login
     *   3. Enable "Email Verification" toggle
     *   No custom SMTP needed — Appwrite Cloud has a built-in mailer.
     *
     * Admin email always skips verification.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun createAccountAndSignIn(email: String, password: String, name: String) {
        runBlocking {
            val account = _account ?: throw IllegalStateException("Call init() first")

            Log.d(TAG, "Creating auth account for email=$email")
            account.create(ID.unique(), email, password, name)
            Log.d(TAG, "Auth account created — opening session")

            account.createEmailPasswordSession(email, password)
            Log.d(TAG, "Session opened")

            if (!isAdminEmail(email)) {
                sendVerificationEmail(account)
            } else {
                Log.d(TAG, "Admin email — skipping verification")
            }
        }
    }

    /** Creates only the auth account record (no session, no verification email). */
    @JvmStatic
    @Throws(Exception::class)
    fun createAccountSync(email: String, password: String, name: String) {
        runBlocking {
            val account = _account ?: throw IllegalStateException("Call init() first")
            Log.d(TAG, "createAccountSync email=$email")
            account.create(ID.unique(), email, password, name)
            Log.d(TAG, "createAccountSync — done")
        }
    }

    /** Opens a session for an existing account. */
    @JvmStatic
    @Throws(Exception::class)
    fun createSessionSync(email: String, password: String) {
        runBlocking {
            val account = _account ?: throw IllegalStateException("Call init() first")
            Log.d(TAG, "createSessionSync email=$email")
            account.createEmailPasswordSession(email, password)
            Log.d(TAG, "createSessionSync — session created")
        }
    }

    /**
     * Re-sends a verification email to the currently signed-in user.
     * Requires an active session. Safe to call from any thread.
     */
    @JvmStatic
    fun resendVerificationEmail() {
        try {
            val account = _account ?: run {
                Log.e(TAG, "resendVerificationEmail — client not initialised, call init() first")
                return
            }
            runBlocking { sendVerificationEmail(account) }
        } catch (e: Exception) {
            Log.e(TAG, "resendVerificationEmail — unexpected error: ${e.message}", e)
        }
    }

    /**
     * Internal helper — dispatches createVerification and logs every outcome in detail.
     * Must be called inside a runBlocking / coroutine scope.
     *
     * Redirect URL: "https://cloud.appwrite.io" is intentional for the Free plan.
     * Appwrite sends the email through its own default mailer; no SMTP config needed.
     * The user just clicks the link in their inbox to verify — no deep-link required.
     */
    private suspend fun sendVerificationEmail(account: Account) {
        val redirectUrl = "https://cloud.appwrite.io"
        try {
            Log.d(TAG, "Calling account.createVerification(redirectUrl=$redirectUrl)")
            account.createVerification(redirectUrl)
            Log.d(TAG, "createVerification SUCCESS — email dispatched by Appwrite mailer")
        } catch (e: Exception) {
            // Full stack trace printed so Logcat shows exactly why it failed.
            // Common reasons: Auth → Email Verification not enabled in Console,
            // free-plan daily email quota exhausted, or no active session.
            Log.e(TAG, "createVerification FAILED [${e.javaClass.simpleName}]: ${e.message}", e)
        }
    }

    /**
     * Returns true if the currently signed-in user has verified their email.
     * Admin email always returns true regardless.
     */
    @JvmStatic
    fun isEmailVerified(): Boolean {
        return try {
            val user = runBlocking { _account?.get() } ?: return false
            Log.d(TAG, "isEmailVerified — email=${user.email}  verified=${user.emailVerification}")
            if (isAdminEmail(user.email)) {
                Log.d(TAG, "Admin email — treating as verified")
                return true
            }
            user.emailVerification
        } catch (e: Exception) {
            Log.e(TAG, "isEmailVerified error: ${e.message}", e)
            false
        }
    }
}