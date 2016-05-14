package com.intellij.structure.impl.resolvers;

import com.intellij.structure.impl.utils.AsmUtil;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Sergey Patrikeev
 */
public class FilesResolver extends Resolver {

  private final Map<String, File> myClass2File = new HashMap<String, File>();
  private final String myPresentableName;

  public FilesResolver(@NotNull String presentableName, @NotNull Collection<File> classFiles) throws IOException {
    myPresentableName = presentableName;
    for (File classFile : classFiles) {
      if (classFile.getName().endsWith(".class")) {
        //TODO: rewrite without reading classes
        ClassNode node = AsmUtil.readClassFromFile(classFile);
        myClass2File.put(node.name, classFile);
      }
    }
  }

  @Nullable
  @Override
  public ClassNode findClass(@NotNull String className) throws IOException {
    if (!myClass2File.containsKey(className)) {
      return null;
    }
    return AsmUtil.readClassFromFile(myClass2File.get(className));
  }

  @Nullable
  @Override
  public Resolver getClassLocation(@NotNull String className) {
    return myClass2File.containsKey(className) ? this : null;
  }

  @NotNull
  @Override
  public Set<String> getAllClasses() {
    return Collections.unmodifiableSet(myClass2File.keySet());
  }

  @Override
  public boolean isEmpty() {
    return myClass2File.isEmpty();
  }

  @Override
  public String toString() {
    return myPresentableName;
  }


}
