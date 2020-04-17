/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.teamcity.beans

import javax.xml.bind.annotation.*

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "teamcity-plugin")
class TeamcityPluginBean {
  @get:XmlElement(name = "info")
  var info: TeamCityPluginInfoBean? = null
  @get:XmlElement(name = "requirements")
  var requirements: TeamCityPluginRequirementsBean? = null
  @get:XmlElement(name = "deployment")
  var deployment: TeamCityPluginDeploymentBean? = null
  @get:XmlElementWrapper(name = "parameters")
  @get:XmlElement(name = "parameter")
  var parameters: List<Parameter> = ArrayList()
}

@XmlAccessorType(XmlAccessType.PROPERTY)
class TeamCityPluginInfoBean {
  @get:XmlElement(name = "name")
  var name: String? = null
  @get:XmlElement(name = "display-name")
  var displayName: String? = null
  @get:XmlElement(name = "version")
  var version: String? = null
  @get:XmlElement(name = "description")
  var description: String? = null
  @get:XmlElement(name = "download-url")
  var downloadUrl: String? = null
  @get:XmlElement(name = "email")
  var email: String? = null
  @get:XmlElement(name = "vendor")
  var vendor: Vendor? = null
}

@XmlAccessorType(XmlAccessType.PROPERTY)
class TeamCityPluginRequirementsBean {
  @get:XmlAttribute(name = "min-build")
  var minBuild: String? = null
  @get:XmlAttribute(name = "max-build")
  var maxBuild: String? = null
}

@XmlAccessorType(XmlAccessType.PROPERTY)
class TeamCityPluginDeploymentBean {
  @get:XmlAttribute(name = "use-separate-classloader")
  var useSeparateClassLoader: String? = null
}


class Parameter {
  @get:XmlAttribute(name = "name")
  var name: String? = null
  @get:XmlValue
  var value: String? = null
}

@XmlAccessorType(XmlAccessType.PROPERTY)
class Vendor {
  @get:XmlElement(name = "name")
  var name: String? = null
  @get:XmlElement(name = "url")
  var url: String? = null
  @get:XmlElement(name = "logo")
  var logo: String? = null
}