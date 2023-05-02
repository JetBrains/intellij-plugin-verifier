plugins {
  id("com.github.johnrengelman.shadow") version "7.1.2"
}

dependencies {
  api(project(":verifier-intellij"))

  runtimeOnly("ch.qos.logback:logback-classic:1.4.6")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
  implementation("com.github.spullara.cli-parser:cli-parser:1.1.6")
  implementation("org.apache.commons:commons-lang3:3.12.0")
}

val projectVersion: String by rootProject.extra

val versionTxt by tasks.registering {
  val versionTxt = File(buildDir, "intellij-plugin-verifier-version.txt")
  outputs.file(versionTxt)
  doLast {
    versionTxt.writeText(projectVersion)
  }
}

tasks {
  jar {
    metaInf {
      from(versionTxt)
    }
    finalizedBy(shadowJar)
  }
  shadowJar {
    metaInf {
      from(versionTxt)
    }
    manifest {
      attributes("Main-Class" to "com.jetbrains.pluginverifier.PluginVerifierMain")
    }
    archiveClassifier.set("all")
    //Exclude resources/dlls and other stuff coming from the dependencies.
    exclude(
        "/win32/**",
        "/tips/**",
        "/search/**",
        "/linux/**",
        "/intentionDescriptions/**",
        "/inspectionDescriptions/**",
        "/fileTemplates/**",
        "/darwin/**",
        "**.dll"
    )
  }
}
