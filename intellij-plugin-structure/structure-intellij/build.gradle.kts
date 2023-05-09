dependencies {
  api(project(":structure-base"))
  api(libs.jdom)

  implementation(libs.jsoup)
  implementation(libs.jaxb.api)
  implementation(libs.jaxb.runtime)
}
