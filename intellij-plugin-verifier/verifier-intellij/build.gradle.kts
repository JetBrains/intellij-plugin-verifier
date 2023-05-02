repositories {
  mavenCentral()
}

val intellijStructureVersion : String by rootProject.extra

dependencies {
  api(project(":verifier-core"))
  api(project(":verifier-repository"))

  api("org.jgrapht:jgrapht-core:1.5.1")

  implementation("org.jsoup:jsoup:1.15.4")
  api("org.jetbrains.intellij.plugins:structure-ide-classes:$intellijStructureVersion")
}
