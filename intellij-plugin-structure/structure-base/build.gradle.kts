dependencies {
  api(sharedLibs.jetbrains.annotations)
  api(sharedLibs.slf4j.api)

  api(sharedLibs.commons.io)

  api(sharedLibs.caffeine)

  implementation(libs.commons.compress)
  implementation(sharedLibs.xz)
  implementation(libs.trove4j)

  //Provides English class capable of pluralizing english words.
  implementation(libs.evo.inflector)
}