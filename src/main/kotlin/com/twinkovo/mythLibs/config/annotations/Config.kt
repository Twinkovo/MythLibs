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
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Config(
    val name: String,
    val path: String = "",
    val header: Array<String> = []
) 