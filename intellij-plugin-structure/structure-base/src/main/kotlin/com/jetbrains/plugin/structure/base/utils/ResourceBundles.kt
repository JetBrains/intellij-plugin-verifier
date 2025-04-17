package com.jetbrains.plugin.structure.base.utils

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