package com.jetbrains.plugin.structure.fleet.mock

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.fleet.FleetPluginManager
import fleet.bundles.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

class FleetPluginGenerator(private val path: String = "/tmp") {
  fun generateWithRandomFiles(
    id: String,
    name: String,
    vendor: String = "JetBrains",
    version: String = "1.0.0",
    description: String? = null,
    icon: String? = null,
    depends: Map<String, String> = mapOf(),
    generateFrontend: Boolean = false,
    generateWorkspace: Boolean = false,
  ): File =
    generate(id, name, icon, vendor, version, description, depends, RandomFilesGenerator(generateFrontend, generateWorkspace))

  fun generateWithFileNames(
    id: String,
    name: String,
    vendor: String = "JetBrains",
    version: String = "1.0.0",
    description: String? = null,
    icon: String? = null,
    depends: Map<String, String> = mapOf(),
    frontend: PluginPartFileNames? = null,
    workspace: PluginPartFileNames? = null,
  ): File =
    generate(id, name, icon, vendor, version, description, depends, ByNameFilesGenerator(frontend, workspace))


  private fun generate(
    id: String,
    name: String,
    icon: String? = null,
    vendor: String = "JetBrains",
    version: String = "1.0.0",
    description: String? = null,
    depends: Map<String, String> = mapOf(),
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
      descriptor.encodeToString(),
      fileName,
      icon,
      listOfNotNull(
        frontend?.modulePath, frontend?.classPath, frontend?.squashedAutomaticModules?.flatten(),
        workspace?.modulePath, workspace?.classPath, workspace?.squashedAutomaticModules?.flatten()
      ).flatten()
    ).toFile()
  }

  private fun substitute(modules: List<String>): Set<Barrel.Coordinates> {
    val digest = MessageDigest.getInstance("SHA-1")
    return modules.map {
      val file = Paths.get(it)
      val fileName = file.fileName.toString()
      val hash = digest.sha(file.toFile())
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
    val path = Paths.get(path, "$fileName.zip")
    Files.createDirectories(path.parent)
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

  private fun MessageDigest.sha(file: File): String {
    val buffer = ByteArray(1 * 1024 * 1024)
    reset()
    file.inputStream().buffered().use {
      while (true) {
        val read = it.read(buffer)
        if (read <= 0) break
        update(buffer, 0, read)
      }
    }

    val shaBytes = digest()
    return buildString {
      shaBytes.forEach { byte -> append(java.lang.Byte.toUnsignedInt(byte).toString(16)) }
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

private class RandomFilesGenerator(
  private val generateFrontend: Boolean,
  private val generateWorkspace: Boolean
) : FilesGenerator() {
  override fun generateFiles() {
    val filesNum = 5
    if (generateFrontend) frontendFiles = createPartFiles(filesNum)
    if (generateWorkspace) workspaceFiles = createPartFiles(filesNum)
  }

  private fun createPartFiles(filesNum: Int): PluginPartFiles =
    PluginPartFiles(
      createFiles(filesNum),
      createFiles(filesNum),
      mutableSetOf(createFiles(filesNum))
    )

  private fun createFiles(number: Int): MutableList<String> {
    return (0 until number).map {
      val file = Files.createTempFile("file$it-", ".txt")
      Files.writeString(file, "File #$it")
      return@map file.toString()
    }.toMutableList()
  }
}

fun main() {
  //FleetPluginGenerator("test").generate("test.plugin.first", "First Test", frontend = PluginPart(listOf("~/module.jar"), listOf("~/classpath.jar"), listOf()))
  FleetPluginGenerator("test").generateWithRandomFiles("test.plugin.second", "Second Test", generateFrontend = true)
  FleetPluginGenerator("test").generateWithFileNames(
    "test.plugin.third", "Third Test",
    frontend = PluginPartFileNames(listOf("f-module1.txt"), listOf("f-cp1.txt"), setOf(listOf("f-sq_m1.txt"))),
    workspace = PluginPartFileNames(listOf("w-module1.txt"), listOf("w-cp1.txt"), setOf(listOf("w-sq_m1.txt"))),
  )
}

