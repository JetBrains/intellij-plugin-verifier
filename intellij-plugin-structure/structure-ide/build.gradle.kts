
dependencies {
  api(project(":structure-base"))
  api(project(":structure-intellij"))

  implementation(sharedLibs.jetbrains.annotations)
  implementation(sharedLibs.slf4j.api)
  implementation(sharedLibs.woodstox)
  testImplementation(sharedLibs.junit)
}
