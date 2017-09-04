package com.jetbrains.plugin.structure.impl.domain;

import com.jetbrains.plugin.structure.classes.utils.xml.JDOMXIncluder;
import com.jetbrains.plugin.structure.classes.utils.xml.URLUtil;
import com.jetbrains.plugin.structure.classes.utils.xml.XIncludeException;
import org.jdom2.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class PluginXmlExtractor {
  static final JDOMXIncluder.PathResolver DEFAULT_PLUGIN_XML_PATH_RESOLVER = new PluginXmlPathResolver();

  static Document resolveXIncludes(@NotNull Document document, @NotNull URL documentUrl, JDOMXIncluder.PathResolver pathResolver) throws XIncludeException {
    return JDOMXIncluder.resolve(document, documentUrl.toExternalForm(), false, pathResolver);
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
