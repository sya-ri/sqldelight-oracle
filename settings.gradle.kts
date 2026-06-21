pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "sqldelight-oracle"

include(
    ":sqldelight-oracle-dialect",
    ":sqldelight-check-oracle-dialect",
    ":sqldelight-check-oracle-rule",
)
