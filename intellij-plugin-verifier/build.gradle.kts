import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "1.8.10"
}

val kotlin_version by extra("1.8.20")

val projectVersion: String by extra {
  project.properties.getOrDefault("verifierProjectVersion", "dev").toString()
}

val isDevMode = projectVersion == "dev"
var ijStructureVersion = "dev"
if (!isDevMode) {
  if (project.hasProperty("intellijStructureVersion")) {
    ijStructureVersion = project.properties["intellijStructureVersion"].toString()
  } else {
    throw RuntimeException("Version of intellij-structure library to be used is not specified via 'intellijStructureVersion'")
  }
}

val intellijStructureVersion by extra(ijStructureVersion)

allprojects {
  apply {
    plugin("java")
    plugin("kotlin")
  }

  group = "org.jetbrains.intellij.plugins"
  version = projectVersion

  java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "11"
      // FIXME novotnyr upgraded to 1.5
      apiVersion = "1.5"
      languageVersion = "1.5"
      freeCompilerArgs = listOf("-Xjvm-default=enable")
    }
  }

  dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")

    implementation("org.jetbrains.intellij.plugins:structure-intellij:$intellijStructureVersion")

    testImplementation("junit:junit:4.13.2")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    implementation("com.intellij:annotations:12.0")
    implementation("commons-io:commons-io:2.10.0")
    implementation("com.google.code.gson:gson:2.10.1")
  }

  repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
    maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
  }
}

subprojects {
  val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
  }

  artifacts {
    archives(sourcesJar)
  }

  tasks {
    jar {
      manifest {
        attributes("Verifier-Version" to projectVersion)
      }
    }
  }
}

publishing {
  publications {
    fun configurePublication(publicationName: String, projectName: String): MavenPublication {
      return create<MavenPublication>(publicationName) {
        val proj = project(":$projectName")
        groupId = proj.group.toString()
        artifactId = proj.name
        version = proj.version.toString()

        from(proj.components["java"])
        artifact(proj.tasks["sourcesJar"])
      }
    }
    project(":verifier-cli").afterEvaluate {
      configurePublication("VerifierCliPublication", ":verifier-cli")
    }
    configurePublication("VerifierCorePublication", ":verifier-core")
    configurePublication("VerifierIntelliJPublication", ":verifier-intellij")
    configurePublication("RepositoryPublication", ":verifier-repository")
  }

  repositories {
    maven {
      url = uri("https://packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-verifier")
      credentials {
        username = if(project.hasProperty("publishUser")) project.properties["publishUser"].toString() else System.getenv("PUBLISH_USER")
        password = if(project.hasProperty("publishPassword")) project.properties["publishPassword"].toString() else System.getenv("PUBLISH_PASSWORD")
      }
    }
  }
}

tasks {
  test<Test> {
    dependsOn(":verifier-test:test")
  }
  publishToMavenLocal {
    dependsOn(test)
  }
  publish {
    dependsOn(test)
  }
}

