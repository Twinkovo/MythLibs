package com.twinkovo.mythLibs.utils

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level

/**
 * An enhanced logger that extends Bukkit's logging capabilities.
 * 增强的日志工具，扩展了Bukkit的日志功能
 *
 * Features:
 * 特性：
 * 1. Smart asynchronous logging (automatically handles sync/async)
 *    智能异步日志（自动处理同步/异步）
 * 2. Debug mode with configurable log levels
 *    可配置日志级别的调试模式
 * 3. Plugin-specific logging with context management
 *    带上下文管理的插件特定日志记录
 * 4. Enhanced logging methods with formatted messages
 *    增强的日志方法，支持格式化消息
 */
object Logger {
    // Thread pool for asynchronous logging
    // 用于异步日志的线程池
    private val loggerExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Debug mode flag
    // 调试模式标志
    private var debugMode: Boolean = false
    
    // Plugin context for logging
    // 日志记录的插件上下文
    private var pluginContext = ThreadLocal<Plugin>()

    /**
     * Initializes the logger with plugin context and debug mode
     * 使用插件上下文和调试模式初始化日志记录器
     *
     * @param plugin The plugin to set as context
     *               要设置为上下文的插件
     * @param debug Whether to enable debug mode (default: false)
     *              是否启用调试模式（默认：false）
     */
    fun init(plugin: Plugin, debug: Boolean = false) {
        pluginContext.set(plugin)
        debugMode = debug
    }

    /**
     * Clears the plugin context for the current thread and shuts down the logger
     * 清除当前线程的插件上下文并关闭日志记录器
     */
    fun close() {
        pluginContext.remove()
        loggerExecutor.shutdown()
    }

    /**
     * Gets the current plugin context or throws an exception if not set
     * 获取当前插件上下文，如果未设置则抛出异常
     */
    private fun getPluginContext(): Plugin {
        return pluginContext.get() ?: throw IllegalStateException(
            "Logger not initialized. Please call Logger.init(plugin) first or use the plugin parameter version of the logging methods."
        )
    }



    /**
     * Logs an informational message
     * 记录信息级别的日志
     *
     * @param message The message to log
     *                要记录的消息
     */
    fun info(message: String) {
        log(getPluginContext(), Level.INFO, message)
    }

    /**
     * Logs a warning message
     * 记录警告级别的日志
     *
     * @param message The message to log
     *                要记录的消息
     */
    fun warn(message: String) {
        log(getPluginContext(), Level.WARNING, message)
    }

    /**
     * Logs a severe error message
     * 记录严重错误级别的日志
     *
     * @param message The message to log
     *                要记录的消息
     */
    fun severe(message: String) {
        // Always log severe messages synchronously for immediate attention
        // 始终同步记录严重错误消息以立即引起注意
        log(getPluginContext(), Level.SEVERE, message, forceSync = true)
    }

    /**
     * Logs a debug message (only shown when debug mode is enabled)
     * 记录调试信息（仅在调试模式启用时显示）
     *
     * @param message The message to log
     *                要记录的消息
     */
    fun debug(message: String) {
        if (debugMode) {
            log(getPluginContext(), Level.FINE, "[DEBUG] $message")
        }
    }

    /**
     * Internal method to handle the actual logging
     * 内部方法，用于处理实际的日志记录
     *
     * @param plugin The plugin that is logging the message
     *               记录日志的插件
     * @param level The logging level
     *              日志级别
     * @param message The message to log
     *                要记录的消息
     * @param forceSync Whether to force synchronous logging
     *                  是否强制同步记录日志
     */
    private fun log(plugin: Plugin, level: Level, message: String, forceSync: Boolean = false) {
        val logTask = {
            val logger = plugin.logger
            when (level) {
                Level.INFO -> logger.info(message)
                Level.WARNING -> logger.warning(message)
                Level.SEVERE -> logger.severe(message)
                else -> logger.log(level, message)
            }
        }

        if (!forceSync && !Bukkit.isPrimaryThread()) {
            loggerExecutor.execute(logTask)
        } else {
            logTask.invoke()
        }
    }
} 