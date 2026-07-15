import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish") version "0.32.0"
}

version = "0.1.0"

mavenPublishing {
    coordinates("io.github.adinahhh", "compile-sast-plugin", version.toString())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    pom {
        name.set("compile-sast")
        description.set(
            "A proof-of-concept Kotlin K2/FIR compiler plugin exploring compile-time security " +
                "enforcement. CWE-mapped checks surfaced as native compiler diagnostics via " +
                "conservative constant analysis, not full taint tracking."
        )
        url.set("https://github.com/adinahhh/compile-sast")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/adinahhh/compile-sast/blob/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("adinahhh")
                name.set("Adinah Zilton")
            }
        }
        scm {
            url.set("https://github.com/adinahhh/compile-sast")
            connection.set("scm:git:git://github.com/adinahhh/compile-sast.git")
            developerConnection.set("scm:git:ssh://git@github.com/adinahhh/compile-sast.git")
        }
    }
}

val kotlinCompilerVersion = "2.1.10"

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinCompilerVersion")

    testImplementation(kotlin("test-junit5"))
    testImplementation("dev.zacsweers.kctfork:core:0.7.1")
    testImplementation("org.yaml:snakeyaml:2.0")
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
}
