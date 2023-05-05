val intellijStructureVersion : String by rootProject.extra

dependencies {
  api(project(":verifier-core"))
  api(project(":verifier-repository"))

  api("org.jgrapht:jgrapht-core:1.5.1")

  implementation("org.jsoup:jsoup:1.16.1")
  api("org.jetbrains.intellij.plugins:structure-ide-classes:$intellijStructureVersion")
}
