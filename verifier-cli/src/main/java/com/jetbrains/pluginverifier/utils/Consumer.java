package com.jetbrains.pluginverifier.utils;

/**
 * @author Sergey Evdokimov
 */
public interface Consumer<T> {

  void consume(T element);

}
