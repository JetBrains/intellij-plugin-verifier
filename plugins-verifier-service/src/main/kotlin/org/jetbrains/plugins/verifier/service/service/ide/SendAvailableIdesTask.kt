package org.jetbrains.plugins.verifier.service.service.ide

import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task

/**
 * [Task] to request available IDEs in [ideRepository]
 * and send them to the Marketplace for verifications scheduling.
 */
class SendAvailableIdesTask(
    private val ideRepository: IdeRepository,
    private val protocol: AvailableIdeProtocol
) : Task<Unit>("Send available IDEs to Marketplace", "SendIdes") {

  override fun execute(progress: ProgressIndicator) {
    val availableIdes = ideRepository.fetchIndex()
    protocol.sendAvailableIdes(availableIdes)
  }

}