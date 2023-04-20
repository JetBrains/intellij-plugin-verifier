package mock.plugin.invokeDynamic;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.List;

public class NoErrorOnIndy {
  public static void main(String... args) throws Throwable {
    new NoErrorOnIndy().varHandle();
    new NoErrorOnIndy().methodHandle();
  }

  public void varHandle() {
    SomeClass someClass = new SomeClass();
    // LINENUMBER 22 L2
    // GETSTATIC mock/plugin/invokeDynamic/NoErrorOnIndy.someClass_list_VH : Ljava/lang/invoke/VarHandle;
    // ALOAD 1
    // INVOKEVIRTUAL java/lang/invoke/VarHandle.get (Lmock/plugin/invokeDynamic/SomeClass;)Ljava/util/List;
    // ASTORE 2
    var list = (List<String>) someClass_list_VH.get(someClass);

    List<String> newList = List.of("1", "2", "3");

    // LINENUMBER 33 L3
    // GETSTATIC mock/plugin/invokeDynamic/NoErrorOnIndy.someClass_list_VH : Ljava/lang/invoke/VarHandle;
    // ALOAD 1
    // ALOAD 2
    // ALOAD 3
    // INVOKEVIRTUAL java/lang/invoke/VarHandle.compareAndSet (Lmock/plugin/invokeDynamic/SomeClass;Ljava/util/List;Ljava/util/List;)Z
    // ISTORE 4
    boolean wasSet = someClass_list_VH.compareAndSet(someClass, list, newList);

    System.out.println(wasSet);
    System.out.println(list);
  }

  public void methodHandle() throws Throwable {
    SomeClass someClass = new SomeClass();
    // LINENUMBER 46 L0
    // GETSTATIC mock/plugin/invokeDynamic/NoErrorOnIndy.someClass_getList_MH : Ljava/lang/invoke/MethodHandle;
    // ALOAD 1
    // INVOKEVIRTUAL java/lang/invoke/MethodHandle.invokeExact (Lmock/plugin/invokeDynamic/SomeClass;)Ljava/util/List;
    // ASTORE 2
    var list = (List<String>) someClass_getList_MH.invokeExact(someClass);

    System.out.println(list);
  }


  private static final MethodHandle someClass_getList_MH;
  static {
    try {
      someClass_getList_MH = MethodHandles.privateLookupIn(
          SomeClass.class,
          MethodHandles.lookup()
        ).findVirtual(
          SomeClass.class,
          "getList",
          MethodType.methodType(List.class)
        );
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static final VarHandle someClass_list_VH;
  static {
    try {
      someClass_list_VH = MethodHandles.privateLookupIn(
          SomeClass.class,
          MethodHandles.lookup()
        ).findVarHandle(
          SomeClass.class,
          "list",
          List.class
        );
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
