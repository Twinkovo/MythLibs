package com.twinkovo.mythLibs.database.migration

import com.twinkovo.mythLibs.database.driver.DatabaseDriver
import com.twinkovo.mythLibs.utils.Logger
import java.time.LocalDateTime

/**
 * Singleton object responsible for managing database migrations.
 * Handles migration registration, execution, rollback, and tracking of migration history.
 * 
 * 负责管理数据库迁移的单例对象。
 * 处理迁移注册、执行、回滚和迁移历史跟踪。
 */
object MigrationManager {
    private val migrations = mutableListOf<Migration>()
    private lateinit var driver: DatabaseDriver

    /**
     * Initializes the migration manager with a database driver.
     * Creates the migrations tracking table if it doesn't exist.
     * 
     * 使用数据库驱动初始化迁移管理器。
     * 如果迁移跟踪表不存在，则创建它。
     *
     * @param driver Database driver to use for migrations
     *               用于迁移的数据库驱动
     */
    fun init(driver: DatabaseDriver) {
        this.driver = driver
        createMigrationTable()
    }

    /**
     * Creates the migrations tracking table in the database.
     * This table stores information about executed migrations.
     * 
     * 在数据库中创建迁移跟踪表。
     * 此表存储有关已执行迁移的信息。
     */
    private fun createMigrationTable() {
        driver.createTable(
            "migrations",
            mapOf(
                "version" to "VARCHAR(50) PRIMARY KEY",
                "description" to "TEXT",
                "executed_at" to "TIMESTAMP"
            )
        )
    }

    /**
     * Registers a new migration with the manager.
     * Migrations are sorted by version for ordered execution.
     * 
     * 向管理器注册新的迁移。
     * 迁移按版本排序以便有序执行。
     *
     * @param migration Migration instance to register
     *                  要注册的迁移实例
     */
    fun register(migration: Migration) {
        migration.driver = driver
        migrations.add(migration)
        migrations.sortBy { it.version }
    }

    /**
     * Executes all pending migrations in version order.
     * Only executes migrations that haven't been previously executed.
     * 
     * 按版本顺序执行所有待处理的迁移。
     * 仅执行之前未执行过的迁移。
     */
    fun migrate() {
        val executedMigrations = getExecutedMigrations()
        val pendingMigrations = migrations.filter { it.version !in executedMigrations }

        if (pendingMigrations.isEmpty()) {
            Logger.info("No pending migrations")
            return
        }

        Logger.info("Executing ${pendingMigrations.size} pending migrations...")

        pendingMigrations.forEach { migration ->
            try {
                driver.beginTransaction()
                migration.up()
                recordMigration(migration)
                driver.commit()
                Logger.info("Migration ${migration.version} executed successfully")
            } catch (e: Exception) {
                driver.rollback()
                Logger.severe("Failed to execute migration ${migration.version}: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Rolls back a specified number of migrations in reverse order.
     * If steps is 0, rolls back all migrations.
     * 
     * 按相反顺序回滚指定数量的迁移。
     * 如果steps为0，则回滚所有迁移。
     *
     * @param steps Number of migrations to roll back (0 for all)
     *              要回滚的迁移数量（0表示全部）
     */
    fun rollback(steps: Int = 1) {
        val executedMigrations = getExecutedMigrations()
        val migrationsToRollback = migrations
            .filter { it.version in executedMigrations }
            .sortedByDescending { it.version }
            .let { if (steps > 0) it.take(steps) else it }

        if (migrationsToRollback.isEmpty()) {
            Logger.info("No migrations to rollback")
            return
        }

        Logger.info("Rolling back ${migrationsToRollback.size} migrations...")

        migrationsToRollback.forEach { migration ->
            try {
                driver.beginTransaction()
                migration.down()
                removeMigration(migration)
                driver.commit()
                Logger.info("Migration ${migration.version} rolled back successfully")
            } catch (e: Exception) {
                driver.rollback()
                Logger.severe("Failed to rollback migration ${migration.version}: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Retrieves a list of all executed migration versions.
     * 
     * 获取所有已执行迁移版本的列表。
     *
     * @return List of executed migration version strings
     *         已执行迁移版本字符串的列表
     */
    private fun getExecutedMigrations(): List<String> {
        return driver.query("SELECT version FROM migrations ORDER BY version")
            .column("version")
    }

    /**
     * Records a successfully executed migration in the tracking table.
     * 
     * 在跟踪表中记录成功执行的迁移。
     *
     * @param migration Migration that was executed
     *                  已执行的迁移
     */
    private fun recordMigration(migration: Migration) {
        migration.executedAt = LocalDateTime.now()
        driver.update(
            "INSERT INTO migrations (version, description, executed_at) VALUES (?, ?, ?)",
            migration.version,
            migration.description,
            migration.executedAt
        )
    }

    /**
     * Removes a migration record from the tracking table.
     * Used during rollback operations.
     * 
     * 从跟踪表中删除迁移记录。
     * 在回滚操作期间使用。
     *
     * @param migration Migration to remove from tracking
     *                  要从跟踪中删除的迁移
     */
    private fun removeMigration(migration: Migration) {
        driver.update("DELETE FROM migrations WHERE version = ?", migration.version)
        migration.executedAt = null
    }

    /**
     * Gets the current status of all registered migrations.
     * Shows which migrations have been executed and when.
     * 
     * 获取所有已注册迁移的当前状态。
     * 显示哪些迁移已执行以及执行时间。
     *
     * @return List of migration status information
     *         迁移状态信息列表
     */
    fun status(): List<MigrationStatus> {
        val executedMigrations = getExecutedMigrations()
        return migrations.map { migration ->
            MigrationStatus(
                version = migration.version,
                description = migration.description,
                executed = migration.version in executedMigrations,
                executedAt = migration.executedAt
            )
        }
    }
}

/**
 * Data class representing the status of a single migration.
 * Used for reporting migration execution status.
 * 
 * 表示单个迁移状态的数据类。
 * 用于报告迁移执行状态。
 *
 * @property version Migration version identifier
 *                   迁移版本标识符
 * @property description Human-readable description of the migration
 *                      迁移的人类可读描述
 * @property executed Whether this migration has been executed
 *                    此迁移是否已执行
 * @property executedAt When this migration was executed (null if not executed)
 *                     此迁移的执行时间（如果未执行则为null）
 */
data class MigrationStatus(
    val version: String,
    val description: String,
    val executed: Boolean,
    val executedAt: LocalDateTime?
) 