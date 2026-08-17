dependencies {
  api(project(":structure-base"))
  api(libs.jdom)

  implementation(libs.jaxb.api)
  runtimeOnly(libs.jaxb.runtime)

  // POC: JetBrains' own plugin.xml parser, extracted from the IntelliJ Platform.
  // Consumed by PlatformPluginDescriptorParser / RawPluginDescriptorToIdePluginConverter as an
  // alternative to the JDOM+JAXB pipeline above (PluginBeanExtractor / PluginBeanToIdePluginConverter).
  // Selected per plugin via PluginCreator.shouldUsePlatformParser (based on its declared since-build).
  implementation(libs.platform.plugin.system.parser.impl) {
    // Its transitive com.jetbrains.intellij.platform:util pulls in at.yawk.lz4:lz4-java, a newer fork
    // that declares itself a Gradle-capability replacement for net.jpountz.lz4:lz4. Any consumer that
    // also depends on something needing the old coordinates (e.g. org.mapdb:mapdb, as in
    // plugins-verifier-service) then fails to resolve at all - Gradle refuses to pick between two
    // different artifacts claiming the same capability. Excluded here since plugin.xml parsing never
    // touches LZ4 compression; util's own (unrelated) usage of it isn't something we call into.
    exclude(group = "at.yawk.lz4", module = "lz4-java")
  }
  // See comment on the catalog entry: needed directly because Maven "runtime" scope keeps it off
  // the default compileClasspath, but we reference com.intellij.util.xml.dom.* types directly.
  implementation(libs.platform.util.xml.dom)

  testImplementation(sharedLibs.junit)
  testImplementation(sharedLibs.mockk)
  testImplementation(libs.commons.compress)
}
