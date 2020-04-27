// PLUGINS -- BEGIN
plugins {
    kotlin("jvm") version "1.3.72"
    jacoco
    id("org.sonarqube") version "2.8"
    id("com.jfrog.bintray") version "1.8.5"
    `maven-publish`
    id("com.diffplug.gradle.spotless") version "3.28.1"
}

allprojects {
    apply(plugin = "kotlin")
    java.sourceCompatibility = JavaVersion.VERSION_1_8
}
// PLUGINS -- END

// SPOTLESS -- BEGIN
allprojects {
    apply(plugin = "com.diffplug.gradle.spotless")

    spotless {
        kotlin {
            ktlint()
        }
        kotlinGradle {
            ktlint()
        }
    }

    listOf(tasks.compileJava, tasks.compileKotlin, tasks.compileTestJava, tasks.compileTestKotlin).forEach {
        it.get().mustRunAfter(tasks.spotlessCheck)
    }

    tasks.check {
        dependsOn(tasks.spotlessCheck)
    }
}
// SPOTLESS -- END

// SOURCES -- BEGIN
java {
    withSourcesJar()
}
// SOURCES -- END

// JAVADOC -- BEGIN
java {
    withJavadocJar()
}
// JAVADOC -- END

// JACOCO -- BEGIN
allprojects {
    apply(plugin = "jacoco")

    jacoco {
        toolVersion = "0.8.5"
    }

    tasks.jacocoTestReport {
        reports {
            xml.isEnabled = true
            html.isEnabled = true
        }

        dependsOn(tasks.test)
    }

    tasks.build {
        dependsOn(tasks.jacocoTestReport)
    }
}
// JACOCO -- END

// SonarQube -- BEGIN
sonarqube {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "kerberos-platform")
        property("sonar.projectKey", "kerberos-platform_cli-kotlin")
    }
}
// SonarQube -- END

// TEST LOGGING -- BEGIN
allprojects {
    tasks.withType<Test> {
        testLogging {
            showStandardStreams = false
            events("skipped", "failed")
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        afterSuite(printTestResult)
    }
}

val printTestResult: KotlinClosure2<TestDescriptor, TestResult, Void>
    get() = KotlinClosure2({ desc, result ->

        if (desc.parent == null) { // will match the outermost suite
            println("------")
            println(
                    "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} " +
                            "successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)"
            )
            println(
                    "Tests took: ${result.endTime - result.startTime} ms."
            )
            println("------")
        }
        null
    })
// TEST LOGGING -- END

// JUNIT -- BEGIN
allprojects {
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
// JUNIT -- END

// Dependencies -- BEGIN
allprojects {
    repositories {
        jcenter()
    }

    dependencies {
        "implementation"(platform(kotlin("bom")))
        "implementation"(kotlin("stdlib-jdk8"))

        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.6.2")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.6.2")
        "testImplementation"("io.mockk:mockk:1.10.0")
        "testImplementation"("org.assertj:assertj-core:3.15.0")
    }
}
// Dependencies -- END

// Publishing -- BEGIN
val mavenPublicationName: String = "maven"

publishing {
    publications {
        create<MavenPublication>(mavenPublicationName) {
            groupId = "com.davidgracia.software.authorizationmanager"
            artifactId = "cli-kotlin"
            version = project.version.toString()

            from(components["java"])
            pom {
                name.set("CLI Kotlin")
                description.set("A concise description of my library")
                url.set("https://github.com/kerberos-platform/cli-kotlin")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("dgraciac")
                        name.set("David Gracia Celemendi")
                        email.set("david.gracia.celemendi@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/kerberos-platform/cli-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/kerberos-platform/cli-kotlin.git")
                    url.set("https://github.com/kerberos-platform/cli-kotlin")
                }
            }
        }
    }
}

bintray {
    user = System.getProperty("bintray.user")
    key = System.getProperty("bintray.key")
    setPublications(mavenPublicationName)
    publish = true
    pkg.apply {
        repo = "maven"
        name = "cli-kotlin"
        userOrg = "kerberos-platform"
        vcsUrl = "https://github.com/kerberos-platform/cli-kotlin"
        version.apply {
            name = project.version.toString()
        }
    }
}
// Publishing -- END
