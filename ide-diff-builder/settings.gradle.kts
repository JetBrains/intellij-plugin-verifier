rootProject.name = "ide-diff-builder"
include("mock-old-ide")
include("mock-new-ide")

dependencyResolutionManagement {
    versionCatalogs {
        create("sharedLibs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
