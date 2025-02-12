plugins {
    kotlin("jvm") version "2.1.20-Beta2"
    id("com.gradleup.shadow") version "9.0.0-beta8"
}

group = "com.twinkovo"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0-RC2")

    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    // Adventure (for text components)
    implementation("net.kyori:adventure-api:4.18.0")
    implementation("net.kyori:adventure-text-minimessage:4.18.0")

    // Configuration
    implementation("org.yaml:snakeyaml:2.3")

    // 数据库相关依赖
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("com.mysql:mysql-connector-j:9.2.0")
    implementation("org.xerial:sqlite-jdbc:3.49.0.0")
    implementation("redis.clients:jedis:5.2.0")
    implementation("org.mongodb:mongodb-driver-sync:5.3.1")
    
    // Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "21"
    }
    
    compileTestKotlin {
        kotlinOptions.jvmTarget = "21"
    }
    
    processResources {
        filesMatching("plugin.yml") {
            expand(project.properties)
        }
    }
    
    shadowJar {
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())
        
        // Relocate dependencies to avoid conflicts
        relocate("kotlin", "com.twinkovo.mythlibs.lib.kotlin")
        relocate("org.yaml.snakeyaml", "com.twinkovo.mythlibs.lib.snakeyaml")
        relocate("net.kyori", "com.twinkovo.mythlibs.lib.kyori")
        relocate("com.github.benmanes.caffeine", "com.twinkovo.mythlibs.lib.caffeine")
        relocate("org.jetbrains.kotlinx", "com.twinkovo.mythlibs.lib.kotlinx")
    }
    
    build {
        dependsOn(shadowJar)
    }
}