package com.jetbrains.plugin.structure.intellij.version


/**
 * Type-safe `release-version` of paid plugins.
 *
 * Relevant docs:
 * - See
 * [Plugin Configuration file](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html#idea-plugin__product-descriptor)
 * at IntelliJ Platform Plugin SDK
 * - [JetBrains Marketplace](https://plugins.jetbrains.com/docs/marketplace/add-required-parameters.html) guidelines
 */
data class ProductReleaseVersion(val value: Int) {
  val major: Int
    get() = if (isSingleDigit) 0 else value / 10

  val minor: Int
    get() = if (isSingleDigit) value else value % 10

  val isSingleDigit: Boolean
    get() = value in 1..9

  override fun toString() = value.toString()

  companion object {
    fun parse(releaseVersionValue: String): ProductReleaseVersion {
      return ProductReleaseVersion(releaseVersionValue.parseIntOrThrow())
    }

    @Throws(NumberFormatException::class)
    private fun String.parseIntOrThrow() =
      this.toIntOrNull()
        ?: throw NumberFormatException("Release version [${this}] must be an integer")
  }
}