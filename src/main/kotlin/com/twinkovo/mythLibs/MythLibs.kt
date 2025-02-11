package com.twinkovo.mythLibs

import com.twinkovo.mythLibs.language.LanguageManager
import com.twinkovo.mythLibs.utils.Logger
import org.bukkit.plugin.java.JavaPlugin

class MythLibs : JavaPlugin() {

    override fun onEnable() {
        // Initialize logger with debug mode disabled
        // 初始化日志记录器，禁用调试模式
        Logger.init(this)
        
        // Initialize language system
        // 初始化语言系统
        LanguageManager.init(this)
        
        // Register language files
        // 注册语言文件
        registerLanguages()
        
        // Test logging
        // 测试日志记录
        Logger.info("MythLibs has been enabled!")
    }

    override fun onDisable() {
        // Shutdown language manager
        // 关闭语言管理器
        LanguageManager.shutdown()
        
        // Log shutdown message and close logger
        // 记录关闭消息并关闭日志记录器
        Logger.info("MythLibs has been disabled!")
        Logger.close()
    }

    /**
     * Register language files
     * 注册语言文件
     */
    private fun registerLanguages() {
        // Register language configurations
        // 注册语言配置
        LanguageManager.register(ZhCNLanguage::class)
        LanguageManager.register(EnUSLanguage::class)
    }
}

@com.twinkovo.mythLibs.language.annotations.Language(
    name = "zh_CN",
    displayName = "简体中文",
    version = 1,
    isDefault = true,
    cloudUrl = "https://raw.githubusercontent.com/YourRepo/MythLibs/main/languages/zh_CN.yml",
    autoUpdate = true,
    updateInterval = 60
)
class ZhCNLanguage

@com.twinkovo.mythLibs.language.annotations.Language(
    name = "en_US",
    displayName = "English",
    version = 1,
    cloudUrl = "https://raw.githubusercontent.com/YourRepo/MythLibs/main/languages/en_US.yml",
    autoUpdate = true,
    updateInterval = 60
)
class EnUSLanguage
