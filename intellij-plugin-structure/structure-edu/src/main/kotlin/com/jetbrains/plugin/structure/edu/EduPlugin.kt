/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu

import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.edu.bean.EduPluginDescriptor
import com.jetbrains.plugin.structure.edu.bean.EduTask


data class EduPlugin(
  override val pluginName: String? = null,
  override val description: String? = null,
  override var vendor: String? = null,
  override var vendorEmail: String? = null,
  override var vendorUrl: String? = null,
  override val icons: List<PluginIcon> = emptyList(),
  override val pluginId: String? = null,
  override val pluginVersion: String? = null,
  override val thirdPartyDependencies: List<ThirdPartyDependency> = emptyList(),
  val descriptorVersion: Int? = null,
  val language: String? = null,
  val programmingLanguage: String? = null,
  val programmingLanguageId: String? = null,
  val programmingLanguageVersion: String? = null,
  val environment: String? = null,
  val isPrivate: Boolean = false,
  val eduStat: EduStat? = null
) : Plugin {

  override val url: String = ""
  override val changeNotes: String? = null
}

data class EduStat(
  val sections: List<Section>,
  val lessons: List<String>,
  val tasksByLessons: Map<String, List<EduTask>>
) {

  val tasks = tasksByLessons.values.flatten().groupingBy { it.taskType }.eachCount()

  fun countInteractiveChallenges(): Int {
    val ideTasks = tasks[TaskType.IDE.id] ?: 0
    val outputTasks = tasks[TaskType.OUTPUT.id] ?: 0
    val eduTasks = tasks[TaskType.EDU.id] ?: 0
    return ideTasks + outputTasks + eduTasks
  }

  fun countQuizzes(): Int {
    return tasks[TaskType.CHOICE.id] ?: 0
  }

  fun countTheoryTasks(): Int {
    return tasks[TaskType.THEORY.id] ?: 0
  }

  companion object {
    fun fromDescriptor(descriptor: EduPluginDescriptor): EduStat {
      val allItems = descriptor.items.flatMap { it.items } + descriptor.items

      val sections = descriptor.items.filter { it.type == ItemType.SECTION.id }
        .map { Section(it.presentableName, it.items.map { lesson -> lesson.presentableName }) }
      val lessons = allItems.filter {
        it.type == ItemType.LESSON.id || it.type == ItemType.FRAMEWORK.id || it.type.isBlank()
      }.map { it.presentableName }
      val tasks = allItems.filter { 
        it.type == ItemType.LESSON.id || it.type == ItemType.FRAMEWORK.id || it.type.isBlank()
      }.associate { it.presentableName to it.taskList }

      return EduStat(sections, lessons, tasks)
    }
  }
}

data class Section(val title: String, val items: List<String>)

enum class TaskType(val id: String) {
  EDU("edu"),
  OUTPUT("output"),
  IDE("ide"),
  THEORY("theory"),
  CHOICE("choice")
}

enum class ItemType(val id: String) {
  LESSON("lesson"),
  FRAMEWORK("framework"),
  SECTION("section")
}
