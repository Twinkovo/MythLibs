package com.twinkovo.mythLibs.database.driver

import com.twinkovo.mythLibs.database.model.QueryResult
import java.io.Closeable

/**
 * Database driver interface that defines the standard operations for database interaction.
 * This interface provides a unified way to interact with different types of databases.
 * 
 * 数据库驱动接口，定义了数据库交互的标准操作。
 * 此接口提供了与不同类型数据库交互的统一方式。
 */
interface DatabaseDriver : Closeable {
    /**
     * The name of the database driver implementation.
     * 数据库驱动实现的名称。
     */
    val name: String

    /**
     * Establishes a connection to the database.
     * Should handle all necessary initialization and connection setup.
     * 
     * 建立与数据库的连接。
     * 应处理所有必要的初始化和连接设置。
     */
    fun connect()

    /**
     * Closes the current database connection.
     * Should properly clean up all resources.
     * 
     * 关闭当前数据库连接。
     * 应正确清理所有资源。
     */
    fun disconnect()

    /**
     * Checks if there is an active connection to the database.
     * 
     * 检查是否存在活动的数据库连接。
     *
     * @return true if connected, false otherwise
     *         如果已连接则返回true，否则返回false
     */
    fun isConnected(): Boolean

    /**
     * Executes a SQL query and returns the result.
     * 
     * 执行SQL查询并返回结果。
     *
     * @param sql The SQL query to execute
     *            要执行的SQL查询
     * @param params Query parameters to bind
     *               要绑定的查询参数
     * @return Query result containing the data
     *         包含数据的查询结果
     */
    fun query(sql: String, vararg params: Any?): QueryResult

    /**
     * Executes a SQL update statement (INSERT, UPDATE, DELETE).
     * 
     * 执行SQL更新语句（INSERT、UPDATE、DELETE）。
     *
     * @param sql The SQL update statement to execute
     *            要执行的SQL更新语句
     * @param params Update parameters to bind
     *               要绑定的更新参数
     * @return Number of affected rows
     *         受影响的行数
     */
    fun update(sql: String, vararg params: Any?): Int

    /**
     * Executes a batch of SQL update statements.
     * 
     * 执行一批SQL更新语句。
     *
     * @param sql The SQL update statement to execute
     *            要执行的SQL更新语句
     * @param paramsList List of parameter arrays for batch execution
     *                   用于批量执行的参数数组列表
     * @return Array of affected row counts for each statement
     *         每个语句受影响的行数数组
     */
    fun batchUpdate(sql: String, paramsList: List<Array<Any?>>): IntArray

    /**
     * Starts a new database transaction.
     * 
     * 开始新的数据库事务。
     */
    fun beginTransaction()

    /**
     * Commits the current transaction.
     * 
     * 提交当前事务。
     */
    fun commit()

    /**
     * Rolls back the current transaction.
     * 
     * 回滚当前事务。
     */
    fun rollback()

    /**
     * Creates a new table in the database.
     * 
     * 在数据库中创建新表。
     *
     * @param tableName Name of the table to create
     *                  要创建的表名
     * @param columns Map of column names to their SQL type definitions
     *                列名到其SQL类型定义的映射
     */
    fun createTable(tableName: String, columns: Map<String, String>)

    /**
     * Drops (deletes) a table from the database.
     * 
     * 从数据库中删除表。
     *
     * @param tableName Name of the table to drop
     *                  要删除的表名
     */
    fun dropTable(tableName: String)

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
    fun tableExists(tableName: String): Boolean

    /**
     * Gets a list of all tables in the database.
     * 
     * 获取数据库中所有表的列表。
     *
     * @return List of table names
     *         表名列表
     */
    fun getTables(): List<String>

    /**
     * Gets the schema (structure) of a table.
     * 
     * 获取表的架构（结构）。
     *
     * @param tableName Name of the table
     *                  表名
     * @return Map of column names to their SQL type definitions
     *         列名到其SQL类型定义的映射
     */
    fun getTableSchema(tableName: String): Map<String, String>

    /**
     * Adds a new column to an existing table.
     * 
     * 向现有表添加新列。
     *
     * @param tableName Name of the table
     *                  表名
     * @param columnName Name of the new column
     *                   新列的名称
     * @param columnType SQL type definition for the new column
     *                   新列的SQL类型定义
     */
    fun addColumn(tableName: String, columnName: String, columnType: String)

    /**
     * Removes a column from an existing table.
     * 
     * 从现有表中删除列。
     *
     * @param tableName Name of the table
     *                  表名
     * @param columnName Name of the column to drop
     *                   要删除的列名
     */
    fun dropColumn(tableName: String, columnName: String)

    /**
     * Creates an index on specified columns of a table.
     * 
     * 在表的指定列上创建索引。
     *
     * @param tableName Name of the table
     *                  表名
     * @param indexName Name of the index to create
     *                  要创建的索引名称
     * @param columnNames List of column names to index
     *                    要索引的列名列表
     * @param unique Whether the index should enforce uniqueness
     *               索引是否应强制唯一性
     */
    fun createIndex(tableName: String, indexName: String, columnNames: List<String>, unique: Boolean = false)

    /**
     * Drops an index from a table.
     * 
     * 从表中删除索引。
     *
     * @param tableName Name of the table
     *                  表名
     * @param indexName Name of the index to drop
     *                  要删除的索引名称
     */
    fun dropIndex(tableName: String, indexName: String)

    /**
     * Gets the total size of the database in bytes.
     * 
     * 获取数据库的总大小（字节）。
     *
     * @return Size of the database in bytes
     *         数据库大小（字节）
     */
    fun getDatabaseSize(): Long

    /**
     * Gets the size of a specific table in bytes.
     * 
     * 获取特定表的大小（字节）。
     *
     * @param tableName Name of the table
     *                  表名
     * @return Size of the table in bytes
     *         表的大小（字节）
     */
    fun getTableSize(tableName: String): Long

    /**
     * Gets the number of rows in a table.
     * 
     * 获取表中的行数。
     *
     * @param tableName Name of the table
     *                  表名
     * @return Number of rows in the table
     *         表中的行数
     */
    fun getRowCount(tableName: String): Long

    /**
     * Optimizes a table's storage and index structure.
     * 
     * 优化表的存储和索引结构。
     *
     * @param tableName Name of the table to optimize
     *                  要优化的表名
     */
    fun optimizeTable(tableName: String)

    /**
     * Creates a backup of the database.
     * 
     * 创建数据库的备份。
     *
     * @param backupPath Path where the backup should be stored
     *                   备份应存储的路径
     */
    fun backup(backupPath: String)

    /**
     * Restores the database from a backup.
     * 
     * 从备份恢复数据库。
     *
     * @param backupPath Path to the backup file
     *                   备份文件的路径
     */
    fun restore(backupPath: String)
} 