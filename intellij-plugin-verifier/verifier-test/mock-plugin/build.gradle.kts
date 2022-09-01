version = "1.0"

dependencies {
  //compile against "before-idea", "additional-before-idea" and verify against "after-idea", "additional-after-idea"

  compileOnly(project(":verifier-test:before-idea"))
  runtimeOnly(project(":verifier-test:after-idea"))
  compileOnly(project(":verifier-test:additional-before-idea"))
  runtimeOnly(project(":verifier-test:additional-after-idea"))
}
