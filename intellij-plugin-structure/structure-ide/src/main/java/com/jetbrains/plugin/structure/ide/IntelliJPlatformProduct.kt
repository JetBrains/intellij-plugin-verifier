package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

enum class IntelliJPlatformProduct(
    val productCode: String,
    val productName: String,
    val platformPrefix: String
) {

  IDEA("IU", "IntelliJ IDEA", "idea"),
  IDEA_IC("IC", "IntelliJ IDEA Community Edition", "Idea"),
  RUBYMINE("RM", "RubyMine", "Ruby"),
  PYCHARM("PY", "PyCharm", "Python"),
  PYCHARM_PC("PC", "PyCharm Community Edition", "PyCharmCore"),
  PYCHARM_EDU("PE", "PyCharm Educational Edition", "PyCharmEdu"),
  PHPSTORM("PS", "PhpStorm", "PhpStorm"),
  WEBSTORM("WS", "WebStorm", "WebStorm"),
  APPCODE("OC", "AppCode", "AppCode"),
  CLION("CL", "CLion", "CLion"),
  DBE("DB", "DataGrip", "DataGrip"),
  RIDER("RD", "Rider", "Rider"),
  GOIDE("GO", "GoLand", "GoLand"),
  ANDROID_STUDIO("AI", "Android Studio", "AndroidStudio");

  companion object {
    fun fromIdeVersion(ideVersion: IdeVersion) =
        values().find { it.productCode == ideVersion.productCode } ?: IDEA
  }
}