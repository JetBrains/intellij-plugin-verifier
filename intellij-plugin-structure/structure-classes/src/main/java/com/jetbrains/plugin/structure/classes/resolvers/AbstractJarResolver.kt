package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Path
import java.util.*

abstract class AbstractJarResolver(
  protected open val jarPath: Path,
  override val readMode: ReadMode,
  protected open val fileOrigin: FileOrigin,
  name: String = jarPath.fileName.toString()
) : NamedResolver(name) {

  protected abstract val bundleNames: MutableMap<String, MutableSet<String>>

  abstract val implementedServiceProviders: Map<String, Set<String>>

  protected open fun readClass(className: CharSequence, classPath: Path): ResolutionResult<ClassNode> {
    return try {
      val classNode = classPath.inputStream().use {
        AsmUtil.readClassNode(className, it, readMode == ReadMode.FULL)
      }
      ResolutionResult.Found(classNode, fileOrigin)
    } catch (e: InvalidClassFileException) {
      ResolutionResult.Invalid(e.message)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
    }
  }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale): ResolutionResult<PropertyResourceBundle> {
    if (baseName !in bundleNames) {
      return ResolutionResult.NotFound
    }

    val control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES)
    val bundleName = control.toBundleName(baseName, locale)

    val resourceName = control.toResourceName(bundleName, "properties")
    val propertyResourceBundle = try {
      readPropertyResourceBundle(resourceName)
    } catch (e: IllegalArgumentException) {
      return ResolutionResult.Invalid(e.message ?: e.javaClass.name)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      return ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
    }

    if (propertyResourceBundle != null) {
      return ResolutionResult.Found(propertyResourceBundle, fileOrigin)
    }

    return ResolutionResult.NotFound
  }

  protected abstract fun readPropertyResourceBundle(bundleResourceName: String): PropertyResourceBundle?
}