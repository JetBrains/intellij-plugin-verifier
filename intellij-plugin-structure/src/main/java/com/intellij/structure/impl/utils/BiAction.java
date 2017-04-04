package com.intellij.structure.impl.utils;

/**
 * @author Sergey Patrikeev
 */
public interface BiAction<T, U> {

  void call(T t, U u);

}
