package com.twinkovo.mythLibs.database.migration

import com.twinkovo.mythLibs.database.driver.DatabaseDriver
import java.time.LocalDateTime

/**
 * Abstract base class for database migrations.
 * Provides a framework for implementing database schema changes in a versioned and reversible way.
 * 
 * 数据库迁移的抽象基类。
 * 提供了一个框架，用于以版本化和可逆的方式实现数据库架构更改。
 *
 * @property version Migration version identifier, should be unique across all migrations
 *                   迁移版本标识符，在所有迁移中应该是唯一的
 * @property description Human-readable description of what this migration does
 *                      此迁移作用的人类可读描述
 */
abstract class Migration(
    val version: String,
    val description: String
) {
    /**
     * Database driver instance to be used for executing migration operations.
     * Set by the migration manager before executing the migration.
     * 
     * 用于执行迁移操作的数据库驱动实例。
     * 在执行迁移之前由迁移管理器设置。
     */
    lateinit var driver: DatabaseDriver

    /**
     * Timestamp when this migration was executed.
     * Set by the migration manager after successful execution.
     * 
     * 此迁移执行的时间戳。
     * 成功执行后由迁移管理器设置。
     */
    var executedAt: LocalDateTime? = null

    /**
     * Implements the forward migration operation.
     * Should contain all necessary database changes for this version.
     * 
     * 实现正向迁移操作。
     * 应包含此版本的所有必要数据库更改。
     */
    abstract fun up()

    /**
     * Implements the rollback operation.
     * Should reverse all changes made in the up() method.
     * 
     * 实现回滚操作。
     * 应该撤销up()方法中所做的所有更改。
     */
    abstract fun down()

    /**
     * Returns a string representation of this migration.
     * Includes version, description, and execution timestamp if available.
     * 
     * 返回此迁移的字符串表示。
     * 包括版本、描述和执行时间戳（如果可用）。
     *
     * @return String representation of the migration
     *         迁移的字符串表示
     */
    override fun toString(): String {
        return "Migration(version='$version', description='$description', executedAt=$executedAt)"
    }
} 