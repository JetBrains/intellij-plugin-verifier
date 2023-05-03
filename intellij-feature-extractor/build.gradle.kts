plugins {
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  `maven-publish`
}

val kotlin_version = "1.8.20"

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
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
    maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
  }

  dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

dependencies {
  val structureVersion = "dev"
  implementation("org.jetbrains.intellij.plugins:structure-intellij-classes:$structureVersion")
  implementation("org.jetbrains.intellij.plugins:structure-ide-classes:$structureVersion")
  implementation("org.jetbrains.intellij.plugins:verifier-core:$structureVersion")

  implementation("com.google.code.gson:gson:2.10.1")
  implementation("org.slf4j:slf4j-api:2.0.7")
  implementation("commons-io:commons-io:2.5")

  testImplementation("junit:junit:4.13.2")
  testImplementation(project(":test-classes"))
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier.set("sources")
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
