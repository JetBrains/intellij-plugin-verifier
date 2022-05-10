package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.fleet.FleetPluginManager
import fleet.bundles.*
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream

class FleetPluginGenerator(val pluginsPath: String = "/tmp") {
  fun generate(
    id: String,
    name: String,
    icon: String? = null,
    vendor: String = "JetBrains",
    version: String = "1.0.0",
    description: String? = null,
    depends: Map<String, String> = mapOf(),
    frontend: PluginPartFileNames? = null,
    workspace: PluginPartFileNames? = null,
  ): File =
    generate(
      id = id,
      name = name,
      icon = icon,
      vendor = vendor,
      version = version,
      description = description,
      depends = depends,
      filesGenerator = ByNameFilesGenerator(frontend, workspace)
    )


  private fun generate(
    id: String,
    name: String,
    icon: String?,
    vendor: String,
    version: String,
    description: String?,
    depends: Map<String, String>,
    filesGenerator: FilesGenerator,
  ): File {
    filesGenerator.generateFiles()
    val frontend = filesGenerator.frontendFiles
    val workspace = filesGenerator.workspaceFiles

    val fileName = "$id-$version"
    val bundleId = BundleId(BundleName(id), BundleVersion(version))
    val descriptor = BundleSpec(
      BundleId(bundleId.name, bundleId.version),
      Bundle(
        meta = listOfNotNull(
          KnownMeta.ReadableName to name,
          description?.let { KnownMeta.Description to it },
          KnownMeta.Vendor to vendor
        ).toMap(),
        deps = depends.map { (k, v) -> Bundle.Dependency(BundleName(k), VersionRequirement.CompatibleWith(BundleVersion(v))) }.toSet(),
        barrels = listOfNotNull(
          frontend?.let {
            BarrelSelector.Frontend to
              Barrel(
                modulePath = substitute(bundleId, frontend.modulePath),
                classPath = substitute(bundleId, frontend.classPath),
                squashedAutomaticModules = frontend.squashedAutomaticModules.map { substitute(bundleId, it).toList() }.toSet()
              )
          },
          workspace?.let {
            BarrelSelector.Workspace to
              Barrel(
                modulePath = substitute(bundleId, workspace.modulePath),
                classPath = substitute(bundleId, workspace.classPath),
                squashedAutomaticModules = workspace.squashedAutomaticModules.map { substitute(bundleId, it).toList() }.toSet()
              )
          }
        ).toMap()
      )
    )
    return build(
      text = descriptor.encodeToString(),
      fileName = fileName,
      icon = icon,
      files = listOfNotNull(
        frontend?.modulePath, frontend?.classPath, frontend?.squashedAutomaticModules?.flatten(),
        workspace?.modulePath, workspace?.classPath, workspace?.squashedAutomaticModules?.flatten()
      ).flatten()
    ).toFile()
  }

  private fun substitute(bundleId: BundleId, modules: List<String>): Set<Coordinates> {
    return modules.map {
      val file = Paths.get(it)
      val fileName = file.fileName.toString()
      val hash = file.inputStream().use(::hash)
      val fileUrl = "https://plugins.jetbrains.com/files/fleet/${bundleId.name.name}/${bundleId.version.version.value}/modules/$fileName"
      return@map Coordinates.Remote(fileUrl, hash)
    }.toSet()
  }

  private fun build(
    text: String,
    fileName: String,
    icon: String?,
    files: List<String>
  ): Path {
    val existing = mutableMapOf<String, Path>()
    val path = Paths.get(pluginsPath, "$fileName.zip")
    if (!Files.exists(path.parent)) {
      path.parent.createDirectories()
    }
    return buildZipFile(path) {
      files.forEach {
        val p = Paths.get(it)
        val name = p.fileName.toString()
        assert(existing[name] in listOf(null, p)) { "Duplicated filename: $name, existing path: ${existing[name]}, new path: $p" }
        file(name, Files.readAllBytes(p))
      }
      file(FleetPluginManager.DESCRIPTOR_NAME, text)
      if (icon != null) {
        file("pluginIcon.svg", icon)
      }
    }
  }
}

data class PluginPartFileNames(
  val moduleFileNames: List<String>,
  val classPathFileNames: List<String>,
  val autoModulesFileNames: Set<List<String>>,
) {
  fun getNames(): Set<String> {
    return (classPathFileNames + moduleFileNames + autoModulesFileNames.flatten()).toSet()
  }
}

data class PluginPartFiles(
  val modulePath: MutableList<String> = mutableListOf(),
  val classPath: MutableList<String> = mutableListOf(),
  val squashedAutomaticModules: MutableSet<MutableList<String>> = mutableSetOf(),
)

private abstract class FilesGenerator {
  var workspaceFiles: PluginPartFiles? = null
  var frontendFiles: PluginPartFiles? = null

  abstract fun generateFiles();
}

private class ByNameFilesGenerator(
  private val frontend: PluginPartFileNames? = null,
  private val workspace: PluginPartFileNames? = null
) : FilesGenerator() {

  override fun generateFiles() {
    frontendFiles = frontend?.let { createPartFiles(it) }
    workspaceFiles = workspace?.let { createPartFiles(it) }
  }

  private fun createPartFiles(fileNames: PluginPartFileNames): PluginPartFiles =
    PluginPartFiles(
      createFiles(fileNames.moduleFileNames),
      createFiles(fileNames.classPathFileNames),
      fileNames.autoModulesFileNames.map { createFiles(it).toMutableList() }.toMutableSet()
    )

  private fun createFiles(names: List<String>): MutableList<String> {
    return names.map {
      val tmpDir = Files.createTempDirectory("testPluginData")
      val file = Files.createFile(tmpDir.resolve(it))
      Files.writeString(file, "File #$it")
      return@map file.toString()
    }.toMutableList()
  }
}

private val digestToClone = MessageDigest.getInstance(Coordinates.Remote.HASH_ALGORITHM)

private fun hash(s: InputStream): String {
  val digest = digestToClone.clone() as MessageDigest
  val buffer = ByteArray(1 * 1024 * 1024)
  digest.reset()
  s.buffered().use {
    while (true) {
      val read = it.read(buffer)
      if (read <= 0) break
      digest.update(buffer, 0, read)
    }
  }
  val shaBytes = digest.digest()
  return buildString {
    shaBytes.forEach { byte -> append(java.lang.Byte.toUnsignedInt(byte).toString(16)) }
  }
}


