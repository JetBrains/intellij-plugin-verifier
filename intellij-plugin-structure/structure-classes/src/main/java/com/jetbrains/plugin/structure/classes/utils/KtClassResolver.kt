package com.jetbrains.plugin.structure.classes.utils

import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.Metadata
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import java.util.*

internal const val KOTLIN_METADATA_ANNOTATION_DESC = "Lkotlin/Metadata;"

class KtClassResolver {
  private val cache = TreeMap<ClassNode, KtClassNode>(Comparator<ClassNode> { o1, o2 ->
    val s1 = o1?.signature ?: ""
    val s2 = o2?.signature ?: ""
    s1.compareTo(s2)
  })

  operator fun get(classNode: ClassNode): KtClassNode? {
    return if (cache.containsKey(classNode)) {
      cache[classNode]
    } else {
      classNode.ktClassNode?.also {
        cache[classNode] = it
      }
    }
  }

  private val ClassNode.ktClassNode: KtClassNode?
    get() = findMetadataAnnotation(this)
      ?.let { annotation -> getKtClassNode(this, annotation) }


  private fun getKtClassNode(classNode: ClassNode, metadataAnnotation: Metadata): KtClassNode? {
    val metadata = KotlinClassMetadata.readStrict(metadataAnnotation)
    return if (metadata is KotlinClassMetadata.Class) {
      KtClassNode(classNode, metadata)
    } else {
      null
    }
  }

  private fun findMetadataAnnotation(classNode: ClassNode): Metadata? {
    return classNode.allAnnotations
      .firstOrNull { it.desc == KOTLIN_METADATA_ANNOTATION_DESC }
      ?.toMetadata()
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