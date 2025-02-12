package com.twinkovo.mythLibs.database.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.twinkovo.mythLibs.database.model.QueryResult
import java.time.Duration

/**
 * High-performance database caching system using Caffeine cache.
 * Provides separate caches for query results and count operations with automatic expiration.
 * 
 * 使用Caffeine缓存的高性能数据库缓存系统。
 * 为查询结果和计数操作提供独立的缓存，具有自动过期功能。
 */
object DatabaseCache {
    private val queryCache: Cache<String, QueryResult> = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build()

    private val countCache: Cache<String, Long> = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build()

    /**
     * Retrieves a cached query result by its key.
     * Returns null if the result is not in cache or has expired.
     * 
     * 通过键获取缓存的查询结果。
     * 如果结果不在缓存中或已过期，则返回null。
     *
     * @param key Unique identifier for the cached query result
     *            缓存查询结果的唯一标识符
     * @return Cached QueryResult if found and valid, null otherwise
     *         如果找到且有效则返回缓存的QueryResult，否则返回null
     */
    fun getQueryResult(key: String): QueryResult? = queryCache.getIfPresent(key)

    /**
     * Stores a query result in the cache with the specified key.
     * Overwrites any existing value for the same key.
     * 
     * 使用指定的键将查询结果存储在缓存中。
     * 覆盖同一键的任何现有值。
     *
     * @param key Unique identifier for the query result
     *            查询结果的唯一标识符
     * @param result QueryResult to cache
     *               要缓存的QueryResult
     */
    fun cacheQueryResult(key: String, result: QueryResult) {
        queryCache.put(key, result)
    }

    /**
     * Retrieves a cached count value by its key.
     * Returns null if the count is not in cache or has expired.
     * 
     * 通过键获取缓存的计数值。
     * 如果计数不在缓存中或已过期，则返回null。
     *
     * @param key Unique identifier for the cached count
     *            缓存计数的唯一标识符
     * @return Cached count value if found and valid, null otherwise
     *         如果找到且有效则返回缓存的计数值，否则返回null
     */
    fun getCount(key: String): Long? = countCache.getIfPresent(key)

    /**
     * Stores a count value in the cache with the specified key.
     * Overwrites any existing value for the same key.
     * 
     * 使用指定的键将计数值存储在缓存中。
     * 覆盖同一键的任何现有值。
     *
     * @param key Unique identifier for the count value
     *            计数值的唯一标识符
     * @param count Count value to cache
     *              要缓存的计数值
     */
    fun cacheCount(key: String, count: Long) {
        countCache.put(key, count)
    }

    /**
     * Invalidates (removes) cache entries for both query and count caches with the specified key.
     * Safe to call even if the key doesn't exist in either cache.
     * 
     * 使指定键的查询和计数缓存条目失效（删除）。
     * 即使键在任一缓存中不存在也可以安全调用。
     *
     * @param key Cache key to invalidate
     *            要使其失效的缓存键
     */
    fun invalidate(key: String) {
        queryCache.invalidate(key)
        countCache.invalidate(key)
    }

    /**
     * Invalidates (removes) all entries from both query and count caches.
     * Useful when the database schema changes or during system maintenance.
     * 
     * 使查询和计数缓存中的所有条目失效（删除）。
     * 在数据库架构更改或系统维护期间很有用。
     */
    fun invalidateAll() {
        queryCache.invalidateAll()
        countCache.invalidateAll()
    }

    /**
     * Retrieves current statistics about both caches.
     * Includes hit rates, miss counts, and cache sizes.
     * 
     * 获取两个缓存的当前统计信息。
     * 包括命中率、未命中计数和缓存大小。
     *
     * @return CacheStats object containing detailed statistics for both caches
     *         包含两个缓存详细统计信息的CacheStats对象
     */
    fun getStats(): CacheStats {
        val queryStats = queryCache.stats()
        val countStats = countCache.stats()

        return CacheStats(
            queryHitCount = queryStats.hitCount(),
            queryMissCount = queryStats.missCount(),
            queryHitRate = queryStats.hitRate(),
            querySize = queryCache.estimatedSize(),
            countHitCount = countStats.hitCount(),
            countMissCount = countStats.missCount(),
            countHitRate = countStats.hitRate(),
            countSize = countCache.estimatedSize()
        )
    }
}

/**
 * Data class containing comprehensive statistics about the database caches.
 * Provides metrics for both query and count caches to monitor performance.
 * 
 * 包含数据库缓存综合统计信息的数据类。
 * 提供查询和计数缓存的指标以监控性能。
 *
 * @property queryHitCount Number of times a query was successfully found in cache
 *                        查询在缓存中成功找到的次数
 * @property queryMissCount Number of times a query was not found in cache
 *                         查询在缓存中未找到的次数
 * @property queryHitRate Ratio of cache hits to total lookups for queries
 *                       查询缓存命中与总查找的比率
 * @property querySize Current number of entries in the query cache
 *                    查询缓存中的当前条目数
 * @property countHitCount Number of times a count was successfully found in cache
 *                        计数在缓存中成功找到的次数
 * @property countMissCount Number of times a count was not found in cache
 *                         计数在缓存中未找到的次数
 * @property countHitRate Ratio of cache hits to total lookups for counts
 *                       计数缓存命中与总查找的比率
 * @property countSize Current number of entries in the count cache
 *                    计数缓存中的当前条目数
 */
data class CacheStats(
    val queryHitCount: Long,
    val queryMissCount: Long,
    val queryHitRate: Double,
    val querySize: Long,
    val countHitCount: Long,
    val countMissCount: Long,
    val countHitRate: Double,
    val countSize: Long
) 