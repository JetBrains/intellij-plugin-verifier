package com.intellij.structure.impl.beans;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.jdom2.*;
import org.jdom2.transform.JDOMSource;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.intellij.structure.impl.utils.StringUtil.containsIgnoreCase;

public class PluginBeanExtractor {
  private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");
  private static final String[] INTERESTING_STRINGS = new String[]{"class", "interface", "implementation", "instance"};

  @NotNull
  public static PluginBean extractPluginBean(Document document) throws JAXBException {
    JAXBContext jc = JAXBContext.newInstance(PluginBean.class);
    Unmarshaller unmarshaller = jc.createUnmarshaller();

    Element rootElement = document.getRootElement();
    PluginBean bean = (PluginBean) unmarshaller.unmarshal(new JDOMSource(document));
    bean.extensions = extractExtensions(rootElement);
    bean.classes = extractReferencedClasses(rootElement);
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

  private static List<String> extractReferencedClasses(@NotNull Element rootElement) {
    List<String> referencedClasses = new ArrayList<String>();
    Iterator<Content> descendants = rootElement.getDescendants();
    while (descendants.hasNext()) {
      Content next = descendants.next();
      if (next instanceof Element) {
        Element element = (Element) next;

        if (isInterestingName(element.getName())) {
          referencedClasses.addAll(extractClasses(element.getTextNormalize()));
        }

        for (Attribute attribute : element.getAttributes()) {
          if (isInterestingName(attribute.getName())) {
            referencedClasses.addAll(extractClasses(attribute.getValue().trim()));
          }
        }
      } else if (next instanceof Text) {
        Parent parent = next.getParent();
        if (parent instanceof Element) {
          if (isInterestingName(((Element) parent).getName())) {
            referencedClasses.addAll(extractClasses(((Text) next).getTextTrim()));
          }
        }
      }
    }
    return referencedClasses;
  }

  private static boolean isInterestingName(@NotNull String label) {
    for (String string : INTERESTING_STRINGS) {
      if (containsIgnoreCase(label, string)) {
        return true;
      }
    }
    return false;
  }

  private static List<String> extractClasses(@NotNull String text) {
    List<String> result = new ArrayList<String>();
    Matcher matcher = JAVA_CLASS_PATTERN.matcher(text);
    while (matcher.find()) {
      result.add(matcher.group().replace('.', '/'));
    }
    return result;
  }
}
