/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.location.Location
import org.objectweb.asm.tree.AnnotationNode

interface ClassFileMember {
  val containingClassFile: ClassFile
  val location: Location
  val isDeprecated: Boolean
  val runtimeInvisibleAnnotations: List<AnnotationNode>

  val accessType: AccessType
  val isPublic: Boolean
  val isProtected: Boolean
  val isPrivate: Boolean
  val isPackagePrivate: Boolean

  val isSynthetic: Boolean
}