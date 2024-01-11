import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `maven-publish`
  alias(sharedLibs.plugins.kotlin.jvm)
  alias(sharedLibs.plugins.changelog)
}

val projectVersion: String by extra {
  project.properties.getOrDefault("verifierProjectVersion", "dev").toString()
}

val isDevMode = projectVersion == "dev"
var ijStructureVersion = if (!isDevMode) {
  project.properties.getOrDefault("intellijStructureVersion") {
    throw RuntimeException("Version of intellij-structure library to be used is not specified via 'intellijStructureVersion'")
  }
} else {
  "dev"
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
      apiVersion = "1.5"
      languageVersion = "1.5"
      freeCompilerArgs = listOf("-Xjvm-default=enable")
    }
  }

  dependencies {

    implementation(rootProject.sharedLibs.kotlin.reflect)

    implementation("org.jetbrains.intellij.plugins:structure-intellij:$intellijStructureVersion")

    testImplementation(rootProject.sharedLibs.junit)

    implementation(rootProject.sharedLibs.slf4j.api)
    implementation(rootProject.libs.bouncycastle.pkix)

    implementation(rootProject.sharedLibs.jetbrains.annotations)
    implementation(rootProject.sharedLibs.gson)
  }

  repositories {
    maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    mavenCentral()
    mavenLocal()
  }
}

subprojects {
  val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier = "sources"
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

changelog {
  version = projectVersion
  headerParserRegex = Regex("""(\d+\.\d+)""")
  groups = listOf("Added", "Changed", "Fixed")
  path = file("../CHANGELOG.md").canonicalPath
}
