package com.twinkovo.mythLibs.config.validator

import java.net.InetAddress
import java.net.URI
import java.util.regex.Pattern

/**
 * Number range validator
 * 数值范围验证器
 *
 * @param min Minimum value 最小值
 * @param max Maximum value 最大值
 */
class RangeValidator(private val min: Number, private val max: Number) : ConfigValidator<Number> {
    override fun validate(value: Number): Boolean {
        val doubleValue = value.toDouble()
        return doubleValue in min.toDouble()..max.toDouble()
    }

    override fun getErrorMessage(value: Number): String =
        "Value must be between $min and $max, but got $value"
}

/**
 * String length validator
 * 字符串长度验证器
 *
 * @param minLength Minimum length 最小长度
 * @param maxLength Maximum length 最大长度
 */
class StringLengthValidator(
    private val minLength: Int,
    private val maxLength: Int
) : ConfigValidator<String> {
    override fun validate(value: String): Boolean =
        value.length in minLength..maxLength

    override fun getErrorMessage(value: String): String =
        "String length must be between $minLength and $maxLength, but got ${value.length}"
}

/**
 * Regex pattern validator
 * 正则表达式验证器
 *
 * @param pattern Regex pattern 正则表达式模式
 * @param description Pattern description 模式描述
 */
class RegexValidator(
    private val pattern: String,
    private val description: String
) : ConfigValidator<String> {
    private val compiledPattern = Pattern.compile(pattern)

    override fun validate(value: String): Boolean =
        compiledPattern.matcher(value).matches()

    override fun getErrorMessage(value: String): String =
        "Value must match pattern: $description"
}

/**
 * Email address validator
 * 电子邮件地址验证器
 */
class EmailValidator : ConfigValidator<String> {
    private val emailPattern = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
        "@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    )

    override fun validate(value: String): Boolean =
        emailPattern.matcher(value).matches()

    override fun getErrorMessage(value: String): String =
        "Invalid email address: $value"
}

/**
 * URL validator
 * URL验证器
 *
 * @param allowedSchemes Allowed URL schemes 允许的URL协议
 */
class UrlValidator(
    private val allowedSchemes: Set<String> = setOf("http", "https")
) : ConfigValidator<String> {
    override fun validate(value: String): Boolean {
        return try {
            val uri = URI(value)
            uri.scheme in allowedSchemes
        } catch (e: Exception) {
            false
        }
    }

    override fun getErrorMessage(value: String): String =
        "Invalid URL or unsupported scheme (allowed: ${allowedSchemes.joinToString(", ")}): $value"
}

/**
 * IP address validator
 * IP地址验证器
 *
 * @param allowIPv6 Whether to allow IPv6 addresses 是否允许IPv6地址
 */
class IpAddressValidator(
    private val allowIPv6: Boolean = true
) : ConfigValidator<String> {
    override fun validate(value: String): Boolean {
        return try {
            val addr = InetAddress.getByName(value)
            allowIPv6 || addr.address.size == 4
        } catch (e: Exception) {
            false
        }
    }

    override fun getErrorMessage(value: String): String =
        "Invalid IP address: $value"
}

/**
 * Port number validator
 * 端口号验证器
 */
class PortValidator : ConfigValidator<Int> {
    override fun validate(value: Int): Boolean =
        value in 1..65535

    override fun getErrorMessage(value: Int): String =
        "Port number must be between 1 and 65535"
}

/**
 * Enum value validator
 * 枚举值验证器
 *
 * @param enumClass Enum class to validate against 用于验证的枚举类
 */
class EnumValidator<T : Enum<T>>(
    private val enumClass: Class<T>
) : ConfigValidator<String> {
    override fun validate(value: String): Boolean =
        try {
            java.lang.Enum.valueOf(enumClass, value.uppercase())
            true
        } catch (e: IllegalArgumentException) {
            false
        }

    override fun getErrorMessage(value: String): String =
        "Value must be one of: ${enumClass.enumConstants.joinToString(", ") { it.name }}"
}

/**
 * List size validator
 * 列表大小验证器
 *
 * @param minSize Minimum size 最小大小
 * @param maxSize Maximum size 最大大小
 */
class ListSizeValidator(
    private val minSize: Int,
    private val maxSize: Int
) : ConfigValidator<List<*>> {
    override fun validate(value: List<*>): Boolean =
        value.size in minSize..maxSize

    override fun getErrorMessage(value: List<*>): String =
        "List size must be between $minSize and $maxSize, but got ${value.size}"
} 