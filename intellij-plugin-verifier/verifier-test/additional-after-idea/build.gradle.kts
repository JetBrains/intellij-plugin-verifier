version = "1.0"

dependencies {
  implementation(project(":verifier-test:after-idea"))
}

// Fixture sources use bare `@ApiStatus.*` annotations on package declarations, which
// the javadoc tool reads as unknown tags. These modules are not consumed for docs.
tasks.javadoc { enabled = false }
