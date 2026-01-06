/*expected(PROBLEM)
Package 'non' is not found

Package 'non' is not found along with its class non.existing.NonExistingClass.
Probably the package 'non' belongs to a library or dependency that is not resolved by the checker.
It is also possible, however, that this package was actually removed from a dependency causing the detected problems. Access to unresolved classes at runtime may lead to **NoSuchClassError**.
The following classes of 'non' are not resolved:
  Class non.existing.NonExistingClass is referenced in
    mock.plugin.FieldTypeNotFound.myNonExistingClass : NonExistingClass
    mock.plugin.MethodProblems.brokenArg(NonExistingClass brokenArg) : void
    mock.plugin.ParentDoesntExist
    mock.plugin.arrays.ANewArrayInsn.foo(long l, double d, Object a) : void
    mock.plugin.field.FieldProblemsContainer.accessUnknownClass() : void
    ...and 7 other places...

*/