package com.jetbrains.pluginverifier.verifiers.packages

interface PackageFilter {
  fun acceptPackageOfClass(binaryClassName: String): Boolean
}