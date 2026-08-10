dependencies {
  api(project(":structure-base"))
  api(libs.jdom)

  implementation(libs.jaxb.api)
  runtimeOnly(libs.jaxb.runtime)

  // POC: JetBrains' own plugin.xml parser, extracted from the IntelliJ Platform.
  // Consumed by PlatformPluginDescriptorParser / RawPluginDescriptorToIdePluginConverter as an
  // alternative to the JDOM+JAXB pipeline above (PluginBeanExtractor / PluginBeanToIdePluginConverter).
  // Selected at runtime via Settings.USE_PLATFORM_PLUGIN_PARSER - see PluginCreator.resolveDocumentAndValidateBean.
  implementation(libs.platform.plugin.system.parser.impl)
  // See comment on the catalog entry: needed directly because Maven "runtime" scope keeps it off
  // the default compileClasspath, but we reference com.intellij.util.xml.dom.* types directly.
  implementation(libs.platform.util.xml.dom)

  testImplementation(sharedLibs.junit)
  testImplementation(sharedLibs.mockk)
  testImplementation(libs.commons.compress)
}
