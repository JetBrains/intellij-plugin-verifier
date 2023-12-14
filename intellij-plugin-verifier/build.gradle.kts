import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `maven-publish`
  signing
  alias(sharedLibs.plugins.kotlin.jvm)
  alias(sharedLibs.plugins.nexus.publish)
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

val mavenCentralUsername = findProperty("mavenCentralUsername")?.toString()
val mavenCentralPassword = findProperty("mavenCentralPassword")?.toString()

nexusPublishing {
  repositories {
    sonatype {
      username = mavenCentralUsername
      password = mavenCentralPassword
    }
  }
}

object Publications {
  val cli = Pub(":verifier-cli", "VerifierCli")
  val core = Pub(":verifier-core", "VerifierCore")
  val intellij = Pub(":verifier-intellij", "VerifierIntelliJ")
  val repository = Pub(":verifier-repository", "Repository")
  data class Pub(val project: String, val name: String)
}

publishing {
  publications {
    fun configurePublication(publication: Publications.Pub): MavenPublication {
      val (projectName, publicationName) = publication
      return create<MavenPublication>(publicationName) {
        val proj = project(":$projectName")
        groupId = proj.group.toString()
        artifactId = proj.name
        version = proj.version.toString()

        from(proj.components["java"])
        artifact(proj.tasks["sourcesJar"])
      }
    }
    configurePublication(Publications.cli)
    configurePublication(Publications.core)
    configurePublication(Publications.intellij)
    configurePublication(Publications.repository)
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

signing {
  isRequired = mavenCentralUsername != null
  if (isRequired) {
    val signingKey = findProperty("signingKey").toString()
    val signingPassword = findProperty("signingPassword").toString()

    useInMemoryPgpKeys(signingKey, signingPassword)

    sign(publishing.publications[Publications.cli.name])
    sign(publishing.publications[Publications.core.name])
    sign(publishing.publications[Publications.intellij.name])
    sign(publishing.publications[Publications.repository.name])
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

