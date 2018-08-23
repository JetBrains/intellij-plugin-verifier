/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.plugin.structure.intellij.utils;

import kotlin.io.ByteStreamsKt;
import kotlin.io.ConstantsKt;
import org.jdom2.*;
import org.jdom2.filter.AbstractFilter;
import org.jdom2.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.InputSource;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/**
 * @author mike
 */
@SuppressWarnings("unchecked")
public class JDOMUtil {
  private static final EmptyTextFilter CONTENT_FILTER = new EmptyTextFilter();
  private static final char[] EMPTY_CHAR_ARRAY = new char[0];

  private JDOMUtil() {
  }

  private static boolean areElementsEqual(Element e1, Element e2) {
    if (e1 == null && e2 == null) return true;
    //noinspection SimplifiableIfStatement
    if (e1 == null || e2 == null) return false;

    return StringUtil.equal(e1.getName(), e2.getName())
        && attListsEqual(e1.getAttributes(), e2.getAttributes())
        && contentListsEqual(e1.getContent(CONTENT_FILTER), e2.getContent(CONTENT_FILTER));
  }

  private static boolean contentListsEqual(final List c1, final List c2) {
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

  @NotNull
  public static Document loadDocument(URL url) throws JDOMException, IOException {
    try (InputStream stream = URLUtil.openStream(url)) {
      return loadDocument(stream);
    }
  }

  @NotNull
  public static Document loadDocument(@NotNull InputStream stream) throws JDOMException, IOException {
    //to prevent closing the supplied stream from InputStreamReader.close()
    InputStream copied = copyInputStream(stream);
    try (InputStreamReader reader = new InputStreamReader(copied, Charset.forName("UTF-8"))) {
      SAXBuilder saxBuilder = new SAXBuilder();
      saxBuilder.setEntityResolver((publicId, systemId) -> new InputSource(new CharArrayReader(EMPTY_CHAR_ARRAY)));
      return saxBuilder.build(reader);
    }
  }

  @NotNull
  public static Document loadResourceDocument(URL url) throws JDOMException, IOException {
    try (InputStream stream = URLUtil.openResourceStream(url)) {
      return loadDocument(stream);
    }
  }

  public static boolean isEmpty(@NotNull Element element) {
    return element.getAttributes().isEmpty() && element.getContent().isEmpty();
  }

  @NotNull
  private static InputStream copyInputStream(@NotNull InputStream is) throws IOException {
    return new ByteArrayInputStream(ByteStreamsKt.readBytes(is, ConstantsKt.DEFAULT_BUFFER_SIZE));
  }

  private static class EmptyTextFilter extends AbstractFilter {
    @Override
    public Object filter(Object obj) {
      if (obj instanceof Text) {
        if (((Text) obj).getText().trim().isEmpty()) {
          return null;
        }
      }
      return obj;
    }
  }
}
