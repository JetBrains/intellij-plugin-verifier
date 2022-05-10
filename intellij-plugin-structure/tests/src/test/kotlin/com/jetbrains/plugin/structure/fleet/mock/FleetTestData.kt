package com.jetbrains.plugin.structure.fleet.mock

import fleet.bundles.*

val perfectFleetPluginBuilder
  get() = BundleSpec(
    BundleId(BundleName("fleet.language.css"), BundleVersion("1.0.0-SNAPSHOT")),
    Bundle(
      deps = setOf(),
      barrels = mapOf(
        BarrelSelector.Frontend to
          Barrel(
            setOf(Coordinates.Remote("https://plugins.jetbrains.com/files/fleet/fleet/fleet.plugin/1.0.0/modules/f-1.1.1.jar", "123")),
            setOf(Coordinates.Remote("https://plugins.jetbrains.com/files/fleet/fleet/fleet.plugin/1.0.0/modules/f-cp.jar", "abc")),
            setOf(),
            setOf("f")
          )
      ),
      meta = mapOf(
        KnownMeta.ReadableName to "CSS",
        KnownMeta.Description to "CSS language support",
        KnownMeta.Vendor to "JetBrains",
      )
    )
  )

fun getMockPluginJsonContent(fileName: String): String {
  return object {}.javaClass.getResource("/fleet/$fileName.json").readText()
}
