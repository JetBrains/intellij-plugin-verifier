package com.jetbrains.pluginverifier.output

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.NoModuleDependencies
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.problems.MethodNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalClassUsage
import com.jetbrains.pluginverifier.usages.internal.InternalClassUsage
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableTypeInherited
import com.jetbrains.pluginverifier.warnings.PluginStructureWarning

const val PLUGIN_ID = "pluginId"
const val PLUGIN_VERSION = "1.0"

open class BaseOutputTest {
    protected fun mockPluginInfo(): PluginInfo =
        object : PluginInfo(PLUGIN_ID, PLUGIN_ID, PLUGIN_VERSION, null, null, null) {}

    private val sampleStuffFactoryLocation = ClassLocation("SampleStuffFactory", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)

    private val javaLangObjectClassHierarchy = ClassHierarchy(
        "java/lang/Object",
        false,
        null,
        emptyList()
    )

    private val internalApiClassLocation = ClassLocation("InternalApiRegistrar", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)

    private val mockMethodLocationInSampleStuffFactory = MethodLocation(
        sampleStuffFactoryLocation,
        "produceStuff",
        "()V",
        emptyList(),
        null,
        Modifiers.of(Modifiers.Modifier.PUBLIC)
    )

    protected fun methodNotFoundProblem(): MethodNotFoundProblem {
        val deletedClassRef = ClassReference("org/some/deleted/Class")
        val referencingMethodLocation = MethodLocation(
            ClassLocation("SomeClassUsingDeletedClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin),
            "someMethodReferencingDeletedClass",
            "()V",
            emptyList(),
            null,
            Modifiers.of(Modifiers.Modifier.PUBLIC)
        )
        return MethodNotFoundProblem(
            MethodReference(deletedClassRef, "foo", "()V"),
            referencingMethodLocation,
            Instruction.INVOKE_VIRTUAL,
            javaLangObjectClassHierarchy
        )
    }

    protected fun methodNotFoundProblemInSampleStuffFactoryClass(): MethodNotFoundProblem {
        val deletedClassRef = ClassReference("org/some/deleted/Class")
        return MethodNotFoundProblem(
            MethodReference(deletedClassRef, "foo", "()V"),
            mockMethodLocationInSampleStuffFactory,
            Instruction.INVOKE_VIRTUAL,
            javaLangObjectClassHierarchy
        )
    }

    protected fun superInterfaceBecameClassProblem(): SuperInterfaceBecameClassProblem {
        val child = ClassLocation("com.jetbrains.plugin.Child", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
        val clazz = ClassLocation("com.jetbrains.plugin.Parent", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
        return SuperInterfaceBecameClassProblem(child, clazz)
    }

    protected fun superInterfaceBecameClassProblemInOtherLocation(): SuperInterfaceBecameClassProblem {
        val child = ClassLocation("com.jetbrains.plugin.pkg.Child", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
        val clazz = ClassLocation("com.jetbrains.plugin.pkg.Parent", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
        return SuperInterfaceBecameClassProblem(child, clazz)
    }

    protected fun internalApiUsages() =  setOf(
        InternalClassUsage(ClassReference("com.jetbrains.InternalClass"), internalApiClassLocation, mockMethodLocationInSampleStuffFactory)
    )

    protected fun mockStructureWarnings() = setOf(
        PluginStructureWarning(NoModuleDependencies(IdePluginManager.PLUGIN_XML))
    )

    protected fun mockNonExtendableApiUsages(): Set<NonExtendableTypeInherited> {
        val nonExtendableClass = ClassLocation("NonExtendableClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
        val extendingClass = ClassLocation("ExtendingClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)

        return setOf(
            NonExtendableTypeInherited(nonExtendableClass, extendingClass)
        )
    }

    protected fun mockExperimentalApiUsages(): Set<ExperimentalApiUsage> {
        val experimentalClass = ClassLocation("ExperimentalClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
        val extendingClass = ClassLocation("ExtendingClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
        val usageLocation = MethodLocation(
            extendingClass,
            "someMethod",
            "()V",
            emptyList(),
            null,
            Modifiers.of(Modifiers.Modifier.PUBLIC)
        )

        return setOf(
            ExperimentalClassUsage(experimentalClass.toReference(), experimentalClass, usageLocation)
        )
    }

    private object SomeFileOrigin : FileOrigin {
        override val parent: FileOrigin? = null
    }
}