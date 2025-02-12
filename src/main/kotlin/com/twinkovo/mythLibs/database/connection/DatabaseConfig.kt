package com.twinkovo.mythLibs.database.connection

import com.twinkovo.mythLibs.config.annotations.Config
import com.twinkovo.mythLibs.config.annotations.ConfigSection
import com.twinkovo.mythLibs.config.annotations.ConfigValue
import com.twinkovo.mythLibs.config.validator.ConfigValidator

/**
 * Configuration class for database settings.
 * Provides centralized configuration for multiple database types and their connection parameters.
 * 
 * 数据库设置的配置类。
 * 为多种数据库类型及其连接参数提供集中配置。
 */
@Config(
    name = "database",
    header = ["Database Configuration File", "数据库配置文件"],
    version = 1
)
class DatabaseConfig {
    /**
     * Default database type to use (sqlite, mysql, redis, mongodb).
     * 要使用的默认数据库类型（sqlite、mysql、redis、mongodb）。
     */
    @ConfigValue(comment = ["Default database type (sqlite, mysql, redis, mongodb)", "默认数据库类型 (sqlite, mysql, redis, mongodb)"])
    var defaultType: String = "sqlite"

    /**
     * SQLite database configuration section.
     * SQLite数据库配置部分。
     */
    @ConfigSection(comment = ["SQLite Configuration", "SQLite配置"])
    val sqlite = SQLiteConfig()

    /**
     * MySQL database configuration section.
     * MySQL数据库配置部分。
     */
    @ConfigSection(comment = ["MySQL Configuration", "MySQL配置"])
    val mysql = MySQLConfig()

    /**
     * Redis database configuration section.
     * Redis数据库配置部分。
     */
    @ConfigSection(comment = ["Redis Configuration", "Redis配置"])
    val redis = RedisConfig()

    /**
     * MongoDB database configuration section.
     * MongoDB数据库配置部分。
     */
    @ConfigSection(comment = ["MongoDB Configuration", "MongoDB配置"])
    val mongodb = MongoDBConfig()
}

/**
 * Configuration class for SQLite database settings.
 * Contains settings specific to SQLite database connections.
 * 
 * SQLite数据库设置的配置类。
 * 包含SQLite数据库连接的特定设置。
 */
class SQLiteConfig {
    /**
     * Path to the SQLite database file.
     * 
     * SQLite数据库文件的路径。
     */
    @ConfigValue(comment = ["Database file path", "数据库文件路径"])
    var file: String = "database.db"
}

/**
 * Configuration class for MySQL database settings.
 * Contains settings specific to MySQL database connections.
 * 
 * MySQL数据库设置的配置类。
 * 包含MySQL数据库连接的特定设置。
 */
class MySQLConfig {
    /**
     * MySQL server host address.
     * MySQL服务器主机地址。
     */
    @ConfigValue(comment = ["Host address", "主机地址"])
    var host: String = "localhost"

    /**
     * MySQL server port number.
     * MySQL服务器端口号。
     */
    @ConfigValue(comment = ["Port number", "端口"])
    var port: Int = 3306

    /**
     * MySQL database name.
     * MySQL数据库名称。
     */
    @ConfigValue(comment = ["Database name", "数据库名"])
    var database: String = "minecraft"

    /**
     * MySQL user username.
     * MySQL用户名。
     */
    @ConfigValue(comment = ["Username", "用户名"])
    var username: String = "root"

    /**
     * MySQL user password.
     * MySQL用户密码。
     */
    @ConfigValue(comment = ["Password", "密码"])
    var password: String = ""

    /**
     * Connection pool configuration for MySQL.
     * MySQL的连接池配置。
     */
    @ConfigSection(comment = ["Connection pool configuration", "连接池配置"])
    val pool = PoolConfig()
}

/**
 * Configuration class for Redis database settings.
 * Contains settings specific to Redis database connections.
 * 
 * Redis数据库设置的配置类。
 * 包含Redis数据库连接的特定设置。
 */
class RedisConfig {
    /**
     * Redis server host address.
     * Redis服务器主机地址。
     */
    @ConfigValue(comment = ["Host address", "主机地址"])
    var host: String = "localhost"

    /**
     * Redis server port number.
     * Redis服务器端口号。
     */
    @ConfigValue(comment = ["Port number", "端口"])
    var port: Int = 6379

    /**
     * Redis server password.
     * Redis服务器密码。
     */
    @ConfigValue(comment = ["Password", "密码"])
    var password: String = ""

    /**
     * Redis database index number.
     * Redis数据库索引号。
     */
    @ConfigValue(comment = ["Database index", "数据库索引"])
    var database: Int = 0

    /**
     * Connection pool configuration for Redis.
     * Redis的连接池配置。
     */
    @ConfigSection(comment = ["Connection pool configuration", "连接池配置"])
    val pool = PoolConfig()
}

/**
 * Configuration class for MongoDB database settings.
 * Contains settings specific to MongoDB database connections.
 * 
 * MongoDB数据库设置的配置类。
 * 包含MongoDB数据库连接的特定设置。
 */
class MongoDBConfig {
    /**
     * MongoDB connection URI.
     * MongoDB连接URI。
     */
    @ConfigValue(comment = ["Connection URI", "连接URI"])
    var uri: String = "mongodb://localhost:27017"

    /**
     * MongoDB database name.
     * MongoDB数据库名称。
     */
    @ConfigValue(comment = ["Database name", "数据库名"])
    var database: String = "minecraft"

    /**
     * Connection pool configuration for MongoDB.
     * MongoDB的连接池配置。
     */
    @ConfigSection(comment = ["Connection pool configuration", "连接池配置"])
    val pool = PoolConfig()
}

/**
 * Configuration class for database connection pool settings.
 * Contains common settings for managing database connection pools.
 * 
 * 数据库连接池设置的配置类。
 * 包含管理数据库连接池的通用设置。
 */
class PoolConfig {
    /**
     * Minimum number of connections to maintain in the pool.
     * Must be between 1 and 100.
     * 
     * 连接池中要维护的最小连接数。
     * 必须在1到100之间。
     */
    @ConfigValue(
        comment = ["Minimum number of connections", "最小连接数"],
        validator = MinConnectionsValidator::class
    )
    var minimum: Int = 5

    /**
     * Maximum number of connections allowed in the pool.
     * Must be between 1 and 100.
     * 
     * 连接池中允许的最大连接数。
     * 必须在1到100之间。
     */
    @ConfigValue(
        comment = ["Maximum number of connections", "最大连接数"],
        validator = MaxConnectionsValidator::class
    )
    var maximum: Int = 10

    /**
     * Connection timeout in milliseconds.
     * Time to wait when acquiring a new connection.
     * 
     * 连接超时时间（毫秒）。
     * 获取新连接时的等待时间。
     */
    @ConfigValue(comment = ["Connection timeout (milliseconds)", "连接超时时间（毫秒）"])
    var timeout: Long = 30000

    /**
     * Idle connection timeout in milliseconds.
     * Time after which an unused connection is closed.
     * 
     * 空闲连接超时时间（毫秒）。
     * 未使用的连接被关闭的时间。
     */
    @ConfigValue(comment = ["Idle connection timeout (milliseconds)", "空闲连接超时时间（毫秒）"])
    var idleTimeout: Long = 600000
}

/**
 * Validator for minimum connections configuration.
 * Ensures the minimum connection count is within valid range.
 * 
 * 最小连接数配置的验证器。
 * 确保最小连接数在有效范围内。
 */
class MinConnectionsValidator : ConfigValidator<Int> {
    override fun validate(value: Int): Boolean = value in 1..100
    override fun getErrorMessage(value: Int): String = "Minimum connections must be between 1 and 100 / 最小连接数必须在1到100之间"
}

/**
 * Validator for maximum connections configuration.
 * Ensures the maximum connection count is within valid range.
 * 
 * 最大连接数配置的验证器。
 * 确保最大连接数在有效范围内。
 */
class MaxConnectionsValidator : ConfigValidator<Int> {
    override fun validate(value: Int): Boolean = value in 1..100
    override fun getErrorMessage(value: Int): String = "Maximum connections must be between 1 and 100 / 最大连接数必须在1到100之间"
}