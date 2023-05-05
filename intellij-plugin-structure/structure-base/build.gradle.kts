dependencies {
  api("org.jetbrains:annotations:24.0.1")
  api("org.slf4j:slf4j-api:2.0.7")

  api("commons-io:commons-io:2.10.0")

  implementation("org.apache.commons:commons-compress:1.23.0")
  implementation("org.tukaani:xz:1.9")

  //Provides English class capable of pluralizing english words.
  implementation("org.atteo:evo-inflector:1.3")
}