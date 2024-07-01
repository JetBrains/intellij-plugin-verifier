package com.jetbrains.pluginverifier.verifiers.resolution

/**
 * Indicates that the invocation of the target API element is prohibited
 * even if it occurs within the same module.
 *
 * Mainly, this is used in tests to verify OverrideOnly invocations in the same module or plugin.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class DisableSameModuleInvocations
