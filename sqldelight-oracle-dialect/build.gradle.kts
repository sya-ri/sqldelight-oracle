plugins {
    `java-library`
    alias(libs.plugins.grammar.kit.composer)
    alias(libs.plugins.kotlin.jvm)
}

grammarKit {
    intellijRelease.set(libs.versions.intellijPlatform)
}

tasks.matching { task -> task.name == "lintKotlinMain" }.configureEach {
    dependsOn("createComposabledev_s7a_sqldelight_oracle_grammar_oracleGrammar")
    dependsOn("generatedev_s7a_sqldelight_oracle_grammar_oracleParser")
}

afterEvaluate {
    val mainKotlinSources =
        fileTree("src/main/kotlin") {
            include("**/*.kt")
        }

    tasks.withType<org.jmailen.gradle.kotlinter.tasks.LintTask>()
        .matching { task -> task.name == "lintKotlinMain" }
        .configureEach {
            setSource(mainKotlinSources)
        }

    tasks.withType<org.jmailen.gradle.kotlinter.tasks.FormatTask>()
        .matching { task -> task.name == "formatKotlinMain" }
        .configureEach {
            setSource(mainKotlinSources)
        }
}

dependencies {
    api(libs.sqldelight.dialect.api)
    compileOnly(libs.sqldelight.compiler.env)
    testCompileOnly(libs.sqldelight.compiler.env)
    testRuntimeOnly(libs.sqldelight.compiler.env)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.ojdbc11)
    testImplementation(libs.testcontainers.oracle.xe)
}
