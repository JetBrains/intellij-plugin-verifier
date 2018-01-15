package org.jetbrains.plugins.verifier.service.setting

/**
 * Authorization data used for privileged actions.
 */
data class AuthorizationData(
    /**
     * The user name used to authenticate the
     * [verifier] [org.jetbrains.plugins.verifier.service.service.verifier.VerifierService]
     * and [feature extractor] [org.jetbrains.plugins.verifier.service.service.features.FeatureExtractorService] services
     * when connecting the plugin repository.
     */
    val pluginRepositoryUserName: String,

    /**
     * The password used to authenticate the
     * [verifier] [org.jetbrains.plugins.verifier.service.service.verifier.VerifierService]
     * and [feature extractor] [org.jetbrains.plugins.verifier.service.service.features.FeatureExtractorService] services
     * when connecting the plugin repository.
     */
    val pluginRepositoryPassword: String,

    /**
     * The admin password which is required
     * to execute protected server's action.
     */
    val serviceAdminPassword: String
)