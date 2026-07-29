plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "IntelliJ Plugin Verifier"

includeBuild("intellij-plugin-structure")
includeBuild("intellij-plugin-verifier")

includeBuild("ide-diff-builder")
includeBuild("intellij-feature-extractor")
