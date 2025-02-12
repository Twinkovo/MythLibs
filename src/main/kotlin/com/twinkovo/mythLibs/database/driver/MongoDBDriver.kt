package com.twinkovo.mythLibs.database.driver

import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.IndexOptions
import com.twinkovo.mythLibs.database.model.QueryResult
import com.twinkovo.mythLibs.utils.Logger
import org.bson.Document

/**
 * MongoDB database driver implementation that provides high-level access to MongoDB operations.
 * Implements the DatabaseDriver interface with MongoDB-specific functionality.
 * 
 * MongoDB数据库驱动实现，提供对MongoDB操作的高级访问。
 * 使用MongoDB特定功能实现DatabaseDriver接口。
 *
 * @property uri MongoDB connection URI containing all necessary connection parameters
 *               包含所有必要连接参数的MongoDB连接URI
 * @property databaseName Name of the target MongoDB database
 *                       目标MongoDB数据库的名称
 */
class MongoDBDriver(
    private val uri: String,
    private val databaseName: String
) : DatabaseDriver {
    private var client: MongoClient? = null
    private var database: MongoDatabase? = null

    override val name: String = "MongoDB"

    /**
     * Establishes a connection to the MongoDB server using the provided URI.
     * Initializes the client and database instances.
     * 
     * 使用提供的URI建立与MongoDB服务器的连接。
     * 初始化客户端和数据库实例。
     *
     * @throws RuntimeException if connection fails
     *                         如果连接失败则抛出异常
     */
    override fun connect() {
        try {
            val settings = MongoClientSettings.builder()
                .applyConnectionString(com.mongodb.ConnectionString(uri))
                .build()
            
            client = MongoClients.create(settings)
            database = client?.getDatabase(databaseName)
            Logger.info("Successfully connected to MongoDB server")
        } catch (e: Exception) {
            throw RuntimeException("Failed to connect to MongoDB server: ${e.message}")
        }
    }

    /**
     * Closes the connection to the MongoDB server.
     * Releases all resources associated with the connection.
     * 
     * 关闭与MongoDB服务器的连接。
     * 释放与连接相关的所有资源。
     */
    override fun disconnect() {
        try {
            client?.close()
            client = null
            database = null
            Logger.info("Successfully disconnected from MongoDB server")
        } catch (e: Exception) {
            Logger.warn("Error closing MongoDB connection: ${e.message}")
        }
    }

    /**
     * Verifies the connection to the MongoDB server is active.
     * Performs a ping command to test the connection.
     * 
     * 验证与MongoDB服务器的连接是否活动。
     * 执行ping命令来测试连接。
     *
     * @return true if connected and operational, false otherwise
     *         如果已连接且可操作则返回true，否则返回false
     */
    override fun isConnected(): Boolean {
        return try {
            database?.runCommand(Document("ping", 1))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retrieves a MongoDB collection by its name.
     * Ensures the database connection is active before accessing the collection.
     * 
     * 通过名称获取MongoDB集合。
     * 在访问集合之前确保数据库连接处于活动状态。
     *
     * @param collectionName Name of the collection to retrieve
     *                      要获取的集合名称
     * @return MongoCollection instance for the specified collection
     *         指定集合的MongoCollection实例
     * @throws IllegalStateException if database is not connected
     *                              如果数据库未连接则抛出异常
     */
    fun getCollection(collectionName: String): MongoCollection<Document> {
        return database?.getCollection(collectionName)
            ?: throw IllegalStateException("Database not connected")
    }

    /**
     * Inserts a single document into a collection.
     * 
     * 向集合中插入单个文档。
     *
     * @param collectionName Target collection name
     *                      目标集合名称
     * @param document Document to insert
     *                要插入的文档
     */
    fun insertOne(collectionName: String, document: Document) {
        getCollection(collectionName).insertOne(document)
    }

    /**
     * Inserts multiple documents into a collection in a single operation.
     * 
     * 在单个操作中向集合插入多个文档。
     *
     * @param collectionName Target collection name
     *                      目标集合名称
     * @param documents List of documents to insert
     *                 要插入的文档列表
     */
    fun insertMany(collectionName: String, documents: List<Document>) {
        getCollection(collectionName).insertMany(documents)
    }

    /**
     * Finds documents in a collection that match the specified filter criteria.
     * 
     * 查找与指定过滤条件匹配的集合中的文档。
     *
     * @param collectionName Target collection name
     *                      目标集合名称
     * @param filter Document containing filter criteria
     *              包含过滤条件的文档
     * @return List of matching documents
     *         匹配的文档列表
     */
    fun find(collectionName: String, filter: Document): List<Document> {
        return getCollection(collectionName).find(filter).toList()
    }

    /**
     * Updates multiple documents in a collection that match the filter criteria.
     * 
     * 更新集合中与过滤条件匹配的多个文档。
     *
     * @param collectionName Target collection name
     *                      目标集合名称
     * @param filter Document containing filter criteria
     *              包含过滤条件的文档
     * @param update Document containing update operations
     *              包含更新操作的文档
     * @return Number of documents modified
     *         修改的文档数量
     */
    fun updateMany(collectionName: String, filter: Document, update: Document): Long {
        return getCollection(collectionName).updateMany(filter, update).modifiedCount
    }

    /**
     * Deletes multiple documents from a collection that match the filter criteria.
     * 
     * 从集合中删除与过滤条件匹配的多个文档。
     *
     * @param collectionName Target collection name
     *                      目标集合名称
     * @param filter Document containing filter criteria
     *              包含过滤条件的文档
     * @return Number of documents deleted
     *         删除的文档数量
     */
    fun deleteMany(collectionName: String, filter: Document): Long {
        return getCollection(collectionName).deleteMany(filter).deletedCount
    }

    /**
     * Creates a new collection with the specified options.
     * 
     * 使用指定的选项创建新集合。
     *
     * @param collectionName Name of the collection to create
     *                      要创建的集合名称
     * @param options Collection creation options
     *                集合创建选项
     */
    fun createCollection(collectionName: String, options: CreateCollectionOptions = CreateCollectionOptions()) {
        database?.createCollection(collectionName, options)
    }

    /**
     * Drops (deletes) an existing collection.
     * 
     * 删除现有集合。
     *
     * @param collectionName Name of the collection to drop
     *                      要删除的集合名称
     */
    fun dropCollection(collectionName: String) {
        getCollection(collectionName).drop()
    }

    /**
     * Creates an index on the specified collection with the given options.
     * 
     * 使用给定的选项在指定的集合上创建索引。
     *
     * @param collectionName Target collection name
     *                      目标集合名称
     * @param keys Document specifying the index keys
     *             指定索引键的文档
     * @param options Index creation options
     *                索引创建选项
     */
    fun createIndex(
        collectionName: String,
        keys: Document,
        options: IndexOptions = IndexOptions()
    ) {
        getCollection(collectionName).createIndex(keys, options)
    }

    /**
     * Drops an index from a collection.
     * 
     * 从集合中删除索引。
     *
     * @param tableName Name of the collection containing the index
     *                  包含索引的集合名称
     * @param indexName Name of the index to drop
     *                  要删除的索引名称
     */
    override fun dropIndex(tableName: String, indexName: String) {
        dropCollectionIndex(tableName, indexName)
    }

    /**
     * Internal method to drop a collection index.
     * 
     * 删除集合索引的内部方法。
     *
     * @param collectionName Collection name
     *                      集合名称
     * @param indexName Index name
     *                  索引名称
     */
    private fun dropCollectionIndex(collectionName: String, indexName: String) {
        getCollection(collectionName).dropIndex(indexName)
    }

    /**
     * Creates a backup of the MongoDB database.
     * Uses mongodump utility to create the backup.
     * 
     * 创建MongoDB数据库的备份。
     * 使用mongodump实用程序创建备份。
     *
     * @param backupPath Path where the backup should be stored
     *                   备份应存储的路径
     */
    override fun backup(backupPath: String) {
        val process = Runtime.getRuntime().exec(arrayOf(
            "mongodump",
            "--uri=$uri",
            "--db=$databaseName",
            "--out=$backupPath"
        ))
        process.waitFor()
        if (process.exitValue() != 0) {
            throw RuntimeException("Backup failed with exit code: ${process.exitValue()}")
        }
    }

    /**
     * Restore database from backup
     * 从备份恢复数据库
     *
     * @param backupPath Backup directory path
     *                   备份目录路径
     */
    override fun restore(backupPath: String) {
        val process = Runtime.getRuntime().exec(arrayOf(
            "mongorestore",
            "--uri=$uri",
            "--db=$databaseName",
            "--drop",
            "$backupPath/$databaseName"
        ))

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("Database restore failed with exit code: $exitCode")
        }
    }

    /**
     * Get database size in bytes
     * 获取数据库大小（字节）
     *
     * @return Database size in bytes
     *         数据库大小（字节）
     */
    override fun getDatabaseSize(): Long {
        val stats = database?.runCommand(Document("dbStats", 1))
        return stats?.getLong("dataSize") ?: 0
    }

    /**
     * Close MongoDB driver
     * 关闭MongoDB驱动
     */
    override fun close() {
        disconnect()
    }

    // 以下是为了实现DatabaseDriver接口而添加的方法，但在MongoDB中并不适用
    // The following methods are added to implement the DatabaseDriver interface,
    // but they are not applicable in MongoDB

    override fun query(sql: String, vararg params: Any?): QueryResult {
        throw UnsupportedOperationException("SQL queries are not supported in MongoDB")
    }

    override fun update(sql: String, vararg params: Any?): Int {
        throw UnsupportedOperationException("SQL updates are not supported in MongoDB")
    }

    override fun batchUpdate(sql: String, paramsList: List<Array<Any?>>): IntArray {
        throw UnsupportedOperationException("SQL batch updates are not supported in MongoDB")
    }

    override fun beginTransaction() {
        client?.startSession()
    }

    override fun commit() {
        // MongoDB transactions are handled by the session
    }

    override fun rollback() {
        // MongoDB transactions are handled by the session
    }

    override fun createTable(tableName: String, columns: Map<String, String>) {
        createCollection(tableName)
    }

    override fun dropTable(tableName: String) {
        dropCollection(tableName)
    }

    override fun tableExists(tableName: String): Boolean {
        return database?.listCollectionNames()?.contains(tableName) ?: false
    }

    override fun getTables(): List<String> {
        return database?.listCollectionNames()?.toList() ?: emptyList()
    }

    override fun getTableSchema(tableName: String): Map<String, String> {
        throw UnsupportedOperationException("Schema operations are not supported in MongoDB")
    }

    override fun addColumn(tableName: String, columnName: String, columnType: String) {
        throw UnsupportedOperationException("Column operations are not supported in MongoDB")
    }

    override fun dropColumn(tableName: String, columnName: String) {
        throw UnsupportedOperationException("Column operations are not supported in MongoDB")
    }

    override fun createIndex(tableName: String, indexName: String, columnNames: List<String>, unique: Boolean) {
        val keys = Document()
        columnNames.forEach { keys[it] = 1 }
        createIndex(tableName, keys, IndexOptions().unique(unique))
    }

    override fun getTableSize(tableName: String): Long {
        val stats = database?.runCommand(Document("collStats", tableName))
        return stats?.getLong("size") ?: 0
    }

    override fun getRowCount(tableName: String): Long {
        return getCollection(tableName).countDocuments()
    }

    override fun optimizeTable(tableName: String) {
        // MongoDB handles optimization automatically
    }
} 