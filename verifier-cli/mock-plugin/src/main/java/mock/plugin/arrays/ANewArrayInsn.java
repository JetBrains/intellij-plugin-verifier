package mock.plugin.arrays;

import non.existing.NonExistingClass;

/**
 * @author Sergey Patrikeev
 */
public class ANewArrayInsn {
  public static void foo2(long l, double d, Object a) {
    foo2(0, 0, new NonExistingClass[10]);
  }

  public void foo(long l, double d, Object a) {
    foo(0, 0, new NonExistingClass[10]);
  }

}
