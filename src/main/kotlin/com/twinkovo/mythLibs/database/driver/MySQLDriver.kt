package com.twinkovo.mythLibs.database.driver

import com.twinkovo.mythLibs.database.connection.ConnectionPool
import com.twinkovo.mythLibs.database.model.QueryResult
import com.twinkovo.mythLibs.utils.Logger
import java.sql.Connection
import java.sql.SQLException

/**
 * MySQL database driver implementation that provides high-level access to MySQL operations.
 * Implements the DatabaseDriver interface with MySQL-specific functionality and connection pooling.
 * 
 * MySQL数据库驱动实现，提供对MySQL操作的高级访问。
 * 使用MySQL特定功能和连接池实现DatabaseDriver接口。
 *
 * @property poolId Identifier for the connection pool to use
 *                  要使用的连接池标识符
 * @property database Name of the target MySQL database
 *                    目标MySQL数据库的名称
 */
class MySQLDriver(
    private val poolId: String,
    private val database: String
) : DatabaseDriver {
    private var currentConnection: Connection? = null
    private var inTransaction = false

    override val name: String = "MySQL"

    /**
     * Establishes a connection to the MySQL database using the connection pool.
     * Sets up initial connection properties including auto-commit behavior.
     * 
     * 使用连接池建立与MySQL数据库的连接。
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
            throw SQLException("Unable to connect to MySQL database: ${e.message}")
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
            Logger.warn("Error closing MySQL connection: ${e.message}")
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
     * Uses InnoDB engine and UTF-8 character set by default.
     * 
     * 在数据库中创建具有指定列的新表。
     * 默认使用InnoDB引擎和UTF-8字符集。
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
        val sql = "CREATE TABLE IF NOT EXISTS $tableName ($columnDefs) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
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
     * 
     * 检查数据库中是否存在表。
     *
     * @param tableName Name of the table to check
     *                  要检查的表名
     * @return true if the table exists, false otherwise
     *         如果表存在则返回true，否则返回false
     */
    override fun tableExists(tableName: String): Boolean {
        val result = query(
            "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?",
            database, tableName
        )
        return !result.isEmpty
    }

    /**
     * Retrieves a list of all tables in the database.
     * 
     * 获取数据库中所有表的列表。
     *
     * @return List of table names
     *         表名列表
     */
    override fun getTables(): List<String> {
        val result = query(
            "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?",
            database
        )
        return result.column("TABLE_NAME")
    }

    /**
     * Retrieves the schema (structure) of a table.
     * 
     * 获取表的架构（结构）。
     *
     * @param tableName Name of the table
     *                  表名
     * @return Map of column names to their SQL type definitions
     *         列名到其SQL类型定义的映射
     */
    override fun getTableSchema(tableName: String): Map<String, String> {
        val result = query(
            "SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?",
            database, tableName
        )
        return result.all().associate { row ->
            row["COLUMN_NAME"].toString() to row["COLUMN_TYPE"].toString()
        }
    }

    /**
     * Add a column to a table
     * 向表中添加列
     *
     * @param tableName Table name
     *                  表名
     * @param columnName Column name
     *                   列名
     * @param columnType Column type
     *                   列类型
     */
    override fun addColumn(tableName: String, columnName: String, columnType: String) {
        update("ALTER TABLE $tableName ADD COLUMN $columnName $columnType")
    }

    /**
     * Drop a column from a table
     * 从表中删除列
     *
     * @param tableName Table name
     *                  表名
     * @param columnName Column name
     *                   列名
     */
    override fun dropColumn(tableName: String, columnName: String) {
        update("ALTER TABLE $tableName DROP COLUMN $columnName")
    }

    /**
     * Create an index
     * 创建索引
     *
     * @param tableName Table name
     *                  表名
     * @param indexName Index name
     *                  索引名
     * @param columnNames Column names
     *                    列名列表
     * @param unique Whether the index is unique
     *               是否为唯一索引
     */
    override fun createIndex(
        tableName: String,
        indexName: String,
        columnNames: List<String>,
        unique: Boolean
    ) {
        val uniqueStr = if (unique) "UNIQUE" else ""
        val columns = columnNames.joinToString(", ")
        update("CREATE $uniqueStr INDEX $indexName ON $tableName ($columns)")
    }

    /**
     * Drop an index
     * 删除索引
     *
     * @param tableName Table name
     *                  表名
     * @param indexName Index name
     *                  索引名
     */
    override fun dropIndex(tableName: String, indexName: String) {
        update("DROP INDEX $indexName ON $tableName")
    }

    /**
     * Get database size in bytes
     * 获取数据库大小（字节）
     *
     * @return Database size in bytes
     *         数据库大小（字节）
     */
    override fun getDatabaseSize(): Long {
        val result = query(
            """
            SELECT SUM(data_length + index_length) as size
            FROM information_schema.TABLES
            WHERE table_schema = ?
            """.trimIndent(),
            database
        )
        return result.columnOne<Long>("size") ?: 0
    }

    /**
     * Get table size in bytes
     * 获取表大小（字节）
     *
     * @param tableName Table name
     *                  表名
     * @return Table size in bytes
     *         表大小（字节）
     */
    override fun getTableSize(tableName: String): Long {
        val result = query(
            """
            SELECT data_length + index_length as size
            FROM information_schema.TABLES
            WHERE table_schema = ? AND table_name = ?
            """.trimIndent(),
            database, tableName
        )
        return result.columnOne<Long>("size") ?: 0
    }

    /**
     * Get row count of a table
     * 获取表的行数
     *
     * @param tableName Table name
     *                  表名
     * @return Row count
     *         行数
     */
    override fun getRowCount(tableName: String): Long {
        val result = query("SELECT COUNT(*) as count FROM $tableName")
        return result.columnOne<Long>("count") ?: 0
    }

    /**
     * Optimize a table
     * 优化表
     *
     * @param tableName Table name
     *                  表名
     */
    override fun optimizeTable(tableName: String) {
        update("OPTIMIZE TABLE $tableName")
    }

    /**
     * Backup the database
     * 备份数据库
     *
     * @param backupPath Backup file path
     *                   备份文件路径
     */
    override fun backup(backupPath: String) {
        // Use mysqldump command for backup
        // 使用mysqldump命令进行备份
        val process = Runtime.getRuntime().exec(arrayOf(
            "mysqldump",
            "--host=${getConfig("host")}",
            "--port=${getConfig("port")}",
            "--user=${getConfig("user")}",
            "--password=${getConfig("password")}",
            "--result-file=$backupPath",
            "--databases",
            database
        ))

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw SQLException("Database backup failed with exit code: $exitCode")
        }
    }

    /**
     * Restore from backup
     * 从备份恢复
     *
     * @param backupPath Backup file path
     *                   备份文件路径
     */
    override fun restore(backupPath: String) {
        // Use mysql command to restore
        // 使用mysql命令恢复
        val process = Runtime.getRuntime().exec(arrayOf(
            "mysql",
            "--host=${getConfig("host")}",
            "--port=${getConfig("port")}",
            "--user=${getConfig("user")}",
            "--password=${getConfig("password")}",
            database
        ))

        process.outputStream.use { out ->
            java.io.File(backupPath).inputStream().use { it.copyTo(out) }
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw SQLException("Database restore failed with exit code: $exitCode")
        }
    }

    /**
     * Close the driver and cleanup resources
     * 关闭驱动并清理资源
     */
    override fun close() {
        if (inTransaction) {
            rollback()
        }
        disconnect()
    }

    /**
     * Ensure database connection is active
     * 确保数据库连接处于活动状态
     */
    private fun ensureConnected() {
        if (!isConnected()) {
            connect()
        }
    }

    /**
     * Get configuration value
     * 获取配置值
     *
     * @param key Configuration key
     *            配置键
     * @return Configuration value
     *         配置值
     */
    private fun getConfig(key: String): String {
        return currentConnection!!.metaData.let { meta ->
            when (key) {
                "host" -> meta.url.substringAfter("//").substringBefore(":")
                "port" -> meta.url.substringAfter(":").substringBefore("/")
                "user" -> meta.userName
                "password" -> "" // For security reasons, we don't return the password
                               // 出于安全考虑，不返回密码
                else -> throw IllegalArgumentException("Unknown configuration key: $key")
            }
        }
    }
} 