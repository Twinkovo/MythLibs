package com.twinkovo.mythLibs.config

import com.twinkovo.mythLibs.config.annotations.Config
import com.twinkovo.mythLibs.config.annotations.ConfigSection
import com.twinkovo.mythLibs.config.annotations.ConfigValue
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

        // Create instance and load values
        // 创建实例并加载值
        val instance = configClass.constructors.first().call()
        loadConfig(instance, yamlConfig)

        // Save config to ensure all values are present
        // 保存配置以确保所有值都存在
        saveConfig(instance, yamlConfig)
        yamlConfig.save(file)

        configs[configClass] = instance
        return instance
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
                    val nestedNeedsSave = loadConfig(sectionValue, yaml.getConfigurationSection(path) 
                        ?: yaml.createSection(path))
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
                    saveConfig(sectionValue, sectionYaml)
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