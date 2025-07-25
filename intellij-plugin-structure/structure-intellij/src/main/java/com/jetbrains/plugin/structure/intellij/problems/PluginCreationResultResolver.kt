package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.UNACCEPTABLE_WARNING
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

  /**
   * Allows remapping a specific collection of plugin problems to another collection of plugin problems.
   * Typically, this is used to change a [PluginProblem.Level] in specific scenarios.
   *
   * Note that the result collection might be smaller than the original collection due to ignored problems.
   */
  fun classify(plugin: IdePlugin, problems: List<PluginProblem>): List<PluginProblem> = problems
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
    InvalidKotlinPluginMode::class,
    InvalidUrl::class,
    SinceBuildNotSpecified::class,
    InvalidSinceBuild::class,
    InvalidUntilBuild::class,
    InvalidUntilBuildWithJustBranch::class,
    InvalidUntilBuildWithMagicNumber::class,
    SinceBuildGreaterThanUntilBuild::class,
    ErroneousSinceBuild::class,
    ProductCodePrefixInBuild::class,
    XIncludeResolutionErrors::class,
    TooLongPropertyValue::class,
    ReleaseDateWrongFormat::class,
    ReleaseDateInFuture::class,
    ReleaseVersionWrongFormat::class,
    ReleaseVersionAndPluginVersionMismatch::class,
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
    UnreadableZipOrJarFile::class,
    IncorrectJarOrDirectory::class,
    PluginFileSizeIsTooLarge::class,
    UnableToExtractZip::class,

    InvalidPluginIDProblem::class,
    UnexpectedDescriptorElements::class,
    PropertyNotSpecified::class,
    NotBoolean::class,
    InvalidPluginName::class,

    NotNumber::class,
    UnableToReadDescriptor::class,
    ContainsNewlines::class,
    ReusedDescriptorInMultipleDependencies::class,
    VendorCannotBeEmpty::class,

    PluginDescriptorIsNotFound::class,
    MultiplePluginDescriptors::class,
  )
}

/**
 * Remaps any kind of plugin problem to `WARNING` level.
 *
 * The plugin creation result will always be _successful_.
 */
object AnyProblemToWarningPluginCreationResultResolver : PluginCreationResultResolver {
  override fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin> {
    return PluginCreationSuccess(plugin, problems.map { remapToWarning(it) })
  }

  override fun classify(plugin: IdePlugin, problems: List<PluginProblem>): List<PluginProblem> =
    problems.map(::remapToWarning)

  private fun remapToWarning(problem: PluginProblem) = when (problem.level) {
    ERROR, UNACCEPTABLE_WARNING -> ReclassifiedPluginProblem(PluginProblem.Level.WARNING, problem)
    else -> problem
  }
}