plugins {
    kotlin("jvm") version "2.1.10" apply false
}

group = "com.compile.sast"
version = "0.1.0"

subprojects {
    repositories {
        mavenCentral()
    }
}
