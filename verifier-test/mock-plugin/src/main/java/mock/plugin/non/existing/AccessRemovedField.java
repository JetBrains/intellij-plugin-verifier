package mock.plugin.non.existing;

public class AccessRemovedField {
  public void foo() {
    InheritField inheritField = new InheritField();
    int removedField = inheritField.removedField;
    Object field = InheritField.FINAL_FIELD;
  }
}
