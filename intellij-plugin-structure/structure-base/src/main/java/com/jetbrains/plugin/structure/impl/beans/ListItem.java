package com.jetbrains.plugin.structure.impl.beans;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;

class ListItemAdapter extends XmlAdapter<ListItemBean, String> {
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

class ListItemBean {
  @XmlAttribute(name = "value") String value;
}
