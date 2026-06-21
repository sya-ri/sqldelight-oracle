plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.sqldelight.dialect.api)
    compileOnly(libs.sqldelight.compiler.env)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.ojdbc11)
    testImplementation(libs.testcontainers.oracle.xe)
}
