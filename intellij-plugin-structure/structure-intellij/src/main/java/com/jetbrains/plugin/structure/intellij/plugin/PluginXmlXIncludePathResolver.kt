package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.slf4j.LoggerFactory
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class PluginXmlXIncludePathResolver extends DefaultXIncludePathResolver {

  private static final Logger logger = LoggerFactory.getLogger(PluginXmlXIncludePathResolver.class)

      private final List < URL > myPluginMetaInfUrls

      public PluginXmlXIncludePathResolver (@NotNull List<File> files) {
    myPluginMetaInfUrls = getUrls(files)
  }

      @ NotNull
      private List < URL > getUrls (@NotNull List<File> files) {
    List<URL> inLibJarUrls = new ArrayList<URL>()
    for (File file : files) {
    if (FileUtil.INSTANCE.isJar(file) || FileUtil.INSTANCE.isZip(file)) {
      try {
        String metaInfUrl = URLUtil . getJarEntryURL (file, "META-INF").toExternalForm()
        inLibJarUrls.add(new URL (metaInfUrl))
      } catch (Exception e) {
        logger.warn("Unable to create URL for " + file + " META-INF root", e)
      }
    }
  }
    return inLibJarUrls
  }

  @NotNull
  private URL defaultResolve(@NotNull String relativePath, @Nullable String base) {
    if (base != null && relativePath.startsWith("/META-INF/")) {
      //for plugin descriptor the root is a directory containing the META-INF
      try {
        return new URL (new URL (base), ".."+relativePath)
      } catch (MalformedURLException e) {
        throw new XIncludeException (e)
      }
    }
    return super.resolvePath(relativePath, base)
  }

  @NotNull
  private URL getMetaInfRelativeUrl(@NotNull URL metaInf, @NotNull String relativePath) throws MalformedURLException {
    if (relativePath.startsWith("/")) {
      return new URL (metaInf, ".."+relativePath)
    } else {
      return new URL (metaInf, relativePath)
    }
  }

  @NotNull
  @Override
  public URL resolvePath(@NotNull String relativePath, @Nullable String base) {
    URL url = defaultResolve (relativePath, base)
    if (!URLUtil.resourceExists(url)) {
      for (URL metaInf : myPluginMetaInfUrls) {
        try {
          URL entryUrl = getMetaInfRelativeUrl (metaInf, relativePath)
          if (URLUtil.resourceExists(entryUrl)) {
            return entryUrl
          }
        } catch (MalformedURLException ignored) {
        }
      }
    }
    return url
  }
}
