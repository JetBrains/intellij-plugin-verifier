package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.Plugin;
import com.intellij.structure.domain.PluginManager;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.utils.FileWrapper;
import com.intellij.structure.impl.utils.StringUtil;
import com.intellij.structure.impl.utils.xml.JDOMUtil;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * @author Sergey Patrikeev
 */
public class PluginManagerImpl extends PluginManager {

  private static final String PLUGIN_XML = "plugin.xml";
  private static final String META_INF = "META-INF/";

  @Nullable
  private static PluginImpl loadDescriptorFromJar(@NotNull FileWrapper jarFile, @NotNull String fileName) throws IncorrectPluginException {
    final FileWrapper descriptorPath;
    try {
      descriptorPath = FileWrapper.createRelative(jarFile, META_INF + fileName);
    } catch (IOException e) {
      throw new IncorrectPluginException("Unable to read " + META_INF + fileName);
    }

    if (!descriptorPath.exists()) {
      return null;
    }

    final String filePath = descriptorPath.getPresentablePath();

    URL descriptorUrl;
    try {
      descriptorUrl = descriptorPath.getUrl();
    } catch (MalformedURLException e) {
      throw new IncorrectPluginException("Unable to find " + filePath, e);
    }

    try {
      try {
        InputStream is = descriptorPath.getInputStream();

        Document document;
        try {
          document = JDOMUtil.loadDocument(is);
        } catch (JDOMException e) {
          throw new IncorrectPluginException("Unable to read file " + filePath, e);
        } catch (IOException e) {
          throw new IncorrectPluginException("Unable to read file " + filePath, e);
        }
        PluginImpl descriptor = new PluginImpl(jarFile.getOriginalIOFile());
        descriptor.readExternal(document, descriptorUrl);
        return descriptor;

      } catch (IOException e) {
        throw new IncorrectPluginException("Unable to read file " + filePath, e);
      }

    } finally {
      IOUtils.closeQuietly(jarFile);
    }
  }


  private static boolean isJarOrZip(@NotNull FileWrapper file) {
    if (file.isDirectory()) {
      return false;
    }
    final String name = file.getName();
    return StringUtil.endsWithIgnoreCase(name, ".jar") || StringUtil.endsWithIgnoreCase(name, ".zip");
  }


  @Nullable
  private PluginImpl loadDescriptor(@NotNull final FileWrapper file, @NotNull String fileName) throws IncorrectPluginException {
    PluginImpl descriptor = null;

    if (file.isDirectory()) {
      descriptor = loadDescriptorFromDir(file, fileName);
    } else if (file.exists() && StringUtil.endsWithIgnoreCase(file.getName(), ".jar")) {
      descriptor = loadDescriptorFromJar(file, fileName);
    } else if (file.exists() && StringUtil.endsWithIgnoreCase(file.getName(), ".zip")) {
      descriptor = loadDescriptorFromZip(file, fileName);
    }

    //TODO:
/*
    if (descriptor != null) {
      resolveOptionalDescriptors(fileName, descriptor, new Function<String, PluginImpl>() {
        @Override
        public PluginImpl apply(String optionalDescriptorName) {
          PluginImpl optionalDescriptor = loadDescriptor(file, optionalDescriptorName);
          if (optionalDescriptor == null && !isJarOrZip(file)) {
            for (URL url : getClassLoaderUrls()) {
              if ("file".equals(url.getProtocol())) {
                optionalDescriptor = loadDescriptor(new File(decodeUrl(url.getFile())), optionalDescriptorName);
                if (optionalDescriptor != null) {
                  break;
                }
              }
            }
          }
          return null;
        }
      });
    }
*/

    return descriptor;
  }

    /*private static void resolveOptionalDescriptors(@NotNull String fileName,
                                                 @NotNull PluginImpl descriptor,
                                                 @NotNull Function<String, PluginImpl> optionalDescriptorLoader) {
    Map<String, String> optionalConfigs = descriptor.getOptionalConfigs();
    if (optionalConfigs != null && !optionalConfigs.isEmpty()) {
      Map<String, PluginImpl> descriptors = new HashMap<String, PluginImpl>(optionalConfigs.size());
      for (Map.Entry<String, String> entry : optionalConfigs.entrySet()) {
        String optionalDescriptorName = entry.getValue();
        assert !StringUtil.equal(fileName, optionalDescriptorName) : "recursive dependency: " + fileName;

        PluginImpl optionalDescriptor = optionalDescriptorLoader.apply(optionalDescriptorName);

        if (optionalDescriptor == null) {
        } else {
          descriptors.put(entry.getKey(), optionalDescriptor);
        }
      }
      descriptor.setOptionalDescriptors(descriptors);
    }
  }*/


  @Nullable
  private PluginImpl loadDescriptorFromZip(@NotNull FileWrapper zipFile, @NotNull String fileName) throws IncorrectPluginException {
    List<FileWrapper> rootFiles = zipFile.listFiles();
    if (rootFiles.isEmpty()) {
      throw new IncorrectPluginException("Plugin root directory is empty");
    }
    for (FileWrapper f : rootFiles) {
      PluginImpl descriptor = loadDescriptor(f, fileName);
      if (descriptor != null) {
        return descriptor;
      }
    }
    return null;
  }


  @Nullable
  private PluginImpl loadDescriptorFromDir(@NotNull final FileWrapper dir, @NotNull String fileName) throws IncorrectPluginException {
    FileWrapper descriptorFile;
    try {
      descriptorFile = FileWrapper.createRelative(dir, META_INF + fileName);
    } catch (IOException e) {
      throw new IncorrectPluginException("Unable to find " + META_INF + fileName);
    }

    if (descriptorFile.exists()) {
      PluginImpl descriptor = new PluginImpl(dir.getOriginalIOFile());
      URL url;
      try {
        url = descriptorFile.getUrl();
      } catch (MalformedURLException e) {
        throw new IncorrectPluginException(String.format("Unable to read %s", META_INF + fileName), e);
      }
      descriptor.readExternal(url);
      return descriptor;
    }
    return loadDescriptorFromLibDir(dir, fileName);
  }

  private PluginImpl loadDescriptorFromLibDir(@NotNull final FileWrapper dir, @NotNull String fileName) {
    FileWrapper libDir;
    try {
      libDir = FileWrapper.createRelative(dir, "lib");
    } catch (IOException e) {
      throw new IncorrectPluginException("Unable to find `lib` directory", e);
    }

    if (!libDir.isDirectory()) {
      throw new IncorrectPluginException("Plugin `lib` directory is not found");
    }

    final List<FileWrapper> files = libDir.listFiles();
    if (files.isEmpty()) {
      throw new IncorrectPluginException("Plugin `lib` directory is empty");
    }
    //move plugin-jar to the beginning: Sample.jar goes first (if Sample is a plugin name)
    Collections.sort(files, new Comparator<FileWrapper>() {
      @Override
      public int compare(@NotNull FileWrapper o1, @NotNull FileWrapper o2) {
        if (o2.getName().startsWith(dir.getName())) return Integer.MAX_VALUE;
        if (o1.getName().startsWith(dir.getName())) return -Integer.MAX_VALUE;
        if (o2.getName().startsWith("resources")) return -Integer.MAX_VALUE;
        if (o1.getName().startsWith("resources")) return Integer.MAX_VALUE;
        return 0;
      }
    });

    PluginImpl descriptor = null;

    for (final FileWrapper f : files) {
      if (isJarOrZip(f)) {
        descriptor = loadDescriptorFromJar(f, fileName);
        if (descriptor != null) {
          descriptor.setPath(dir.getOriginalIOFile());
          break;
        }
      } else if (f.isDirectory()) {
        PluginImpl descriptor1 = loadDescriptorFromDir(f, fileName);
        if (descriptor1 != null) {
          if (descriptor != null) {
            throw new IncorrectPluginException("Two or more META-INF/plugin.xml's detected");
          }
          descriptor = descriptor1;
          descriptor.setPath(dir.getOriginalIOFile());
        }
      }
    }

    return descriptor;
  }

  @NotNull
  @Override
  public Plugin createPlugin(@NotNull File pluginFile) throws IOException, IncorrectPluginException {
    FileWrapper wrapper = createWrapper(pluginFile);
    PluginImpl descriptor = loadDescriptor(wrapper, PLUGIN_XML);
    if (descriptor == null) {
      throw new IncorrectPluginException("META-INF/plugin.xml is not found");
    }
    return descriptor;
  }

  @NotNull
  private FileWrapper createWrapper(@NotNull File pluginFile) {
    try {
      if (StringUtil.endsWithIgnoreCase(pluginFile.getName(), ".jar") || StringUtil.endsWithIgnoreCase(pluginFile.getName(), ".zip")) {
        return FileWrapper.createZipFileWrapper(pluginFile);
      } else {
        return FileWrapper.createIOFileWrapper(pluginFile);
      }
    } catch (IOException e) {
      throw new IncorrectPluginException("Unable to read .zip archive " + pluginFile, e);
    }
  }


}
