import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
  `maven-publish`
  signing
  alias(sharedLibs.plugins.kotlin.jvm)
}

buildscript {
  repositories {
    maven { url = uri("https://packages.jetbrains.team/maven/p/jcs/maven") }
  }
  dependencies {
    classpath("com.jetbrains:jet-sign:45.47")
    classpath("com.squareup.okhttp3:okhttp:4.12.0")
  }
}

var intellijPluginStructureVersion = "dev"
if (project.hasProperty("structureVersion")) {
  val structureVersion = project.properties["structureVersion"].toString()
  intellijPluginStructureVersion = structureVersion
}

allprojects {
  group = "org.jetbrains.intellij.plugins"
  version = intellijPluginStructureVersion

  apply {
    plugin("java")
    plugin("kotlin")
  }

  java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  repositories {
    maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    mavenCentral()
  }
  dependencies {
    implementation(rootProject.sharedLibs.jackson.module.kotlin)
    implementation(rootProject.sharedLibs.kotlin.reflect)
  }

  val sourcesJar by tasks.registering(Jar::class) {
    dependsOn("classes")
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

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "11"
      apiVersion = "1.4"
      languageVersion = "1.4"
    }
  }
}

val publicationConfigurations = mapOf(
  "BasePublication" to Triple("structure-base", "JetBrains Plugins Structure Base", "Base library for parsing JetBrains plugins. Used by other JetBrains Plugins structure libraries."),
  "ClassesPublication" to Triple("structure-classes", "JetBrains Plugins Structure Classes", "Base library for resolving class files and resources. Used by other JetBrains Plugins Structure Classes libraries."),
  "IntellijPublication" to Triple("structure-intellij", "JetBrains Plugins Structure IntelliJ", "Library for parsing JetBrains IDE plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements."),
  "IntellijClassesPublication" to Triple("structure-intellij-classes", "JetBrains Plugins Structure IntelliJ Classes", "Library for resolving class files and resources of JetBrains plugins."),
  "IdePublication" to Triple("structure-ide", "JetBrains Plugins Structure IntelliJ IDE", "Library for resolving class files and resources of IntelliJ Platform IDEs."),
  "IdeClassesPublication" to Triple("structure-ide-classes", "JetBrains Plugins Structure IntelliJ IDE Classes", "Library for resolving class files and resources of IntelliJ Platform IDEs."),
  "TeamCityPublication" to Triple("structure-teamcity", "JetBrains Plugins Structure TeamCity", "Library for parsing JetBrains TeamCity plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements."),
  "DotNetPublication" to Triple("structure-dotnet", "JetBrains Plugins Structure DotNet", "Library for parsing JetBrains DotNet plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements."),
  "HubPublication" to Triple("structure-hub", "JetBrains Plugins Structure Hub", "Library for parsing JetBrains Hub widgets. Can be used to verify that widget complies with JetBrains Marketplace requirements."),
  "EduPublication" to Triple("structure-edu", "JetBrains Plugins Structure Edu", "Library for parsing JetBrains Edu plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements."),
  "FleetPublication" to Triple("structure-fleet", "JetBrains Plugins Structure Fleet", "Library for parsing JetBrains Fleet plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements."),
  "ToolboxPublication" to Triple("structure-toolbox", "JetBrains Plugins Structure Toolbox", "Library for parsing JetBrains Toolbox plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements."),
  "TeamCityRecipesPublications" to Triple("structure-teamcity-recipes", "JetBrains Plugins Structure TeamCity Recipes", "Library for parsing JetBrains TeamCity recipes. Can be used to verify that plugin complies with JetBrains Marketplace requirements."),
  "YoutrackPublication" to Triple("structure-youtrack", "JetBrains Plugins Structure YouTrack Apps", "Library for parsing JetBrains YouTrack Apps. Can be used to verify that plugin complies with JetBrains Marketplace requirements.")
)

publishing {
  repositories {
    maven {
      name = "artifacts"
      url = uri(layout.buildDirectory.dir("artifacts/maven"))
    }
  }

  publications {
    fun configurePublication(publicationName: String): MavenPublication {
      val (projectName, pubName, pubDesc) = requireNotNull(publicationConfigurations[publicationName])
      return create<MavenPublication>(publicationName) {
        val proj = project(":$projectName")
        groupId = proj.group.toString()
        artifactId = proj.name
        version = proj.version.toString()

        from(proj.components["java"])

        artifact(proj.tasks["sourcesJar"])
        artifact(proj.tasks["javadocJar"])

        pom {
          name = pubName
          description = pubDesc
          url = "https://github.com/JetBrains/intellij-plugin-verifier/tree/master/intellij-plugin-structure/$projectName"
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

    publicationConfigurations.keys.forEach {
      configurePublication(it)
    }
  }
}

signing {
  val isUnderTeamCity = System.getenv("TEAMCITY_VERSION") != null
  if (isUnderTeamCity) {
//    signatories = GpgSignSignatoryProvider()

    val signingKey: String? = System.getProperty("signingKey")
    val signingPassword: String? = System.getProperty("signingPassword")
    useInMemoryPgpKeys(signingKey, signingPassword)

    publicationConfigurations.keys.forEach {
      sign(publishing.publications[it])
    }
  }
}

tasks {
  test {
    dependsOn(":tests:test")
  }
  publishToMavenLocal {
    dependsOn(test)
  }
  publish {
    dependsOn(test)
  }
}

tasks {
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

      val userName = rootProject.extra["centralPortalUserName"] as String
      val token = rootProject.extra["centralPortalToken"] as String
      val base64Auth = Base64.getEncoder().encode("$userName:$token".toByteArray()).toString(Charsets.UTF_8)
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