package com.intellij.structure.impl.beans;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;

class ListItemAdapter extends XmlAdapter<ListItemBean, String> {
  @Override
  public String unmarshal(ListItemBean item) throws Exception {
    return item.value;
  }

  @Override
  public ListItemBean marshal(String item) throws Exception {
    ListItemBean result = new ListItemBean();
    result.value = item;
    return result;
  }
}

class ListItemBean {
  @XmlAttribute(name = "value") String value;
}
