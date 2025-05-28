plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    maven(url = "https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
}

// Helper function to exclude Kotlin standard library dependencies
fun <T : ModuleDependency> T.excludeKotlinStdlib() {
    exclude(module = "kotlin-runtime")
    exclude(module = "kotlin-reflect")
    exclude(module = "kotlin-stdlib")
    exclude(module = "kotlin-stdlib-common")
    exclude(module = "kotlin-stdlib-jdk8")
    exclude(module = "kotlin-stdlib-jdk7")
}

dependencies {
    implementation(libs.koog.agents)
    implementation(libs.code.engine)
}

tasks {
    shadowJar {
        archiveBaseName.set("shadow-dependencies")
        archiveClassifier.set("")

        // Relocate Kotlin coroutines packages to avoid conflicts
        relocate("kotlinx.coroutines", "shadow.kotlinx.coroutines")
    }
}
