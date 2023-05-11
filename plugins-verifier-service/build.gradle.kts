plugins {
  war
  idea
  `maven-publish`
  id("org.springframework.boot") version "2.7.5"
  id("io.spring.dependency-management") version "1.1.0"
  alias(sharedLibs.plugins.kotlin.jvm)
  id("org.jetbrains.kotlin.plugin.spring") version sharedLibs.versions.kotlin
}

kotlin {
  jvmToolchain(11)
}

tasks {
  compileKotlin {
    kotlinOptions {
      apiVersion = "1.4"
      languageVersion = "1.4"
    }
  }
  publishToMavenLocal {
    dependsOn(test)
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
      outputDir = File("$buildDir/classes/main")
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
    implementation("org.springframework.boot:spring-boot-starter-web") {
      dependencies {
        implementation("org.apache.logging.log4j:log4j-to-slf4j:2.20.0") {
          because("we need 2.17.0")
        }
        implementation("org.apache.logging.log4j:log4j-api:2.20.0") {
          because("we need 2.17.0")
        }
      }
    }
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("junit:junit:4.13.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("commons-io:commons-io:2.10.0")
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    //Simple map-database engine that allows to store maps on disk: https://github.com/jankotek/mapdb/
    implementation("org.mapdb:mapdb:3.0.9")
    implementation(sharedLibs.slf4j.api)
    implementation("ch.qos.logback:logback-classic:1.2.11")

    runtimeOnly("org.codehaus.groovy:groovy:3.0.16")
    implementation("commons-fileupload:commons-fileupload:1.5")
    implementation("org.jetbrains.intellij.plugins:intellij-feature-extractor:dev")
    implementation("org.jetbrains.intellij.plugins:verifier-intellij:dev")

    implementation("org.jetbrains.teamcity:teamcity-rest-client:1.14.0")
  }
}
