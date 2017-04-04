package mock.plugin.arrays;

import non.existing.NonExistingClass;

/**
 * @author Sergey Patrikeev
 */
public class ANewArrayInsn {
  public void foo(long l, double d, Object a) {
    foo(0, 0, new NonExistingClass[10]);
  }

}
