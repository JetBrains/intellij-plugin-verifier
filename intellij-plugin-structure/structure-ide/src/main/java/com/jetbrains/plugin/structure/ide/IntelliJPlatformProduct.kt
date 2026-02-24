/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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

  IDEA("IU", "IntelliJ IDEA Ultimate", "idea", "IIU"),
  IDEA_IC("IC", "IntelliJ IDEA Community", "Idea", "IIC"),
  IDEA_IE("IE", "IntelliJ IDEA Educational", "IdeaEdu", "IIC"),
  RUBY_MINE("RM", "RubyMine", "Ruby"),
  PY_CHARM("PY", "PyCharm Professional", "Python", "PCP"),
  PY_CHARM_PC("PC", "PyCharm Community", "PyCharmCore", "PCC"),
  PYCHARM_EDU("PE", "PyCharm Educational", "PyCharmEdu", "PCE"),
  PHP_STORM("PS", "PhpStorm", "PhpStorm"),
  WEB_STORM("WS", "WebStorm", "WebStorm"),
  APPCODE("OC", "AppCode", "AppCode", "AC"),
  CLION("CL", "CLion", "CLion"),
  DATA_GRIP("DB", "DataGrip", "DataGrip", "DG"),
  RIDER("RD", "Rider", "Rider"),
  GO_LAND("GO", "GoLand", "GoLand"),
  MPS("MPS", "MPS", "Idea"),
  ANDROID_STUDIO("AI", "Android Studio", "AndroidStudio"),
  DATA_SPELL("DS", "DataSpell", "DataSpell"),
  JETBRAINS_CLIENT("JBC", "JetBrains Client", "JetBrainsClient"),
  GATEWAY("GW", "Gateway", "Gateway"),
  FLEET("FL", "Fleet", "Fleet"),
  AQUA("QA", "Aqua", "Aqua");

  companion object {
    fun fromProductCode(productCode: String): IntelliJPlatformProduct? =
      values().find { it.productCode == productCode || it.alternativeProductCode == productCode }

    fun fromIdeVersion(ideVersion: IdeVersion): IntelliJPlatformProduct? =
      fromProductCode(ideVersion.productCode)
  }
}