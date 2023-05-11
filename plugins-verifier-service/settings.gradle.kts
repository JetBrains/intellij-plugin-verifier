rootProject.name = "plugins-verifier-service"

includeBuild("../intellij-feature-extractor")
includeBuild("../intellij-plugin-verifier")
includeBuild("../intellij-plugin-structure")

dependencyResolutionManagement {
  versionCatalogs {
    create("sharedLibs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
