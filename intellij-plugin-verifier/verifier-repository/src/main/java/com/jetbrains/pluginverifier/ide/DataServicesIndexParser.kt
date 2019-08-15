package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.ide.IntelliJPlatformProduct
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.repositories.Download
import com.jetbrains.pluginverifier.ide.repositories.Product
import com.jetbrains.pluginverifier.ide.repositories.Release
import com.jetbrains.pluginverifier.ide.repositories.setProductCodeIfAbsent
import java.net.URL
import java.time.LocalDate

internal class DataServicesIndexParser {
  internal fun parseAvailableIdes(products: List<Product>): List<AvailableIde> {
    val availableIdes = arrayListOf<AvailableIde>()
    for (product in products) {
      val intelliJPlatformProduct = getIntelliJProduct(product)
      if (intelliJPlatformProduct != null) {
        for (release in product.releases) {
          if (release.downloads != null) {
            val download = getBuildDownload(release.downloads)
            if (download != null && release.build != null) {
              val downloadUrl = URL(download.link)
              val ideVersion = IdeVersion.createIdeVersionIfValid(release.build)
                  ?.setProductCodeIfAbsent(intelliJPlatformProduct.productCode)
              if (ideVersion != null) {
                val releaseVersion = getReleaseVersion(release)
                val uploadDate = LocalDate.parse(release.date)
                val availableIde = AvailableIde(ideVersion, releaseVersion, downloadUrl, uploadDate)
                availableIdes.add(availableIde)
              }
            }
          }
        }
      }
    }
    return availableIdes
  }

  /**
   * Release version is only applicable for "release" IDEs.
   */
  private fun getReleaseVersion(release: Release) =
      if (release.type == "release") release.version else null

  private fun getIntelliJProduct(product: Product) =
      IntelliJPlatformProduct.fromProductCode(product.code)

  /**
   * "downloads" map provides download URL for different distribution types.
   *
   * All IntelliJ IDEs have either "linux" (IU, IC, PC...) or "zip" (MPS) distribution.
   */
  private fun getBuildDownload(downloads: Map<String, Download>): Download? =
      downloads["linux"] ?: downloads["zip"]
}