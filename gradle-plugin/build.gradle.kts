plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.1.1"
}

version = "0.1.0"

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.10")
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

gradlePlugin {
    website.set("https://github.com/adinahhh/compile-sast")
    vcsUrl.set("https://github.com/adinahhh/compile-sast")

    plugins {
        create("sast") {
            id = "com.compile.sast"
            implementationClass = "com.compile.sast.gradle.SastGradlePlugin"
            displayName = "compile-sast"
            description = "Wires the compile-sast K2 compiler plugin into Kotlin compilation - " +
                "CWE-mapped security checks surfaced as native compiler diagnostics."
            tags.set(listOf("kotlin", "security", "static-analysis", "compiler-plugin"))
        }
    }
}
