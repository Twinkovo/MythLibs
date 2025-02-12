package com.twinkovo.mythLibs.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.awt.Color
import kotlin.math.sin

/**
 * A comprehensive utility class for handling various types of color formats in text.
 * 全面的文本颜色处理工具类
 *
 * Supports the following color formats:
 * 支持以下颜色格式：
 * 1. Standard colors (&3, &b, <italic>, <gold>, etc.)
 *    标准颜色 (&3、&b、<italic>、<gold> 等)
 * 2. Hex colors (<#XXXXXX>)
 *    十六进制颜色 (<#XXXXXX>)
 * 3. Gradient colors (<gradient:#COLOR1:#COLOR2>text</gradient>)
 *    渐变颜色 (<gradient:#颜色1:#颜色2>文本</gradient>)
 * 4. Rainbow colors (<rainbow>text</rainbow>)
 *    彩虹颜色 (<rainbow>文本</rainbow>)
 */
object ColorUtil {
    // Regular expressions for pattern matching
    // 用于模式匹配的正则表达式
    private val GRADIENT_PATTERN = Regex("<(?<type>gradient|g)(#(?<speed>\\d+))?(?<hex>(:#([A-Fa-f\\d]{6}|[A-Fa-f\\d]{3})){2,})(:(?<loop>l|L|loop))?>")
    private val RAINBOW_PATTERN = Regex("<(?<type>rainbow|r)(#(?<speed>\\d+))?(:(?<saturation>\\d*\\.?\\d+))?(:(?<brightness>\\d*\\.?\\d+))?(:(?<loop>l|L|loop))?>")
    private val HEX_PATTERN = Regex("<#([A-Fa-f0-9]{6})>")
    
    /**
     * Converts a legacy color code string to MiniMessage format
     * 将传统颜色代码字符串转换为 MiniMessage 格式
     *
     * @param text The text to process 要处理的文本
     * @return Processed text 处理后的文本
     */
    fun legacyToMini(text: String): String {
        return text.replace("&", "§")
            .replace("§0", "<black>")
            .replace("§1", "<dark_blue>")
            .replace("§2", "<dark_green>")
            .replace("§3", "<dark_aqua>")
            .replace("§4", "<dark_red>")
            .replace("§5", "<dark_purple>")
            .replace("§6", "<gold>")
            .replace("§7", "<gray>")
            .replace("§8", "<dark_gray>")
            .replace("§9", "<blue>")
            .replace("§a", "<green>")
            .replace("§b", "<aqua>")
            .replace("§c", "<red>")
            .replace("§d", "<light_purple>")
            .replace("§e", "<yellow>")
            .replace("§f", "<white>")
            .replace("§k", "<obfuscated>")
            .replace("§l", "<bold>")
            .replace("§m", "<strikethrough>")
            .replace("§n", "<underlined>")
            .replace("§o", "<italic>")
            .replace("§r", "<reset>")
    }

    /**
     * Processes all color formats in the text
     * 处理文本中的所有颜色格式
     *
     * @param text The text to process 要处理的文本
     * @return Component with processed colors 处理后的带颜色组件
     */
    fun process(text: String): Component {
        var processed = text
        processed = legacyToMini(processed)
        processed = processGradients(processed)
        processed = processRainbow(processed)
        return MiniMessage.miniMessage().deserialize(processed)
    }

    /**
     * Processes gradient color patterns in text
     * 处理文本中的渐变颜色模式
     *
     * @param text The text to process 要处理的文本
     * @return Processed text 处理后的文本
     */
    private fun processGradients(text: String): String {
        var result = text
        val matches = GRADIENT_PATTERN.findAll(text)
        
        for (match in matches) {
            val fullMatch = match.value
            val content = result.substring(
                result.indexOf(fullMatch) + fullMatch.length,
                result.indexOf("</gradient>", result.indexOf(fullMatch))
            )
            
            val colors = match.groups["hex"]?.value
                ?.split(":")
                ?.filter { it.isNotEmpty() }
                ?.map { if (it.startsWith("#")) it else "#$it" }
                ?: continue
                
            val speed = match.groups["speed"]?.value?.toIntOrNull() ?: 1
            val isLoop = match.groups["loop"]?.value != null
            
            result = result.replace(
                "$fullMatch$content</gradient>",
                createGradient(content, colors, speed, isLoop)
            )
        }
        
        return result
    }

    /**
     * Processes rainbow color patterns in text
     * 处理文本中的彩虹颜色模式
     *
     * @param text The text to process 要处理的文本
     * @return Processed text 处理后的文本
     */
    private fun processRainbow(text: String): String {
        var result = text
        val matches = RAINBOW_PATTERN.findAll(text)
        
        for (match in matches) {
            val fullMatch = match.value
            val content = result.substring(
                result.indexOf(fullMatch) + fullMatch.length,
                result.indexOf("</rainbow>", result.indexOf(fullMatch))
            )
            
            val speed = match.groups["speed"]?.value?.toIntOrNull() ?: 1
            val saturation = match.groups["saturation"]?.value?.toFloatOrNull() ?: 1f
            val brightness = match.groups["brightness"]?.value?.toFloatOrNull() ?: 1f
            val isLoop = match.groups["loop"]?.value != null
            
            result = result.replace(
                "$fullMatch$content</rainbow>",
                createRainbow(content, speed, saturation, brightness, isLoop)
            )
        }
        
        return result
    }

    /**
     * Creates a gradient color effect
     * 创建渐变颜色效果
     *
     * @param text The text to apply gradient to 要应用渐变的文本
     * @param colors List of hex colors 十六进制颜色列表
     * @param speed Animation speed 动画速度
     * @param loop Whether to loop the gradient 是否循环渐变
     * @return Formatted text with gradient 带渐变效果的格式化文本
     */
    private fun createGradient(text: String, colors: List<String>, speed: Int, loop: Boolean): String {
        val chars = text.toCharArray()
        val result = StringBuilder()
        
        for ((index, char) in chars.withIndex()) {
            val percent = index.toFloat() / chars.size
            val colorIndex = (percent * (colors.size - 1)).toInt()
            val color1 = Color.decode(colors[colorIndex])
            val color2 = Color.decode(colors[minOf(colorIndex + 1, colors.size - 1)])
            
            val ratio = (percent * (colors.size - 1)) % 1
            val r = (color1.red * (1 - ratio) + color2.red * ratio).toInt()
            val g = (color1.green * (1 - ratio) + color2.green * ratio).toInt()
            val b = (color1.blue * (1 - ratio) + color2.blue * ratio).toInt()
            
            result.append("<#${String.format("%02x%02x%02x", r, g, b)}>$char")
        }
        
        return result.toString()
    }

    /**
     * Creates a rainbow color effect
     * 创建彩虹颜色效果
     *
     * @param text The text to apply rainbow to 要应用彩虹效果的文本
     * @param speed Animation speed 动画速度
     * @param saturation Color saturation 颜色饱和度
     * @param brightness Color brightness 颜色亮度
     * @param loop Whether to loop the rainbow 是否循环彩虹效果
     * @return Formatted text with rainbow 带彩虹效果的格式化文本
     */
    private fun createRainbow(
        text: String,
        speed: Int,
        saturation: Float,
        brightness: Float,
        loop: Boolean
    ): String {
        val chars = text.toCharArray()
        val result = StringBuilder()
        
        for ((index, char) in chars.withIndex()) {
            val hue = (index.toFloat() / chars.size + sin(speed.toDouble() / 10) * 0.5).toFloat()
            val color = Color.getHSBColor(hue, saturation, brightness)
            result.append("<#${String.format("%02x%02x%02x", color.red, color.green, color.blue)}>$char")
        }
        
        return result.toString()
    }
} 