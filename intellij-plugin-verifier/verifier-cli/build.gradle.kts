plugins {
  alias(sharedLibs.plugins.shadow)
}

dependencies {
  api(project(":verifier-intellij"))

  runtimeOnly(sharedLibs.logback.classic)
  implementation(sharedLibs.jackson.module.kotlin)
  implementation(sharedLibs.spullara.cliParser)
}

val projectVersion: String by rootProject.extra

val versionTxt by tasks.registering {
  val versionTxt = layout.buildDirectory.file("intellij-plugin-verifier-version.txt")
  outputs.file(versionTxt)
  doLast {
    versionTxt.orNull?.asFile?.run {
      writeText(projectVersion)
    }
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
