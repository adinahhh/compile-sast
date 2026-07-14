pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "compile-sast"

include(":plugin")
include(":gradle-plugin")
include(":sample-app")
