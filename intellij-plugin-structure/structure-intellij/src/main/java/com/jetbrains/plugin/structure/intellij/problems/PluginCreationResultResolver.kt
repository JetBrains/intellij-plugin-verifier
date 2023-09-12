package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

/**
 * Resolves a collection of [plugin problems][PluginProblem] to the [result of plugin creation][PluginCreationResult],
 * which is either a success or a failure.
 *
 * @see com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
 */
interface PluginCreationResultResolver {
  fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin>

  fun isError(problem: PluginProblem): Boolean = problem.level == ERROR
}

/**
 * Resolves a collection of [plugin problems][PluginProblem] to the [result of plugin creation][PluginCreationResult]
 * by consulting an allow-list of supported plugin problems that are considered errors.
 *
 * Non-listed errors are treated as an error with fail-fast mechanism, and they are wrapped into a single
 * [BlocklistedPluginError].
 */
class IntelliJPluginCreationResultResolver : PluginCreationResultResolver {
  override fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin> {
    val errors = problems.filter { it.level == ERROR }
    return if (errors.isEmpty()) {
      PluginCreationSuccess(plugin, problems)
    } else {
      val unclassifiedErrors = errors.filterNot(::isError)
      if (unclassifiedErrors.isNotEmpty()) {
        PluginCreationFail(BlocklistedPluginError(unclassifiedErrors.first()))
      } else {
        PluginCreationFail(problems)
      }
    }
  }

  override fun isError(problem: PluginProblem): Boolean = intellijPluginErrorProblems.contains(problem::class)

  /**
   * Explicit list of all IntelliJ [PluginProblem][plugin problems] with level set to [ERROR].
   */
  private val intellijPluginErrorProblems = listOf(
    PropertyWithDefaultValue::class,
    InvalidDependencyId::class,
    InvalidModuleBean::class,
    SinceBuildNotSpecified::class,
    InvalidSinceBuild::class,
    InvalidUntilBuild::class,
    SinceBuildGreaterThanUntilBuild::class,
    ErroneousSinceBuild::class,
    ErroneousUntilBuild::class,
    ProductCodePrefixInBuild::class,
    XIncludeResolutionErrors::class,
    TooLongPropertyValue::class,
    ReleaseDateWrongFormat::class,
    UnableToFindTheme::class,
    UnableToReadTheme::class,
    OptionalDependencyDescriptorCycleProblem::class,
    OptionalDependencyConfigFileIsEmpty::class,

    PluginLibDirectoryIsEmpty::class,
    PluginZipContainsMultipleFiles::class,
    PluginZipContainsSingleJarInRoot::class,
    PluginZipContainsUnknownFile::class,
    PluginZipIsEmpty::class,
    UnableToReadPluginFile::class,
    UnexpectedPluginZipStructure::class,

    IncorrectPluginFile::class,
    IncorrectZipOrJarFile::class,
    PluginFileSizeIsTooLarge::class,
    UnableToExtractZip::class,

    InvalidPluginIDProblem::class,
    UnexpectedDescriptorElements::class,
    PropertyNotSpecified::class,
    NotBoolean::class,

    NotNumber::class,
    UnableToReadDescriptor::class,
    ContainsNewlines::class,
    ReusedDescriptorInMultipleDependencies::class,
    VendorCannotBeEmpty::class,

    PluginDescriptorIsNotFound::class,
    MultiplePluginDescriptors::class,
  )
}