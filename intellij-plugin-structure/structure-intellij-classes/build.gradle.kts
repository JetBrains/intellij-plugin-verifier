dependencies {
  api(project(":structure-intellij"))
  api(project(":structure-classes"))
  api(project(":structure-ide-classes"))

  implementation(sharedLibs.jdom)
}