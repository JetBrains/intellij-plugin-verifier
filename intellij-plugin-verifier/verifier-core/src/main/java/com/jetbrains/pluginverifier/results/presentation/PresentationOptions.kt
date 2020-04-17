/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.presentation

enum class ClassGenericsSignatureOption {
  NO_GENERICS, WITH_GENERICS
}

enum class ClassOption {
  SIMPLE_NAME, FULL_NAME
}

enum class HostClassOption {
  NO_HOST, SIMPLE_HOST_NAME, FULL_HOST_NAME, FULL_HOST_WITH_SIGNATURE
}

enum class FieldTypeOption {
  NO_TYPE, SIMPLE_TYPE, FULL_TYPE
}

enum class MethodParameterTypeOption {
  SIMPLE_PARAM_CLASS_NAME, FULL_PARAM_CLASS_NAME
}

enum class MethodParameterNameOption {
  NO_PARAMETER_NAMES, WITH_PARAM_NAMES_IF_AVAILABLE
}

enum class MethodReturnTypeOption {
  NO_RETURN_TYPE, SIMPLE_RETURN_TYPE_CLASS_NAME, FULL_RETURN_TYPE_CLASS_NAME
}
