package com.twinkovo.mythLibs.config.annotations

/**
 * Annotation to mark a nested class or property as a configuration section
 * 用于标记嵌套类或属性作为配置节的注解
 *
 * @property path The path to the section in the configuration file
 *                配置文件中该节的路径
 * @property comment The comment for this section
 *                   该配置节的注释
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigSection(
    val path: String = "",
    val comment: Array<String> = []
) 