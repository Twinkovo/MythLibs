package com.twinkovo.mythLibs.database.connection

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap

/**
 * Database connection pool management class.
 * Manages HikariCP connection pools for different database types.
 * 
 * 数据库连接池管理类。
 * 管理不同数据库类型的HikariCP连接池。
 */
object ConnectionPool {
    private val pools = ConcurrentHashMap<String, HikariDataSource>()

    /**
     * Creates a MySQL connection pool with the specified configuration.
     * If a pool with the same configuration already exists, returns its ID.
     * 
     * 使用指定的配置创建MySQL连接池。
     * 如果具有相同配置的连接池已存在，则返回其ID。
     *
     * @param config MySQL configuration settings
     *               MySQL配置设置
     * @return Pool identifier string
     *         连接池标识符字符串
     */
    fun createMySQLPool(config: MySQLConfig): String {
        val poolId = "mysql-${config.host}-${config.port}-${config.database}"
        if (pools.containsKey(poolId)) {
            return poolId
        }

        val hikariConfig = HikariConfig().apply {
            driverClassName = "com.mysql.cj.jdbc.Driver"
            jdbcUrl = "jdbc:mysql://${config.host}:${config.port}/${config.database}?" +
                    "useUnicode=true&" +
                    "characterEncoding=utf8&" +
                    "useSSL=false&" +
                    "allowPublicKeyRetrieval=true&" +
                    "serverTimezone=UTC"
            username = config.username
            password = config.password
            minimumIdle = config.pool.minimum
            maximumPoolSize = config.pool.maximum
            connectionTimeout = config.pool.timeout
            idleTimeout = config.pool.idleTimeout
            poolName = poolId
        }

        val dataSource = HikariDataSource(hikariConfig)
        pools[poolId] = dataSource
        return poolId
    }

    /**
     * Creates a SQLite connection pool with the specified configuration.
     * If a pool with the same configuration already exists, returns its ID.
     * Note: SQLite pools are limited to a single connection due to SQLite's nature.
     * 
     * 使用指定的配置创建SQLite连接池。
     * 如果具有相同配置的连接池已存在，则返回其ID。
     * 注意：由于SQLite的特性，SQLite连接池限制为单个连接。
     *
     * @param config SQLite configuration settings
     *               SQLite配置设置
     * @return Pool identifier string
     *         连接池标识符字符串
     */
    fun createSQLitePool(config: SQLiteConfig): String {
        val poolId = "sqlite-${config.file}"
        if (pools.containsKey(poolId)) {
            return poolId
        }

        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.sqlite.JDBC"
            jdbcUrl = "jdbc:sqlite:${config.file}"
            minimumIdle = 1
            maximumPoolSize = 1
            connectionTimeout = 30000
            poolName = poolId
        }

        val dataSource = HikariDataSource(hikariConfig)
        pools[poolId] = dataSource
        return poolId
    }

    /**
     * Gets a connection from the specified pool.
     * Throws IllegalArgumentException if the pool doesn't exist.
     * 
     * 从指定的连接池获取连接。
     * 如果连接池不存在，则抛出IllegalArgumentException。
     *
     * @param poolId Pool identifier string
     *               连接池标识符字符串
     * @return Database connection
     *         数据库连接
     * @throws IllegalArgumentException if the pool is not found
     *                                 如果找不到连接池
     */
    fun getConnection(poolId: String): Connection {
        val pool = pools[poolId] ?: throw IllegalArgumentException("Pool not found: $poolId")
        return pool.connection
    }

    /**
     * Closes and removes a specific connection pool.
     * 
     * 关闭并移除特定的连接池。
     *
     * @param poolId Pool identifier string
     *               连接池标识符字符串
     */
    fun closePool(poolId: String) {
        pools.remove(poolId)?.close()
    }

    /**
     * Closes and removes all connection pools.
     * Should be called during system shutdown.
     * 
     * 关闭并移除所有连接池。
     * 应在系统关闭时调用。
     */
    fun closeAllPools() {
        pools.forEach { (_, pool) -> pool.close() }
        pools.clear()
    }

    /**
     * Gets statistics about a specific connection pool.
     * Provides information about connection usage and pool status.
     * 
     * 获取特定连接池的统计信息。
     * 提供有关连接使用情况和连接池状态的信息。
     *
     * @param poolId Pool identifier string
     *               连接池标识符字符串
     * @return Pool statistics object
     *         连接池统计信息对象
     * @throws IllegalArgumentException if the pool is not found
     *                                 如果找不到连接池
     */
    fun getPoolStats(poolId: String): PoolStats {
        val pool = pools[poolId] ?: throw IllegalArgumentException("Pool not found: $poolId")
        val poolMXBean = pool.hikariPoolMXBean
        return PoolStats(
            activeConnections = poolMXBean.activeConnections,
            idleConnections = poolMXBean.idleConnections,
            totalConnections = poolMXBean.totalConnections,
            threadsAwaitingConnection = poolMXBean.threadsAwaitingConnection,
            maximumPoolSize = pool.maximumPoolSize
        )
    }
}

/**
 * Data class containing statistics about a connection pool.
 * Provides metrics about connection usage and pool capacity.
 * 
 * 包含连接池统计信息的数据类。
 * 提供有关连接使用情况和连接池容量的指标。
 *
 * @property activeConnections Number of connections currently in use
 *                            当前正在使用的连接数
 * @property idleConnections Number of idle connections in the pool
 *                          连接池中的空闲连接数
 * @property totalConnections Total number of connections in the pool
 *                           连接池中的总连接数
 * @property threadsAwaitingConnection Number of threads waiting for a connection
 *                                    等待连接的线程数
 * @property maximumPoolSize Maximum number of connections the pool can hold
 *                          连接池可以容纳的最大连接数
 */
data class PoolStats(
    val activeConnections: Int,
    val idleConnections: Int,
    val totalConnections: Int,
    val threadsAwaitingConnection: Int,
    val maximumPoolSize: Int
)