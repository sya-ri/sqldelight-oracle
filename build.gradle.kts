@file:OptIn(ExperimentalAbiValidation::class)
@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.dokka.javadoc) apply false
    alias(libs.plugins.maven.publish) apply false
}

group = "dev.s7a.sqldelight.oracle"
version = "0.1.0"

val publishedArtifacts =
    mapOf(
        ":sqldelight-oracle-dialect" to "sqldelight-oracle-dialect",
        ":sqldelight-check-oracle-dialect" to "sqldelight-check-oracle-dialect",
        ":sqldelight-check-oracle-rule" to "sqldelight-check-oracle-rule",
    )

dependencies {
    publishedArtifacts.keys.forEach { projectPath ->
        dokka(dependencyFactory.createProjectDependency(projectPath))
        kover(dependencyFactory.createProjectDependency(projectPath))
    }
    dokkaPlugin(libs.dokka.versioning.plugin)
}

dokka {
    pluginsConfiguration {
        versioning {
            version.set(project.version.toString())
            olderVersionsDir.set(layout.buildDirectory.dir("dokka/olderVersions"))
        }
    }
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

tasks.register("releaseCheck") {
    group = "verification"
    description = "Runs local release-blocking checks before publishing sqldelight-oracle."
    dependsOn("check")
    dependsOn(publishedArtifacts.keys.map { projectPath -> "$projectPath:check" })
    dependsOn("dokkaGeneratePublicationHtml")
    dependsOn(publishedArtifacts.keys.map { projectPath -> "$projectPath:dokkaGeneratePublicationJavadoc" })
    dependsOn(publishedArtifacts.keys.map { projectPath -> "$projectPath:publishToMavenLocal" })
}

subprojects {
    apply(plugin = "org.jmailen.kotlinter")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    group = rootProject.group
    version = rootProject.version

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(25)
            explicitApi()
            abiValidation()
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    val publishedArtifactId = publishedArtifacts[path]
    if (publishedArtifactId != null) {
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "org.jetbrains.dokka-javadoc")
        apply(plugin = "com.vanniktech.maven.publish")

        extensions.configure<DokkaExtension>("dokka") {
            moduleName.set(publishedArtifactId)
        }

        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
                publishToMavenCentral()
                if (providers.gradleProperty("signingInMemoryKey").isPresent) {
                    signAllPublications()
                }
                coordinates(rootProject.group.toString(), publishedArtifactId, rootProject.version.toString())
                configure(
                    KotlinJvm(
                        javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationJavadoc"),
                        sourcesJar = SourcesJar.Sources(),
                    ),
                )
                pom {
                    name.set(publishedArtifactId)
                    description.set("Oracle Database dialect and sqldelight-check support for SQLDelight.")
                    inceptionYear.set("2026")
                    url.set("https://github.com/sya-ri/sqldelight-oracle")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://github.com/sya-ri/sqldelight-oracle/blob/main/LICENSE")
                        }
                    }
                    developers {
                        developer {
                            id.set("sya-ri")
                            name.set("sya-ri")
                            email.set("contact@s7a.dev")
                        }
                    }
                    scm {
                        url.set("https://github.com/sya-ri/sqldelight-oracle")
                    }
                }
            }
        }
    }
}
