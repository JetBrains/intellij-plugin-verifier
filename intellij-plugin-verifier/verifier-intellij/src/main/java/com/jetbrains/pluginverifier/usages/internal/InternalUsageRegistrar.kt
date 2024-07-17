package com.jetbrains.pluginverifier.usages.internal

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference

interface InternalUsageRegistrar {
  fun registerClass(
    classReference: ClassReference,
    apiElement: ClassLocation,
    usageLocation: Location
  )

  fun registerMethod(
    methodReference: MethodReference,
    apiElement: MethodLocation,
    usageLocation: MethodLocation
  )

  fun registerField(
    fieldReference: FieldReference,
    apiElement: FieldLocation,
    usageLocation: MethodLocation
  )

}