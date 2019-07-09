package com.jetbrains.pluginverifier.verifiers.packages

class NegatedPackageFilter(private val packageFilter: PackageFilter) : PackageFilter {
  override fun acceptPackageOfClass(binaryClassName: String) =
      !packageFilter.acceptPackageOfClass(binaryClassName)
}