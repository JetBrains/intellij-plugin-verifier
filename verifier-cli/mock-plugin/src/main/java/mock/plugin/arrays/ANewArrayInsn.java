package mock.plugin.arrays;

import non.existing.NonExistingClass;

/**
 * @author Sergey Patrikeev
 */
public class ANewArrayInsn {
  public void foo(Object a) {
    foo(new NonExistingClass[10]);
  }
}
