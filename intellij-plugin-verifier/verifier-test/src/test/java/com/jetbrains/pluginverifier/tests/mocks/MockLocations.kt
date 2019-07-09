package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.classes.resolvers.UnknownClassFileOrigin
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers

val PUBLIC_MODIFIERS = Modifiers.of(Modifiers.Modifier.PUBLIC)

val MOCK_METHOD_LOCATION = MethodLocation(
    ClassLocation("SomeClass", null, PUBLIC_MODIFIERS, UnknownClassFileOrigin),
    "someMethod",
    "()V",
    emptyList(),
    null,
    PUBLIC_MODIFIERS
)