dependencies {
  api(sharedLibs.jetbrains.annotations)
  api(sharedLibs.slf4j.api)

  api(sharedLibs.commons.io)

  api(sharedLibs.caffeine)

  implementation(libs.commons.compress)
  implementation(sharedLibs.xz)

  //Provides English class capable of pluralizing english words.
  implementation(libs.evo.inflector)
}