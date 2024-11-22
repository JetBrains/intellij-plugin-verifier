package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.verifiers.packages.PackageFilter

class MockPackageFilter : PackageFilter {
  override fun acceptPackageOfClass(binaryClassName: String) = true
}