package com.intellij.structure.impl.resolvers;

import com.intellij.structure.impl.utils.AsmUtil;
import com.intellij.structure.resolvers.Resolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class CompileOutputResolver extends Resolver {

  private final String myPresentableName;
  private final Map<String, PackageDescriptor> myPackageMap = new HashMap<String, PackageDescriptor>();
  private final Set<String> myAllClasses = new HashSet<String>();

  public CompileOutputResolver(@NotNull File dir) throws IOException {
    myPresentableName = dir.getPath();

    List<DirDescriptor> dirs = new ArrayList<DirDescriptor>();

    File[] children = dir.listFiles();
    if (children != null) {
      for (File file : children) {
        if (file.isDirectory()) {
          dirs.add(new DirDescriptor(file));
        }
      }
    }

    for (File file : FileUtils.listFiles(dir, new String[]{"class"}, true)) {
      ClassNode node = AsmUtil.readClassFromFile(file);
      myAllClasses.add(node.name);
    }

    myPackageMap.put("", new PackageDescriptor("", dirs));
  }

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) {
    int dotIdx = className.lastIndexOf('/');
    String packageName = dotIdx == -1 ? "" : className.substring(0, dotIdx);
    String simpleName = className.substring(dotIdx + 1);

    PackageDescriptor packageDescriptor = getPackageDescriptor(packageName);

    String classFileName = simpleName + ".class";

    for (DirDescriptor descriptor : packageDescriptor.getDirectories()) {
      File classFile = descriptor.findChild(classFileName);
      if (classFile != null) {
        try {
          InputStream in = null;
          try {
            in = new BufferedInputStream(new FileInputStream(classFile));
            return AsmUtil.readClassNode(classFileName, in);

          } finally {
            IOUtils.closeQuietly(in);
          }
        } catch (IOException ignored) {
        }
      }
    }
    return null;

  }

  @NotNull
  private PackageDescriptor getPackageDescriptor(String packageName) {
    PackageDescriptor res = myPackageMap.get(packageName);
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
      myPackageMap.put(packageName, res);
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
  public Set<String> getAllClasses() {
    return Collections.unmodifiableSet(myAllClasses);
  }

  @Override
  public String toString() {
    return myPresentableName;
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
