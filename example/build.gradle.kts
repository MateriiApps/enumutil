plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    ksp(project(":enumutil-ksp"))
    implementation(project(":enumutil"))
}

kotlin {
    jvmToolchain(11)

    sourceSets {
        getByName("main") {
            kotlin.srcDir("build/generated/ksp/main/kotlin")
        }
    }
}
