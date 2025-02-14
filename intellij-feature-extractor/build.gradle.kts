import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(sharedLibs.plugins.kotlin.jvm)
  `maven-publish`
}

val extractorVersion = project.properties.getOrDefault("featureExtractorVersion", "dev").toString()

allprojects {
  apply {
    plugin("java")
    plugin("kotlin")
    plugin("maven-publish")
  }

  version = extractorVersion
  group = "org.jetbrains.intellij.plugins"

  repositories {
    maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    mavenCentral()
    mavenLocal()
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }


  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "11"
      apiVersion = "1.4"
      languageVersion = "1.4"
    }
  }
}

dependencies {
  val structureVersion = "dev"
  implementation("org.jetbrains.intellij.plugins:structure-intellij-classes:$structureVersion")
  implementation("org.jetbrains.intellij.plugins:structure-ide-classes:$structureVersion")
  implementation("org.jetbrains.intellij.plugins:verifier-core:$structureVersion")

  implementation(sharedLibs.slf4j.api)
  implementation(sharedLibs.jackson.module.kotlin)

  testImplementation(sharedLibs.junit)
  testImplementation(project(":test-classes"))
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier = "sources"
  from(sourceSets.main.get().allSource)
}

artifacts {
  archives(sourcesJar)
}

publishing {
  publications {
    fun configurePublication(publicationName: String): MavenPublication {
      return create<MavenPublication>(publicationName) {
        groupId = project.group.toString()
        artifactId = project.name
        version = project.version.toString()

        from(project.components["java"])
        artifact(sourcesJar)
      }
    }
    configurePublication("ProjectPublication")
  }
}

tasks {
  publishToMavenLocal {
    dependsOn(test)
  }
}
