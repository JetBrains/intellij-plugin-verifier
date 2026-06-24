dependencies {
  api(project(":structure-base"))
  implementation(project(":structure-intellij"))

  implementation(sharedLibs.slf4j.api)
  implementation(libs.semver4j)
}