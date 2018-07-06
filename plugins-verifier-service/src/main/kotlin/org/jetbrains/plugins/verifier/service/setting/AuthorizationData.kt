package org.jetbrains.plugins.verifier.service.setting

/**
 * Authorization data used for privileged actions.
 */
data class AuthorizationData(

    /**
     * The admin password required
     * to execute protected verifier service's actions
     * (such as start/stop services, change runtime parameters)
     */
    val serviceAdminPassword: String,


    /**
     * Token used to authorize the service in the Plugin Repository
     */
    val pluginRepositoryAuthorizationToken: String

)