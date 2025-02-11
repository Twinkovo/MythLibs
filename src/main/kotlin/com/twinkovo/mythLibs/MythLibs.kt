package com.twinkovo.mythLibs

import com.twinkovo.mythLibs.utils.Logger
import org.bukkit.plugin.java.JavaPlugin

class MythLibs : JavaPlugin() {

    override fun onEnable() {
        // Initialize logger with debug mode disabled
        // 初始化日志记录器，禁用调试模式
        Logger.init(this)
        
        // Test logging
        // 测试日志记录
        Logger.info("MythLibs has been enabled!")
    }

    override fun onDisable() {
        // Log shutdown message and close logger
        // 记录关闭消息并关闭日志记录器
        Logger.info("MythLibs has been disabled!")
        Logger.close()
    }
}
