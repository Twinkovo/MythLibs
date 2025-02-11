package com.twinkovo.mythLibs.language.model

import com.twinkovo.mythLibs.language.annotations.Language
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * Class representing a language configuration
 * 语言配置类
 *
 * @property file The language file
 *                语言文件
 * @property config The YAML configuration
 *                  YAML配置
 * @property annotation The language annotation
 *                      语言注解
 */
data class LanguageConfig(
    val file: File,
    var config: YamlConfiguration,
    val annotation: Language
) {
    /**
     * The cache for messages
     * 消息缓存
     */
    private val messageCache = mutableMapOf<String, LanguageMessage>()

    /**
     * Gets a message from the configuration
     * 从配置中获取消息
     *
     * @param path The path to the message
     *             消息的路径
     * @return The language message, or null if not found
     *         语言消息，如果未找到则返回null
     */
    fun getMessage(path: String): LanguageMessage? {
        return messageCache.getOrPut(path) {
            val message = config.getString(path) ?: return null
            LanguageMessage(message)
        }
    }

    /**
     * Reloads the configuration from file
     * 从文件重新加载配置
     */
    fun reload() {
        config = YamlConfiguration.loadConfiguration(file)
        messageCache.clear()
    }

    /**
     * Saves the configuration to file
     * 保存配置到文件
     */
    fun save() {
        config.save(file)
    }

    /**
     * Gets the version of the language file
     * 获取语言文件的版本
     */
    fun getVersion(): Int {
        return config.getInt("version", 1)
    }

    /**
     * Sets the version of the language file
     * 设置语言文件的版本
     *
     * @param version The version to set
     *                要设置的版本
     */
    fun setVersion(version: Int) {
        config.set("version", version)
    }
} 