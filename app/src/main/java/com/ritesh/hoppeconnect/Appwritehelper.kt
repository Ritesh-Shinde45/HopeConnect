package com.ritesh.hoppeconnect

import io.appwrite.Query
import io.appwrite.models.InputFile
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.runBlocking

object AppwriteHelper {

    // ── CREATE ────────────────────────────────────────────────────────────────
    @JvmStatic
    fun createDocument(
        db: Databases, databaseId: String, collectionId: String,
        documentId: String, data: Map<String, Any>
    ) = runBlocking { db.createDocument(databaseId, collectionId, documentId, data) }
    @JvmStatic
    @Throws(Exception::class)
    fun deleteCurrentSession(account: io.appwrite.services.Account) = runBlocking {
        account.deleteSession("current")
    }
    // ── GET ───────────────────────────────────────────────────────────────────
    @JvmStatic
    fun getDocument(
        db: Databases, databaseId: String, collectionId: String, documentId: String
    ) = runBlocking { db.getDocument(databaseId, collectionId, documentId) }

    // ── LIST ──────────────────────────────────────────────────────────────────
    @JvmStatic
    fun listDocuments(
        db: Databases, databaseId: String, collectionId: String, queries: List<String>
    ) = runBlocking { db.listDocuments(databaseId, collectionId, queries) }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    @JvmStatic
    fun updateDocument(
        db: Databases, databaseId: String, collectionId: String,
        documentId: String, data: Map<String, Any>
    ) = runBlocking { db.updateDocument(databaseId, collectionId, documentId, data) }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @JvmStatic
    fun deleteDocument(
        db: Databases, databaseId: String, collectionId: String, documentId: String
    ) = runBlocking { db.deleteDocument(databaseId, collectionId, documentId) }

    // ── Find user by field (users collection — existing) ─────────────────────
    @JvmStatic
    fun findUserByField(db: Databases, field: String, value: String) = runBlocking {
        db.listDocuments(
            AppwriteService.DB_ID,
            AppwriteService.COL_USERS,
            listOf(Query.equal(field, listOf(value)))
        )
    }

    // ── Find document by field in ANY collection (admin login, reports, etc.) ─
    // Used by: LoginActivity (admin lookup), AdminApproveReportsFragment,
    //          AdminSpammedReportsFragment
    @JvmStatic
    fun findUserByField(
        db: Databases,
        collectionId: String,
        field: String,
        value: String
    ) = runBlocking {
        db.listDocuments(
            AppwriteService.DB_ID,
            collectionId,
            listOf(Query.equal(field, listOf(value)))
        )
    }

    // ── Find documents by field in any db/collection (generic) ───────────────
    // Used by: AdminOverviewFragment, AdminApproveReportsFragment,
    //          AdminSpammedReportsFragment
    @JvmStatic
    fun findDocumentsByField(
        db: Databases,
        databaseId: String,
        collectionId: String,
        field: String,
        value: String
    ) = runBlocking {
        db.listDocuments(
            databaseId,
            collectionId,
            listOf(Query.equal(field, listOf(value)))
        )
    }

    // ── List ALL documents in a collection (no filter) ────────────────────────
    // Used by: AdminOverviewFragment (total user count, etc.)
    @JvmStatic
    fun listAllDocuments(
        db: Databases,
        databaseId: String,
        collectionId: String
    ) = runBlocking {
        db.listDocuments(
            databaseId,
            collectionId,
            emptyList()
        )
    }

    // ── Chat: get messages for a chatId ───────────────────────────────────────
    @JvmStatic
    fun getChatMessages(db: Databases, chatId: String) = runBlocking {
        db.listDocuments(
            AppwriteService.DB_ID,
            AppwriteService.COL_MSGS,
            listOf(
                Query.equal("chatId", listOf(chatId)),
                Query.orderAsc("\$createdAt"),
                Query.limit(100)
            )
        )
    }

    // ── Chat: get all chats for a userId ──────────────────────────────────────
    @JvmStatic
    fun getUserChats(db: Databases, userId: String) = runBlocking {
        db.listDocuments(
            AppwriteService.DB_ID,
            AppwriteService.COL_CHATS,
            listOf(Query.search("participants", userId))
        )
    }

    /**
     * Upload file bytes — declared to throw Exception so Java callers
     * MUST wrap in try/catch. This is the key fix: @Throws(Exception::class)
     * makes the Kotlin AppwriteException visible to Java as a checked exception.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun uploadFileBlocking(
        storage: Storage,
        bucketId: String,
        fileId: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): io.appwrite.models.File = runBlocking {
        storage.createFile(
            bucketId,
            fileId,
            InputFile.fromBytes(bytes, fileName, mimeType)
        )
    }
}