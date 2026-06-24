dependencies {
  implementation(sharedLibs.slf4j.api)
  implementation(project(":structure-base"))
  implementation(project(":structure-intellij"))
  implementation(libs.semver4j)
}