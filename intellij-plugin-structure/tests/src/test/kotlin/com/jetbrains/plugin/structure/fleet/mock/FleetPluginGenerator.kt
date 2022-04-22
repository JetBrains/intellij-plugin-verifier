package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.fleet.FleetPluginManager
import fleet.bundles.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.createDirectories

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
    val descriptor = PluginDescriptor(
      id = BundleName(id),
      readableName = name,
      vendor = vendor,
      version = BundleVersion(version),
      description = description,
      deps = depends.map { (k, v) -> Pair(BundleName(k), VersionRequirement.CompatibleWith(BundleVersion(v))) }.toMap(),
      frontend = frontend?.let {
        Barrel(
          modulePath = substitute(frontend.modulePath),
          classPath = substitute(frontend.classPath),
          squashedAutomaticModules = frontend.squashedAutomaticModules.map { substitute(it).toList() }.toSet()
        )
      },
      workspace = workspace?.let {
        Barrel(
          modulePath = substitute(workspace.modulePath),
          classPath = substitute(workspace.classPath),
          squashedAutomaticModules = workspace.squashedAutomaticModules.map { substitute(it).toList() }.toSet()
        )
      }
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

  private fun substitute(modules: List<String>): Set<Barrel.Coordinates> {
    return modules.map {
      val file = Paths.get(it)
      val fileName = file.fileName.toString()
      val hash = hash(file.toFile())
      return@map Barrel.Coordinates.Relative(fileName, hash)
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
    if (!Files.exists(path.parent)){
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
)

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

private val digestToClone = MessageDigest.getInstance("SHA-1")
private fun hash(file: File): String {
  val digest  = digestToClone.clone() as MessageDigest
  val buffer = ByteArray(1 * 1024 * 1024)
  digest.reset()
  file.inputStream().buffered().use {
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

fun main() {
  FleetPluginGenerator().generate(
    "test.plugin.third", "Third Test",
    frontend = PluginPartFileNames(listOf("f-module1.txt"), listOf("f-cp1.txt"), setOf(listOf("f-sq_m1.txt"))),
    workspace = PluginPartFileNames(listOf("w-module1.txt"), listOf("w-cp1.txt"), setOf(listOf("w-sq_m1.txt"))),
  )
}