import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  war
  idea
  `maven-publish`
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependencyManagement)
  alias(sharedLibs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring) version sharedLibs.versions.kotlin
}

kotlin {
  jvmToolchain(11)
}

tasks {
  publishToMavenLocal {
    dependsOn(test)
  }
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    apiVersion = KotlinVersion.KOTLIN_1_8
    languageVersion = KotlinVersion.KOTLIN_1_8
  }
}

val serviceVersion = project.properties
        .getOrDefault("verifierServiceProjectVersion", "1.0").toString()

allprojects {
  version = serviceVersion
  group = "org.jetbrains.intellij.plugins.verifier"

  idea {
    module {
      inheritOutputDirs = false
      outputDir = layout.buildDirectory.file("classes/main").get().asFile
    }
  }

  tasks {
    bootRun {
      @Suppress("UNCHECKED_CAST")
      systemProperties = System.getProperties().toMap() as Map<String, Any>
    }
    springBoot {
      buildInfo()
    }
  }

  repositories {
    mavenLocal()
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://packages.jetbrains.team/maven/p/teamcity-rest-client/teamcity-rest-client")
  }

  configurations {
    developmentOnly
    runtimeClasspath {
      extendsFrom(developmentOnly.get())
    }
  }

  dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.tomcat)
    implementation(libs.spring.boot.devtools)

    testImplementation(sharedLibs.junit)
    implementation(sharedLibs.kotson)
    implementation(sharedLibs.gson)

    implementation(sharedLibs.okhttp)
    implementation(sharedLibs.retrofit)
    implementation(sharedLibs.retrofit.gson)
    implementation(sharedLibs.okhttp.loggingInterceptor)

    //Simple map-database engine that allows to store maps on disk: https://github.com/jankotek/mapdb/
    implementation(libs.mapdb)
    implementation(sharedLibs.slf4j.api)
    implementation(libs.logback.classic)

    implementation(libs.commons.fileupload)
    implementation("org.jetbrains.intellij.plugins:intellij-feature-extractor:dev")
    implementation("org.jetbrains.intellij.plugins:verifier-intellij:dev")

    implementation(libs.teamcity.restClient)
  }
}

