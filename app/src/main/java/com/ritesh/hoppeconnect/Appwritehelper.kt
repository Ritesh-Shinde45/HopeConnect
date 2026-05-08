package com.ritesh.hoppeconnect

import io.appwrite.Query
import io.appwrite.models.InputFile
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.runBlocking

object AppwriteHelper {

   
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

   
    @JvmStatic
    fun getDocument(
        db: Databases, databaseId: String, collectionId: String, documentId: String
    ) = runBlocking { db.getDocument(databaseId, collectionId, documentId) }

   
    @JvmStatic
    fun listDocuments(
        db: Databases, databaseId: String, collectionId: String, queries: List<String>
    ) = runBlocking { db.listDocuments(databaseId, collectionId, queries) }

   
    @JvmStatic
    fun updateDocument(
        db: Databases, databaseId: String, collectionId: String,
        documentId: String, data: Map<String, Any>
    ) = runBlocking { db.updateDocument(databaseId, collectionId, documentId, data) }

   
   
    @JvmStatic
    @Throws(Exception::class)
    fun deleteDocument(
        db: Databases, databaseId: String, collectionId: String, documentId: String
    ) = runBlocking { db.deleteDocument(databaseId, collectionId, documentId) }

    @JvmStatic
    @Throws(Exception::class)
    fun deleteFile(
        storage: Storage,
        bucketId: String,
        fileId: String
    ) = runBlocking {
        storage.deleteFile(bucketId, fileId)
    }
    
    @JvmStatic
    fun findUserByField(
        db: Databases, field: String, value: String
    ) = runBlocking {
        db.listDocuments(
            AppwriteService.DB_ID,
            AppwriteService.COL_USERS,
            listOf(Query.equal(field, listOf(value)))
        )
    }

   
    @JvmStatic
    fun findUserByField(
        db: Databases, collectionId: String, field: String, value: String
    ) = runBlocking {
        db.listDocuments(
            AppwriteService.DB_ID,
            collectionId,
            listOf(Query.equal(field, listOf(value)))
        )
    }

   
    @JvmStatic
    fun findDocumentsByField(
        db: Databases, databaseId: String, collectionId: String,
        field: String, value: String
    ) = runBlocking {
        db.listDocuments(
            databaseId,
            collectionId,
            listOf(Query.equal(field, listOf(value)))
        )
    }

   
    @JvmStatic
    fun listAllDocuments(
        db: Databases, databaseId: String, collectionId: String
    ) = runBlocking {
        db.listDocuments(databaseId, collectionId, emptyList())
    }

   
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


   
    @JvmStatic
    fun getUserChats(db: Databases, userId: String) = runBlocking {
        db.listDocuments(
            AppwriteService.DB_ID,
            AppwriteService.COL_CHATS,
            listOf(Query.search("participants", userId))
        )
    }

   
    @JvmStatic
    @Throws(Exception::class)
    fun uploadFileBlocking(
        storage: Storage, bucketId: String, fileId: String,
        bytes: ByteArray, fileName: String, mimeType: String
    ): io.appwrite.models.File = runBlocking {
        storage.createFile(
            bucketId, fileId,
            InputFile.fromBytes(bytes, fileName, mimeType)
        )
    }
}