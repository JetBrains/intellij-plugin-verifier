package com.jetbrains.structure.teamcity.mock

data class TeamcityPluginXmlBuilder(
    var teamcityPluginTagOpen: String = "<teamcity-plugin>",
    var teamcityPluginTagClose: String = "</teamcity-plugin>",
    var name: String = "<name>name</name>",
    var displayName: String = "<display-name>Display name</display-name>",
    var version: String = "<version>0.1.1</version>",
    var description: String = "",
    var downloadUrl: String = "",
    var email: String = "",
    var deployment: String = "",
    val vendor: String = "",
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