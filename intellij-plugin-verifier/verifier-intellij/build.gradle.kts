val intellijStructureVersion : String by rootProject.extra

dependencies {
  api("org.jetbrains.intellij.plugins:structure-base:${intellijStructureVersion}")
  api("org.jetbrains.intellij.plugins:structure-classes:${intellijStructureVersion}")
  api("org.jetbrains.intellij.plugins:structure-ide-classes:$intellijStructureVersion")
  api("org.jetbrains.intellij.plugins:structure-intellij-classes:${intellijStructureVersion}")
  api("org.jetbrains.intellij.plugins:structure-intellij:${intellijStructureVersion}")

  api(project(":verifier-core"))
  api(project(":verifier-repository"))

  implementation(libs.jgrapht.core)
  implementation(sharedLibs.jdom)
  implementation(sharedLibs.jsoup)
}
