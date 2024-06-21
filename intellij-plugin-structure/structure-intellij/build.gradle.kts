dependencies {
  api(project(":structure-base"))
  api(libs.jdom)

  implementation(sharedLibs.jsoup)
  implementation(libs.jaxb.api)
  implementation(libs.jaxb.runtime)

  testImplementation(sharedLibs.junit)
}
