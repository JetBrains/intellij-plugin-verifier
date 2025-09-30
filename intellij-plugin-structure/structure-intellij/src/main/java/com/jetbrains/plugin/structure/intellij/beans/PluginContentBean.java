/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.beans;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

public class PluginContentBean {
  @XmlAttribute(name = "namespace") public String namespace;
  @XmlElement(name = "module") public List<PluginModuleBean> modules = new ArrayList<>();
}