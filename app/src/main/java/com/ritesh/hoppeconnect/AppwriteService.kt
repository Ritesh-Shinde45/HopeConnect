package com.ritesh.hoppeconnect

import android.content.Context
import android.util.Log
import io.appwrite.Client
import io.appwrite.ID
import com.ritesh.hoppeconnect.BuildConfig
import io.appwrite.models.User
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.runBlocking

object AppwriteService {

    private const val TAG = "AppwriteService"

    const val ENDPOINT         = "https://cloud.appwrite.io/v1"
    const val PROJECT_ID       = BuildConfig.APPWRITE_PROJECT_ID
    const val DB_ID            = BuildConfig.APPWRITE_DB_ID

   
    const val COL_USERS         = "users"
    const val COL_REPORTS       = "reports"
    const val COL_NOTIFICATIONS = "notifications"
    const val COL_CHATS         = "chats"
    const val COL_SIGHTINGS     = "sightings"
    const val COL_HELPS         = "helps"
    const val COL_ADMINS        = "admins"
    const val COL_MSGS          = "messages"




    const val USERS_BUCKET_ID  = BuildConfig.APPWRITE_USERS_BUCKET_ID
    const val REPORT_BUCKET_ID = BuildConfig.APPWRITE_REPORT_BUCKET_ID
    const val CHAT_BUCKET_ID   = BuildConfig.APPWRITE_CHAT_BUCKET_ID


    const val ADMIN_EMAIL      = BuildConfig.ADMIN_EMAIL

   
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

    @JvmStatic
    fun resendVerificationEmail() {
        try {
            val account = _account ?: run {
                Log.e(TAG, "resendVerificationEmail — client not initialised")
                return
            }
            runBlocking { sendVerificationEmail(account) }
        } catch (e: Exception) {
            Log.e(TAG, "resendVerificationEmail error: ${e.message}", e)
        }
    }

    private suspend fun sendVerificationEmail(account: Account) {
        val redirectUrl = "https://cloud.appwrite.io"
        try {
            Log.d(TAG, "Calling account.createVerification(redirectUrl=$redirectUrl)")
            account.createVerification(redirectUrl)
            Log.d(TAG, "createVerification SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "createVerification FAILED [${e.javaClass.simpleName}]: ${e.message}", e)
        }
    }

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

    
    @JvmStatic
    fun buildReportPhotoUrl(fileId: String): String {
        return "$ENDPOINT/storage/buckets/$REPORT_BUCKET_ID/files/$fileId/download?project=$PROJECT_ID"
    }

    
    @JvmStatic
    fun buildUserPhotoUrl(fileId: String): String {
        return "$ENDPOINT/storage/buckets/$USERS_BUCKET_ID/files/$fileId/view?project=$PROJECT_ID"
    }
}