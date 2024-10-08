package com.jetbrains.pluginverifier.usages.properties

import com.jetbrains.plugin.structure.classes.utils.KtClassResolver
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.getAnnotationValue
import com.jetbrains.pluginverifier.verifiers.hasAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodAsm
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.MethodNode

internal const val ENUM_PRIVATE_CONSTRUCTOR_DESC = "(Ljava/lang/String;ILjava/lang/String;)V"

class EnumClassPropertyUsageAdapter(private val classResolver: KtClassResolver = KtClassResolver()) {

  fun resolve(method: Method): ResourceBundledProperty? {
    if (!supports(method)) return null
    val methodParam = method.methodParameters.drop(2).first()
    val propertyKeyAnn = method.getAnnotations { invisibleParameterAnnotations }.findAnnotation("org/jetbrains/annotations/PropertyKey") ?: return null

    val bundleName = propertyKeyAnn.getAnnotationValue("resourceBundle") as? String ?: return null
    return ResourceBundledProperty(methodParam.name, bundleName)
  }

  fun supports(method: Method): Boolean {
    return method.isInKotlinEnumClass()
      && method.descriptor == ENUM_PRIVATE_CONSTRUCTOR_DESC
      && method.hasParameterAnnotation("org/jetbrains/annotations/PropertyKey")
  }

  private fun Method.isInKotlinEnumClass(): Boolean {
    val enclosingClassFile = containingClassFile as? ClassFileAsm ?: return false
    val ktClass = classResolver[enclosingClassFile.asmNode]
    return ktClass != null && ktClass.isEnumClass
  }

  private fun Method.hasParameterAnnotation(annotation: BinaryClassName): Boolean {
    val method = this as? MethodAsm ?: return false
    val visibleAnnotations = method.getAnnotations { visibleParameterAnnotations }
    val invisibleAnnotations = method.getAnnotations { invisibleParameterAnnotations }
    val allAnnotations = (visibleAnnotations + invisibleAnnotations)
    return allAnnotations.hasAnnotation(annotation)
  }

  private fun Method.getAnnotations(propertyExtractor: MethodNode.() -> Array<List<AnnotationNode?>?>?): List<AnnotationNode> {
    val methodAsm = this as? MethodAsm ?: return emptyList()
    val nestedAnnotations = propertyExtractor.invoke(methodAsm.asmNode) ?: emptyArray()
    return nestedAnnotations.filterNotNull().flatMap { it.filterNotNull() }
  }
}