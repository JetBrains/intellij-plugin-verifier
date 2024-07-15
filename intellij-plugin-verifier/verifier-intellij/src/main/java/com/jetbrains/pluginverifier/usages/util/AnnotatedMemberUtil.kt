/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.util

import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember

sealed class MemberAnnotation {

  abstract val member: ClassFileMember

  abstract val annotationName: String

  class AnnotatedDirectly(
    override val member: ClassFileMember,
    override val annotationName: String
  ) : MemberAnnotation()

  class AnnotatedViaContainingClass(
    val containingClass: ClassFileMember,
    override val member: ClassFileMember,
    override val annotationName: String
  ) : MemberAnnotation()

  class AnnotatedViaPackage(
    val packageName: String,
    override val member: ClassFileMember,
    override val annotationName: String
  ) : MemberAnnotation()
}
