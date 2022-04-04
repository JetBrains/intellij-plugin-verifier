package com.jetbrains.plugin.structure.fleet.bean

import com.vdurmont.semver4j.Semver
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

//TODO [MM] package as a lib in Fleet and use the lib instead of duplicating code

@Serializable(with = BundleName.Serializer::class)
data class BundleName(val name: String) {
  class Serializer : StringSerializer<BundleName>(BundleName::name, ::BundleName)
}

/**
 * VersionRequirement is what can present in extension's dependencies
 */
@Serializable(with = VersionRequirement.Serializer::class)
sealed class VersionRequirement {
  data class CompatibleWith(val version: BundleVersion) : VersionRequirement()
  data class Above(val version: BundleVersion) : VersionRequirement()

  class Serializer : StringSerializer<VersionRequirement>(
    toString = {
      when (it) {
        is CompatibleWith -> it.version.version.value
        is Above -> it.version.version.value + "+"
      }
    },
    fromString = {
      when {
        it.endsWith("+") -> Above(BundleVersion(it.dropLast(1)))
        else -> CompatibleWith(BundleVersion(it))
      }
    }
  )
}

open class DataSerializer<T, D>(
  val serializer: KSerializer<D>,
  val toData: (T) -> D,
  val fromData: (D) -> T
) : KSerializer<T> {

  override val descriptor: SerialDescriptor
    get() = serializer.descriptor

  override fun deserialize(decoder: Decoder): T {
    return fromData(serializer.deserialize(decoder))
  }

  override fun serialize(encoder: Encoder, value: T) {
    serializer.serialize(encoder, toData(value))
  }
}

open class StringSerializer<T>(toString: (T) -> String,
                               fromString: (String) -> T) : DataSerializer<T, String>(String.serializer(), toString, fromString)

@Serializable(with = BundleVersion.Serializer::class)
data class BundleVersion(val version: Semver) {
  class Serializer : StringSerializer<BundleVersion>({ it.version.value }, { BundleVersion(Semver(it)) })

  constructor(version: String) : this(Semver(version))
}

/**
 * Represents a [Bundle]'s value
 * Barrel is a deliverable unit to be loaded into a single [fleet.kernel.Kernel]
 * */
@Serializable
data class Bundle(
  @SerialName("depends")
  val deps: Set<Dependency> = setOf(),
  val barrels: Map<BarrelSelector, Barrel>
) {
  @Serializable
  data class Dependency(val name: BundleName,
                        val version: VersionRequirement
  )
}

@Serializable(with = BarrelSelector.Serializer::class)
data class BarrelSelector(val selector: String) {
  companion object {
    val Frontend: BarrelSelector = BarrelSelector("frontend")
    val Workspace: BarrelSelector = BarrelSelector("workspace")
  }

  class Serializer : StringSerializer<BarrelSelector>(BarrelSelector::selector, ::BarrelSelector)
}

/**
 * Represents a "name" of a Barrel's value
 * */
@Serializable
data class Barrel(
  val modulePath: Set<Coordinates> = setOf(),
  val classPath: Set<Coordinates> = setOf(),
  val squashedAutomaticModules: Set<List<Coordinates>> = setOf(),
  val modules: Set<String> = setOf()
) {
  @Serializable(with = Coordinates.Serializer::class)
  sealed class Coordinates {
    //to reference e.g. a plugin file in marketplace (which might also be in code-cache already)
    data class Remote(val url: String, val hash: String) : Coordinates()

    //to reference an exact file in code cache, should be used in distrib only
    data class Bundled(val fileName: String) : Coordinates() //todo[MM] should be replaced by sha and resolution?

    //to reference a folder with classes, should be used when running from sources only
    data class Local(val path: String) : Coordinates()

    //to reference a file inside zip, used in plugin distribution zip
    data class Relative(val relPath: String, val hash: String) : Coordinates() {
      init {
        require(!relPath.startsWith("/"))
      }
    }

    //to reference a file that WS serves
    //data class Workspace(val filename: String, val sha2: String) : Coordinates()

    class Serializer : StringSerializer<Coordinates>(
      toString = {
        when (it) {
          is Bundled -> "b@" + it.fileName
          is Local -> "l@" + it.path
          is Relative -> "/" + it.relPath + "#" + it.hash
          is Remote -> it.url + "#" + it.hash
        }
      },
      fromString = {
        when {
          it.startsWith("b@") -> Bundled(it.substring(2))
          it.startsWith("l@") -> Local(it.substring(2))
          it.startsWith("/") -> {
            val pathWithHash = it.substring(1)
            Relative(pathWithHash.substringBeforeLast("#"), pathWithHash.substringAfterLast("#"))
          }
          else -> Remote(it.substringBeforeLast("#"), it.substringAfterLast("#"))
        }
      }
    )

  }
}

@Serializable
data class BundleId(
  val name: BundleName,
  val version: BundleVersion
) {
  constructor(name: String, version: Semver) : this(BundleName(name), BundleVersion(version))

  companion object
}

@Serializable(with = BundleSpecSerializer::class)
data class BundleSpec(
  val bundleId: BundleId,
  val bundle: Bundle
)

@Serializable(with = ResolvedBundlesConfigSerializer::class)
data class ResolvedBundlesConfig(val bundlesToLoad: List<BundleSpec>)