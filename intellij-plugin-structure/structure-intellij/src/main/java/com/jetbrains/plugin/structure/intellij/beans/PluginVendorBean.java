/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.beans;


import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

public class PluginVendorBean {
  @XmlAttribute(name = "url") public String url = "";
  @XmlAttribute(name = "email") public String email = "";
  @XmlAttribute(name = "logo") public String logo;
  @XmlValue public String name;
}

