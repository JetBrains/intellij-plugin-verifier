/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.analysis

import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.utils.AsmUtil
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil
import com.jetbrains.pluginverifier.analysis.Location.Annotation
import com.jetbrains.pluginverifier.analysis.Location.Field
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.TypePath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

private typealias ClassName = String
private typealias Descriptor = String

private val logger: Logger = LoggerFactory.getLogger("com.jetbrains.pluginverifier.analysis.ClassReachabilityAnalysis")

class TypeGraph {

  private val backTypeEdges: MutableMap<ClassName, MutableSet<ClassName>> = hashMapOf()

  fun addEdge(from: ClassName, to: ClassName) {
    backTypeEdges.getOrPut(to) { hashSetOf() } += from
  }

  fun getEdgesTo(to: ClassName): Set<ClassName> =
    backTypeEdges[to].orEmpty()
}

class ReachabilityGraph(private val graph: TypeGraph) {

  enum class ReachabilityMark {
    MAIN_PLUGIN,
    OPTIONAL_PLUGIN
  }

  private val classMarks: MutableMap<ClassName, EnumSet<ReachabilityMark>> = hashMapOf()

  fun markClass(className: ClassName, mark: ReachabilityMark) {
    classMarks.getOrPut(className) { EnumSet.noneOf(ReachabilityMark::class.java) } += mark
  }

  fun isClassReachableFromMark(className: ClassName, mark: ReachabilityMark): Boolean {
    val visitedClasses = hashSetOf<ClassName>()
    val stack = LinkedList<ClassName>()
    val way = LinkedList<ClassName>()

    stack += className
    while (stack.isNotEmpty()) {
      val currentClass = stack.peekFirst()
      if (!visitedClasses.add(currentClass)) {
        //Second time visiting the same node => all node's children are visited => poll from the queue.
        stack.pollFirst()
        way.pollLast()
        continue
      }
      way.addLast(currentClass)

      val referencingTypes = graph.getEdgesTo(currentClass)
      for (typeName in referencingTypes) {
        if (classMarks[typeName]?.contains(mark) == true) {
          //Mark all the classes on the way as memoization.
          for (name in way) {
            markClass(name, mark)
          }
          return true
        }

        if (typeName !in visitedClasses) {
          stack.addFirst(typeName)
        }
      }
    }
    return false
  }
}


fun buildClassReachabilityGraph(
  idePlugin: IdePlugin,
  pluginResolver: Resolver,
  dependenciesGraph: DependenciesGraph
): ReachabilityGraph {
  val typeGraph = buildTypeGraph(pluginResolver)
  val reachabilityGraph = ReachabilityGraph(typeGraph)

  val mainClasses = PluginXmlUtil.getAllClassesReferencedFromXml(idePlugin)
  for (className in mainClasses) {
    reachabilityGraph.markClass(className, ReachabilityGraph.ReachabilityMark.MAIN_PLUGIN)
  }

  val missingOptionalDependencies = dependenciesGraph.getDirectMissingDependencies().filter { it.dependency.isOptional }
  for (missingOptionalDependency in missingOptionalDependencies) {
    val optionalPlugin = idePlugin.optionalDescriptors.find { it.dependency == missingOptionalDependency.dependency }?.optionalPlugin
    val modules = idePlugin.modulesDescriptors
      .filter { it.dependencies.map { it.id }.contains(missingOptionalDependency.dependency.id) }
      .map { it.module }
    (if (optionalPlugin != null) modules + optionalPlugin else modules).forEach {
      val optionalClasses = PluginXmlUtil.getAllClassesReferencedFromXml(it)
      for (className in optionalClasses) {
        reachabilityGraph.markClass(className, ReachabilityGraph.ReachabilityMark.OPTIONAL_PLUGIN)
      }
    }
  }

  return reachabilityGraph
}

private fun buildTypeGraph(pluginResolver: Resolver): TypeGraph {
  val graph = TypeGraph()

  pluginResolver.processAllClasses { resolutionResult ->
    if (resolutionResult !is ResolutionResult.Found) {
      return@processAllClasses true
    }
    val classNode = resolutionResult.value

    val references = TypeReferences()
    classNode.accept(TypeReferencesClassVisitor(references))
    val typeReferences = references.typeReferences
    for (typeReference in typeReferences) {
      if (pluginResolver.containsClass(typeReference)) {
        graph.addEdge(classNode.name, typeReference)
      }
    }
    true
  }

  return graph
}

private class TypeReferences {

  val typeReferences = hashSetOf<ClassName>()

  fun addReferences(type: Type) {
    when {
      type.sort == Type.ARRAY -> addReferences(type.elementType)
      type.sort == Type.OBJECT -> {
        val internalName = type.internalName
        check(!internalName.startsWith("["))
        typeReferences += internalName
      }
      type.sort == Type.METHOD -> {
        addReferences(type.returnType)
        type.argumentTypes.forEach { addReferences(it) }
      }
    }
  }
}

private fun TypeReferences.addHandle(handle: Handle) {
  addReferences(Type.getObjectType(handle.owner))
  val handleType = Type.getType(handle.desc)
  addReferences(handleType)
}

private fun TypeReferences.addConstantDynamic(constantDynamic: ConstantDynamic) {
  addReferences(Type.getType(constantDynamic.descriptor))
  addHandle(constantDynamic.bootstrapMethod)
}

private class TypeReferencesClassVisitor(private val references: TypeReferences) : ClassVisitor(AsmUtil.ASM_API_LEVEL) {
  override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
    if (superName != null) {
      references.addReferences(Type.getObjectType(superName))
    }
    interfaces?.forEach { references.addReferences(Type.getObjectType(it)) }
  }

  override fun visitMethod(access: Int, name: String?, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
    val methodType = Type.getMethodType(descriptor)
    references.addReferences(methodType)
    exceptions?.forEach { references.addReferences(Type.getObjectType(it)) }
    return TypeReferencesMethodVisitor(references)
  }

  override fun visitField(access: Int, name: String?, descriptor: String, signature: String?, value: Any?): FieldVisitor {
    return TypeReferencesFieldVisitor(references.withType(descriptor, field(name)))
  }

  override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
    references.addReferences(Type.getObjectType(name))
    if (outerName != null) {
      references.addReferences(Type.getObjectType(outerName))
    }
  }

  override fun visitOuterClass(owner: String, name: String?, descriptor: String?) {
    references.addReferences(Type.getObjectType(owner))
  }

  override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
    references.addReferences(Type.getObjectType(descriptor))
    return TypeReferencesAnnotationVisitor(references)
  }

  override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor {
    references.addReferences(Type.getType(descriptor))
    return TypeReferencesAnnotationVisitor(references)
  }

  override fun visitNestHost(nestHost: String) {
    references.addReferences(Type.getObjectType(nestHost))
  }

  override fun visitNestMember(nestMember: String) {
    references.addReferences(Type.getObjectType(nestMember))
  }
}

private class TypeReferencesMethodVisitor(private val references: TypeReferences) : MethodVisitor(AsmUtil.ASM_API_LEVEL) {
  override fun visitMultiANewArrayInsn(descriptor: String, numDimensions: Int) {
    references.addReferences(Type.getType(descriptor))
  }

  override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
    if (type != null) {
      references.addReferences(Type.getObjectType(type))
    }
  }

  override fun visitLdcInsn(value: Any?) {
    when (value) {
      is Type -> references.addReferences(value)
      is Handle -> references.addHandle(value)
      is ConstantDynamic -> {
        references.addReferences(Type.getType(value.descriptor))
        val bootstrapMethod = value.bootstrapMethod
        references.addHandle(bootstrapMethod)
      }
    }
  }

  override fun visitTypeInsn(opcode: Int, type: String) {
    references.addReferences(Type.getObjectType(type))
  }

  override fun visitAnnotationDefault(): AnnotationVisitor {
    return TypeReferencesAnnotationVisitor(references)
  }

  override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
    return TypeReferencesAnnotationVisitor(references.withType(descriptor, Annotation(visible)))
  }

  override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor {
    references.addReferences(Type.getType(descriptor))
    return TypeReferencesAnnotationVisitor(references)
  }

  override fun visitInvokeDynamicInsn(name: String?, descriptor: String, bootstrapMethodHandle: Handle, vararg bootstrapMethodArguments: Any) {
    val type = Type.getType(descriptor)
    references.addReferences(type)
    references.addHandle(bootstrapMethodHandle)
    for (methodArgument in bootstrapMethodArguments) {
      when (methodArgument) {
        is Type -> references.addReferences(methodArgument)
        is Handle -> references.addHandle(methodArgument)
        is ConstantDynamic -> references.addConstantDynamic(methodArgument)
      }
    }
  }

  override fun visitTryCatchAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor {
    references.addReferences(Type.getType(descriptor))
    return TypeReferencesAnnotationVisitor(references)
  }

  override fun visitMethodInsn(opcode: Int, owner: String, name: String?, descriptor: String) {
    references.addReferences(Type.getObjectType(owner))
    references.addReferences(Type.getMethodType(descriptor))
  }

  override fun visitMethodInsn(opcode: Int, owner: String, name: String?, descriptor: String, isInterface: Boolean) {
    references.addReferences(Type.getObjectType(owner))
    references.addReferences(Type.getMethodType(descriptor))
  }

  override fun visitInsnAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor {
    references.addReferences(Type.getType(descriptor))
    return TypeReferencesAnnotationVisitor(references)
  }

  override fun visitParameterAnnotation(parameter: Int, descriptor: String, visible: Boolean): AnnotationVisitor {
    references.addReferences(Type.getType(descriptor))
    return TypeReferencesAnnotationVisitor(references)
  }

  override fun visitLocalVariableAnnotation(typeRef: Int, typePath: TypePath?, start: Array<out Label>?, end: Array<out Label>?, index: IntArray?, descriptor: String, visible: Boolean): AnnotationVisitor {
    references.addReferences(Type.getType(descriptor))
    return TypeReferencesAnnotationVisitor(references)
  }

  override fun visitLocalVariable(name: String?, descriptor: String, signature: String?, start: Label?, end: Label?, index: Int) {
    references.addReferences(Type.getType(descriptor))
  }

  override fun visitFieldInsn(opcode: Int, owner: String, name: String?, descriptor: String) {
    references.addReferences(Type.getObjectType(owner))
    references.addReferences(Type.getType(descriptor))
  }
}

private class TypeReferencesFieldVisitor(private val references: TypeReferences) : FieldVisitor(AsmUtil.ASM_API_LEVEL) {
  override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
    return TypeReferencesAnnotationVisitor(references.withType(descriptor, Annotation(visible)))
  }

  override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor {
    references.addReferences(Type.getType(descriptor))
    return TypeReferencesAnnotationVisitor(references)
  }
}

private class TypeReferencesAnnotationVisitor(private val references: TypeReferences) : AnnotationVisitor(AsmUtil.ASM_API_LEVEL) {
  override fun visitAnnotation(name: String?, descriptor: String): AnnotationVisitor {
    return TypeReferencesAnnotationVisitor(references.withType(descriptor))
  }

  override fun visitEnum(name: String?, descriptor: String, value: String?) {
    references.addReferences(Type.getType(descriptor))
  }

  override fun visitArray(name: String?): AnnotationVisitor {
    return TypeReferencesAnnotationVisitor(references)
  }
}

private fun TypeReferences.withType(descriptor: Descriptor): TypeReferences = apply {
  return withType(descriptor, Location.Unknown)
}

private fun TypeReferences.withType(descriptor: Descriptor, location: Location): TypeReferences = apply {
  try {
    val type: Type = Type.getType(descriptor)
    addReferences(type)
  } catch (e: IllegalArgumentException) {
    logger.debug("Skipping invalid descriptor at {} {}", location, descriptor.toLogString())
  }
}

private fun Descriptor.toLogString(): String {
  val trimmedDesc = this.trim()
  val len = this.length - trimmedDesc.length
  return if (len > 0) {
    "[$trimmedDesc] ($len whitespace characters trimmed)"
  } else {
    "[$trimmedDesc]"
  }
}

private sealed class Location {
  data class Field(val fieldName: String) : Location() {
    override fun toString() = "field $fieldName"
  }
  data class Annotation(val isVisible: Boolean) : Location() {
    override fun toString() = if (isVisible) {
      "visible"
    } else {
      "invisible"
    } + " annotation"
  }
  object Unknown : Location() {
    override fun toString() = "unknown location"
  }
}

private fun field(name: String?): Location = name?.let {
  Field(it)
} ?: Location.Unknown
