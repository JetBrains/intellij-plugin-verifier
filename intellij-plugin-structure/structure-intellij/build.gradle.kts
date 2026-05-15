dependencies {
  api(project(":structure-base"))
  api(libs.jdom)

  implementation(libs.jaxb.api)
  implementation(libs.jaxb.runtime)

  testImplementation(sharedLibs.junit)
  testImplementation(sharedLibs.mockk)
  testImplementation(libs.commons.compress)
}
