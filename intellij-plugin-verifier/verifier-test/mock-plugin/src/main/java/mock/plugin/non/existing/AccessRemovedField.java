package mock.plugin.non.existing;

public class AccessRemovedField {
  /*expected(PROBLEM)
  Access to unresolved field mock.plugin.non.existing.InheritField.removedField : int

  Method mock.plugin.non.existing.AccessRemovedField.foo() : void contains a *getfield* instruction referencing an unresolved field mock.plugin.non.existing.InheritField.removedField : int. This can lead to **NoSuchFieldError** exception at runtime. The field might have been declared in the super class (invokevirtual.Parent)
   */

  /*expected(PROBLEM)
  Access to unresolved field mock.plugin.non.existing.InheritField.FINAL_FIELD : Object

  Method mock.plugin.non.existing.AccessRemovedField.foo() : void contains a *getstatic* instruction referencing an unresolved field mock.plugin.non.existing.InheritField.FINAL_FIELD : java.lang.Object. This can lead to **NoSuchFieldError** exception at runtime. The field might have been declared in the super class (invokevirtual.Parent) or in the super interfaces (interfaces.SomeInterface, interfaces.SomeInterface2)
   */
  public void foo() {
    InheritField inheritField = new InheritField();
    int removedField = inheritField.removedField;
    Object field = InheritField.FINAL_FIELD;
  }
}
