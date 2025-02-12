package com.twinkovo.mythLibs.database.monitoring

import com.twinkovo.mythLibs.database.DatabaseManager
import com.twinkovo.mythLibs.database.cache.DatabaseCache
import com.twinkovo.mythLibs.database.connection.ConnectionPool
import com.twinkovo.mythLibs.utils.Logger
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Database monitoring system that tracks database performance and usage metrics.
 * Provides real-time monitoring of query execution, cache performance, and connection pool status.
 * 
 * 数据库监控系统，用于跟踪数据库性能和使用指标。
 * 提供查询执行、缓存性能和连接池状态的实时监控。
 */
object DatabaseMonitor {
    private val queryCount = AtomicLong(0)
    private val updateCount = AtomicLong(0)
    private val errorCount = AtomicLong(0)
    private val queryTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val startTime = Instant.now()

    /**
     * Records the execution of a database query and its duration.
     * 记录数据库查询的执行及其持续时间。
     *
     * @param sql The SQL query that was executed
     *            执行的SQL查询
     * @param duration The time taken to execute the query
     *                 查询执行所需的时间
     */
    fun recordQuery(sql: String, duration: Duration) {
        queryCount.incrementAndGet()
        queryTimes.computeIfAbsent(sql) { mutableListOf() }.add(duration.toMillis())
    }

    /**
     * Records the execution of a database update operation.
     * 记录数据库更新操作的执行。
     */
    fun recordUpdate() {
        updateCount.incrementAndGet()
    }

    /**
     * Records the occurrence of a database error.
     * 记录数据库错误的发生。
     */
    fun recordError() {
        errorCount.incrementAndGet()
    }

    /**
     * Retrieves current monitoring statistics.
     * Includes query performance, cache statistics, and connection pool status.
     * 
     * 获取当前监控统计信息。
     * 包括查询性能、缓存统计和连接池状态。
     *
     * @return Current monitoring statistics
     *         当前监控统计信息
     */
    fun getStats(): MonitoringStats {
        val uptime = Duration.between(startTime, Instant.now())
        val slowQueries = queryTimes.entries
            .filter { (_, times) -> times.any { it > 1000 } }
            .map { (sql, times) ->
                SlowQuery(
                    sql = sql,
                    avgTime = times.average(),
                    maxTime = times.maxOrNull() ?: 0,
                    count = times.size
                )
            }
            .sortedByDescending { it.avgTime }
            .take(10)

        val poolStats = DatabaseManager.getDefaultDriver().let { driver ->
            when (driver.name) {
                "MySQL", "SQLite" -> ConnectionPool.getPoolStats(driver.name.lowercase())
                else -> null
            }
        }

        val cacheStats = DatabaseCache.getStats()

        return MonitoringStats(
            uptime = uptime,
            queryCount = queryCount.get(),
            updateCount = updateCount.get(),
            errorCount = errorCount.get(),
            slowQueries = slowQueries,
            poolStats = poolStats,
            cacheStats = cacheStats
        )
    }

    /**
     * Logs current monitoring statistics to the system log.
     * Provides a detailed breakdown of database performance metrics.
     * 
     * 将当前监控统计信息记录到系统日志。
     * 提供数据库性能指标的详细分析。
     */
    fun logStats() {
        val stats = getStats()
        Logger.info("""
            Database Monitoring Statistics:
            Uptime: ${stats.uptime}
            Queries: ${stats.queryCount}
            Updates: ${stats.updateCount}
            Errors: ${stats.errorCount}
            
            Slow Queries:
            ${stats.slowQueries.joinToString("\n") { "- ${it.sql}: avg=${it.avgTime}ms, max=${it.maxTime}ms, count=${it.count}" }}
            
            Connection Pool:
            ${stats.poolStats?.let {
                """
                Active Connections: ${it.activeConnections}
                Idle Connections: ${it.idleConnections}
                Total Connections: ${it.totalConnections}
                Threads Awaiting Connection: ${it.threadsAwaitingConnection}
                Maximum Pool Size: ${it.maximumPoolSize}
                """.trimIndent()
            } ?: "No pool statistics available"}
            
            Cache:
            Query Cache:
            - Hit Count: ${stats.cacheStats.queryHitCount}
            - Miss Count: ${stats.cacheStats.queryMissCount}
            - Hit Rate: ${String.format("%.2f%%", stats.cacheStats.queryHitRate * 100)}
            - Size: ${stats.cacheStats.querySize}
            
            Count Cache:
            - Hit Count: ${stats.cacheStats.countHitCount}
            - Miss Count: ${stats.cacheStats.countMissCount}
            - Hit Rate: ${String.format("%.2f%%", stats.cacheStats.countHitRate * 100)}
            - Size: ${stats.cacheStats.countSize}
        """.trimIndent())
    }

    /**
     * Resets all monitoring statistics to their initial values.
     * Clears all counters and timing information.
     * 
     * 将所有监控统计信息重置为初始值。
     * 清除所有计数器和计时信息。
     */
    fun reset() {
        queryCount.set(0)
        updateCount.set(0)
        errorCount.set(0)
        queryTimes.clear()
    }
}

/**
 * Data class containing comprehensive database monitoring statistics.
 * Includes various metrics about database performance and resource usage.
 * 
 * 包含全面数据库监控统计信息的数据类。
 * 包括有关数据库性能和资源使用的各种指标。
 *
 * @property uptime System uptime since monitoring started
 *                  监控开始以来的系统运行时间
 * @property queryCount Total number of queries executed
 *                     执行的查询总数
 * @property updateCount Total number of update operations executed
 *                      执行的更新操作总数
 * @property errorCount Total number of errors encountered
 *                     遇到的错误总数
 * @property slowQueries List of slow query statistics
 *                      慢查询统计列表
 * @property poolStats Connection pool statistics (if available)
 *                    连接池统计信息（如果可用）
 * @property cacheStats Cache performance statistics
 *                     缓存性能统计信息
 */
data class MonitoringStats(
    val uptime: Duration,
    val queryCount: Long,
    val updateCount: Long,
    val errorCount: Long,
    val slowQueries: List<SlowQuery>,
    val poolStats: com.twinkovo.mythLibs.database.connection.PoolStats?,
    val cacheStats: com.twinkovo.mythLibs.database.cache.CacheStats
)

/**
 * Data class representing statistics for a slow query.
 * Provides detailed timing information about potentially problematic queries.
 * 
 * 表示慢查询统计信息的数据类。
 * 提供有关潜在问题查询的详细计时信息。
 *
 * @property sql The SQL query string
 *               SQL查询字符串
 * @property avgTime Average execution time in milliseconds
 *                   平均执行时间（毫秒）
 * @property maxTime Maximum execution time in milliseconds
 *                   最大执行时间（毫秒）
 * @property count Number of times this query was executed
 *                 此查询执行的次数
 */
data class SlowQuery(
    val sql: String,
    val avgTime: Double,
    val maxTime: Long,
    val count: Int
) 