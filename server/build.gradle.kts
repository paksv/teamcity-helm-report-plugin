
val BUNDLED_TFENV_TOOL_VERSION = "2.2.2"

plugins {
    kotlin("jvm")
    id("io.github.rodm.teamcity-server")
    id("io.github.rodm.teamcity-environments")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":common"))

    ///for BuildProblemManager
    compileOnly("org.jetbrains.teamcity.internal:server:${rootProject.extra["teamcityVersion"]}")
    compileOnly("org.jetbrains.teamcity.internal:server-tools:${rootProject.extra["teamcityVersion"]}")

    agent(project(path = ":agent", configuration = "plugin"))
}

teamcity {
    // Use TeamCity 8.1 API
    version = rootProject.extra["teamcityVersion"] as String
    server {
        descriptor = file("teamcity-plugin.xml")
        tokens = mapOf("Version" to rootProject.version)
        archiveName = "helm-diff-report-plugin"
    }

    environments {
        create("teamcity") {
            homeDir = "/Users/spak/TeamCity"
            version = rootProject.extra["teamcityVersion"] as String
            dataDir = "/Users/spak/TeamCity/adata"
        }
    }
}


tasks.withType<Jar> {
    archiveBaseName.set("helm-diff-report-plugin")
}

task("teamcity") {
    dependsOn("serverPlugin")

    doLast {
        println("##teamcity[publishArtifacts '${(tasks["serverPlugin"] as Zip).archiveFile}']")
    }
}