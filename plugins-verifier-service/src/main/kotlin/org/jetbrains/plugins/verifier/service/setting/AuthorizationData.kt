package org.jetbrains.plugins.verifier.service.setting

/**
 * Authorization data used for privileged actions.
 */
data class AuthorizationData(
    val pluginRepositoryUserName: String,

    val pluginRepositoryPassword: String,

    /**
     * The admin password which is required
     * to execute protected server's action.
     */
    val serviceAdminPassword: String
)