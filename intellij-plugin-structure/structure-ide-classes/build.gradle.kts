dependencies {
  api(project(":structure-ide"))
  api(project(":structure-classes"))

  implementation(sharedLibs.slf4j.api)
  implementation(sharedLibs.caffeine)
}