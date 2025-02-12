package com.twinkovo.mythLibs.database

import com.twinkovo.mythLibs.database.connection.ConnectionPool
import com.twinkovo.mythLibs.database.connection.DatabaseConfig
import com.twinkovo.mythLibs.database.driver.*
import com.twinkovo.mythLibs.utils.Logger
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Singleton object responsible for managing database connections and operations.
 * Provides centralized database management including initialization, driver management, and backup functionality.
 * 
 * 负责管理数据库连接和操作的单例对象。
 * 提供集中的数据库管理，包括初始化、驱动管理和备份功能。
 */
object DatabaseManager {
    private lateinit var plugin: Plugin
    private lateinit var config: DatabaseConfig
    private val drivers = ConcurrentHashMap<String, DatabaseDriver>()
    private val backupExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var backupTask: ScheduledExecutorService? = null

    /**
     * Initializes the database manager with the specified plugin instance.
     * Sets up the default database and automatic backup schedule.
     * 
     * 使用指定的插件实例初始化数据库管理器。
     * 设置默认数据库和自动备份计划。
     *
     * @param plugin Plugin instance to associate with this manager
     *               要与此管理器关联的插件实例
     */
    fun init(plugin: Plugin) {
        this.plugin = plugin
        this.config = DatabaseConfig()
        
        // Register configuration
        com.twinkovo.mythLibs.config.ConfigManager.register(config::class)
        
        // Initialize default database
        initializeDefaultDatabase()
        
        // Start automatic backup task
        scheduleBackupTask()
    }

    /**
     * Initializes the default database based on configuration.
     * Falls back to SQLite if the configured database type fails to initialize.
     * 
     * 根据配置初始化默认数据库。
     * 如果配置的数据库类型初始化失败，则回退到SQLite。
     */
    private fun initializeDefaultDatabase() {
        when (config.defaultType.lowercase()) {
            "sqlite" -> initializeSQLite()
            "mysql" -> initializeMySQL()
            "redis" -> initializeRedis()
            "mongodb" -> initializeMongoDB()
            else -> {
                Logger.warn("Unknown database type: ${config.defaultType}, using SQLite")
                initializeSQLite()
            }
        }
    }

    /**
     * Initializes SQLite database connection.
     * Creates necessary directories and sets up the connection pool.
     * 
     * 初始化SQLite数据库连接。
     * 创建必要的目录并设置连接池。
     */
    private fun initializeSQLite() {
        try {
            val dbFile = File(plugin.dataFolder, config.sqlite.file)
            dbFile.parentFile?.mkdirs()
            
            val poolId = ConnectionPool.createSQLitePool(config.sqlite)
            val driver = SQLiteDriver(poolId, dbFile)
            
            driver.connect()
            drivers["sqlite"] = driver
            
            Logger.info("SQLite database initialized successfully")
        } catch (e: Exception) {
            Logger.severe("Failed to initialize SQLite database: ${e.message}")
        }
    }

    /**
     * Initializes MySQL database connection.
     * Falls back to SQLite if MySQL initialization fails.
     * 
     * 初始化MySQL数据库连接。
     * 如果MySQL初始化失败，则回退到SQLite。
     */
    private fun initializeMySQL() {
        try {
            val poolId = ConnectionPool.createMySQLPool(config.mysql)
            val driver = MySQLDriver(poolId, config.mysql.database)
            
            driver.connect()
            drivers["mysql"] = driver
            
            Logger.info("MySQL database initialized successfully")
        } catch (e: Exception) {
            Logger.severe("Failed to initialize MySQL database: ${e.message}")
            Logger.info("Falling back to SQLite...")
            initializeSQLite()
        }
    }

    /**
     * Initializes Redis database connection.
     * Falls back to SQLite if Redis initialization fails.
     * 
     * 初始化Redis数据库连接。
     * 如果Redis初始化失败，则回退到SQLite。
     */
    private fun initializeRedis() {
        try {
            val poolConfig = RedisDriver.createDefaultPoolConfig(
                minIdle = config.redis.pool.minimum,
                maxIdle = config.redis.pool.maximum,
                maxTotal = config.redis.pool.maximum * 2
            )
            
            val driver = RedisDriver(
                host = config.redis.host,
                port = config.redis.port,
                password = config.redis.password,
                database = config.redis.database,
                poolConfig = poolConfig
            )
            
            driver.connect()
            drivers["redis"] = driver
            
            Logger.info("Redis database initialized successfully")
        } catch (e: Exception) {
            Logger.severe("Failed to initialize Redis database: ${e.message}")
            Logger.info("Falling back to SQLite...")
            initializeSQLite()
        }
    }

    /**
     * Initializes MongoDB database connection.
     * Falls back to SQLite if MongoDB initialization fails.
     * 
     * 初始化MongoDB数据库连接。
     * 如果MongoDB初始化失败，则回退到SQLite。
     */
    private fun initializeMongoDB() {
        try {
            val driver = MongoDBDriver(
                uri = config.mongodb.uri,
                databaseName = config.mongodb.database
            )
            
            driver.connect()
            drivers["mongodb"] = driver
            
            Logger.info("MongoDB database initialized successfully")
        } catch (e: Exception) {
            Logger.severe("Failed to initialize MongoDB database: ${e.message}")
            Logger.info("Falling back to SQLite...")
            initializeSQLite()
        }
    }

    /**
     * Configures and schedules automatic database backup task.
     * Backups are performed every 24 hours.
     * 
     * 配置和调度自动数据库备份任务。
     * 每24小时执行一次备份。
     */
    private fun scheduleBackupTask() {
        backupTask?.shutdown()
        backupTask = Executors.newSingleThreadScheduledExecutor().apply {
            scheduleAtFixedRate({
                try {
                    backup()
                } catch (e: Exception) {
                    Logger.warn("Automatic database backup failed: ${e.message}")
                }
            }, 1, 24, TimeUnit.HOURS)
        }
    }

    /**
     * Performs backup of all configured databases.
     * Creates timestamped backup files in the plugin's backup directory.
     * 
     * 执行所有配置的数据库的备份。
     * 在插件的备份目录中创建带时间戳的备份文件。
     */
    fun backup() {
        val backupDir = plugin.dataFolder.resolve("backups")
        backupDir.mkdirs()
        
        val timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        )
        
        drivers.forEach { (name, driver) ->
            try {
                val backupFile = backupDir.resolve("${name}_$timestamp.backup")
                driver.backup(backupFile.absolutePath)
                Logger.info("Database $name backup successful: ${backupFile.name}")
            } catch (e: Exception) {
                Logger.warn("Database $name backup failed: ${e.message}")
            }
        }
    }

    /**
     * Gets a database driver by name.
     * 通过名称获取数据库驱动。
     *
     * @param name Name of the driver to retrieve
     *             要获取的驱动名称
     * @return The requested database driver
     *         请求的数据库驱动
     * @throws IllegalArgumentException if the specified driver is not found
     *                                 如果指定的驱动未找到则抛出异常
     */
    fun getDriver(name: String): DatabaseDriver {
        return drivers[name] ?: throw IllegalArgumentException("Database driver not found: $name")
    }

    /**
     * Gets the default database driver configured for the system.
     * 获取系统配置的默认数据库驱动。
     *
     * @return The default database driver
     *         默认数据库驱动
     * @throws IllegalStateException if the default driver is not initialized
     *                              如果默认驱动未初始化则抛出异常
     */
    fun getDefaultDriver(): DatabaseDriver {
        return drivers[config.defaultType] ?: throw IllegalStateException("Default database driver not initialized")
    }

    /**
     * Shuts down the database manager and releases all resources.
     * Closes all database connections and stops the backup task.
     * 
     * 关闭数据库管理器并释放所有资源。
     * 关闭所有数据库连接并停止备份任务。
     */
    fun shutdown() {
        // Close backup task
        backupTask?.shutdown()
        backupExecutor.shutdown()
        
        // Close all drivers
        drivers.forEach { (_, driver) ->
            try {
                driver.close()
            } catch (e: Exception) {
                Logger.warn("Failed to close database driver: ${e.message}")
            }
        }
        drivers.clear()
        
        // Close all connection pools
        ConnectionPool.closeAllPools()
        
        Logger.info("Database manager shutdown complete")
    }
} 