/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu.bean

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.plugin.structure.edu.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class EduTask(
  @JsonProperty(NAME)
  val name: String = "",
  @JsonProperty(TASK_TYPE)
  var taskType: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EduItem(
  @JsonProperty(TITLE)
  val title: String = "",
  @JsonProperty(TYPE)
  val type: String = "",
  @JsonProperty(ITEMS)
  val items: List<EduItem> = mutableListOf(),
  @JsonProperty(TASK_LIST)
  var taskList: List<EduTask> = mutableListOf()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EduVendor(
  @JsonProperty(VENDOR_NAME)
  val name: String? = null,
  @JsonProperty(VENDOR_EMAIL)
  val vendorEmail: String = "",
  @JsonProperty(VENDOR_URL)
  val vendorUrl: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EduPluginDescriptor(
  @JsonProperty(TITLE)
  val title: String? = null,
  @JsonProperty(SUMMARY)
  val summary: String? = null,
  @JsonProperty(LANGUAGE)
  val language: String? = null,
  @JsonProperty(PROGRAMMING_LANGUAGE)
  val programmingLanguage: String? = null,
  @JsonProperty(ENVIRONMENT)
  val environment: String? = null,
  @JsonProperty(ITEMS)
  val items: List<EduItem> = mutableListOf(),
  @JsonProperty(VENDOR)
  val vendor: EduVendor? = null,
  @JsonProperty(VERSION)
  val pluginVersion: String? = null,
  @JsonProperty(DESCRIPTOR_VERSION)
  val descriptorVersion: Int? = null,
  @JsonProperty(IS_PRIVATE)
  val isPrivate: Boolean? = false
)