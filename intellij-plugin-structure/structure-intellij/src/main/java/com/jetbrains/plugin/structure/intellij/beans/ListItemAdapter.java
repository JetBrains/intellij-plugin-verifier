/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.beans;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ListItemAdapter extends XmlAdapter<ListItemBean, String> {
  @Override
  public String unmarshal(ListItemBean item) {
    return item.value;
  }

  @Override
  public ListItemBean marshal(String item) {
    ListItemBean result = new ListItemBean();
    result.value = item;
    return result;
  }
}

