/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin;

import kotlin.text.StringsKt;
import org.jdom2.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginXmlUtil {
  private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");
  private static final String[] CLASS_LIKE_STRINGS = new String[]{"class", "interface", "implementation", "instance"};

  private static Set<String> extractReferencedClasses(@NotNull Element rootElement) {
    Set<String> referencedClasses = new HashSet<>();
    Iterator<Content> descendants = rootElement.getDescendants();
    while (descendants.hasNext()) {
      Content next = descendants.next();
      if (next instanceof Element) {
        Element element = (Element) next;

        if (isClassLikeName(element.getName())) {
          referencedClasses.addAll(extractClasses(element.getTextNormalize()));
        }

        for (Attribute attribute : element.getAttributes()) {
          if (isClassLikeName(attribute.getName())) {
            referencedClasses.addAll(extractClasses(attribute.getValue().trim()));
          }
        }
      } else if (next instanceof Text) {
        Parent parent = next.getParent();
        if (parent instanceof Element) {
          if (isClassLikeName(((Element) parent).getName())) {
            referencedClasses.addAll(extractClasses(((Text) next).getTextTrim()));
          }
        }
      }
    }
    return referencedClasses;
  }

  private static boolean isClassLikeName(@NotNull String label) {
    for (String string : CLASS_LIKE_STRINGS) {
      if (StringsKt.contains(label, string, true)) {
        return true;
      }
    }
    return false;
  }

  private static List<String> extractClasses(@NotNull String text) {
    List<String> result = new ArrayList<>();
    Matcher matcher = JAVA_CLASS_PATTERN.matcher(text);
    while (matcher.find()) {
      result.add(matcher.group().replace('.', '/'));
    }
    return result;
  }

  @NotNull
  public static Set<String> getAllClassesReferencedFromXml(IdePlugin plugin) {
    Document document = plugin.getUnderlyingDocument();
    Element rootElement = document.getRootElement();
    if (rootElement != null) {
      return extractReferencedClasses(rootElement);
    }
    return Collections.emptySet();
  }
}