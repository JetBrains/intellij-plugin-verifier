package com.jetbrains.pluginverifier.usages.properties

import com.jetbrains.pluginverifier.results.presentation.JvmDescriptorsPresentation
import com.jetbrains.pluginverifier.verifiers.findAnnotation
import com.jetbrains.pluginverifier.verifiers.getAnnotationValue
import com.jetbrains.pluginverifier.verifiers.hasAnnotation
import com.jetbrains.pluginverifier.verifiers.resolution.BinaryClassName
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileAsm
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import com.jetbrains.pluginverifier.verifiers.resolution.MethodAsm
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.MethodNode

class EnumClassPropertyUsageAdapter() {

  fun resolve(method: Method): ResourceBundledProperty? {
    if (!supports(method)) return null
    val methodParam = method.methodParameters.drop(2).first()
    val propertyKeyAnn = method.getAnnotations { invisibleParameterAnnotations }.findAnnotation("org/jetbrains/annotations/PropertyKey") ?: return null

    val bundleName = propertyKeyAnn.getAnnotationValue("resourceBundle") as? String ?: return null
    return ResourceBundledProperty(methodParam.name, bundleName)
  }

  fun supports(method: Method): Boolean {
    return method.isEnumClass() && isEnumConstructorDesc(method.descriptor)
      && method.hasParameterAnnotation("org/jetbrains/annotations/PropertyKey")
  }

  fun isEnumConstructorDesc(descriptor: String): Boolean {
    val (paramTypes, returnType) = JvmDescriptorsPresentation.splitMethodDescriptorOnRawParametersAndReturnTypes(descriptor)

    return paramTypes.size > 2 && paramTypes[0] == "Ljava/lang/String;" && paramTypes[1] == "I" && returnType == "V"
      && paramTypes.slice(2 until paramTypes.size).contains("Ljava/lang/String;")
  }

  private fun Method.isEnumClass(): Boolean {
    val enclosingClassFile = containingClassFile as? ClassFileAsm ?: return false
    return enclosingClassFile.superName == "java/lang/Enum"
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