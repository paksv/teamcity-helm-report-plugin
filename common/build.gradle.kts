plugins {
    kotlin("jvm")
    id("io.github.rodm.teamcity-common")
}

dependencies {
    implementation(kotlin("stdlib"))
}


teamcity {
    version = rootProject.extra["teamcityVersion"] as String
}


tasks.withType<Jar> {
    archiveBaseName.set("terraform-common")
}