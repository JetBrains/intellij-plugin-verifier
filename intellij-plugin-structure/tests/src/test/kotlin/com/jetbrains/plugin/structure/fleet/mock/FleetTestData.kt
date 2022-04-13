package com.jetbrains.plugin.structure.fleet.mock

import fleet.bundles.Barrel
import fleet.bundles.BundleName
import fleet.bundles.BundleVersion
import fleet.bundles.PluginDescriptor

val perfectFleetPluginBuilder
  get() = PluginDescriptor(
    id = BundleName("fleet.language.css"),
    version = BundleVersion("1.0.0-SNAPSHOT"),
    readableName = "CSS",
    description = "CSS language support",
    vendor = "JetBrains",
    deps = mapOf(),
    frontend = Barrel(
      setOf(Barrel.Coordinates.Relative("f-1.1.1.jar", "123")),
      setOf(Barrel.Coordinates.Relative("f-cp.jar", "abc")),
      setOf(),
      setOf("f")
    ),
    workspace = null
  )

fun getMockPluginJsonContent(fileName: String): String {
  return object {}.javaClass.getResource("/fleet/$fileName.json").readText()
}
