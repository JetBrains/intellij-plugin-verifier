package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

/**
 * Enumerates known IntelliJ Platform IDEs.
 */
enum class IntelliJPlatformProduct(
    val productCode: String,
    val productName: String,
    val platformPrefix: String,
    val alternativeProductCode: String? = null
) {

  IDEA("IU", "IntelliJ IDEA", "idea", "IIU"),
  IDEA_IC("IC", "IntelliJ IDEA Community Edition", "Idea", "IIC"),
  RUBY_MINE("RM", "RubyMine", "Ruby"),
  PY_CHARM("PY", "PyCharm", "Python", "PCP"),
  PY_CHARM_PC("PC", "PyCharm Community Edition", "PyCharmCore", "PCC"),
  PYCHARM_EDU("PE", "PyCharm Educational Edition", "PyCharmEdu", "PCE"),
  PHP_STORM("PS", "PhpStorm", "PhpStorm"),
  WEB_STORM("WS", "WebStorm", "WebStorm"),
  APPCODE("OC", "AppCode", "AppCode"),
  CLION("CL", "CLion", "CLion"),
  DATA_GRIP("DB", "DataGrip", "DataGrip", "DG"),
  RIDER("RD", "Rider", "Rider"),
  GO_LAND("GO", "GoLand", "GoLand"),
  MPS("MPS", "MPS", "MPS"),
  ANDROID_STUDIO("AI", "Android Studio", "AndroidStudio");

  companion object {
    fun fromProductCode(productCode: String): IntelliJPlatformProduct? =
        values().find { it.productCode == productCode || it.alternativeProductCode == productCode }

    fun fromIdeVersion(ideVersion: IdeVersion): IntelliJPlatformProduct? =
        fromProductCode(ideVersion.productCode)
  }
}