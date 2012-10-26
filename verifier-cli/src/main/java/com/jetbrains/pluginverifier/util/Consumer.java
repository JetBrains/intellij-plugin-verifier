package com.jetbrains.pluginverifier.util;

/**
 * @author Sergey Evdokimov
 */
public interface Consumer<T> {

  void consume(T element);

}
