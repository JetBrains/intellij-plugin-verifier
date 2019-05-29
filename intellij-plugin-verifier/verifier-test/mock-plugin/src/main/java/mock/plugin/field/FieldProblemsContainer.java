package mock.plugin.field;

import fields.FieldsContainer;
import fields.otherPackage.OtherFieldsContainer;
import non.existing.NonExistingClass;

public class FieldProblemsContainer {

  /*expected(PROBLEM)
  Access to unresolved field fields.FieldsContainer.deletedField : int

  Method mock.plugin.field.FieldProblemsContainer.accessDeletedField() : void contains a *getfield* instruction referencing an unresolved field fields.FieldsContainer.deletedField : int. This can lead to **NoSuchFieldError** exception at runtime.
  */
  public void accessDeletedField() {
    int deletedField = new FieldsContainer().deletedField;
  }

  /*expected(PROBLEM)
  Illegal access to a private field fields.FieldsContainer.privateField : int

  Method mock.plugin.field.FieldProblemsContainer.accessPrivateField() : void contains a *getfield* instruction referencing a private field fields.FieldsContainer.privateField : int inaccessible to a class mock.plugin.field.FieldProblemsContainer. This can lead to **IllegalAccessError** exception at runtime.
   */
  public void accessPrivateField() {
    int privateField = new FieldsContainer().privateField;
  }

  /*expected(PROBLEM)
  Illegal access to a protected field fields.otherPackage.OtherFieldsContainer.protectedField : int

  Method mock.plugin.field.FieldProblemsContainer.accessProtectedField() : void contains a *getfield* instruction referencing a protected field fields.otherPackage.OtherFieldsContainer.protectedField : int inaccessible to a class mock.plugin.field.FieldProblemsContainer. This can lead to **IllegalAccessError** exception at runtime.
  */
  public void accessProtectedField() {
    int x = new OtherFieldsContainer().protectedField;
  }

  /*expected(PROBLEM)
  Illegal access to a package-private field fields.otherPackage.OtherFieldsContainer.packageField : int

  Method mock.plugin.field.FieldProblemsContainer.accessPackageField() : void contains a *getfield* instruction referencing a package-private field fields.otherPackage.OtherFieldsContainer.packageField : int inaccessible to a class mock.plugin.field.FieldProblemsContainer. This can lead to **IllegalAccessError** exception at runtime.
   */
  public void accessPackageField() {
    int x = new OtherFieldsContainer().packageField;
  }

  /*expected(PROBLEM)
  Attempt to execute instance access instruction *getfield* on static field fields.FieldsContainer.staticField : int

  Method mock.plugin.field.FieldProblemsContainer.instanceAccessOnStatic() : void has instance field access instruction *getfield* referencing static field fields.FieldsContainer.staticField : int, what might have been caused by incompatible change of the field to static. This can lead to **IncompatibleClassChangeError** exception at runtime.
  */
  public void instanceAccessOnStatic() {
    int instanceField = new FieldsContainer().staticField;
  }

  /*expected(PROBLEM)
  Attempt to execute static access instruction *getstatic* on instance field fields.FieldsContainer.instanceField : int

  Method mock.plugin.field.FieldProblemsContainer.staticAccessOnInstance() : void has static field access instruction *getstatic* referencing an instance field fields.FieldsContainer.instanceField : int, what might have been caused by incompatible change of the field from static to instance. This can lead to **IncompatibleClassChangeError** exception at runtime.
   */
  public void staticAccessOnInstance() {
    int instanceField = FieldsContainer.instanceField;
  }

  public void accessUnknownClass() {
    int nonExistingField = NonExistingClass.nonExistingField;
  }

  public void accessUnknownClassOfArray() {
    int size = new NonExistingClass[2].length;
  }

  /*expected(PROBLEM)
  Attempt to change a final field fields.FieldsContainer.finalField : int

  Method mock.plugin.field.FieldProblemsContainer.setOnFinalFieldFromNotInitMethod() : void has modifying instruction *putfield* referencing a final field fields.FieldsContainer.finalField : int. This can lead to **IllegalAccessError** exception at runtime.
 */
  public void setOnFinalFieldFromNotInitMethod() {
    new FieldsContainer().finalField = 10;
  }

  /*expected(PROBLEM)
  Attempt to change a final field fields.FieldsContainer.staticFinalField : int

  Method mock.plugin.field.FieldProblemsContainer.setOnStaticFinalFieldFromNotClinitMethod() : void has modifying instruction *putstatic* referencing a final field fields.FieldsContainer.staticFinalField : int. This can lead to **IllegalAccessError** exception at runtime.
  */
  public void setOnStaticFinalFieldFromNotClinitMethod() {
    FieldsContainer.staticFinalField = 10;
  }
}
