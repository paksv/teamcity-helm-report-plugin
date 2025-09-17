import java.text.SimpleDateFormat
import java.util.*

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://download.jetbrains.com/teamcity-repository")
        maven(url = "https://repo.labs.intellij.net/teamcity")
    }
}

plugins {
    id("io.github.rodm.teamcity-base") version "1.5.6" apply false
    id("io.github.rodm.teamcity-common") version "1.5.6" apply false
    id("io.github.rodm.teamcity-agent")  version "1.5.6" apply false
    id("io.github.rodm.teamcity-server") version "1.5.6" apply false
    id("io.github.rodm.teamcity-environments") version "1.5.6" apply false

    kotlin("jvm") version "1.4.10"
}

ext {
    set("teamcityVersion", "2025.03")
}

group = "jetbrains.buildserver"
val envBuildNumber = System.getenv("BUILD_NUMBER")
if (envBuildNumber != null) {
    version = envBuildNumber
} else {
    val sdf = SimpleDateFormat("yyyyMMddHHmmss")
    version = sdf.format(Date())
}

subprojects {

    repositories {
        mavenCentral()
        jcenter()
    }

    group = rootProject.group
    version = rootProject.version

    tasks.withType<Zip> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

task("teamcity") {
    // dependsOn(":tests:test")
    dependsOn(":server:teamcity")
}