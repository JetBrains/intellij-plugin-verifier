package com.jetbrains.plugin.structure.classes.utils

/**
 * ```
 * path/SomeBundle.properties -> path.SomeBundle
 * path/SomeBundle_en.properties -> path.SomeBundle_en
 * path/SomeBundle_en_UK.properties -> path.SomeBundle_en_UK
 * ```
 */
fun getBundleNameByBundlePath(bundlePath: String): String =
    bundlePath.substringBeforeLast(".properties").replace('/', '.')

/**
 * ```
 * path.SomeBundle        -> path.SomeBundle
 * path.SomeBundle_en     -> path.SomeBundle
 * path.SomeBundle_en_UK  -> path.SomeBundle
 * ```
 */
fun getBundleBaseName(fullBundleName: String): String {
  val packageName = fullBundleName.substringBeforeLast(".", "")
  val simpleBaseBundleName = fullBundleName.substringAfterLast(".").substringBefore("_")
  return (if (packageName.isEmpty()) "" else "$packageName.") + simpleBaseBundleName
}