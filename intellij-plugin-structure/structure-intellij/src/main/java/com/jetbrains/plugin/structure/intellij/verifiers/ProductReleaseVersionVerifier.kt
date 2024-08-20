package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.NotNumber
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.problems.ReleaseVersionWrongFormat
import com.jetbrains.plugin.structure.intellij.verifiers.ProductReleaseVersionVerifier.VerificationResult.Invalid
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion

class ProductReleaseVersionVerifier {
  fun verify(plugin: PluginBean, descriptorPath: String, problemRegistrar: ProblemRegistrar): VerificationResult {
    if (plugin.productDescriptor == null) return VerificationResult.NotApplicable

    val releaseVersionValue = plugin.productDescriptor?.releaseVersion
    if (releaseVersionValue.isNullOrEmpty()) {
      return Invalid("Attribute '$RELEASE_VERSION_ATTRIBUTE_NAME' is missing").also {
        problemRegistrar.registerProblem(PropertyNotSpecified(RELEASE_VERSION_ATTRIBUTE_NAME, descriptorPath))
      }
    }

    return try {
      ProductReleaseVersion.parse(releaseVersionValue).run {
        if (isSingleDigit) {
          Invalid("Attribute '$RELEASE_VERSION_ATTRIBUTE_NAME' is must have two or more digits: '$releaseVersionValue'").also {
            problemRegistrar.registerProblem(ReleaseVersionWrongFormat(descriptorPath, releaseVersionValue))
          }
        } else {
          VerificationResult.Valid(this)
        }
      }
    } catch (e: NumberFormatException) {
      Invalid("Attribute '$RELEASE_VERSION_ATTRIBUTE_NAME' is not an integer: '$releaseVersionValue'").also {
        problemRegistrar.registerProblem(NotNumber(RELEASE_VERSION_ATTRIBUTE_NAME, descriptorPath))
      }
    }
  }

  sealed class VerificationResult {
    data class Valid(val version: ProductReleaseVersion) : VerificationResult()
    data class Invalid(val message: String) : VerificationResult()
    object NotApplicable : VerificationResult()
  }
}

private const val RELEASE_VERSION_ATTRIBUTE_NAME = "release-version"