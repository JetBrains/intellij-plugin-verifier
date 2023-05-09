dependencies {
  api(libs.jetbrains.annotations)
  api(libs.slf4j.api)

  api(libs.commons.io)

  implementation(libs.commons.compress)
  implementation(libs.xz)

  //Provides English class capable of pluralizing english words.
  implementation(libs.evo.inflector)
}