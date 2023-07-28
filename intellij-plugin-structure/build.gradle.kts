import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `maven-publish`
  signing
  alias(sharedLibs.plugins.kotlin.jvm)
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
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
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
  }

  val javadocJar by tasks.registering(Jar::class) {
    val javadoc = tasks.javadoc
    dependsOn(javadoc)
    archiveClassifier.set("javadoc")
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
  this.repositories {
    sonatype {
      username.set(mavenCentralUsername)
      password.set(mavenCentralPassword)
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
          name.set(pubName)
          description.set(pubDesc)
          url.set("https://github.com/JetBrains/intellij-plugin-verifier/tree/master/intellij-plugin-structure/$projectName")
          licenses {
            license {
              name.set("The Apache Software License, Version 2.0")
              url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
          }
          developers {
            developer {
              id.set("satamas")
              name.set("Semyon Atamas")
              organization.set("JetBrains")
            }
            developer {
              id.set("AlexanderPrendota")
              name.set("Alexander Prendota")
              organization.set("JetBrains")
            }
            developer {
              id.set("ktisha")
              name.set("Ekaterina Smal")
              organization.set("JetBrains")
            }
            developer {
              id.set("chashnikov")
              name.set("Nikolay Chashnikov")
              organization.set("JetBrains")
            }
            developer {
              id.set("chrkv")
              name.set("Ivan Chirkov")
              organization.set("JetBrains")
            }
            developer {
              id.set("Ololoshechkin")
              name.set("Brilyantov Vadim")
              organization.set("JetBrains")
            }
            developer {
              id.set("shalupov")
              name.set("Leonid Shalupov")
              organization.set("JetBrains")
            }
            developer {
              id.set("hsz")
              name.set("Jakub Chrzanowski")
              organization.set("JetBrains")
            }
            developer {
              id.set("kesarevs")
              name.set("Kesarev Sergey")
              organization.set("JetBrains")
            }
            developer {
              id.set("LChernigovskaya")
              name.set("Lidiya Chernigovskaya")
              organization.set("JetBrains")
            }
            developer {
              id.set("novotnyr")
              name.set("Robert Novotny")
              organization.set("JetBrains")
            }
          }
          scm {
            connection.set("scm:git:git://github.com/JetBrains/intellij-plugin-verifier.git")
            developerConnection.set("scm:git:ssh://github.com/JetBrains/intellij-plugin-verifier.git")
            url.set("https://github.com/JetBrains/intellij-plugin-verifier")
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
