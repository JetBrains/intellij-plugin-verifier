plugins {
  alias(libs.plugins.shadow)
}

dependencies {
  api(project(":verifier-intellij"))

  runtimeOnly(libs.logback.classic)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.spullara.cli.parser)
  implementation(libs.commons.lang3)
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
