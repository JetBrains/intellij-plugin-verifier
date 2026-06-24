val intellijStructureVersion : String by rootProject.extra

dependencies {
  implementation(project(":verifier-core"))
  implementation(project(":verifier-repository"))

  implementation(libs.jgrapht.core)

  implementation(sharedLibs.jsoup)

  implementation("org.jetbrains.intellij.plugins:structure-ide-classes:$intellijStructureVersion")
}
