package com.jetbrains.plugin.structure.fleet.bean

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

//TODO [MM] package as a lib in Fleet and use the lib instead of duplicating code

class ResolvedBundlesConfigSerializer() : KSerializer<ResolvedBundlesConfig> {
  private val delegate = ListSerializer(BundleSpec.serializer())
  override val descriptor: SerialDescriptor = delegate.descriptor
  override fun serialize(encoder: Encoder, value: ResolvedBundlesConfig): Unit = delegate.serialize(encoder, value.bundlesToLoad)
  override fun deserialize(decoder: Decoder): ResolvedBundlesConfig = ResolvedBundlesConfig(delegate.deserialize(decoder))
}

@Serializable
data class PluginDescriptor(
  val id: BundleName,
  val version: BundleVersion,
  @SerialName("name")
  val readableName: String? = null,
  val description: String? = null,
  val vendor: String? = null,
  @SerialName("depends")
  val deps: Map<BundleName, VersionRequirement>,
  val frontend: Barrel? = null,
  val workspace: Barrel? = null
)

fun BundleSpec.toPluginDescriptor(): PluginDescriptor =
  PluginDescriptor(
    bundleId.name, bundleId.version,
    null, null, null,
    bundle.deps.map { Pair(it.name, it.version) }.toMap(),
    bundle.barrels[KnownBarrels.FRONTEND], bundle.barrels[KnownBarrels.WORKSPACE]
  )

fun PluginDescriptor.toBundleSpec(): BundleSpec =
  BundleSpec(
    BundleId(id, version),
    Bundle(deps.map { Bundle.Dependency(it.key, it.value) }.toSet(),
           listOfNotNull(frontend?.let { KnownBarrels.FRONTEND to it },
                         workspace?.let { KnownBarrels.WORKSPACE to it }).toMap())
  )

//todo [MM] get rid of this, find a way to switch to map while preserving layout
object KnownBarrels {
  val FRONTEND = BarrelSelector("frontend")
  val WORKSPACE = BarrelSelector("workspace")
}

class BundleSpecSerializer() : KSerializer<BundleSpec> {
  override val descriptor: SerialDescriptor = PluginDescriptor.serializer().descriptor
  override fun serialize(encoder: Encoder, value: BundleSpec) {
    encoder.encodeSerializableValue(PluginDescriptor.serializer(), value.toPluginDescriptor())
  }

  override fun deserialize(decoder: Decoder): BundleSpec {
    return decoder.decodeSerializableValue(PluginDescriptor.serializer()).toBundleSpec()
  }
}