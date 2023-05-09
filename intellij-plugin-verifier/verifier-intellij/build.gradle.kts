val intellijStructureVersion : String by rootProject.extra

dependencies {
  api(project(":verifier-core"))
  api(project(":verifier-repository"))

  api(libs.jgrapht.core)

  implementation(sharedLibs.jsoup)

  api("org.jetbrains.intellij.plugins:structure-ide-classes:$intellijStructureVersion")
}
