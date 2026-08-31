package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.NotNumber
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.problems.ReleaseVersionAndPluginVersionMismatch
import com.jetbrains.plugin.structure.intellij.problems.ReleaseVersionWrongFormat
import com.jetbrains.plugin.structure.intellij.verifiers.ProductReleaseVersionVerifier.VerificationResult.Invalid
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion

class ProductReleaseVersionVerifier {
  fun verify(plugin: PluginBean, descriptorPath: String, problemRegistrar: ProblemRegistrar): VerificationResult {
    if (plugin.productDescriptor == null) return VerificationResult.NotApplicable
    return verify(plugin.productDescriptor?.releaseVersion, plugin.pluginVersion, descriptorPath, problemRegistrar)
  }

  /**
   * The raw `release-version` and plugin `version` strings are all this verifier needs, so it takes them
   * directly and both descriptor pipelines can share it. The strings matter: `release-version` is checked
   * for a leading-zero/single-digit shape that is gone the moment it is parsed to an `Int`, which is why
   * [PlatformDescriptorValidator][com.jetbrains.plugin.structure.intellij.plugin.PlatformDescriptorValidator]
   * reads it off the descriptor XML rather than from `RawPluginDescriptor.releaseVersion`.
   *
   * Call only when a `<product-descriptor>` is present; absence is the caller's `NotApplicable`.
   */
  fun verify(
    releaseVersionValue: String?,
    pluginVersion: String?,
    descriptorPath: String,
    problemRegistrar: ProblemRegistrar
  ): VerificationResult {
    if (releaseVersionValue.isNullOrEmpty()) {
      return Invalid("Attribute '$RELEASE_VERSION_ATTRIBUTE_NAME' is missing").also {
        problemRegistrar.registerProblem(PropertyNotSpecified(RELEASE_VERSION_ATTRIBUTE_NAME, descriptorPath))
      }
    }

    return try {
      ProductReleaseVersion.parse(releaseVersionValue).run {
        if (isSingleDigit) {
          Invalid("Attribute '$RELEASE_VERSION_ATTRIBUTE_NAME' must have two or more digits: '$releaseVersionValue'").also {
            problemRegistrar.registerProblem(ReleaseVersionWrongFormat(descriptorPath, releaseVersionValue))
          }
        } else {
          verifyPluginVersionAndReleaseVersionMatch(
            pluginVersion,
            productReleaseVersion = this,
            descriptorPath,
            problemRegistrar
          )
          VerificationResult.Valid(this)
        }
      }
    } catch (e: NumberFormatException) {
      Invalid("Attribute '$RELEASE_VERSION_ATTRIBUTE_NAME' is not an integer: '$releaseVersionValue'").also {
        problemRegistrar.registerProblem(NotNumber(RELEASE_VERSION_ATTRIBUTE_NAME, descriptorPath))
      }
    }
  }

  private fun verifyPluginVersionAndReleaseVersionMatch(
    pluginVersion: String?,
    productReleaseVersion: ProductReleaseVersion,
    descriptorPath: String,
    problemRegistrar: ProblemRegistrar
  ) {
    if (pluginVersion == null) return

    val majorMinorVersion = MajorMinorVersion.parse(pluginVersion) ?: return
    if (!majorMinorVersion.matches(productReleaseVersion)) {
      problemRegistrar.registerProblem(
        ReleaseVersionAndPluginVersionMismatch(
          descriptorPath,
          productReleaseVersion,
          pluginVersion
        )
      )
    }
  }

  private data class MajorMinorVersion(val major: Int, val minor: Int) {
    fun matches(productReleaseVersion: ProductReleaseVersion): Boolean {
      return major == productReleaseVersion.major && minor == productReleaseVersion.minor
    }

    companion object {
      fun parse(pluginVersion: String): MajorMinorVersion? {
        val pluginVersionParts = pluginVersion.split(".")
        val major = pluginVersionParts[0].toIntOrNull() ?: return null
        val minor = if (pluginVersionParts.size > 1) {
          pluginVersionParts[1].split("-")[0].toIntOrNull() ?: 0
        } else 0
        return MajorMinorVersion(major, minor)
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