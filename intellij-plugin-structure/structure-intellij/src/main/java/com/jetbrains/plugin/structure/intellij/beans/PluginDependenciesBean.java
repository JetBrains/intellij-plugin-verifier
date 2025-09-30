/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.beans;

import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

public class PluginDependenciesBean {
  @XmlElement(name = "module") public List<ContentModuleDependencyBean> modules = new ArrayList<>();
  @XmlElement(name = "plugin") public List<PluginDependenciesPluginBean> plugins = new ArrayList<>();
}