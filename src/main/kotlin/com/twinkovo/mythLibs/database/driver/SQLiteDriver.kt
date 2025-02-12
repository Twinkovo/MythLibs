package com.twinkovo.mythLibs.database.driver

import com.twinkovo.mythLibs.database.connection.ConnectionPool
import com.twinkovo.mythLibs.database.model.QueryResult
import com.twinkovo.mythLibs.utils.Logger
import java.io.File
import java.sql.Connection
import java.sql.SQLException

/**
 * SQLite database driver implementation that provides high-level access to SQLite operations.
 * Implements the DatabaseDriver interface with SQLite-specific functionality and connection pooling.
 * 
 * SQLite数据库驱动实现，提供对SQLite操作的高级访问。
 * 使用SQLite特定功能和连接池实现DatabaseDriver接口。
 *
 * @property poolId Identifier for the connection pool to use
 *                  要使用的连接池标识符
 * @property databaseFile File object representing the SQLite database file
 *                       表示SQLite数据库文件的File对象
 */
class SQLiteDriver(
    private val poolId: String,
    private val databaseFile: File
) : DatabaseDriver {
    private var currentConnection: Connection? = null
    private var inTransaction = false

    override val name: String = "SQLite"

    /**
     * Establishes a connection to the SQLite database using the connection pool.
     * Sets up initial connection properties including auto-commit behavior.
     * 
     * 使用连接池建立与SQLite数据库的连接。
     * 设置初始连接属性，包括自动提交行为。
     *
     * @throws SQLException if connection fails
     *                     如果连接失败则抛出异常
     */
    override fun connect() {
        try {
            currentConnection = ConnectionPool.getConnection(poolId)
            currentConnection?.autoCommit = true
        } catch (e: Exception) {
            throw SQLException("Unable to connect to SQLite database: ${e.message}")
        }
    }

    /**
     * Closes the current database connection.
     * Logs any errors that occur during disconnection.
     * 
     * 关闭当前数据库连接。
     * 记录断开连接期间发生的任何错误。
     */
    override fun disconnect() {
        try {
            currentConnection?.close()
            currentConnection = null
        } catch (e: Exception) {
            Logger.warn("Error closing SQLite connection: ${e.message}")
        }
    }

    /**
     * Verifies if the connection to the database is active.
     * Checks if the connection exists and is not closed.
     * 
     * 验证与数据库的连接是否活动。
     * 检查连接是否存在且未关闭。
     *
     * @return true if connected and operational, false otherwise
     *         如果已连接且可操作则返回true，否则返回false
     */
    override fun isConnected(): Boolean {
        return try {
            currentConnection?.isClosed == false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes a SQL query and returns the results.
     * Supports parameterized queries for safe execution.
     * 
     * 执行SQL查询并返回结果。
     * 支持参数化查询以确保安全执行。
     *
     * @param sql SQL query string to execute
     *            要执行的SQL查询字符串
     * @param params Query parameters to bind
     *               要绑定的查询参数
     * @return QueryResult containing the query results
     *         包含查询结果的QueryResult
     * @throws SQLException if query execution fails
     *                     如果查询执行失败则抛出异常
     */
    override fun query(sql: String, vararg params: Any?): QueryResult {
        ensureConnected()
        return try {
            currentConnection!!.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }
                val rs = stmt.executeQuery()
                QueryResult.fromResultSet(rs)
            }
        } catch (e: Exception) {
            throw SQLException("Failed to execute query: ${e.message}")
        }
    }

    /**
     * Executes a SQL update statement (INSERT, UPDATE, DELETE).
     * Supports parameterized statements for safe execution.
     * 
     * 执行SQL更新语句（INSERT、UPDATE、DELETE）。
     * 支持参数化语句以确保安全执行。
     *
     * @param sql SQL update statement to execute
     *            要执行的SQL更新语句
     * @param params Update parameters to bind
     *               要绑定的更新参数
     * @return Number of rows affected by the update
     *         更新影响的行数
     * @throws SQLException if update execution fails
     *                     如果更新执行失败则抛出异常
     */
    override fun update(sql: String, vararg params: Any?): Int {
        ensureConnected()
        return try {
            currentConnection!!.prepareStatement(sql).use { stmt ->
                params.forEachIndexed { index, param ->
                    stmt.setObject(index + 1, param)
                }
                stmt.executeUpdate()
            }
        } catch (e: Exception) {
            throw SQLException("Failed to execute update: ${e.message}")
        }
    }

    /**
     * Executes multiple SQL update statements in a batch.
     * Optimizes performance by executing multiple updates in a single database round-trip.
     * 
     * 批量执行多个SQL更新语句。
     * 通过在单次数据库往返中执行多个更新来优化性能。
     *
     * @param sql SQL update statement template
     *            SQL更新语句模板
     * @param paramsList List of parameter arrays for batch execution
     *                   用于批量执行的参数数组列表
     * @return Array of affected row counts for each statement
     *         每个语句影响的行数数组
     * @throws SQLException if batch execution fails
     *                     如果批量执行失败则抛出异常
     */
    override fun batchUpdate(sql: String, paramsList: List<Array<Any?>>): IntArray {
        ensureConnected()
        return try {
            currentConnection!!.prepareStatement(sql).use { stmt ->
                paramsList.forEach { params ->
                    params.forEachIndexed { index, param ->
                        stmt.setObject(index + 1, param)
                    }
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        } catch (e: Exception) {
            throw SQLException("Failed to execute batch update: ${e.message}")
        }
    }

    /**
     * Starts a new database transaction.
     * Disables auto-commit mode until the transaction is committed or rolled back.
     * 
     * 开始新的数据库事务。
     * 禁用自动提交模式，直到事务提交或回滚。
     */
    override fun beginTransaction() {
        ensureConnected()
        if (!inTransaction) {
            currentConnection!!.autoCommit = false
            inTransaction = true
        }
    }

    /**
     * Commits the current transaction.
     * Restores auto-commit mode after successful commit.
     * 
     * 提交当前事务。
     * 成功提交后恢复自动提交模式。
     */
    override fun commit() {
        if (inTransaction) {
            try {
                currentConnection!!.commit()
            } finally {
                currentConnection!!.autoCommit = true
                inTransaction = false
            }
        }
    }

    /**
     * Rolls back the current transaction.
     * Restores auto-commit mode after rollback.
     * 
     * 回滚当前事务。
     * 回滚后恢复自动提交模式。
     */
    override fun rollback() {
        if (inTransaction) {
            try {
                currentConnection!!.rollback()
            } finally {
                currentConnection!!.autoCommit = true
                inTransaction = false
            }
        }
    }

    /**
     * Creates a new table in the database with specified columns.
     * 
     * 在数据库中创建具有指定列的新表。
     *
     * @param tableName Name of the table to create
     *                  要创建的表名
     * @param columns Map of column names to their SQL type definitions
     *                列名到其SQL类型定义的映射
     */
    override fun createTable(tableName: String, columns: Map<String, String>) {
        val columnDefs = columns.entries.joinToString(", ") { (name, type) ->
            "$name $type"
        }
        val sql = "CREATE TABLE IF NOT EXISTS $tableName ($columnDefs)"
        update(sql)
    }

    /**
     * Drops (deletes) a table from the database.
     * 
     * 从数据库中删除表。
     *
     * @param tableName Name of the table to drop
     *                  要删除的表名
     */
    override fun dropTable(tableName: String) {
        update("DROP TABLE IF EXISTS $tableName")
    }

    /**
     * Checks if a table exists in the database.
     * Uses SQLite's sqlite_master table for verification.
     * 
     * 检查数据库中是否存在表。
     * 使用SQLite的sqlite_master表进行验证。
     *
     * @param tableName Name of the table to check
     *                  要检查的表名
     * @return true if the table exists, false otherwise
     *         如果表存在则返回true，否则返回false
     */
    override fun tableExists(tableName: String): Boolean {
        val result = query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            tableName
        )
        return !result.isEmpty
    }

    /**
     * Retrieves a list of all tables in the database.
     * Queries the sqlite_master table for all table names.
     * 
     * 获取数据库中所有表的列表。
     * 查询sqlite_master表以获取所有表名。
     *
     * @return List of table names
     *         表名列表
     */
    override fun getTables(): List<String> {
        val result = query("SELECT name FROM sqlite_master WHERE type='table'")
        return result.column("name")
    }

    /**
     * Retrieves the schema (structure) of a table.
     * Uses SQLite's PRAGMA statement to get table information.
     * 
     * 获取表的架构（结构）。
     * 使用SQLite的PRAGMA语句获取表信息。
     *
     * @param tableName Name of the table
     *                  表名
     * @return Map of column names to their SQL type definitions
     *         列名到其SQL类型定义的映射
     */
    override fun getTableSchema(tableName: String): Map<String, String> {
        val result = query("PRAGMA table_info($tableName)")
        return result.all().associate { row ->
            row["name"].toString() to row["type"].toString()
        }
    }

    /**
     * Creates a backup of the SQLite database.
     * Handles WAL and SHM files if they exist.
     * 
     * 创建SQLite数据库的备份。
     * 如果存在WAL和SHM文件，也会处理它们。
     *
     * @param backupPath Path where the backup should be stored
     *                   备份应存储的路径
     */
    override fun backup(backupPath: String) {
        val backupFile = File(backupPath)
        backupFile.parentFile?.mkdirs()
        
        // Ensure all changes are written to disk
        // 确保所有更改都已写入磁盘
        update("PRAGMA wal_checkpoint(FULL)")
        
        // Copy database file
        // 复制数据库文件
        databaseFile.copyTo(backupFile, overwrite = true)
        
        // Copy WAL file if exists
        // 如果存在WAL文件则复制
        val walFile = File(databaseFile.path + "-wal")
        if (walFile.exists()) {
            walFile.copyTo(File("$backupPath-wal"), overwrite = true)
        }
        
        // Copy SHM file if exists
        // 如果存在SHM文件则复制
        val shmFile = File(databaseFile.path + "-shm")
        if (shmFile.exists()) {
            shmFile.copyTo(File("$backupPath-shm"), overwrite = true)
        }
    }

    /**
     * Restores the database from a backup.
     * Verifies backup file existence before restoration.
     * 
     * 从备份恢复数据库。
     * 在恢复之前验证备份文件是否存在。
     *
     * @param backupPath Path to the backup file
     *                   备份文件的路径
     * @throws IllegalArgumentException if backup file doesn't exist
     *                                 如果备份文件不存在则抛出异常
     */
    override fun restore(backupPath: String) {
        val backupFile = File(backupPath)
        if (!backupFile.exists()) {
            throw IllegalArgumentException("Backup file does not exist: $backupPath")
        }

        disconnect()
        databaseFile.delete()
        backupFile.copyTo(databaseFile)
        connect()
    }

    override fun createIndex(
        tableName: String,
        indexName: String,
        columnNames: List<String>,
        unique: Boolean
    ) {
        val uniqueStr = if (unique) "UNIQUE" else ""
        val columns = columnNames.joinToString(", ")
        update("CREATE $uniqueStr INDEX IF NOT EXISTS $indexName ON $tableName ($columns)")
    }

    override fun dropIndex(tableName: String, indexName: String) {
        update("DROP INDEX IF EXISTS $indexName")
    }

    override fun getDatabaseSize(): Long {
        return databaseFile.length()
    }

    override fun getTableSize(tableName: String): Long {
        val pageSize = query("PRAGMA page_size").columnOne<Int>("page_size") ?: 4096
        val pageCount = query("PRAGMA page_count").columnOne<Int>("page_count") ?: 0
        return pageSize.toLong() * pageCount
    }

    override fun getRowCount(tableName: String): Long {
        val result = query("SELECT COUNT(*) as count FROM $tableName")
        return result.columnOne<Long>("count") ?: 0
    }

    override fun optimizeTable(tableName: String) {
        update("VACUUM")
    }

    override fun addColumn(tableName: String, columnName: String, columnType: String) {
        update("ALTER TABLE $tableName ADD COLUMN $columnName $columnType")
    }

    override fun dropColumn(tableName: String, columnName: String) {
        // SQLite不直接支持删除列，需要创建新表并复制数据
        val schema = getTableSchema(tableName)
        if (!schema.containsKey(columnName)) {
            return
        }

        val columns = schema.filter { it.key != columnName }
        val columnNames = columns.keys.joinToString(", ")
        val newTableName = "${tableName}_new"

        beginTransaction()
        try {
            // 创建新表
            createTable(newTableName, columns)
            // 复制数据
            update("INSERT INTO $newTableName SELECT $columnNames FROM $tableName")
            // 删除旧表
            dropTable(tableName)
            // 重命名新表
            update("ALTER TABLE $newTableName RENAME TO $tableName")
            commit()
        } catch (e: Exception) {
            rollback()
            throw e
        }
    }

    override fun close() {
        if (inTransaction) {
            rollback()
        }
        disconnect()
    }

    private fun ensureConnected() {
        if (!isConnected()) {
            connect()
        }
    }
} 