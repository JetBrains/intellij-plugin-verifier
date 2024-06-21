package com.jetbrains.plugin.structure.intellij.beans

import java.nio.file.Path
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement

private const val DEFAULT_NAME: String = "##default"
private const val DEFAULT_PATH: String = "##default"

@XmlRootElement(name = "module")
data class ModuleBean(
  @field:XmlAttribute(name = "name") val name: String,

  @field:XmlElementWrapper(name = "dependencies")
  @field:XmlElement(name = "module")
  val dependencies: List<ModuleDependency> = emptyList(),

  @field:XmlElementWrapper(name = "resources")
  @field:XmlElement(name = "resource-root")
  val resources: List<ResourceRoot> = emptyList()
) {
  @Suppress("unused")
  constructor() : this(
    name = "",
    dependencies = mutableListOf(),
    resources = mutableListOf()
  )

  data class ModuleDependency(
    @field:XmlAttribute(name = "name") val name: String
  ) {
    @Suppress("unused")
    constructor() : this(DEFAULT_NAME)
  }

  data class ResourceRoot(
    @field:XmlAttribute(name = "path") private val pathValue: String
  ) {
    @Suppress("unused")
    constructor() : this(DEFAULT_PATH)

    val path: Path
      get() {
        if (pathValue == DEFAULT_PATH) {
          throw IllegalStateException("Resource root path is not set")
        }
        return Path.of(pathValue)
      }
  }
}


