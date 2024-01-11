import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `maven-publish`
  signing
  alias(sharedLibs.plugins.kotlin.jvm)
  alias(sharedLibs.plugins.nexus.publish)
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

publishing {
  publications {
    fun configurePublication(publicationName: String,
                           projectName: String,
                           pubName: String,
                           pubDesc: String): MavenPublication {
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

    configurePublication("BasePublication", "structure-base", "JetBrains Plugins Structure Base", "Base library for parsing JetBrains plugins. Used by other JetBrains Plugins structure libraries.")
    configurePublication("ClassesPublication", "structure-classes", "JetBrains Plugins Structure Classes", "Base library for resolving class files and resources. Used by other JetBrains Plugins Structure Classes libraries.")
    configurePublication("IntellijPublication", "structure-intellij", "JetBrains Plugins Structure IntelliJ", "Library for parsing JetBrains IDE plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements.")
    configurePublication("IntellijClassesPublication", "structure-intellij-classes", "JetBrains Plugins Structure IntelliJ Classes", "Library for resolving class files and resources of JetBrains plugins.")
    configurePublication("IdePublication", "structure-ide", "JetBrains Plugins Structure IntelliJ IDE", "Library for resolving class files and resources of IntelliJ Platform IDEs.")
    configurePublication("IdeClassesPublication", "structure-ide-classes", "JetBrains Plugins Structure IntelliJ IDE Classes", "Library for resolving class files and resources of IntelliJ Platform IDEs.")
    configurePublication("TeamCityPublication", "structure-teamcity", "JetBrains Plugins Structure TeamCity", "Library for parsing JetBrains TeamCity plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements.")
    configurePublication("DotNetPublication", "structure-dotnet", "JetBrains Plugins Structure DotNet", "Library for parsing JetBrains DotNet plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements.")
    configurePublication("HubPublication", "structure-hub", "JetBrains Plugins Structure Hub", "Library for parsing JetBrains Hub widgets. Can be used to verify that widget complies with JetBrains Marketplace requirements.")
    configurePublication("EduPublication", "structure-edu", "JetBrains Plugins Structure Edu", "Library for parsing JetBrains Edu plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements.")
    configurePublication("FleetPublication", "structure-fleet", "JetBrains Plugins Structure Fleet", "Library for parsing JetBrains Fleet plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements.")
    configurePublication("ToolboxPublication", "structure-toolbox", "JetBrains Plugins Structure Toolbox", "Library for parsing JetBrains Toolbox plugins. Can be used to verify that plugin complies with JetBrains Marketplace requirements.")
  }
}

signing {
  isRequired = mavenCentralUsername != null
  if (isRequired) {
    val signingKey = findProperty("signingKey").toString()
    val signingPassword = findProperty("signingPassword").toString()

    useInMemoryPgpKeys(signingKey, signingPassword)

    sign(publishing.publications["BasePublication"])
    sign(publishing.publications["ClassesPublication"])
    sign(publishing.publications["IntellijPublication"])
    sign(publishing.publications["IntellijClassesPublication"])
    sign(publishing.publications["IdePublication"])
    sign(publishing.publications["IdeClassesPublication"])
    sign(publishing.publications["TeamCityPublication"])
    sign(publishing.publications["DotNetPublication"])
    sign(publishing.publications["HubPublication"])
    sign(publishing.publications["EduPublication"])
    sign(publishing.publications["FleetPublication"])
    sign(publishing.publications["ToolboxPublication"])
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
