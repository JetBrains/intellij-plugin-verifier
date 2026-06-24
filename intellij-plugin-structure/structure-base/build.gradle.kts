dependencies {
  implementation(sharedLibs.jetbrains.annotations)
  implementation(sharedLibs.slf4j.api)

  implementation(sharedLibs.commons.io)

  implementation(sharedLibs.caffeine)

  implementation(libs.commons.compress)
  implementation(sharedLibs.xz)
  implementation(libs.trove4j)

  implementation(sharedLibs.jsoup)

  //Provides English class capable of pluralizing english words.
  implementation(libs.evo.inflector)
}