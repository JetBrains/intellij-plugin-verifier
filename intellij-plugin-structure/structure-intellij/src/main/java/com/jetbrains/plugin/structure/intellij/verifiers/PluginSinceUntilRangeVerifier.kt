package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.CompatibilityUtils
import com.jetbrains.plugin.structure.intellij.beans.IdeaVersionBean
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersionImpl


private const val BUILD_NUMBER = "__BUILD_NUMBER__"
private const val SNAPSHOT = "SNAPSHOT"

private const val SINCE_BASELINE_LOWER_BOUND = 130
private const val UNTIL_BASELINE_LOWER_BOUND = 130

private const val SUSPICIOUS_UNTIL_BASELINE_LOWER_BOUND = 281
private const val FIRST_YEARLY_BASED_RELEASE_NUMBER_BASELINE = 162
private const val FIRST_YEARLY_BASED_RELEASE_NUMBER_YEAR = 2016
private const val MAGIC_BASELINE_NUMBER = 999

class PluginSinceUntilRangeVerifier {
  fun verify(
    plugin: PluginBean,
    descriptorPath: String,
    problemRegistrar: ProblemRegistrar
  ) = with(problemRegistrar) {
    val versionBean = plugin.ideaVersion
    if (versionBean == null) {
      registerProblem(PropertyNotSpecified("idea-version", descriptorPath))
      return
    }

    verifySinceBuild(versionBean.sinceBuild, descriptorPath)
    verifyUntilBuild(versionBean.untilBuild, descriptorPath)
  }

  private fun ProblemRegistrar.verifySinceBuild(sinceBuild: String?, descriptorPath: String) {
    if (sinceBuild == null) {
      registerProblem(SinceBuildNotSpecified(descriptorPath))
    } else {
      val sinceBuildParsed = IdeVersion.createIdeVersionIfValid(sinceBuild)
      if (sinceBuildParsed == null) {
        registerProblem(InvalidSinceBuild(descriptorPath, sinceBuild))
      } else {
        if (sinceBuild.endsWith(".*")) {
          registerProblem(SinceBuildCannotContainWildcard(descriptorPath, sinceBuildParsed))
        }
        if (sinceBuildParsed.productCode.isNotEmpty()) {
          registerProblem(ProductCodePrefixInBuild(descriptorPath))
        }
        verifyIdeBuildComponentsRanges(
          ideVersion = sinceBuildParsed,
          baselineLowerBound = SINCE_BASELINE_LOWER_BOUND,
          attributeName = IdeaVersionBean.SINCE_BUILD_ATTRIBUTE_NAME,
          descriptorPath = descriptorPath
        )
      }
    }
  }

  private fun ProblemRegistrar.verifyIdeBuildComponentsRanges(
    ideVersion: IdeVersion,
    baselineLowerBound: Int,
    attributeName: String,
    descriptorPath: String
  ) {
    val baselineRange = IntRange(baselineLowerBound, CompatibilityUtils.MAX_BRANCH_VALUE - 1)
    if (ideVersion.baselineVersion !in baselineRange) {
      registerProblem(IdeBuildComponentsOutOfRange(
        ideVersion = ideVersion,
        failedComponent = ideVersion.baselineVersion,
        range = baselineRange,
        attributeName = attributeName,
        descriptorPath = descriptorPath
      ))
    }
    if (ideVersion.build >= CompatibilityUtils.MAX_BUILD_VALUE && ideVersion.build != IdeVersionImpl.SNAPSHOT_VALUE) {
      registerProblem(IdeBuildComponentsOutOfRange(
        ideVersion = ideVersion,
        failedComponent = ideVersion.build,
        range = IntRange(0, CompatibilityUtils.MAX_BUILD_VALUE - 1),
        attributeName = attributeName,
        descriptorPath = descriptorPath
      ))
    }
    val thirdComponent = ideVersion.components.getOrNull(2)
    if (thirdComponent != null && thirdComponent >= CompatibilityUtils.MAX_COMPONENT_VALUE && thirdComponent != IdeVersionImpl.SNAPSHOT_VALUE) {
      registerProblem(IdeBuildComponentsOutOfRange(
        ideVersion = ideVersion,
        failedComponent = thirdComponent,
        range = IntRange(0, CompatibilityUtils.MAX_COMPONENT_VALUE - 1),
        attributeName = attributeName,
        descriptorPath = descriptorPath
      ))
    }
  }

  private fun ProblemRegistrar.verifyUntilBuild(untilBuild: String?, descriptorPath: String) {
    if (untilBuild == null) {
      return
    }
    val untilBuildParsed = IdeVersion.createIdeVersionIfValid(untilBuild)

    if (untilBuildParsed == null) {
      registerProblem(InvalidUntilBuild(descriptorPath, untilBuild))
      return
    }

    if (untilBuildParsed.isJustASingleComponent()) {
      if (untilBuildParsed.baselineVersion == MAGIC_BASELINE_NUMBER) {
        registerProblem(InvalidUntilBuildWithMagicNumber(descriptorPath, untilBuild, untilBuildParsed.baselineVersion))
        return // fast fail for magic number, no additional checks needed
      }

      registerProblem(InvalidUntilBuildWithJustBranch(descriptorPath, untilBuild))
    }

    verifyIdeBuildComponentsRanges(
      ideVersion = untilBuildParsed,
      baselineLowerBound = UNTIL_BASELINE_LOWER_BOUND,
      attributeName = IdeaVersionBean.UNTIL_BUILD_ATTRIBUTE_NAME,
      descriptorPath = descriptorPath
    )
    verifyUntilBuildBaselineMagicNumbers(untilBuildParsed, untilBuild, descriptorPath)

    if (untilBuildParsed.productCode.isNotEmpty()) {
      registerProblem(ProductCodePrefixInBuild(descriptorPath))
    }
  }

  private fun ProblemRegistrar.verifyUntilBuildBaselineMagicNumbers(
    untilBuild: IdeVersion,
    untilBuildValue: String,
    descriptorPath: String
  ) {
    if (untilBuildValue == BUILD_NUMBER || untilBuildValue == SNAPSHOT) {
      return
    }
    val baseline = untilBuild.baselineVersion
    if (baseline >= FIRST_YEARLY_BASED_RELEASE_NUMBER_YEAR) {
      registerProblem(InvalidUntilBuild(descriptorPath, untilBuildValue, untilBuild))
    } else if (baseline == MAGIC_BASELINE_NUMBER) {
      registerProblem(InvalidUntilBuildWithMagicNumber(descriptorPath, untilBuildValue, baseline))
    } else if (baseline >= SUSPICIOUS_UNTIL_BASELINE_LOWER_BOUND) {
      registerProblem(SuspiciousUntilBuild(untilBuildValue))
    } else {
      verifyInThreeReleasesPerYear(untilBuildValue, baselineVersion = baseline)
    }
  }

  private fun ProblemRegistrar.verifyInThreeReleasesPerYear(untilBuildValue: String, baselineVersion: Int) {
    if (baselineVersion >= FIRST_YEARLY_BASED_RELEASE_NUMBER_BASELINE) {
      val lastDigit = baselineVersion % 10
      val releaseVersion = baselineVersion / 10
      if (lastDigit !in 1..3) {
        registerProblem(NonexistentReleaseInUntilBuild(untilBuildValue, "20$releaseVersion.$lastDigit"))
      }
    }
  }

  private fun IdeVersion.isJustASingleComponent(): Boolean {
    val meaningfulComponents = components.filter { it != 0 }
    return meaningfulComponents.size == 1
  }
}
