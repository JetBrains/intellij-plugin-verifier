tasks.register("clean") {
    dependsOn(gradle.includedBuild("ide-diff-builder").task(":clean"))
    dependsOn(gradle.includedBuild("intellij-feature-extractor").task(":clean"))
    dependsOn(gradle.includedBuild("intellij-plugin-structure").task(":clean"))
    dependsOn(gradle.includedBuild("intellij-plugin-verifier").task(":clean"))
    dependsOn(gradle.includedBuild("plugins-verifier-service").task(":clean"))
}