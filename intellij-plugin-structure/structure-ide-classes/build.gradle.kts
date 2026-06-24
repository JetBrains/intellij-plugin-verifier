dependencies {
  implementation(sharedLibs.slf4j.api)
  implementation(project(":structure-ide"))
  implementation(project(":structure-classes"))

  implementation(sharedLibs.caffeine)
}