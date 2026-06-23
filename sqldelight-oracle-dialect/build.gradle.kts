plugins {
    `java-library`
    alias(libs.plugins.grammar.kit.composer)
    alias(libs.plugins.kotlin.jvm)
}

grammarKit {
    intellijRelease.set(libs.versions.intellijPlatform)
}

tasks.matching { task -> task.name == "lintKotlinMain" }.configureEach {
    dependsOn("createComposabledev_s7a_sqldelight_oracle_dialects_oracle_grammar_OracleGrammar")
    dependsOn("generatedev_s7a_sqldelight_oracle_dialects_oracle_grammar_OracleParser")
}

afterEvaluate {
    val mainKotlinSources =
        fileTree("src/main/kotlin") {
            include("**/*.kt")
        }

    tasks.withType<org.jmailen.gradle.kotlinter.tasks.LintTask>()
        .matching { task -> task.name == "lintKotlinMain" }
        .configureEach {
            source = mainKotlinSources
        }

    tasks.withType<org.jmailen.gradle.kotlinter.tasks.FormatTask>()
        .matching { task -> task.name == "formatKotlinMain" }
        .configureEach {
            source = mainKotlinSources
        }
}

dependencies {
    compileOnly(libs.sqldelight.core)
    api(libs.sqldelight.dialect.api)
    compileOnly(libs.sqldelight.compiler.env)
    testCompileOnly(libs.sqldelight.compiler.env)
    testRuntimeOnly(libs.sqldelight.compiler.env)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.ojdbc11)
    testImplementation(libs.sqldelight.gradle.plugin)
    testImplementation(libs.sql.psi.environment)
    testImplementation(libs.testcontainers.oracle.xe)
}
