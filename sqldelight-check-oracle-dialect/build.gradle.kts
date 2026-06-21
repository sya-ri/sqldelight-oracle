plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.sqldelight.check.api)
    testImplementation(libs.sqldelight.check.api)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.kotest.runner.junit5)
}
