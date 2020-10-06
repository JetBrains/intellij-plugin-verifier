/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.beans;

import javax.xml.bind.annotation.XmlAttribute;

public class ProductDescriptorBean {
  @XmlAttribute(name = "code") public String code;
  @XmlAttribute(name = "release-date") public String releaseDate;
  @XmlAttribute(name = "release-version") public String releaseVersion;
  @XmlAttribute(name = "eap") public String eap;
}
