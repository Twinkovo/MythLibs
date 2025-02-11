package com.twinkovo.mythLibs.language.cloud

import com.twinkovo.mythLibs.language.annotations.Language
import com.twinkovo.mythLibs.utils.Logger
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Class for handling cloud language file downloads
 * 处理云端语言文件下载的类
 */
object LanguageDownloader {
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val updateTasks = mutableMapOf<String, CompletableFuture<Boolean>>()
    
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * Downloads a language file from cloud
     * 从云端下载语言文件
     *
     * @param annotation The language annotation
     *                   语言注解
     * @param targetFile The target file to save to
     *                   要保存到的目标文件
     * @return CompletableFuture<Boolean> indicating success
     *         表示成功的CompletableFuture<Boolean>
     */
    fun downloadLanguageFile(annotation: Language, targetFile: File): CompletableFuture<Boolean> {
        if (annotation.cloudUrl.isEmpty()) {
            return CompletableFuture.completedFuture(false)
        }

        return CompletableFuture.supplyAsync {
            try {
                val tempFile = File.createTempFile("lang_", ".yml")
                
                // Create HTTP request
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(annotation.cloudUrl))
                    .header("User-Agent", "MythLibs Language Downloader")
                    .GET()
                    .build()
                
                // Download file
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
                
                if (response.statusCode() != 200) {
                    Logger.warn("Failed to download language file: HTTP ${response.statusCode()}")
                    return@supplyAsync false
                }
                
                response.body().use { input ->
                    Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

                // Validate YAML format
                val yaml = YamlConfiguration.loadConfiguration(tempFile)
                val version = yaml.getInt("version", -1)
                
                if (version == -1) {
                    Logger.warn("Invalid language file format from URL: ${annotation.cloudUrl}")
                    tempFile.delete()
                    return@supplyAsync false
                }

                // Check if update is needed
                if (targetFile.exists()) {
                    val currentYaml = YamlConfiguration.loadConfiguration(targetFile)
                    val currentVersion = currentYaml.getInt("version", -1)
                    if (currentVersion >= version) {
                        tempFile.delete()
                        return@supplyAsync false
                    }
                }

                // Copy file to target location
                Files.copy(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                tempFile.delete()
                
                Logger.info("Successfully downloaded language file: ${annotation.name}")
                true
            } catch (e: Exception) {
                Logger.warn("Failed to download language file: ${annotation.name}")
                Logger.warn("Error: ${e.message}")
                false
            }
        }
    }

    /**
     * Schedules automatic updates for a language file
     * 为语言文件安排自动更新
     *
     * @param annotation The language annotation
     *                   语言注解
     * @param targetFile The target file to update
     *                   要更新的目标文件
     */
    fun scheduleAutoUpdate(annotation: Language, targetFile: File) {
        if (!annotation.autoUpdate || annotation.cloudUrl.isEmpty()) {
            return
        }

        val task = scheduler.scheduleAtFixedRate({
            if (!updateTasks.containsKey(annotation.name) || updateTasks[annotation.name]?.isDone == true) {
                updateTasks[annotation.name] = downloadLanguageFile(annotation, targetFile)
            }
        }, annotation.updateInterval, annotation.updateInterval, TimeUnit.MINUTES)
    }

    /**
     * Shuts down the downloader
     * 关闭下载器
     */
    fun shutdown() {
        scheduler.shutdown()
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Logger.warn("Failed to shutdown language downloader gracefully")
        }
    }
} 