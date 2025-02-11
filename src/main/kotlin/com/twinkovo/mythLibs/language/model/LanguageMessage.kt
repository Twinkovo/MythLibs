package com.twinkovo.mythLibs.language.model

import com.twinkovo.mythLibs.utils.ColorUtil
import net.kyori.adventure.text.Component

/**
 * Class representing a language message
 * 语言消息实体类
 *
 * @property raw The raw message string
 *               原始消息字符串
 * @property placeholders The placeholders map
 *                        占位符映射
 */
data class LanguageMessage(
    val raw: String,
    private val placeholders: Map<String, Any> = emptyMap()
) {
    /**
     * Gets the processed message with placeholders replaced and colors applied
     * 获取处理后的消息（替换占位符并应用颜色）
     *
     * @return The processed message component
     *         处理后的消息组件
     */
    fun getMessage(): Component {
        var processed = raw
        
        // Replace placeholders
        // 替换占位符
        placeholders.forEach { (key, value) ->
            processed = processed.replace("{$key}", value.toString())
        }
        
        // Process colors and return component
        // 处理颜色并返回组件
        return ColorUtil.process(processed)
    }

    /**
     * Gets the raw string with only placeholders replaced
     * 获取仅替换占位符的原始字符串
     *
     * @return The processed string
     *         处理后的字符串
     */
    fun getRawString(): String {
        var processed = raw
        placeholders.forEach { (key, value) ->
            processed = processed.replace("{$key}", value.toString())
        }
        return processed
    }

    /**
     * Creates a new message with additional placeholders
     * 创建带有额外占位符的新消息
     *
     * @param additionalPlaceholders The additional placeholders to add
     *                              要添加的额外占位符
     * @return A new message with combined placeholders
     *         带有合并占位符的新消息
     */
    fun withPlaceholders(additionalPlaceholders: Map<String, Any>): LanguageMessage {
        return LanguageMessage(raw, placeholders + additionalPlaceholders)
    }
} 