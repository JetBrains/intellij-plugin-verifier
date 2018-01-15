package org.jetbrains.plugins.verifier.service.service.jdks

/**
 * JDK versions that can be used by the
 * [services] [org.jetbrains.plugins.verifier.service.service.BaseService]
 * for internal purposes.
 *
 * For instance, the [verifier service] [org.jetbrains.plugins.verifier.service.service.verifier.VerifierService]
 * uses the JDKs for the bytecode verifications.
 *
 * The path to the JDK of a concrete version
 * can be obtained using the [JdkDescriptorsCache].
 */
enum class JdkVersion {
  JAVA_8_ORACLE
}