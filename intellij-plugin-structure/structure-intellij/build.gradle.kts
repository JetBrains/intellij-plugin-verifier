dependencies {
  implementation(project(":structure-base"))
  implementation(sharedLibs.slf4j.api)
  implementation(libs.jdom)

  implementation(libs.jaxb.api)
  runtimeOnly(libs.jaxb.runtime)

  testImplementation(sharedLibs.junit)
  testImplementation(sharedLibs.mockk)
  testImplementation(libs.commons.compress)
}
