package com.ritesh.hoppeconnect

import android.content.Context
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.models.User
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.runBlocking

object AppwriteService {

    const val USERS_BUCKET_ID = "gvjgvjgvjgvjvg"
    const val ENDPOINT         = "https://cloud.appwrite.io/v1"
    const val PROJECT_ID       = "hoppeconnect"
    const val DB_ID            = "69a559b30025d6fa1396"
    const val COL_USERS        = "users"
    const val COL_REPORTS        = "approve_reports"
    const val COL_CHATS        = "chats"
    const val COL_ADMINS        = "admins"
    const val COL_MSGS         = "messages"
    const val CHAT_BUCKET_ID   = "chat_media"

    @JvmField val APPWRITE_ENDPOINT   = ENDPOINT
    @JvmField val APPWRITE_PROJECT_ID = PROJECT_ID

    private var client    : Client?    = null
    private var _account  : Account?   = null
    private var _databases: Databases? = null
    private var _storage  : Storage?   = null

    @JvmStatic
    fun init(context: Context) {
        if (client == null) {
            client = Client(context)
                .setEndpoint(ENDPOINT)
                .setProject(PROJECT_ID)
            _account   = Account(client!!)
            _databases = Databases(client!!)
            _storage   = Storage(client!!)
        }
    }

    @JvmStatic fun getAccount()  : Account   = _account   ?: error("Call init() first")
    @JvmStatic fun getDatabases(): Databases = _databases ?: error("Call init() first")
    @JvmStatic fun getStorage()  : Storage   = _storage   ?: error("Call init() first")

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
    fun createAccountSync(email: String, password: String, name: String) {
        runBlocking {
            val account = _account ?: throw IllegalStateException("Call init() first")
            account.create(ID.unique(), email, password, name)
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun createSessionSync(email: String, password: String) {
        runBlocking {
            val account = _account ?: throw IllegalStateException("Call init() first")
            account.createEmailPasswordSession(email, password)
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun createAccountAndSignIn(email: String, password: String, name: String) {
        runBlocking {
            val account = _account ?: throw IllegalStateException("Call init() first")
            account.create(ID.unique(), email, password, name)
            account.createEmailPasswordSession(email, password)
        }
    }
}