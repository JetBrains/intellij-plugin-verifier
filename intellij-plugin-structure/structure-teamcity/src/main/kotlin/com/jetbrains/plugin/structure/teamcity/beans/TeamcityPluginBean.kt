package com.jetbrains.plugin.structure.teamcity.beans

import org.jonnyzzz.kotlin.xml.bind.XAttribute
import org.jonnyzzz.kotlin.xml.bind.XElements
import org.jonnyzzz.kotlin.xml.bind.XSub
import org.jonnyzzz.kotlin.xml.bind.XText
import org.jonnyzzz.kotlin.xml.bind.jdom.JXML

class TeamcityPluginBean {
  val name by JXML / "info" / "name" / XText
  val displayName by JXML / "info" / "display-name" / XText
  val version by JXML / "info" / "version" / XText
  val description by JXML / "info" / "description" / XText
  val downloadUrl by JXML / "info" / "download-url" / XText
  val email by JXML / "info" / "email" / XText
  val vendor by JXML / "info" / "vendor" / XSub(Vendor::class.java)
  val minBuild by JXML / "requirements" / XAttribute("min-build")
  val maxBuild by JXML / "requirements" / XAttribute("max-build")
  val useSeparateClassLoader by JXML / "deployment" / XAttribute("use-separate-classloader")
  val parameters by JXML / "parameters" / XElements("parameter") / XSub(Parameter::class.java)
}

class Parameter {
  val name by JXML / XAttribute("name")
  val value by JXML / XText
}

class Vendor {
  val name by JXML / "name" / XText
  val url by JXML / "url" / XText
  val logo by JXML / "logo" / XText
}