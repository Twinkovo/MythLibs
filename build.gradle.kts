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

    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    // Adventure (for text components)
    implementation("net.kyori:adventure-api:4.18.0")
    implementation("net.kyori:adventure-text-minimessage:4.18.0")

    // Configuration
    implementation("org.yaml:snakeyaml:2.3")
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
    }
    
    build {
        dependsOn(shadowJar)
    }
}