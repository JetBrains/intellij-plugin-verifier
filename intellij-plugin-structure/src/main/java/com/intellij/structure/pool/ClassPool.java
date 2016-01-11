package com.intellij.structure.pool;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * More specific bytecode accessor than {@link Resolver}.
 * It's intended to be implemented with .jar-file container
 * supporting request of all the available class-files.
 *
 * <p>
 * <i>Note</i> that here are no method which would return {@code List<ClassFile>}
 * because it may be potentially slow and memory-consuming operation:
 * many class files are not actually located in memory, but on the disk until required.
 * So a user has to access all the files manually by invoking {@link #findClass(String)}
 * for every class-name string returned from {@link #getAllClasses()}
 *
 * @author Dennis.Ushakov
 */
public interface ClassPool extends Resolver {

  /**
   * @return list of names of all containing classes. Names are present in binary form.
   */
  @NotNull
  Collection<String> getAllClasses();

  boolean isEmpty();
}
