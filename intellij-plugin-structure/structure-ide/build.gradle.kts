
dependencies {
  api(project(":structure-base"))
  api(project(":structure-intellij"))

  implementation(sharedLibs.slf4j.api)
  implementation(sharedLibs.woodstox)
  testImplementation(sharedLibs.junit)
}
