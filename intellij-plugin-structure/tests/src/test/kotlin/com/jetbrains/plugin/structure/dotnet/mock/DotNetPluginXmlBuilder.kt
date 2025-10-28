package com.jetbrains.plugin.structure.dotnet.mock

data class DotNetPluginXmlBuilder(
  var dotNetPluginTagOpen: String = "<package><metadata>",
  var dotNetPluginTagClose: String = "</metadata></package>",
  var id: String = "<id>Vendor.PluginName</id>",
  var title: String? = "<title>Some title</title>",
  var version: String = "<version>10.2.55</version>",
  var authors: String = "<authors>JetBrains</authors>",
  var owners: String = "<owners>JetBrains</owners>",
  var licenseUrl: String = "<licenseUrl>https://url.com</licenseUrl>",
  var projectUrl: String = "<projectUrl>https://github.com/JetBrains/ExternalAnnotations</projectUrl>",
  var description: String = "<description>ReSharper External Annotations for .NET framework and popular libraries.</description>",
  var summary: String = "<summary>Some summary</summary>",
  var releaseNotes: String = "<releaseNotes>ReSharper 8.2 compatibility</releaseNotes>",
  var copyright: String = "<copyright>Copyright 2014 JetBrains</copyright>",
  var dependencies: String = "<dependencies><dependency id=\"ReSharper\" version=\"[8.0, 8.3)\" /><dependency id=\"Wave\"/></dependencies>"
) {

  fun asString(): String = listOfNotNull(
    dotNetPluginTagOpen,
    id,
    title,
    version,
    authors,
    owners,
    licenseUrl,
    projectUrl,
    description,
    summary,
    releaseNotes,
    copyright,
    dependencies,
    dotNetPluginTagClose
  ).joinToString(separator = "\n")
}

val perfectDotNetBuilder: DotNetPluginXmlBuilder = DotNetPluginXmlBuilder().apply {
}

fun DotNetPluginXmlBuilder.modify(block: DotNetPluginXmlBuilder.() -> Unit): String {
  val copy = copy()
  copy.block()
  return copy.asString()
}