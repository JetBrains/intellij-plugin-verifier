rootProject.name = "intellij-feature-extractor"
include("test-classes")

dependencyResolutionManagement {
  versionCatalogs {
    create("sharedLibs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}