package com.jetbrains.plugin.structure.classes.utils

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal const val KOTLIN_METADATA_ANNOTATION_DESC = "Lkotlin/Metadata;"

private typealias Signature = String

private val LOG: Logger = LoggerFactory.getLogger(KtClassResolver::class.java)

class KtClassResolver {
  private val cache = Caffeine.newBuilder()
    .maximumSize(16_384)
    .build<Signature, KtClassNode>()

  operator fun get(classNode: ClassNode): KtClassNode? {
    val signature: Signature = classNode.signature ?: return classNode.ktClassNode
    return cache.get(signature) {
      classNode.ktClassNode
    }
  }

  private val ClassNode.ktClassNode: KtClassNode?
    get() = findMetadataAnnotation(this)
      ?.let { annotation -> getKtClassNode(this, annotation) }

  private fun getKtClassNode(classNode: ClassNode, metadataAnnotation: Metadata): KtClassNode? {
    return readMetadata(classNode, metadataAnnotation)?.let { metadata ->
      KtClassNode(classNode, metadata)
    }
  }

  private fun findMetadataAnnotation(classNode: ClassNode): Metadata? {
    return classNode.allAnnotations
      .firstOrNull { it.desc == KOTLIN_METADATA_ANNOTATION_DESC }
      ?.toMetadata()
  }

  private fun readMetadata(classNode: ClassNode, metadataAnnotation: Metadata): KotlinClassMetadata.Class? {
    return try {
      if (metadataAnnotation.metadataVersion.isNotEmpty()) {
        KotlinClassMetadata.readLenient(metadataAnnotation) as? KotlinClassMetadata.Class
      } else {
        LOG.debug("Class [${classNode.name}] has @kotlin.Metadata annotation without 'metadataVersion' attribute. It cannot be read")
        null
      }
    } catch (e: IllegalArgumentException) {
      LOG.debug("Failed to read Kotlin metadata for class ${classNode.name}", e)
      null
    }
  }

  private fun AnnotationNode.toMetadata() = Metadata(
    kind = int("k"),
    metadataVersion = ints("mv"),
    data1 = strings("d1"),
    data2 = strings("d2"))

  private fun AnnotationNode.int(key: String) = get<Int>(key)
  private fun AnnotationNode.ints(key: String) = get<List<Int>?>(key)?.toIntArray()
  private fun AnnotationNode.strings(key: String) = get<List<String>?>(key)?.toTypedArray()

  private inline fun <reified T> AnnotationNode.get(key: String): T? {
    return getAnnotationValue(key)?.let { value ->
      if (value is T) value else null
    }
  }

  private fun AnnotationNode.getAnnotationValue(key: String): Any? {
    val values = values ?: return null
    for (i in 0 until values.size step 2) {
      if (values[i] == key) {
        return values[i + 1]
      }
    }
    return null
  }

  companion object {
    fun hasKotlinMetadataAnnotation(classNode: ClassNode): Boolean {
      return classNode.allAnnotations.any { it.desc == KOTLIN_METADATA_ANNOTATION_DESC }
    }
  }
}

private val ClassNode.allAnnotations: List<AnnotationNode>
  get() = (visibleAnnotations.orEmpty() + invisibleAnnotations.orEmpty())