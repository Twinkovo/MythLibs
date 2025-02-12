package com.twinkovo.mythLibs.database.async

import com.twinkovo.mythLibs.database.driver.DatabaseDriver
import com.twinkovo.mythLibs.database.model.QueryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.CompletableFuture

/**
 * Provides asynchronous database operations using both Kotlin Coroutines and CompletableFuture.
 * Supports async queries, updates, batch operations, and transactions.
 * 
 * 使用Kotlin协程和CompletableFuture提供异步数据库操作。
 * 支持异步查询、更新、批量操作和事务。
 */
object AsyncDatabaseOperations {
    private lateinit var plugin: Plugin

    /**
     * Initializes the async operations system with a plugin instance.
     * Must be called before using any async operations.
     * 
     * 使用插件实例初始化异步操作系统。
     * 必须在使用任何异步操作之前调用。
     *
     * @param plugin Plugin instance for scheduling async tasks
     *               用于调度异步任务的插件实例
     */
    fun init(plugin: Plugin) {
        this.plugin = plugin
    }

    /**
     * Executes a database query asynchronously using Kotlin Coroutines.
     * Suspends the current coroutine until the query completes.
     * 
     * 使用Kotlin协程异步执行数据库查询。
     * 挂起当前协程直到查询完成。
     *
     * @param driver Database driver to execute the query
     *               执行查询的数据库驱动
     * @param sql SQL query string
     *            SQL查询语句
     * @param params Query parameters to bind
     *               要绑定的查询参数
     * @return Query result containing the data
     *         包含数据的查询结果
     */
    suspend fun query(
        driver: DatabaseDriver,
        sql: String,
        vararg params: Any?
    ): QueryResult = withContext(Dispatchers.IO) {
        driver.query(sql, *params)
    }

    /**
     * Executes a database update asynchronously using Kotlin Coroutines.
     * Suspends the current coroutine until the update completes.
     * 
     * 使用Kotlin协程异步执行数据库更新。
     * 挂起当前协程直到更新完成。
     *
     * @param driver Database driver to execute the update
     *               执行更新的数据库驱动
     * @param sql SQL update statement
     *            SQL更新语句
     * @param params Update parameters to bind
     *               要绑定的更新参数
     * @return Number of rows affected by the update
     *         更新影响的行数
     */
    suspend fun update(
        driver: DatabaseDriver,
        sql: String,
        vararg params: Any?
    ): Int = withContext(Dispatchers.IO) {
        driver.update(sql, *params)
    }

    /**
     * Executes a batch update asynchronously using Kotlin Coroutines.
     * Suspends the current coroutine until all updates complete.
     * 
     * 使用Kotlin协程异步执行批量更新。
     * 挂起当前协程直到所有更新完成。
     *
     * @param driver Database driver to execute the batch update
     *               执行批量更新的数据库驱动
     * @param sql SQL update statement template
     *            SQL更新语句模板
     * @param paramsList List of parameter arrays for batch execution
     *                   用于批量执行的参数数组列表
     * @return Array of affected row counts for each update
     *         每个更新影响的行数数组
     */
    suspend fun batchUpdate(
        driver: DatabaseDriver,
        sql: String,
        paramsList: List<Array<Any?>>
    ): IntArray = withContext(Dispatchers.IO) {
        driver.batchUpdate(sql, paramsList)
    }

    /**
     * Executes a database query asynchronously using CompletableFuture.
     * Returns immediately with a future that will complete when the query finishes.
     * 
     * 使用CompletableFuture异步执行数据库查询。
     * 立即返回一个future，该future将在查询完成时完成。
     *
     * @param driver Database driver to execute the query
     *               执行查询的数据库驱动
     * @param sql SQL query string
     *            SQL查询语句
     * @param params Query parameters to bind
     *               要绑定的查询参数
     * @return CompletableFuture that will contain the query result
     *         将包含查询结果的CompletableFuture
     */
    fun queryAsync(
        driver: DatabaseDriver,
        sql: String,
        vararg params: Any?
    ): CompletableFuture<QueryResult> {
        val future = CompletableFuture<QueryResult>()
        object : BukkitRunnable() {
            override fun run() {
                try {
                    val result = driver.query(sql, *params)
                    future.complete(result)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        }.runTaskAsynchronously(plugin)
        return future
    }

    /**
     * Executes a database update asynchronously using CompletableFuture.
     * Returns immediately with a future that will complete when the update finishes.
     * 
     * 使用CompletableFuture异步执行数据库更新。
     * 立即返回一个future，该future将在更新完成时完成。
     *
     * @param driver Database driver to execute the update
     *               执行更新的数据库驱动
     * @param sql SQL update statement
     *            SQL更新语句
     * @param params Update parameters to bind
     *               要绑定的更新参数
     * @return CompletableFuture that will contain the number of affected rows
     *         将包含受影响行数的CompletableFuture
     */
    fun updateAsync(
        driver: DatabaseDriver,
        sql: String,
        vararg params: Any?
    ): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        object : BukkitRunnable() {
            override fun run() {
                try {
                    val result = driver.update(sql, *params)
                    future.complete(result)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        }.runTaskAsynchronously(plugin)
        return future
    }

    /**
     * Executes a batch update asynchronously using CompletableFuture.
     * Returns immediately with a future that will complete when all updates finish.
     * 
     * 使用CompletableFuture异步执行批量更新。
     * 立即返回一个future，该future将在所有更新完成时完成。
     *
     * @param driver Database driver to execute the batch update
     *               执行批量更新的数据库驱动
     * @param sql SQL update statement template
     *            SQL更新语句模板
     * @param paramsList List of parameter arrays for batch execution
     *                   用于批量执行的参数数组列表
     * @return CompletableFuture that will contain an array of affected row counts
     *         将包含受影响行数数组的CompletableFuture
     */
    fun batchUpdateAsync(
        driver: DatabaseDriver,
        sql: String,
        paramsList: List<Array<Any?>>
    ): CompletableFuture<IntArray> {
        val future = CompletableFuture<IntArray>()
        object : BukkitRunnable() {
            override fun run() {
                try {
                    val result = driver.batchUpdate(sql, paramsList)
                    future.complete(result)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        }.runTaskAsynchronously(plugin)
        return future
    }

    /**
     * Executes a transaction asynchronously using Kotlin Coroutines.
     * Automatically handles commit and rollback based on block execution success.
     * 
     * 使用Kotlin协程异步执行事务。
     * 根据代码块执行成功与否自动处理提交和回滚。
     *
     * @param driver Database driver to execute the transaction
     *               执行事务的数据库驱动
     * @param block Suspending block of code to execute within the transaction
     *              在事务中执行的挂起代码块
     */
    suspend fun transaction(
        driver: DatabaseDriver,
        block: suspend () -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            driver.beginTransaction()
            block()
            driver.commit()
        } catch (e: Exception) {
            driver.rollback()
            throw e
        }
    }

    /**
     * Executes a transaction asynchronously using CompletableFuture.
     * Automatically handles commit and rollback based on block execution success.
     * 
     * 使用CompletableFuture异步执行事务。
     * 根据代码块执行成功与否自动处理提交和回滚。
     *
     * @param driver Database driver to execute the transaction
     *               执行事务的数据库驱动
     * @param block Block of code to execute within the transaction
     *              在事务中执行的代码块
     * @return CompletableFuture that will complete when the transaction finishes
     *         将在事务完成时完成的CompletableFuture
     */
    fun transactionAsync(
        driver: DatabaseDriver,
        block: () -> Unit
    ): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        object : BukkitRunnable() {
            override fun run() {
                try {
                    driver.beginTransaction()
                    block()
                    driver.commit()
                    future.complete(Unit)
                } catch (e: Exception) {
                    driver.rollback()
                    future.completeExceptionally(e)
                }
            }
        }.runTaskAsynchronously(plugin)
        return future
    }
} 