package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.api.PluginInfo
import com.jetbrains.pluginverifier.api.Result

/**
 * @author Sergey Patrikeev
 */
data class CheckRangeCompatibilityResult(val plugin: PluginInfo,
                                         val resultType: ResultType,
                                         val verificationResults: List<Result>? = null,
                                         val invalidPluginProblems: List<PluginProblem>? = null,
                                         val nonDownloadableReason: String? = null) {
  enum class ResultType {
    NON_DOWNLOADABLE,
    NO_COMPATIBLE_IDES,
    INVALID_PLUGIN,
    VERIFICATION_DONE
  }

}