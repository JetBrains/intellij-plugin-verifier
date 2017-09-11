package com.jetbrains.plugin.structure.intellij.plugin;

import com.jetbrains.plugin.structure.intellij.utils.URLUtil;
import com.jetbrains.plugin.structure.intellij.utils.xincludes.DefaultXIncludePathResolver;
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class PluginXmlXIncludePathResolver extends DefaultXIncludePathResolver {

  private final List<URL> myPluginMetaInfUrls;

  public PluginXmlXIncludePathResolver(List<URL> metaInfUrl) {
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
