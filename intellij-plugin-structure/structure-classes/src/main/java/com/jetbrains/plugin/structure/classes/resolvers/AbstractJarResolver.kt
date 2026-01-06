package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException
import java.nio.file.Path
import java.util.*

abstract class AbstractJarResolver(
  protected open val jarPath: Path,
  override val readMode: ReadMode,
  protected open val fileOrigin: FileOrigin,
  name: String = jarPath.fileName.toString()
) : NamedResolver(name) {

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)

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
    val bundleName: String = control.toBundleName(baseName, locale)

    val resourceName: String = control.toResourceName(bundleName, "properties")

    return try {
      val propertyResourceBundle = readPropertyResourceBundle(resourceName) ?: return ResolutionResult.NotFound
      return ResolutionResult.Found(propertyResourceBundle, fileOrigin)
    } catch (e: IOException) {
      failedToRead(resourceName, e, "I/O error")
    } catch (e: NullPointerException) {
      failedToRead(resourceName, e, "Stream is null")
    } catch (e: IllegalArgumentException) {
      failedToRead(resourceName, e, "Stream contains malformed Unicode sequences")
    } catch (e: MalformedInputException) {
      failedToRead(resourceName, e, "Stream contains an invalid UTF-8 sequence")
    } catch (e: UnmappableCharacterException) {
      failedToRead(resourceName, e, "Stream contains an unmappable UTF-8 sequence")
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      ResolutionResult.Invalid(e.message ?: e.javaClass.name)
    }
  }

  protected abstract fun readPropertyResourceBundle(bundleResourceName: String): PropertyResourceBundle?

  private fun failedToRead(bundleResourceName: String, e: Exception, reason: String): ResolutionResult.FailedToRead {
    logger.debug("Cannot read resource bundle '{}'. {}: {}", bundleResourceName, reason, e.message, e)
    return ResolutionResult.FailedToRead(e.message ?: e.javaClass.name)
  }
}