import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

buildscript {
    repositories {
        maven {
            setUrl("https://arti.tw.ee/artifactory/plugins-release")
        }
        mavenLocal()
    }
}

plugins {
    `java-library`
    `maven-publish`
    `idea`
    `checkstyle`
    id("com.github.spotbugs") version "3.0.0"
}

group = "com.transferwise.common"
ext["artifactoryUser"] = if (project.hasProperty("artifactoryUser")) project.property("artifactoryUser") else System.getenv("ARTIFACTORY_USER")
ext["artifactoryPassword"] = if (project.hasProperty("artifactoryPassword")) project.property("artifactoryPassword") else System.getenv("ARTIFACTORY_PASSWORD")
ext["projectName"] = "Tw Context"
ext["projectDescription"] = "Tw Context - Hierarhical context for carrying over information through threads and even over services calls chains"
ext["projectGitHubRepoName"] = "tw-context"
ext["projectArtifactName"] = "tw-context"

repositories {
    maven {
        setUrl("https://arti.tw.ee/artifactory/libs-release")
    }
    mavenLocal()
}

dependencies {
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:2.2.4.RELEASE"))
    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:2.2.4.RELEASE"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:2.2.4.RELEASE"))

    annotationProcessor("org.projectlombok:lombok")

    compileOnly("org.projectlombok:lombok")

    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("com.transferwise.common:tw-base-utils:1.2.7")
    implementation("io.micrometer:micrometer-core")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Implementation-Title"] = archiveBaseName
        attributes["Implementation-Version"] = archiveVersion
    }
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-Xlint")
    options.encoding = "utf-8"
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }

    repositories {
        maven {
            setUrl("https://arti.tw.ee/artifactory/libs-${if (project.version.toString().endsWith("-SNAPSHOT")) "snapshot" else "release"}-local")
            credentials {
                username = properties["artifactoryUser"] as String
                password = properties["artifactoryPassword"] as String
            }
        }
    }
}

spotbugs {
    effort = "max"
    reportLevel = "high"
}

tasks.withType<com.github.spotbugs.SpotBugsTask>().configureEach {
    reports {
        xml.isEnabled = false
        html.isEnabled = true
    }
}

checkstyle {
    config = resources.text.fromFile("google_checks.xml")
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.isEnabled = false
        html.isEnabled = true
    }
}

idea {
    project {
        languageLevel = IdeaLanguageLevel(JavaVersion.VERSION_1_8)
        vcs = "git"
        targetBytecodeVersion = JavaVersion.VERSION_1_8
    }
    module {}
}

tasks {
    test {
        testLogging {
            showExceptions = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STARTED, TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.STANDARD_ERROR)
        }
    }
}

val addTag = tasks.register<Exec>("addTag") {
    commandLine("git", "tag", "$project.name-$project.version", "-m", "$project.name-$project.version")
}

tasks.register<Exec>("pushTag") {
    dependsOn(addTag)
    commandLine("git", "push", "origin", "$project.name-$project.version")
}
