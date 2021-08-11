/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.discouraging

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.isOriginOfType
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption
import com.jetbrains.pluginverifier.results.presentation.ClassOption
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import com.jetbrains.pluginverifier.usages.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.usages.deprecated.DeprecationInfo
import com.jetbrains.pluginverifier.usages.formatUsageLocation
import java.util.*

/**
 * JDK classes of some packages should not be used when they come from JDK or IDEA distribution
 * because these classes will not be available in JDK 9+.
 *
 * See MP-2043: detect usages of discouraging JDK classes in plugins.
 */
class DiscouragingJdkClassUsage(
  override val apiElement: ClassLocation,
  override val usageLocation: Location,
  private val classFileOrigin: FileOrigin
) : DeprecatedApiUsage(DeprecationInfo(false, "JDK 8")) {

  override val problemType: String
    get() = "JDK classes of some packages should not be used when they come from JDK or IDEA distribution because these classes will not be available in JDK 9+."

  override val apiReference
    get() = apiElement.toReference()

  override val shortDescription: String
    get() = "Usage of JDK 8 specific " + apiElement.elementType.presentableName + " " +
      apiElement.formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.NO_GENERICS)

  override val fullDescription: String
    get() = buildString {
      append("JDK 8 specific " + apiElement.elementType.presentableName + " " + apiElement.formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.NO_GENERICS))
      append(" is referenced in " + usageLocation.formatUsageLocation() + ". ")
      val isClassProvidedByIde: Boolean = classFileOrigin.isOriginOfType<IdeFileOrigin>()
      if (isClassProvidedByIde) {
        append(
          "This " + apiElement.elementType.presentableName + " will be temporarily available in IDE distribution for " +
            "compatibility but you should use another API or provide your own dependency containing the classes."
        )
      } else {
        append(
          "This " + apiElement.elementType.presentableName + " is neither available in JDK 9+ " +
            "nor is it available in IDE distribution. This may lead to compatibility problems when running the IDE with newer JDK versions."
        )
      }
    }

  override fun equals(other: Any?): Boolean = other is DiscouragingJdkClassUsage
    && apiElement == other.apiElement
    && usageLocation == other.usageLocation
    && classFileOrigin == other.classFileOrigin

  override fun hashCode() = Objects.hash(apiElement, usageLocation, classFileOrigin)

}