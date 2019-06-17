package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.classes.packages.PackageSet
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

class FixedClassesResolver private constructor(
    private val classes: Map<String, ClassNode>,
    override val readMode: ReadMode
) : Resolver() {

  companion object {
    fun create(vararg classes: ClassNode): Resolver = create(classes.toList())

    fun create(classes: Iterable<ClassNode>): Resolver =
        FixedClassesResolver(classes.reversed().associateBy { it.name }, ReadMode.FULL)

    fun create(readMode: ReadMode, classes: Iterable<ClassNode>): Resolver =
        FixedClassesResolver(classes.reversed().associateBy { it.name }, readMode)

    private val uniqueClassPathSequenceNumber = AtomicInteger()
  }

  private val packageSet = PackageSet()

  private val uniqueClassPath: Path

  init {
    for (className in classes.keys) {
      packageSet.addPackagesOfClass(className)
    }
    uniqueClassPath = Paths.get("fixed-classes-resolver-${uniqueClassPathSequenceNumber.getAndIncrement()}")
  }

  override fun processAllClasses(processor: (ClassNode) -> Boolean) =
      classes.values
          .asSequence()
          .all(processor)

  override fun findClass(className: String): ClassNode? = classes[className]

  override fun getClassLocation(className: String): Resolver? = this

  override val allClasses
    get() = classes.keys

  override val allPackages: Set<String>
    get() = packageSet.getAllPackages()

  override val isEmpty
    get() = classes.isEmpty()

  override val classPath: List<Path>
    get() = listOf(uniqueClassPath)

  override val finalResolvers
    get() = listOf(this)

  override fun containsClass(className: String) = className in classes

  override fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  override fun close() = Unit

  override fun toString() = "Resolver of ${classes.size} predefined class" + (if (classes.size != 1) "es" else "")

}
