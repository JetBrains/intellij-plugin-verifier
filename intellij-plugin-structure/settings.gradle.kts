rootProject.name = "intellij-plugin-structure"
include("structure-base")
include("structure-classes")
include("structure-intellij")
include("structure-intellij-classes")
include("structure-teamcity")
include("structure-dotnet")
include("structure-hub")
include("structure-edu")
include("structure-fleet")
include("structure-toolbox")
include("structure-ide")
include("structure-ide-classes")
include("structure-youtrack")
include("structure-teamcity-recipes")
include("tests")

dependencyResolutionManagement {
  versionCatalogs {
    create("sharedLibs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
