rootProject.name = "intellij-plugin-verifier"
include("verifier-cli")
include("verifier-core")
include("verifier-intellij")
include("verifier-repository")

include("verifier-test")
include("verifier-test:after-idea")
include("verifier-test:before-idea")
include("verifier-test:additional-after-idea")
include("verifier-test:additional-before-idea")
include("verifier-test:mock-plugin")

dependencyResolutionManagement {
  versionCatalogs {
    create("sharedLibs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
