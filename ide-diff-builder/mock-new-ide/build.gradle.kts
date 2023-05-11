version = "1.0"

val prepareIde by tasks.registering(Copy::class) {
  dependsOn(tasks.build)

  into("$buildDir/mock-ide")

  val ideaJar = copySpec {
    from("$buildDir/libs/mock-new-ide-1.0.jar")
    into("lib")
  }

  val buildTxt = copySpec {
    from("$buildDir/resources/main/build.txt")
    into(".")
  }

  with(ideaJar, buildTxt)
}
