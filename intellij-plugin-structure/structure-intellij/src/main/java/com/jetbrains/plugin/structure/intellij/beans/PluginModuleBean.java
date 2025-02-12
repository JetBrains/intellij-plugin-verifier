/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.beans;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class PluginModuleBean {
  @XmlAttribute(name = "name") public String moduleName;
  @XmlAttribute(name = "loading") public String loadingRule;
  @XmlValue public String value;
}