dependencies {
  api(project(":structure-base"))

  implementation(sharedLibs.jetbrains.annotations)
  implementation(sharedLibs.slf4j.api)
  implementation(sharedLibs.caffeine)
  implementation(sharedLibs.commons.io)
  implementation(libs.jdom)

  implementation(libs.jaxb.api)
  runtimeOnly(libs.jaxb.runtime)

  testImplementation(sharedLibs.junit)
  testImplementation(sharedLibs.mockk)
  testImplementation(libs.commons.compress)
}
