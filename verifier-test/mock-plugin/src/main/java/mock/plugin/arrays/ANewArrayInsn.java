package mock.plugin.arrays;

import non.existing.NonExistingClass;

public class ANewArrayInsn {
  public void foo(long l, double d, Object a) {
    foo(0, 0, new NonExistingClass[10]);
  }

}
