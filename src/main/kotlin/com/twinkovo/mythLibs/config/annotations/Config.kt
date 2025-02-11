package com.twinkovo.mythLibs.config.annotations

/**
 * Annotation to mark a class as a configuration file
 * 用于标记一个类作为配置文件的注解
 *
 * @property name The name of the configuration file (without extension)
 *                配置文件的名称（不包含扩展名）
 * @property path The path to the configuration file (relative to plugin data folder)
 *                配置文件的路径（相对于插件数据文件夹）
 * @property header The header comment of the configuration file
 *                  配置文件的头部注释
 * @property version The version of the configuration file
 *                   配置文件的版本号
 * @property versionKey The key in the configuration file that stores the version
 *                      配置文件中存储版本号的键
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Config(
    val name: String,
    val path: String = "",
    val header: Array<String> = [],
    val version: Int = 1,
    val versionKey: String = "config-version"
) 