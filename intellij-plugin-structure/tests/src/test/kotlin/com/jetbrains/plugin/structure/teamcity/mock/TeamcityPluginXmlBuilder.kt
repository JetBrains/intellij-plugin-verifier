package com.jetbrains.plugin.structure.teamcity.mock

data class TeamcityPluginXmlBuilder(
    var teamcityPluginTagOpen: String = "<teamcity-plugin>",
    var teamcityPluginTagClose: String = "</teamcity-plugin>",
    var name: String = "<name>name</name>",
    var displayName: String = "<display-name>Display name</display-name>",
    var version: String = "<version>0.1.1</version>",
    var description: String = "<description>Some short description</description>",
    var downloadUrl: String = "",
    var email: String = "",
    var deployment: String = "",
    val vendor: String = "<vendor><name>JetBrains, s.r.o.</name></vendor>",
    val requirements: String = "",
    val parameters: String = "",
    var additionalContent: String = ""
) {

  fun asString(): String = """
  $teamcityPluginTagOpen
  <info>
    $name
    $displayName
    $version
    $description
    $downloadUrl
    $email
    $vendor
  </info>
  $requirements
  $deployment
  $parameters
  $additionalContent
  $teamcityPluginTagClose
"""
}

val perfectXmlBuilder: TeamcityPluginXmlBuilder = TeamcityPluginXmlBuilder().apply {
}

fun TeamcityPluginXmlBuilder.modify(block: TeamcityPluginXmlBuilder.() -> Unit): String {
  val copy = copy()
  copy.block()
  return copy.asString()
}