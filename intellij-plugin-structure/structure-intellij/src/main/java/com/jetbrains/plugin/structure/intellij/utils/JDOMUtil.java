/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.utils;

import com.jetbrains.plugin.structure.xml.XMLParserConfiguration;
import org.apache.commons.io.IOUtils;
import org.jdom2.*;
import org.jdom2.filter.AbstractFilter;
import org.jdom2.filter.Filter;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.InputSource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class JDOMUtil {
  private static final Filter<Content> CONTENT_FILTER = new EmptyTextFilter();
  private static final char[] EMPTY_CHAR_ARRAY = new char[0];

  private JDOMUtil() {
  }

  private static SAXBuilder createDefaultSaxBuilder() {
    SAXBuilder saxBuilder = new SAXBuilder();
    saxBuilder.setEntityResolver((publicId, systemId) -> new InputSource(new CharArrayReader(EMPTY_CHAR_ARRAY)));
    saxBuilder.setFeature(XMLParserConfiguration.FEATURE_DISALLOW_DOCTYPE_DECL,true);
    saxBuilder.setExpandEntities(false);
    saxBuilder.setFeature(XMLParserConfiguration.FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
    saxBuilder.setFeature(XMLParserConfiguration.FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
    saxBuilder.setFeature(XMLParserConfiguration.FEATURE_LOAD_EXTERNAL_DTD, false);

    return saxBuilder;
  }

  private static boolean areElementsEqual(Element e1, Element e2) {
    if (e1 == null && e2 == null) return true;
    //noinspection SimplifiableIfStatement
    if (e1 == null || e2 == null) return false;

    List<Content> e1Content = e1.getContent(CONTENT_FILTER);
    List<Content> e2Content = e2.getContent(CONTENT_FILTER);
    return Objects.equals(e1.getName(), e2.getName())
        && attListsEqual(e1.getAttributes(), e2.getAttributes())
        && contentListsEqual(e1Content, e2Content);
  }

  private static <T> boolean contentListsEqual(final List<T> c1, final List<T> c2) {
    if (c1 == null && c2 == null) return true;
    if (c1 == null || c2 == null) return false;

    Iterator l1 = c1.listIterator();
    Iterator l2 = c2.listIterator();
    while (l1.hasNext() && l2.hasNext()) {
      if (!contentsEqual((Content) l1.next(), (Content) l2.next())) {
        return false;
      }
    }

    return l1.hasNext() == l2.hasNext();
  }

  private static boolean contentsEqual(Content c1, Content c2) {
    if (!(c1 instanceof Element) && !(c2 instanceof Element)) {
      return c1.getValue().equals(c2.getValue());
    }

    return c1 instanceof Element && c2 instanceof Element && areElementsEqual((Element) c1, (Element) c2);

  }

  private static boolean attListsEqual(@NotNull List a1, @NotNull List a2) {
    if (a1.size() != a2.size()) return false;
    for (int i = 0; i < a1.size(); i++) {
      if (!attEqual((Attribute) a1.get(i), (Attribute) a2.get(i))) return false;
    }
    return true;
  }

  private static boolean attEqual(@NotNull Attribute a1, @NotNull Attribute a2) {
    return a1.getName().equals(a2.getName()) && a1.getValue().equals(a2.getValue());
  }

  /**
   * Read JDOM XML documents from a reader. It is assumed that the reader is UTF-8 encoded.
   * @param reader UTF-8 encoded reader
   * @return a JDOM document read from the reader
   */
  @NotNull
  public static Document loadDocument(@NotNull Reader reader) throws JDOMException, IOException {
    SAXBuilder saxBuilder = createDefaultSaxBuilder();
    return saxBuilder.build(reader);
  }

  @NotNull
  public static Document loadDocument(@NotNull InputStream stream) throws JDOMException, IOException {
    //to prevent closing the supplied stream from InputStreamReader.close()
    InputStream copied = copyInputStream(stream);
    try (InputStreamReader reader = new InputStreamReader(copied, StandardCharsets.UTF_8)) {
      SAXBuilder saxBuilder = createDefaultSaxBuilder();
      return saxBuilder.build(reader);
    }
  }

  public static boolean isEmpty(@NotNull Element element) {
    return element.getAttributes().isEmpty() && element.getContent().isEmpty();
  }

  @NotNull
  private static InputStream copyInputStream(@NotNull InputStream is) throws IOException {
    return new ByteArrayInputStream(IOUtils.toByteArray(is));
  }

  private static class EmptyTextFilter extends AbstractFilter<Content> {
    @Override
    public Content filter(Object obj) {
      if (obj instanceof Text) {
        if (((Text) obj).getText().trim().isEmpty()) {
          return null;
        }
      }
      if (obj instanceof Content) {
        return (Content) obj;
      }
      return null;
    }
  }
}
