pluginManagement {
    plugins {
        kotlin("jvm") version "1.9.20" apply false
        id("maven-publish") apply false
        id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
        id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "enumutil-kt"

include("enumutil")
include("enumutil-ksp")
include("example")
//include("benchmark")
