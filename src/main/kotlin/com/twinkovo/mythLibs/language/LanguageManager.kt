package com.twinkovo.mythLibs.language

import com.twinkovo.mythLibs.language.annotations.Language
import com.twinkovo.mythLibs.language.cloud.LanguageDownloader
import com.twinkovo.mythLibs.language.model.LanguageConfig
import com.twinkovo.mythLibs.language.model.LanguageMessage
import com.twinkovo.mythLibs.utils.Logger
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Manager class for handling language system
 * 语言系统管理器类
 */
object LanguageManager {
    private lateinit var plugin: Plugin
    private val languages = mutableMapOf<String, LanguageConfig>()
    private val playerLanguages = mutableMapOf<UUID, String>()
    private lateinit var defaultLanguage: String

    /**
     * Initializes the language manager
     * 初始化语言管理器
     *
     * @param plugin The plugin instance
     *               插件实例
     */
    fun init(plugin: Plugin) {
        this.plugin = plugin
        val languagesFolder = File(plugin.dataFolder, "languages")
        if (!languagesFolder.exists()) {
            languagesFolder.mkdirs()
        }
    }

    /**
     * Registers a language configuration class
     * 注册语言配置类
     *
     * @param languageClass The language configuration class to register
     *                      要注册的语言配置类
     */
    fun register(languageClass: KClass<*>) {
        val annotation = languageClass.findAnnotation<Language>() ?: throw IllegalArgumentException(
            "Language class must be annotated with @Language"
        )

        val file = getLanguageFile(annotation)
        
        // Try to download from cloud if URL is provided
        // 如果提供了URL，尝试从云端下载
        if (annotation.cloudUrl.isNotEmpty()) {
            LanguageDownloader.downloadLanguageFile(annotation, file).thenAccept { success ->
                if (success) {
                    loadLanguage(annotation, file)
                    if (annotation.autoUpdate) {
                        LanguageDownloader.scheduleAutoUpdate(annotation, file)
                    }
                } else if (!file.exists()) {
                    Logger.warn("Failed to download language file and no local file exists: ${annotation.name}")
                }
            }
        }
        
        // Load from local file if exists
        // 如果本地文件存在则从本地加载
        if (file.exists()) {
            loadLanguage(annotation, file)
        }
    }

    /**
     * Loads a language configuration
     * 加载语言配置
     *
     * @param annotation The language annotation
     *                   语言注解
     * @param file The language file
     *             语言文件
     */
    private fun loadLanguage(annotation: Language, file: File) {
        val config = YamlConfiguration.loadConfiguration(file)
        val languageConfig = LanguageConfig(file, config, annotation)
        languages[annotation.name] = languageConfig

        if (annotation.isDefault) {
            defaultLanguage = annotation.name
        }

        Logger.info("Loaded language: ${annotation.displayName} (${annotation.name})")
    }

    /**
     * Gets all available languages
     * 获取所有可用的语言
     *
     * @return Map of language codes to display names
     *         语言代码到显示名称的映射
     */
    fun getAvailableLanguages(): Map<String, String> {
        return languages.mapValues { it.value.annotation.displayName }
    }

    /**
     * Gets a message from the specified language
     * 从指定语言获取消息
     *
     * @param language The language to get the message from
     *                 要获取消息的语言
     * @param path The path to the message
     *             消息的路径
     * @param placeholders The placeholders to replace in the message
     *                     要在消息中替换的占位符
     * @return The language message, or null if not found
     *         语言消息，如果未找到则返回null
     */
    fun getMessage(language: String, path: String, placeholders: Map<String, Any> = emptyMap()): LanguageMessage? {
        val config = languages[language] ?: languages[defaultLanguage]
        ?: throw IllegalStateException("No default language configured")

        return config.getMessage(path)?.withPlaceholders(placeholders)
    }

    /**
     * Gets a message for a player
     * 获取玩家的消息
     *
     * @param player The player to get the message for
     *               要获取消息的玩家
     * @param path The path to the message
     *             消息的路径
     * @param placeholders The placeholders to replace in the message
     *                     要在消息中替换的占位符
     * @return The language message, or null if not found
     *         语言消息，如果未找到则返回null
     */
    fun getMessage(player: Player, path: String, placeholders: Map<String, Any> = emptyMap()): LanguageMessage? {
        val language = getPlayerLanguage(player)
        return getMessage(language, path, placeholders)
    }

    /**
     * Sets a player's preferred language
     * 设置玩家的首选语言
     *
     * @param player The player to set the language for
     *               要设置语言的玩家
     * @param language The language to set
     *                 要设置的语言
     * @return Whether the language was set successfully
     *         语言是否设置成功
     */
    fun setPlayerLanguage(player: Player, language: String): Boolean {
        return if (languages.containsKey(language)) {
            playerLanguages[player.uniqueId] = language
            true
        } else {
            false
        }
    }

    /**
     * Gets a player's preferred language
     * 获取玩家的首选语言
     *
     * @param player The player to get the language for
     *               要获取语言的玩家
     * @return The player's language, or the default language if not set
     *         玩家的语言，如果未设置则返回默认语言
     */
    fun getPlayerLanguage(player: Player): String {
        return playerLanguages[player.uniqueId] ?: defaultLanguage
    }

    /**
     * Reloads all language configurations
     * 重新加载所有语言配置
     */
    fun reload() {
        languages.values.forEach { config ->
            if (config.annotation.cloudUrl.isNotEmpty()) {
                LanguageDownloader.downloadLanguageFile(config.annotation, config.file)
                    .thenRun { config.reload() }
            } else {
                config.reload()
            }
        }
        Logger.info("Reloaded all language configurations")
    }

    /**
     * Gets the language file for a language annotation
     * 获取语言注解对应的语言文件
     *
     * @param annotation The language annotation
     *                   语言注解
     * @return The language file
     *         语言文件
     */
    private fun getLanguageFile(annotation: Language): File {
        val languagesFolder = File(plugin.dataFolder, "languages")
        val path = if (annotation.path.isNotEmpty()) File(languagesFolder, annotation.path) else languagesFolder
        path.mkdirs()
        return File(path, "${annotation.name}.yml")
    }

    /**
     * Shuts down the language manager
     * 关闭语言管理器
     */
    fun shutdown() {
        LanguageDownloader.shutdown()
    }
} 