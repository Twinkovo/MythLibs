package com.twinkovo.mythLibs.config.annotations

import com.twinkovo.mythLibs.config.validator.ConfigValidator
import com.twinkovo.mythLibs.config.validator.NoValidator
import kotlin.reflect.KClass

/**
 * Annotation to mark a property as a configuration value
 * 用于标记属性作为配置值的注解
 *
 * @property path The path to the value in the configuration file
 *                配置文件中该值的路径
 * @property comment The comment for this value
 *                   该配置值的注释
 * @property required Whether this value is required (cannot be null)
 *                    该配置值是否是必需的（不能为空）
 * @property validator The validator class to use for this value
 *                     用于验证该配置值的验证器类
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigValue(
    val path: String = "",
    val comment: Array<String> = [],
    val required: Boolean = true,
    val validator: KClass<out ConfigValidator<*>> = NoValidator::class
) 