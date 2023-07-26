package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.plugin.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

interface PluginCreationResultResolver {
  fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin>

  fun isError(problem: PluginProblem): Boolean = problem.level == ERROR
}


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

  override fun isError(problem: PluginProblem): Boolean = problems.contains(problem::class)

  private val problems = listOf(
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
    DefaultDescription::class,
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
    PluginFileSizeIsTooLarge::class,
    UnableToExtractZip::class,

    InvalidPluginIDProblem::class,
    UnexpectedDescriptorElements::class,
    TooLongPropertyValue::class,
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