package com.jetbrains.plugin.structure.intellij.beans;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class PluginBeanExtractor {

  @NotNull
  public static PluginBean extractPluginBean(Document document) throws JAXBException {
    JAXBContext jc = JAXBContext.newInstance(PluginBean.class);
    Unmarshaller unmarshaller = jc.createUnmarshaller();

    Element rootElement = document.getRootElement();
    PluginBean bean = (PluginBean) unmarshaller.unmarshal(new JDOMSource(document));
    bean.extensions = extractExtensions(rootElement);
    return bean;
  }

  private static Multimap<String, Element> extractExtensions(Element rootElement) {
    Multimap<String, Element> extensions = ArrayListMultimap.create();
    for (Element extensionsRoot : rootElement.getChildren("extensions")) {
      for (Element element : extensionsRoot.getChildren()) {
        extensions.put(extractEPName(element), element);
      }
    }
    return extensions;
  }

  private static String extractEPName(final Element extensionElement) {
    String epName = extensionElement.getAttributeValue("point");

    if (epName == null) {
      final Element parentElement = extensionElement.getParentElement();
      final String ns = parentElement != null ? parentElement.getAttributeValue("defaultExtensionNs") : null;

      if (ns != null) {
        epName = ns + '.' + extensionElement.getName();
      } else {
        Namespace namespace = extensionElement.getNamespace();
        epName = namespace.getURI() + '.' + extensionElement.getName();
      }
    }
    return epName;
  }

}
