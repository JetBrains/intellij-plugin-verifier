package org.jetbrains.plugins.verifier.service.setting

/**
 * Authorization data used for privileged actions.
 */
data class AuthorizationData(
    /**
     * The user name
     * used to authenticate the services
     * (verifier, feature extractor, available IDE service)
     * when connecting to the plugin repository.
     */
    val pluginRepositoryUserName: String,

    /**
     * The password
     * used to authenticate the services
     * (verifier, feature extractor, available IDE service)
     * when connecting to the plugin repository.
     */
    val pluginRepositoryPassword: String,

    /**
     * The admin password required
     * to execute protected verifier service's actions
     * (such as start/stop services, change runtime parameters)
     */
    val serviceAdminPassword: String
)