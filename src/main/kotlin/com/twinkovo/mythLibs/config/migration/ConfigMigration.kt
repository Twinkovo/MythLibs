package com.twinkovo.mythLibs.config.migration

import org.bukkit.configuration.ConfigurationSection

/**
 * Interface for configuration migration
 * 配置迁移接口
 */
interface ConfigMigration {
    /**
     * Get the version this migration upgrades from
     * 获取此迁移从哪个版本升级
     */
    val fromVersion: Int

    /**
     * Get the version this migration upgrades to
     * 获取此迁移升级到哪个版本
     */
    val toVersion: Int

    /**
     * Migrate the configuration from one version to another
     * 将配置从一个版本迁移到另一个版本
     *
     * @param config The configuration section to migrate
     *               要迁移的配置节
     * @return true if migration was successful, false otherwise
     *         如果迁移成功则返回true，否则返回false
     */
    fun migrate(config: ConfigurationSection): Boolean
}

/**
 * Abstract base class for configuration migrations
 * 配置迁移的抽象基类
 */
abstract class BaseConfigMigration(
    override val fromVersion: Int,
    override val toVersion: Int
) : ConfigMigration {
    init {
        require(toVersion > fromVersion) { "Target version must be greater than source version" }
    }
}

/**
 * Registry for configuration migrations
 * 配置迁移注册表
 */
object MigrationRegistry {
    private val migrations = mutableMapOf<Int, MutableSet<ConfigMigration>>()

    /**
     * Register a migration
     * 注册迁移
     *
     * @param migration The migration to register
     *                  要注册的迁移
     */
    fun register(migration: ConfigMigration) {
        migrations.getOrPut(migration.fromVersion) { mutableSetOf() }.add(migration)
    }

    /**
     * Get all migrations from a specific version
     * 获取从特定版本开始的所有迁移
     *
     * @param fromVersion The version to migrate from
     *                    要迁移的起始版本
     * @return Set of migrations starting from the specified version
     *         从指定版本开始的迁移集合
     */
    fun getMigrations(fromVersion: Int): Set<ConfigMigration> {
        return migrations[fromVersion] ?: emptySet()
    }

    /**
     * Find the best migration path between versions
     * 查找版本之间的最佳迁移路径
     *
     * @param fromVersion Starting version
     *                    起始版本
     * @param toVersion Target version
     *                  目标版本
     * @return List of migrations in order, or null if no path exists
     *         按顺序排列的迁移列表，如果不存在路径则返回null
     */
    fun findMigrationPath(fromVersion: Int, toVersion: Int): List<ConfigMigration>? {
        if (fromVersion >= toVersion) return emptyList()
        
        val visited = mutableSetOf<Int>()
        val path = mutableListOf<ConfigMigration>()
        
        fun dfs(currentVersion: Int): Boolean {
            if (currentVersion == toVersion) return true
            if (currentVersion > toVersion || currentVersion in visited) return false
            
            visited.add(currentVersion)
            
            val possibleMigrations = getMigrations(currentVersion)
                .filter { it.toVersion <= toVersion }
                .sortedBy { it.toVersion }
                
            for (migration in possibleMigrations) {
                path.add(migration)
                if (dfs(migration.toVersion)) return true
                path.removeLast()
            }
            
            return false
        }
        
        return if (dfs(fromVersion)) path else null
    }

    /**
     * Clear all registered migrations
     * 清除所有注册的迁移
     */
    fun clear() {
        migrations.clear()
    }
} 