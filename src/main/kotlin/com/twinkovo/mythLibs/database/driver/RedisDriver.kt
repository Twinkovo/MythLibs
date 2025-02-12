package com.twinkovo.mythLibs.database.driver

import com.twinkovo.mythLibs.database.model.QueryResult
import com.twinkovo.mythLibs.utils.Logger
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.Pipeline
import redis.clients.jedis.Protocol
import redis.clients.jedis.params.ScanParams
import java.io.File
import java.time.Duration

/**
 * Redis database driver implementation that provides high-level access to Redis operations.
 * Implements the DatabaseDriver interface with Redis-specific functionality and connection pooling.
 * 
 * Redis数据库驱动实现，提供对Redis操作的高级访问。
 * 使用Redis特定功能和连接池实现DatabaseDriver接口。
 *
 * @property host Redis server host address
 *                Redis服务器主机地址
 * @property port Redis server port number
 *                Redis服务器端口号
 * @property password Redis server authentication password (optional)
 *                    Redis服务器认证密码（可选）
 * @property database Redis database index number
 *                    Redis数据库索引号
 * @property poolConfig Redis connection pool configuration
 *                      Redis连接池配置
 */
class RedisDriver(
    private val host: String,
    private val port: Int,
    private val password: String?,
    private val database: Int,
    private val poolConfig: JedisPoolConfig
) : DatabaseDriver {
    private var pool: JedisPool? = null
    override val name: String = "Redis"

    /**
     * Establishes a connection to the Redis server using the connection pool.
     * Initializes the connection with optional authentication and database selection.
     * 
     * 使用连接池建立与Redis服务器的连接。
     * 使用可选的认证和数据库选择初始化连接。
     *
     * @throws RuntimeException if connection fails
     *                         如果连接失败则抛出异常
     */
    override fun connect() {
        try {
            pool = if (password.isNullOrEmpty()) {
                JedisPool(poolConfig, host, port, 2000, null, database)
            } else {
                JedisPool(poolConfig, host, port, 2000, password, database)
            }
            Logger.info("Successfully connected to Redis server")
        } catch (e: Exception) {
            throw RuntimeException("Failed to connect to Redis server: ${e.message}")
        }
    }

    /**
     * Closes the connection to the Redis server and releases pool resources.
     * Ensures proper cleanup of all connections in the pool.
     * 
     * 关闭与Redis服务器的连接并释放池资源。
     * 确保正确清理连接池中的所有连接。
     */
    override fun disconnect() {
        try {
            pool?.close()
            pool = null
            Logger.info("Successfully disconnected from Redis server")
        } catch (e: Exception) {
            Logger.warn("Error closing Redis connection: ${e.message}")
        }
    }

    /**
     * Verifies the connection to the Redis server is active.
     * Performs a PING command to test the connection status.
     * 
     * 验证与Redis服务器的连接是否活动。
     * 执行PING命令来测试连接状态。
     *
     * @return true if connected and operational, false otherwise
     *         如果已连接且可操作则返回true，否则返回false
     */
    override fun isConnected(): Boolean {
        return try {
            pool?.resource?.use { it.ping() == "PONG" } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes a Redis command with the specified arguments.
     * Provides low-level access to Redis commands.
     * 
     * 执行带有指定参数的Redis命令。
     * 提供对Redis命令的低级访问。
     *
     * @param command Redis command to execute
     *                要执行的Redis命令
     * @param args Command arguments
     *             命令参数
     * @return Command execution result
     *         命令执行结果
     */
    fun execute(command: String, vararg args: String): Any? {
        return pool?.resource?.use { jedis ->
            jedis.sendCommand(Protocol.Command.valueOf(command.uppercase()), *args)
        }
    }

    /**
     * Queries Redis keys matching the specified pattern.
     * Uses the KEYS command to find matching keys.
     * 
     * 查询匹配指定模式的Redis键。
     * 使用KEYS命令查找匹配的键。
     *
     * @param pattern Key pattern to match
     *                要匹配的键模式
     * @return List of matching keys
     *         匹配的键列表
     */
    fun query(pattern: String): List<String> {
        return pool?.resource?.use { jedis ->
            jedis.keys(pattern).toList()
        } ?: emptyList()
    }

    /**
     * Executes multiple Redis commands in a pipeline.
     * Optimizes performance by sending multiple commands in a single request.
     * 
     * 在管道中执行多个Redis命令。
     * 通过在单个请求中发送多个命令来优化性能。
     *
     * @param block Pipeline operation block
     *              管道操作代码块
     * @return List of command results
     *         命令结果列表
     */
    fun pipeline(block: Pipeline.() -> Unit): List<Any?> {
        return pool?.resource?.use { jedis ->
            val pipeline = jedis.pipelined()
            pipeline.block()
            pipeline.syncAndReturnAll()
        } ?: emptyList()
    }

    /**
     * Scans Redis keys matching the specified pattern incrementally.
     * More memory-efficient than the KEYS command for large datasets.
     * 
     * 增量扫描匹配指定模式的Redis键。
     * 对于大型数据集，比KEYS命令更节省内存。
     *
     * @param pattern Key pattern to match
     *                要匹配的键模式
     * @param count Number of keys to return per iteration
     *              每次迭代返回的键数量
     * @return List of matching keys
     *         匹配的键列表
     */
    fun scan(pattern: String, count: Int = 10): List<String> {
        val keys = mutableListOf<String>()
        pool?.resource?.use { jedis ->
            var cursor = "0"
            do {
                val scanResult = jedis.scan(cursor, ScanParams().match(pattern).count(count))
                cursor = scanResult.cursor
                keys.addAll(scanResult.result)
            } while (cursor != "0")
        }
        return keys
    }

    /**
     * Creates a backup of the Redis database using SAVE command.
     * Copies the RDB file to the specified backup location.
     * 
     * 使用SAVE命令创建Redis数据库的备份。
     * 将RDB文件复制到指定的备份位置。
     *
     * @param backupPath Path where the backup should be stored
     *                   备份应存储的路径
     */
    override fun backup(backupPath: String) {
        pool?.resource?.use { jedis ->
            jedis.save()
            val configDir = jedis.configGet("dir")
            if (configDir.isNotEmpty()) {
                val dumpFile = File(configDir.getValue("dir"), "dump.rdb")
                dumpFile.copyTo(File(backupPath), overwrite = true)
            }
        }
    }

    /**
     * Restores the Redis database from a backup file.
     * Replaces the current database with the backup data.
     * 
     * 从备份文件恢复Redis数据库。
     * 用备份数据替换当前数据库。
     *
     * @param backupPath Path to the backup file
     *                   备份文件的路径
     */
    override fun restore(backupPath: String) {
        pool?.resource?.use { jedis ->
            jedis.flushDB()
            val configDir = jedis.configGet("dir")
            if (configDir.isNotEmpty()) {
                val dumpFile = File(configDir.getValue("dir"), "dump.rdb")
                File(backupPath).copyTo(dumpFile, overwrite = true)
                jedis.save()
            }
        }
    }

    /**
     * Gets the total size of the Redis database.
     * Returns the number of keys in the current database.
     * 
     * 获取Redis数据库的总大小。
     * 返回当前数据库中的键数量。
     *
     * @return Number of keys in the database
     *         数据库中的键数量
     */
    override fun getDatabaseSize(): Long {
        return pool?.resource?.use { jedis ->
            jedis.dbSize()
        } ?: 0
    }

    /**
     * Close Redis driver
     * 关闭Redis驱动
     */
    override fun close() {
        disconnect()
    }

    // The following methods are added to implement the DatabaseDriver interface,
    // but they are not applicable in Redis
    // 以下是为了实现DatabaseDriver接口而添加的方法，但在Redis中并不适用

    override fun query(sql: String, vararg params: Any?): QueryResult {
        throw UnsupportedOperationException("SQL queries are not supported in Redis")
    }

    override fun update(sql: String, vararg params: Any?): Int {
        throw UnsupportedOperationException("SQL updates are not supported in Redis")
    }

    override fun batchUpdate(sql: String, paramsList: List<Array<Any?>>): IntArray {
        throw UnsupportedOperationException("SQL batch updates are not supported in Redis")
    }

    override fun beginTransaction() {
        pool?.resource?.use { it.multi() }
    }

    override fun commit() {
        pool?.resource?.use { it.multi()?.exec() }
    }

    override fun rollback() {
        pool?.resource?.use { it.multi()?.discard() }
    }

    override fun createTable(tableName: String, columns: Map<String, String>) {
        throw UnsupportedOperationException("Table operations are not supported in Redis")
    }

    override fun dropTable(tableName: String) {
        throw UnsupportedOperationException("Table operations are not supported in Redis")
    }

    override fun tableExists(tableName: String): Boolean {
        throw UnsupportedOperationException("Table operations are not supported in Redis")
    }

    override fun getTables(): List<String> {
        throw UnsupportedOperationException("Table operations are not supported in Redis")
    }

    override fun getTableSchema(tableName: String): Map<String, String> {
        throw UnsupportedOperationException("Schema operations are not supported in Redis")
    }

    override fun addColumn(tableName: String, columnName: String, columnType: String) {
        throw UnsupportedOperationException("Column operations are not supported in Redis")
    }

    override fun dropColumn(tableName: String, columnName: String) {
        throw UnsupportedOperationException("Column operations are not supported in Redis")
    }

    override fun createIndex(tableName: String, indexName: String, columnNames: List<String>, unique: Boolean) {
        throw UnsupportedOperationException("Index operations are not supported in Redis")
    }

    override fun dropIndex(tableName: String, indexName: String) {
        throw UnsupportedOperationException("Index operations are not supported in Redis")
    }

    override fun getTableSize(tableName: String): Long {
        throw UnsupportedOperationException("Table operations are not supported in Redis")
    }

    override fun getRowCount(tableName: String): Long {
        throw UnsupportedOperationException("Table operations are not supported in Redis")
    }

    override fun optimizeTable(tableName: String) {
        throw UnsupportedOperationException("Table operations are not supported in Redis")
    }

    companion object {
        /**
         * Creates a default Redis connection pool configuration.
         * Configures pool settings with recommended values for general use.
         * 
         * 创建默认的Redis连接池配置。
         * 使用推荐的通用值配置池设置。
         *
         * @param minIdle Minimum number of idle connections to maintain
         *                要维护的最小空闲连接数
         * @param maxIdle Maximum number of idle connections to allow
         *                允许的最大空闲连接数
         * @param maxTotal Maximum total number of connections
         *                 最大总连接数
         * @return Configured JedisPoolConfig instance
         *         配置的JedisPoolConfig实例
         */
        fun createDefaultPoolConfig(
            minIdle: Int = 1,
            maxIdle: Int = 8,
            maxTotal: Int = 8
        ): JedisPoolConfig = JedisPoolConfig().apply {
            this.minIdle = minIdle
            this.maxIdle = maxIdle
            this.maxTotal = maxTotal
            testOnBorrow = true
            testOnReturn = true
            testWhileIdle = true
            timeBetweenEvictionRuns = Duration.ofMinutes(1)
            numTestsPerEvictionRun = 3
            minEvictableIdleDuration = Duration.ofMinutes(1)
        }
    }
} 