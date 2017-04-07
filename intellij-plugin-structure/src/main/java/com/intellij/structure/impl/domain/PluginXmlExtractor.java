package com.intellij.structure.impl.domain;

import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import com.intellij.structure.impl.utils.xml.URLUtil;
import com.intellij.structure.impl.utils.xml.XIncludeException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class PluginXmlExtractor {
  static final JDOMXIncluder.PathResolver DEFAULT_PLUGIN_XML_PATH_RESOLVER = new PluginXmlPathResolver();
  private static final Logger LOG = LoggerFactory.getLogger(PluginXmlExtractor.class);

  static Document readExternalFromIdeSources(@NotNull URL url, @NotNull JDOMXIncluder.PathResolver pathResolver) throws JDOMException, IOException {
    Document document = JDOMUtil.loadDocument(url);
    try {
      document = JDOMXIncluder.resolve(document, url.toExternalForm(), false, pathResolver);
    } catch (Exception e) {
      LOG.error("Unable to resolve xinclude elements", e);
    }
    return document;
  }

  static Document readExternal(@NotNull Document document, @NotNull URL documentUrl) {
    return readExternal(document, documentUrl, DEFAULT_PLUGIN_XML_PATH_RESOLVER);
  }

  static Document readExternal(@NotNull Document document, @NotNull URL documentUrl, JDOMXIncluder.PathResolver pathResolver) {
    try {
      return JDOMXIncluder.resolve(document, documentUrl.toExternalForm(), false, pathResolver);
    } catch (XIncludeException e) {
      throw new IncorrectPluginException("Unable to resolve xml include elements of " + documentUrl.getFile(), e);
    }
  }

  static class PluginXmlPathResolver extends JDOMXIncluder.DefaultPathResolver {

    private final List<URL> myPluginMetaInfUrls;

    private PluginXmlPathResolver() {
      myPluginMetaInfUrls = Collections.emptyList();
    }

    PluginXmlPathResolver(List<URL> metaInfUrl) {
      myPluginMetaInfUrls = new ArrayList<URL>(metaInfUrl);
    }

    @NotNull
    private URL defaultResolve(@NotNull String relativePath, @Nullable String base) {
      if (base != null && relativePath.startsWith("/META-INF/")) {
        //for plugin descriptor the root is a directory containing the META-INF
        try {
          return new URL(new URL(base), ".." + relativePath);
        } catch (MalformedURLException e) {
          throw new XIncludeException(e);
        }
      }
      return super.resolvePath(relativePath, base);
    }

    @NotNull
    private URL getMetaInfRelativeUrl(@NotNull URL metaInf, @NotNull String relativePath) throws MalformedURLException {
      if (relativePath.startsWith("/")) {
        return new URL(metaInf, ".." + relativePath);
      } else {
        return new URL(metaInf, relativePath);
      }
    }

    @NotNull
    @Override
    public URL resolvePath(@NotNull String relativePath, @Nullable String base) {
      URL url = defaultResolve(relativePath, base);
      if (!URLUtil.resourceExists(url)) {
        for (URL metaInf : myPluginMetaInfUrls) {
          try {
            URL entryUrl = getMetaInfRelativeUrl(metaInf, relativePath);
            if (URLUtil.resourceExists(entryUrl)) {
              return entryUrl;
            }
          } catch (MalformedURLException ignored) {
          }
        }
      }
      return url;
    }
  }
}
