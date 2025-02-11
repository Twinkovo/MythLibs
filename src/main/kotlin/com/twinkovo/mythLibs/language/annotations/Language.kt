package com.twinkovo.mythLibs.language.annotations

/**
 * Annotation to mark a class as a language configuration
 * 用于标记一个类作为语言配置的注解
 *
 * @property name The name of the language (e.g., "zh_CN", "en_US")
 *                语言名称（例如："zh_CN"、"en_US"）
 * @property displayName The display name of the language (e.g., "简体中文", "English")
 *                       语言的显示名称（例如："简体中文"、"English"）
 * @property path The path to the language file (relative to plugin languages folder)
 *                语言文件的路径（相对于插件语言文件夹）
 * @property version The version of the language file
 *                   语言文件的版本号
 * @property isDefault Whether this is the default language
 *                     是否为默认语言
 * @property cloudUrl The URL to download the language file from cloud
 *                    从云端下载语言文件的URL
 * @property autoUpdate Whether to automatically update the language file from cloud
 *                      是否自动从云端更新语言文件
 * @property updateInterval The interval (in minutes) to check for updates
 *                          检查更新的间隔（分钟）
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Language(
    val name: String,
    val displayName: String,
    val path: String = "",
    val version: Int = 1,
    val isDefault: Boolean = false,
    val cloudUrl: String = "",
    val autoUpdate: Boolean = false,
    val updateInterval: Long = 60
) 