package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.Result
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTaskResult

/**
 * @author Sergey Patrikeev
 */
data class CheckRangeCompatibilityResult(val updateInfo: UpdateInfo,
                                         val resultType: ResultType,
                                         val verificationResults: List<Result>? = null,
                                         val invalidPluginProblems: List<PluginProblem>? = null,
                                         val nonDownloadableReason: String? = null) : ServiceTaskResult {
  enum class ResultType {
    NON_DOWNLOADABLE,
    NO_COMPATIBLE_IDES,
    INVALID_PLUGIN,
    VERIFICATION_DONE
  }

}