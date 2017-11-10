package org.jetbrains.plugins.verifier.service.service.repository

data class AuthorizationData(val pluginRepositoryUserName: String,
                             val pluginRepositoryPassword: String,
                             val serviceAdminPassword: String)