dependencies {
  implementation(project(":structure-base"))
  implementation(sharedLibs.slf4j.api)
  implementation(libs.jackson.yaml)
  implementation(libs.semver4j)
}