import org.gradle.kotlin.dsl.get
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension
import org.jetbrains.changelog.tasks.BaseChangelogTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Base64
import org.gradle.api.publish.Publication as GradlePublication
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

plugins {
  `maven-publish`
  signing
  alias(sharedLibs.plugins.kotlin.jvm)
  alias(sharedLibs.plugins.changelog)
}

buildscript {
  dependencies {
    classpath("com.squareup.okhttp3:okhttp:5.3.2")
  }
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
    toolchain {
      languageVersion = JavaLanguageVersion.of(11)
    }
  }

  tasks.withType<JavaCompile>().configureEach {
    options.release = 11
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "11"
      apiVersion = "1.5"
      languageVersion = "1.5"
      freeCompilerArgs += listOf("-Xjvm-default=all-compatibility")
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

  val javadocJar by tasks.registering(Jar::class) {
    val javadoc = tasks.javadoc
    dependsOn(javadoc)
    archiveClassifier = "javadoc"
    from(javadoc)
  }

  artifacts {
    archives(sourcesJar)
    archives(javadocJar)
  }

  tasks {
    jar {
      manifest {
        attributes("Verifier-Version" to projectVersion)
      }
    }
  }
}

data class Publication(val project: String, val name: String, val readableName: String, val description: String) {
  companion object {
    val cli = Publication("verifier-cli", "VerifierCli",
      "JetBrains Plugin Verifier CLI",
      "Command-line interface for JetBrains Plugin Verifier with set of high-level tasks for plugin and IDE validation")
    val core = Publication("verifier-core", "VerifierCore",
      "JetBrains Plugin Verifier Core",
      "Core classes of JetBrains Plugin Verifier with verification rules, general usage detection and bytecode verification engine")
    val intellij = Publication("verifier-intellij", "VerifierIntelliJ",
      "JetBrains Plugin Verifier IntelliJ",
      "JetBrains Plugin Verifier Classes for IntelliJ Platform integration with API usage detection and reporting.")
    val repository = Publication("verifier-repository", "VerifierRepository",
      "JetBrains Plugin Verifier Repository Integration",
      "JetBrains Plugin Verifier integration with JetBrains Marketplace plugin repository")
  }
}

publishing {
  repositories {
    maven {
      name = "artifacts"
      url = uri(layout.buildDirectory.dir("artifacts/maven"))
    }

    maven {
      url = uri("https://packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-verifier")
      credentials {
        val spacePackagesClientId: String? by project
        val spacePackagesSecret: String? by project

        username = spacePackagesClientId
        password = spacePackagesSecret
      }
    }
  }

  publications {
    configurePublication(Publication.cli)
    configurePublication(Publication.core)
    configurePublication(Publication.intellij)
    configurePublication(Publication.repository)
  }
}

signing {
  isRequired = System.getenv("TEAMCITY_VERSION") != null

  val signingKey: String? by project
  val signingPassword: String? by project

  useInMemoryPgpKeys(signingKey, signingPassword)

  sign(Publication.cli)
  sign(Publication.core)
  sign(Publication.intellij)
  sign(Publication.repository)
}

tasks {
  publishToMavenLocal {
    dependsOn(test)
  }
  publish {
    dependsOn(test)
  }

  val packSonatypeCentralBundle by registering(Zip::class) {
    group = "publishing"

    dependsOn(":publishAllPublicationsToArtifactsRepository")

    from(layout.buildDirectory.dir("artifacts/maven"))
    archiveFileName.set("bundle.zip")
    destinationDirectory.set(layout.buildDirectory)
  }

  val publishMavenToCentralPortal by registering {
    group = "publishing"

    dependsOn(packSonatypeCentralBundle)

    doLast {
      val uriBase = "https://central.sonatype.com/api/v1/publisher/upload"
      val publishingType = "USER_MANAGED"
      val deploymentName = "${project.name}-$version"
      val uri = "$uriBase?name=$deploymentName&publishingType=$publishingType"

      val centralPortalUserName: String? by project
      val centralPortalToken: String? by project

      val base64Auth = Base64
        .getEncoder()
        .encode("$centralPortalUserName:$centralPortalToken".toByteArray())
        .toString(Charsets.UTF_8)
      val bundleFile = packSonatypeCentralBundle.get().archiveFile.get().asFile

      println("Sending request to $uri...")

      val client = OkHttpClient()
      val request = Request.Builder()
        .url(uri)
        .header("Authorization", "Bearer $base64Auth")
        .post(
          MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("bundle", bundleFile.name, bundleFile.asRequestBody())
            .build()
        )
        .build()
      client.newCall(request).execute().use { response ->
        val statusCode = response.code
        println("Upload status code: $statusCode")
        println("Upload result: ${response.body!!.string()}")
        if (statusCode != 201) {
          error("Upload error to Central repository. Status code $statusCode.")
        }
      }
    }
  }
}

fun PublicationContainer.configurePublication(publication: Publication) {
  val (projectName, publicationName) = publication
  create<MavenPublication>(publicationName) {
    val proj = project(":$projectName")
    groupId = proj.group.toString()
    artifactId = proj.name
    version = proj.version.toString()

    from(proj.components["java"])
    artifact(proj.tasks["sourcesJar"])
    artifact(proj.tasks["javadocJar"])

    pom {
      name = publication.readableName
      description = publication.description
      url = "https://github.com/JetBrains/intellij-plugin-verifier/tree/master/intellij-plugin-verifier/$projectName"
      licenses {
        license {
          name = "The Apache Software License, Version 2.0"
          url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
        }
      }
      developers {
        developer {
          id = "satamas"
          name = "Semyon Atamas"
          organization = "JetBrains"
        }
        developer {
          id = "AlexanderPrendota"
          name = "Alexander Prendota"
          organization = "JetBrains"
        }
        developer {
          id = "ktisha"
          name = "Ekaterina Smal"
          organization = "JetBrains"
        }
        developer {
          id = "chashnikov"
          name = "Nikolay Chashnikov"
          organization = "JetBrains"
        }
        developer {
          id = "chrkv"
          name = "Ivan Chirkov"
          organization = "JetBrains"
        }
        developer {
          id = "Ololoshechkin"
          name = "Brilyantov Vadim"
          organization = "JetBrains"
        }
        developer {
          id = "shalupov"
          name = "Leonid Shalupov"
          organization = "JetBrains"
        }
        developer {
          id = "hsz"
          name = "Jakub Chrzanowski"
          organization = "JetBrains"
        }
        developer {
          id = "kesarevs"
          name = "Kesarev Sergey"
          organization = "JetBrains"
        }
        developer {
          id = "LChernigovskaya"
          name = "Lidiya Chernigovskaya"
          organization = "JetBrains"
        }
        developer {
          id = "novotnyr"
          name = "Robert Novotny"
          organization = "JetBrains"
        }
      }
      scm {
        connection = "scm:git:git://github.com/JetBrains/intellij-plugin-verifier.git"
        developerConnection = "scm:git:ssh://github.com/JetBrains/intellij-plugin-verifier.git"
        url = "https://github.com/JetBrains/intellij-plugin-verifier"
      }
    }
  }
}

operator fun PublicationContainer.get(publication: Publication): GradlePublication {
  return publishing.publications[publication.name]
}

fun SigningExtension.sign(publication: Publication) {
  sign(publishing.publications[publication])
}

changelog {
  version = projectVersion
  headerParserRegex = Regex("""(\d+\.\d+)""")
  groups = listOf("Added", "Changed", "Fixed")
  path = file("../CHANGELOG.md").canonicalPath
}

/**
 * Writes a changelog for the most recently released version.
 * The 'CHANGELOG.md' set in the 'changelog' plugin is used as a source
 */
abstract class MostRecentVersionChangelog : BaseChangelogTask() {
  @get:OutputFile
  abstract val changelogOutputFile: RegularFileProperty

  @TaskAction
  fun run() {
    with(changelog.get()) {
      releasedItems.first()
        .withHeader(false)
        .let {
          renderItem(it, Changelog.OutputType.MARKDOWN)
        }
        .let { changelogItem ->
          changelogOutputFile.asFile
            .get().writeText(changelogItem)
        }
    }
  }
}

tasks.register<MostRecentVersionChangelog>("mostRecentVersionChangelog") {
  val extension = project.extensions.getByType<ChangelogPluginExtension>()
  changelog.convention(extension.instance)
  changelogOutputFile.convention(project.layout.buildDirectory.file("changelog.md"))
  outputs.upToDateWhen { false }
}
