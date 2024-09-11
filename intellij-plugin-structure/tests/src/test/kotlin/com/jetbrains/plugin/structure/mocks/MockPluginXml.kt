package com.jetbrains.plugin.structure.mocks

data class PluginXmlBuilder(
  var ideaPluginTagOpen: String = "<idea-plugin>",
  var ideaPluginTagClose: String = "</idea-plugin>",
  var id: String = "",
  var name: String = "",
  var version: String = "",
  var vendor: String = "",
  var description: String = "",
  var changeNotes: String = "",
  var ideaVersion: String = "",
  var modules: List<String> = emptyList(),
  var depends: String = "",
  var productDescriptor: String = "",
  var additionalContent: String = ""
) {

  fun asString(): String = """
  $ideaPluginTagOpen
  $id
  $name
  $version
  $vendor
  $description
  $changeNotes
  $ideaVersion
  $productDescriptor
  ${modules.map { "<module value=\"$it\"/>" }.joinToString(separator = "\n")}
  $depends
  $additionalContent
  $ideaPluginTagClose
"""
}

val perfectXmlBuilder: PluginXmlBuilder
  get() = PluginXmlBuilder().apply {
    id = "<id>someId</id>"
    name = "<name>someName</name>"
    version = "<version>1</version>"
    vendor = """<vendor email="vendor.com" url="url">vendor</vendor>"""
    description = "<description>this description is looooooooooong enough</description>"
    changeNotes = "<change-notes>these change-notes are looooooooooong enough</change-notes>"
    ideaVersion = """<idea-version since-build="131.1"/>"""
    depends = "<depends>com.intellij.modules.lang</depends>"
  }

fun PluginXmlBuilder.modify(block: PluginXmlBuilder.() -> Unit): String {
  val copy = copy()
  copy.block()
  return copy.asString()
}