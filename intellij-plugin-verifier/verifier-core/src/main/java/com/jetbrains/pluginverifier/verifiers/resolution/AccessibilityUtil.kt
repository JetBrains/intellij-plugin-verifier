package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.results.reference.SymbolicReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.YesNoUnsure
import com.jetbrains.pluginverifier.verifiers.isSubclassOf
import com.jetbrains.pluginverifier.verifiers.isSubclassOrSelf

//TODO: if class is private, determine its "nest" and perform "nest" test:
// https://docs.oracle.com/javase/specs/jvms/se12/html/jvms-5.html#jvms-5.4.4
fun isClassAccessibleToOtherClass(me: ClassFile, other: ClassFile): Boolean =
    me.isPublic
        || me.isPrivate && me.name == other.name
        || me.javaPackageName == other.javaPackageName
        || isKotlinDefaultConstructorMarker(me)

/**
 * In Kotlin classes the default constructor has a special parameter of type `DefaultConstructorMarker`.
 * This class is package-private but is never instantiated because `null` is always passed as its value.
 * We should not report "illegal access" for this class.
 */
private fun isKotlinDefaultConstructorMarker(classFile: ClassFile): Boolean =
    classFile.name == "kotlin/jvm/internal/DefaultConstructorMarker"

/**
 * A field or method R is accessible to a class or interface D if and only if any of the following is true:
 * - R is public.
 * - R is private and is declared in D.
 * - R is either protected or has default access (that is, neither public nor protected nor private),
 * and is declared by a class in the same run-time package as D.
 * - R is protected and is declared in a class C, and D is either a subclass of C or C itself.
 * Furthermore, if R is not static, then the symbolic reference to R must contain a symbolic reference
 * to a class T, such that T is either a subclass of D, a superclass of D, or D itself.
 * During verification of D, it was required that, even if T is a superclass of D, the target reference
 * of a protected field access or method invocation must be an instance of D or a subclass of D.
 */
fun detectAccessProblem(
    member: ClassFileMember,
    memberReference: SymbolicReference,
    accessor: ClassFileMember,
    context: VerificationContext
): AccessType? {
  when {
    member.isPrivate ->
      if (accessor.containingClassFile.name != member.containingClassFile.name) {
        return AccessType.PRIVATE
      }
    member.isProtected ->
      if (accessor.containingClassFile.packageName != member.containingClassFile.packageName) {
        if (YesNoUnsure.NO == context.classResolver.isSubclassOf(accessor.containingClassFile, member.containingClassFile.name)) {
          return AccessType.PROTECTED
        }
        if (member is Method && !member.isStatic && memberReference is MethodReference) {
          if (YesNoUnsure.NO == context.classResolver.isSubclassOrSelf(memberReference.hostClass.className, accessor.containingClassFile.name)) {
            return AccessType.PROTECTED
          }
        }
        if (member is Field && !member.isStatic && memberReference is FieldReference) {
          if (YesNoUnsure.NO == context.classResolver.isSubclassOrSelf(memberReference.hostClass.className, accessor.containingClassFile.name)) {
            return AccessType.PROTECTED
          }
        }
      }
    member.isPackagePrivate ->
      if (accessor.containingClassFile.packageName != member.containingClassFile.packageName) {
        return AccessType.PACKAGE_PRIVATE
      }
  }
  return null
}