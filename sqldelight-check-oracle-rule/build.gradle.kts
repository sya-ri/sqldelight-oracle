plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.sqldelight.check.rule.api)
    implementation(dependencyFactory.createProjectDependency(":sqldelight-check-oracle-dialect"))
    testImplementation(libs.sqldelight.check.rule.api)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.framework.engine)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.oracle.xe)
    testImplementation(libs.ojdbc11)
}
