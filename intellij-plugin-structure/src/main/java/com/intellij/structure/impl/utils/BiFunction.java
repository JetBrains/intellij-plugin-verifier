package com.intellij.structure.impl.utils;

/**
 * @author Sergey Patrikeev
 */
public interface BiFunction<T, U, R> {

  R apply(T t, U u);

}
