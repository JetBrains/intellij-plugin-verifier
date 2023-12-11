version = "1.0"

val prepareIde by tasks.registering(Copy::class) {
  dependsOn(tasks.build)

  into(layout.buildDirectory.dir("mock-ide"))

  val ideaJar = copySpec {
    from(layout.buildDirectory.file("libs/mock-old-ide-1.0.jar"))
    into("lib")
  }

  val buildTxt = copySpec {
    from(layout.buildDirectory.file("resources/main/build.txt"))
    into(".")
  }

  with(ideaJar, buildTxt)
}
