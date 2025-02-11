package com.twinkovo.mythLibs.config

import com.twinkovo.mythLibs.config.annotations.Config
import com.twinkovo.mythLibs.config.annotations.ConfigSection
import com.twinkovo.mythLibs.config.annotations.ConfigValue
import com.twinkovo.mythLibs.config.migration.MigrationRegistry
import com.twinkovo.mythLibs.utils.Logger
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Manager class for handling configuration files
 * 配置文件管理器类
 *
 * Features:
 * 特性：
 * 1. Load and save configuration files
 *    加载和保存配置文件
 * 2. Validate configuration values
 *    验证配置值
 * 3. Auto-complete missing values
 *    自动补全缺失的值
 * 4. Support for comments
 *    支持注释
 * 5. Version control and migration
 *    版本控制和迁移
 */
object ConfigManager {
    private val configs = mutableMapOf<KClass<*>, Any>()
    private lateinit var plugin: Plugin

    /**
     * Initializes the config manager
     * 初始化配置管理器
     *
     * @param plugin The plugin instance
     *               插件实例
     */
    fun init(plugin: Plugin) {
        this.plugin = plugin
    }

    /**
     * Registers a configuration class
     * 注册配置类
     *
     * @param configClass The configuration class to register
     *                    要注册的配置类
     * @return The loaded configuration instance
     *         加载的配置实例
     */
    fun <T : Any> register(configClass: KClass<T>): T {
        val config = configClass.findAnnotation<Config>() ?: throw IllegalArgumentException(
            "Configuration class must be annotated with @Config"
        )

        val file = getConfigFile(config)
        val yamlConfig = YamlConfiguration.loadConfiguration(file)

        // Check and handle version migration
        // 检查并处理版本迁移
        handleVersionMigration(yamlConfig, config)

        // Create instance and load values
        // 创建实例并加载值
        val instance = configClass.constructors.first().call()
        if (loadConfig(instance, yamlConfig)) {
            // Save if changes were made during loading
            // 如果在加载过程中有更改则保存
            saveConfig(instance, yamlConfig)
            yamlConfig.save(file)
        }

        configs[configClass] = instance
        return instance
    }

    /**
     * Handles version migration for a configuration
     * 处理配置的版本迁移
     *
     * @param yaml The configuration to migrate
     *             要迁移的配置
     * @param config The configuration annotation
     *               配置注解
     */
    private fun handleVersionMigration(yaml: YamlConfiguration, config: Config) {
        val currentVersion = yaml.getInt(config.versionKey, 1)
        val targetVersion = config.version

        if (currentVersion < targetVersion) {
            // Create backup before migration
            // 迁移前创建备份
            createBackup(getConfigFile(config))

            // Find and execute migration path
            // 查找并执行迁移路径
            val migrationPath = MigrationRegistry.findMigrationPath(currentVersion, targetVersion)
            if (migrationPath != null) {
                Logger.info("Migrating configuration '${config.name}' from version $currentVersion to $targetVersion")
                
                for (migration in migrationPath) {
                    if (!migration.migrate(yaml)) {
                        Logger.warn("Failed to migrate configuration '${config.name}' from version ${migration.fromVersion} to ${migration.toVersion}")
                        return
                    }
                    Logger.info("Successfully migrated from version ${migration.fromVersion} to ${migration.toVersion}")
                }
                
                // Update version in configuration
                // 更新配置中的版本号
                yaml.set(config.versionKey, targetVersion)
            } else {
                Logger.warn("No migration path found for configuration '${config.name}' from version $currentVersion to $targetVersion")
            }
        }
    }

    /**
     * Creates a backup of a configuration file
     * 创建配置文件的备份
     *
     * @param file The file to back up
     *             要备份的文件
     */
    private fun createBackup(file: File) {
        if (!file.exists()) return

        val backupDir = File(file.parentFile, "backups")
        backupDir.mkdirs()

        val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val backupFile = File(backupDir, "${file.nameWithoutExtension}_$timestamp.yml")

        file.copyTo(backupFile, overwrite = true)
        Logger.info("Created backup: ${backupFile.name}")
    }

    /**
     * Gets a registered configuration instance
     * 获取已注册的配置实例
     *
     * @param configClass The configuration class
     *                    配置类
     * @return The configuration instance
     *         配置实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(configClass: KClass<T>): T {
        return configs[configClass] as? T ?: throw IllegalStateException(
            "Configuration class ${configClass.simpleName} is not registered"
        )
    }

    private fun getConfigFile(config: Config): File {
        val dataFolder = plugin.dataFolder
        val path = if (config.path.isNotEmpty()) File(dataFolder, config.path) else dataFolder
        path.mkdirs()
        return File(path, "${config.name}.yml")
    }

    private fun loadConfig(instance: Any, yaml: YamlConfiguration): Boolean {
        var needsSave = false

        for (prop in instance::class.memberProperties) {
            val value = prop.findAnnotation<ConfigValue>()
            if (value != null) {
                val path = value.path.ifEmpty { prop.name }
                
                if (yaml.contains(path)) {
                    // Validate existing value
                    // 验证现有值
                    val loadedValue = yaml.get(path)
                    if (!validateValue(loadedValue, value, prop)) {
                        Logger.warn("Invalid value for ${prop.name} in config: $loadedValue")
                        // If validation fails and it's required, set default value
                        // 如果验证失败且该值是必需的，设置默认值
                        if (value.required) {
                            val defaultValue = (prop as KProperty<*>).getter.call(instance)
                            yaml.set(path, defaultValue)
                            needsSave = true
                            Logger.info("Set default value for ${prop.name}: $defaultValue")
                        }
                    }
                } else {
                    // Value missing, set default if required
                    // 值缺失，如果是必需的则设置默认值
                    if (value.required) {
                        val defaultValue = (prop as KProperty<*>).getter.call(instance)
                        yaml.set(path, defaultValue)
                        if (value.comment.isNotEmpty()) {
                            setComments(yaml, path, value.comment)
                        }
                        needsSave = true
                        Logger.info("Added missing required value for ${prop.name}: $defaultValue")
                    }
                }
            }

            val section = prop.findAnnotation<ConfigSection>()
            if (section != null) {
                val path = section.path.ifEmpty { prop.name }
                if (!yaml.contains(path)) {
                    yaml.createSection(path)
                    if (section.comment.isNotEmpty()) {
                        setComments(yaml, path, section.comment)
                    }
                    needsSave = true
                    Logger.info("Created missing section: $path")
                }

                // Recursively load nested configuration
                // 递归加载嵌套配置
                val sectionValue = (prop as KProperty<*>).getter.call(instance)
                if (sectionValue != null) {
                    val nestedNeedsSave = loadConfig(sectionValue, (yaml.getConfigurationSection(path)
                        ?: yaml.createSection(path)) as YamlConfiguration
                    )
                    needsSave = needsSave || nestedNeedsSave
                }
            }
        }

        return needsSave
    }

    private fun saveConfig(instance: Any, yaml: YamlConfiguration) {
        // Add file header if present
        // 如果存在则添加文件头注释
        val config = instance::class.findAnnotation<Config>()
        if (config != null && config.header.isNotEmpty()) {
            // Use root path (empty string) for header comments
            // 使用根路径（空字符串）设置头部注释
            yaml.setComments("", config.header.toList())
        }

        for (prop in instance::class.memberProperties) {
            val value = prop.findAnnotation<ConfigValue>()
            if (value != null) {
                val path = value.path.ifEmpty { prop.name }
                // Always save current values to ensure config is up to date
                // 始终保存当前值以确保配置是最新的
                val currentValue = (prop as KProperty<*>).getter.call(instance)
                yaml.set(path, currentValue)
                if (value.comment.isNotEmpty()) {
                    setComments(yaml, path, value.comment)
                }
            }

            val section = prop.findAnnotation<ConfigSection>()
            if (section != null) {
                val path = section.path.ifEmpty { prop.name }
                if (section.comment.isNotEmpty()) {
                    setComments(yaml, path, section.comment)
                }
                
                // Recursively save nested configuration
                // 递归保存嵌套配置
                val sectionValue = (prop as KProperty<*>).getter.call(instance)
                if (sectionValue != null) {
                    val sectionYaml = yaml.getConfigurationSection(path) ?: yaml.createSection(path)
                    saveConfig(sectionValue, sectionYaml as YamlConfiguration)
                }
            }
        }
    }

    private fun validateValue(value: Any?, configValue: ConfigValue, prop: KProperty<*>): Boolean {
        if (value == null && configValue.required) {
            return false
        }

        val validator = configValue.validator.constructors.first().call()
        return when {
            value == null -> !configValue.required
            else -> {
                try {
                    // 使用反射调用validate方法，避免类型投影问题
                    val validateMethod = validator::class.members.find { it.name == "validate" }
                    validateMethod?.call(validator, value) as? Boolean ?: false
                } catch (e: Exception) {
                    Logger.warn("Failed to validate value for ${prop.name}: ${e.message}")
                    false
                }
            }
        }
    }

    private fun setComments(yaml: YamlConfiguration, path: String, comments: Array<String>) {
        yaml.setComments(path, comments.toList())
    }
} 