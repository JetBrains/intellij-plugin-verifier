package com.intellij.structure.impl.resolvers;

import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class CompileOutputResolver extends Resolver {

  private final String moniker;
  private final Map<String, PackageDescriptor> packageMap = new HashMap<String, PackageDescriptor>();
  private final Map<String, ClassNode> classesMap = new HashMap<String, ClassNode>();

  public CompileOutputResolver(@NotNull File dir) {
    moniker = dir.getPath();

    List<DirDescriptor> dirs = new ArrayList<DirDescriptor>();

    File[] children = dir.listFiles();
    if (children != null) {
      for (File file : children) {
        if (file.isDirectory()) {
          dirs.add(new DirDescriptor(file));
        }
      }
    }

    packageMap.put("", new PackageDescriptor("", dirs));
  }

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) {
    ClassNode res = classesMap.get(className);

    if (res == null) {
      if (classesMap.containsKey(className)) return null;

      int dotIdx = className.lastIndexOf('/');
      String packageName = dotIdx == -1 ? "" : className.substring(0, dotIdx);
      String simpleName = className.substring(dotIdx + 1);

      PackageDescriptor packageDescriptor = getPackageDescriptor(packageName);

      String classFileName = simpleName + ".class";

      for (DirDescriptor descriptor : packageDescriptor.getDirectories()) {
        File classFile = descriptor.findChild(classFileName);
        if (classFile != null) {
          try {
            InputStream in = new BufferedInputStream(new FileInputStream(classFile));
            try {
              ClassNode classNode = new ClassNode();
              new ClassReader(in).accept(classNode, 0);
              res = classNode;
              break;
            } finally {
              IOUtils.closeQuietly(in);
            }
          } catch (IOException ignored) {

          }
        }
      }

      classesMap.put(className, res);
    }

    return res;
  }

  @NotNull
  private PackageDescriptor getPackageDescriptor(String packageName) {
    PackageDescriptor res = packageMap.get(packageName);
    if (res == null) {
      int dotIdx = packageName.lastIndexOf('/');
      String parentPackageName = dotIdx == -1 ? "" : packageName.substring(0, dotIdx);
      String name = packageName.substring(dotIdx + 1);

      PackageDescriptor parentPackage = getPackageDescriptor(parentPackageName);

      List<DirDescriptor> dirs = new ArrayList<DirDescriptor>();

      for (DirDescriptor descriptor : parentPackage.getDirectories()) {
        File child = descriptor.findChild(name);
        if (child != null && child.isDirectory()) {
          dirs.add(new DirDescriptor(child));
        }
      }

      res = new PackageDescriptor(packageName, dirs);
      packageMap.put(packageName, res);
    }

    return res;
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
    if (findClass(className) != null) {
      return this;
    }

    return null;
  }

  @NotNull
  @Override
  public Collection<String> getAllClasses() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return moniker;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  private static class PackageDescriptor {
    private final String packageName;

    private List<DirDescriptor> dirs;

    private PackageDescriptor(String packageName, List<DirDescriptor> dirs) {
      this.packageName = packageName;
      this.dirs = dirs;
    }

    List<DirDescriptor> getDirectories() {
      return dirs;
    }

    public String getPackageName() {
      return packageName;
    }
  }

  private static class DirDescriptor {
    private final File dir;

    private Map<String, File> fileMap;

    DirDescriptor(File dir) {
      this.dir = dir;
    }

    File findChild(String name) {
      Map<String, File> map = fileMap;
      if (map == null) {
        map = new HashMap<String, File>();

        File[] children = dir.listFiles();
        if (children != null) {
          for (File file : children) {
            map.put(file.getName(), file);
          }
        }

        fileMap = map;
      }

      return map.get(name);
    }

    public File getDir() {
      return dir;
    }
  }
}
