package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers

val MOCK_METHOD_LOCATION = MethodLocation(
    ClassLocation("SomeClass", "", Modifiers(1)),
    "someMethod",
    "()V",
    emptyList(),
    "",
    Modifiers(1)
)