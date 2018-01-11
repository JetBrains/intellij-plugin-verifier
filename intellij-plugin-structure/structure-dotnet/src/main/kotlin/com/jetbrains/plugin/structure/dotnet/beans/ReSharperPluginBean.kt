package com.jetbrains.plugin.structure.dotnet.beans

import com.jetbrains.plugin.structure.dotnet.DotNetDependency
import com.jetbrains.plugin.structure.dotnet.ReSharperPlugin
import org.jonnyzzz.kotlin.xml.bind.XAttribute
import org.jonnyzzz.kotlin.xml.bind.XElements
import org.jonnyzzz.kotlin.xml.bind.XSub
import org.jonnyzzz.kotlin.xml.bind.XText
import org.jonnyzzz.kotlin.xml.bind.jdom.JXML

class ReSharperPluginBean {
  val id by JXML / "metadata" / "id" / XText
  val title by JXML / "metadata" / "title" / XText
  val version by JXML / "metadata" / "version" / XText
  val authors by JXML / "metadata" / "authors" / XText
  val summary by JXML / "metadata" / "summary" / XText
  val description by JXML / "metadata" / "description" / XText
  val url by JXML / "metadata" / "projectUrl" / XText
  val changeNotes by JXML / "metadata" / "releaseNotes" / XText
  val licenseUrl by JXML / "metadata" / "licenseUrl" / XText
  val copyright by JXML / "metadata" / "copyright" / XText
  val dependencies by JXML / "metadata" / "dependencies" / XElements("dependency") / XSub(DotNetDependencyBean::class.java)
}

class DotNetDependencyBean {
  val id by JXML / XAttribute("id")
  val version by JXML / XAttribute("version")
}

fun ReSharperPluginBean.toPlugin(): ReSharperPlugin {
  val id = this.id!!
  val idParts = id.split('.')
  val vendor = if (idParts.size > 1) idParts[0] else null
  val authors = authors!!.split(',').map { it.trim() }
  val pluginName = when {
    title != null -> title!!
    idParts.size > 1 -> idParts[1]
    else -> id
  }
  return ReSharperPlugin(
      pluginId = id, pluginName = pluginName, vendor = vendor, pluginVersion = this.version!!, url = this.url,
      changeNotes = this.changeNotes, description = this.description, vendorEmail = null, vendorUrl = null,
      authors = authors,licenseUrl = licenseUrl, copyright = copyright, summary = summary,
      dependencies = dependencies?.map { DotNetDependency(it.id!!, it.version!!) } ?: emptyList()
  )
}