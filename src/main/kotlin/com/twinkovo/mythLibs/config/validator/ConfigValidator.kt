package com.twinkovo.mythLibs.config.validator

/**
 * Interface for configuration value validators
 * 配置值验证器接口
 *
 * @param T The type of value to validate
 *          要验证的值的类型
 */
interface ConfigValidator<T> {
    /**
     * Validates the given value
     * 验证给定的值
     *
     * @param value The value to validate
     *              要验证的值
     * @return true if the value is valid, false otherwise
     *         如果值有效则返回true，否则返回false
     */
    fun validate(value: T): Boolean

    /**
     * Gets the error message for an invalid value
     * 获取无效值的错误消息
     *
     * @param value The invalid value
     *              无效的值
     * @return The error message
     *         错误消息
     */
    fun getErrorMessage(value: T): String
}

/**
 * Default validator that accepts any value
 * 接受任何值的默认验证器
 */
class NoValidator<T> : ConfigValidator<T> {
    override fun validate(value: T): Boolean = true
    override fun getErrorMessage(value: T): String = ""
} 