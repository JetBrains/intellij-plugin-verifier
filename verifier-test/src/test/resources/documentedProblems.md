---
title: Incompatible Changes in IntelliJ Platform and Plugins API
---

<!--
Below is a meta-comment (starting with VERIFIER) used by the plugin verifier tool https://github.com/JetBrains/intellij-plugin-verifier/
to determine a set of problems that have already been documented here. Such problems won't be shown in
the verification reports (optionally, they can be seen in a dedicated log file).

Every non-empty line must be a Java RegExp pattern of the short problem's description.
You can safely copy the description from the corresponding check-configuration (e.g. Detect master API changes)
 
The examples of ignored problems are:
    Access to unresolved class com.example.deleted.package..*
    Illegal access of a private field com.example.Foo.deletedField : int
    Invocation of unresolved method com.example.Bar(Foo, int, String)
    Multiple default implementations of method com.interface.Interface.foo() : void 
    Illegal access to private class com.example.BecamePrivate
    Instantiation of an abstract class com.example.BecameAbstract
    Access to unresolved class com.example.DeletedClass
    Incompatible change of super class com.example.BecameInterface to interface
    Incompatible change of class com.example.BecameInterface to interface
    Incompatible change of super interface com.example.BecameClass to class
    Incompatible change of interface com.example.BecameClass to class
    Inheritance from a final class com.example.BecameFinal
    Instantiation of an interface com.example.BecameInterface
    Attempt to change a final field com.example.FieldHolder.becameFinalField : String
    Access to unresolved field com.example.Foo.deletedField : String
    Illegal access to a package-private field com.example.Foo.fieldAccessWeakened : List
    Illegal invocation of protected method com.example.Foo.becameProtected() : void
    Attempt to execute an *invokeinterface* instruction on a private method com.example.Foo.methodBecamePrivate(String) : int
    Invocation of unresolved method com.example.Foo.deletedField() : String
    Abstract method com.example.Interface.addedAbstractMethod(int) : double is not implemented
    Attempt to invoke an abstract method com.example.Foo.methodBecameAbstract(int) : void
    Overriding a final method com.example.Foo.methodBecameFinal() : void
    Attempt to execute a non-static access instruction *getfield* on a static field com.example.Foo.fieldBecameStatic : int
    Attempt to execute an *invokestatic* instruction on a non-static method com.example.Foo.methodBecameNonStatic() : void
    Attempt to execute a non-static instruction *invokevirtual* on a static method com.example.Foo.methodBecameStatic() : void
    Attempt to execute a static access instruction *putstatic* on a non-static field com.example.Foo.fieldBecameNonStatic : int
    
Note that you can use RegExp to simplify typing of the corresponding descriptor of a method, field or a class.
 So instead of 
    com.example.Class.foo(String, int, double) : List
 you are allowed to write
    com.example.Class.foo.*
 
 Make sure you exclude only the affected method. For instance, in the latter example it means that there 
 should be only one method with name 'foo' in the class 'Class' that the exclusion is talking about.
-->

<!--VERIFIER
Access to unresolved class org.apache.sanselan..*
Access to unresolved class org.jetbrains.asm4..* 
Abstract method com.intellij.openapi.application.ApplicationListener.afterWriteActionFinished.* is not implemented
Access to unresolved field com.intellij.util.net.HttpConfigurable.PROXY_LOGIN.*
Access to unresolved field com.intellij.util.net.HttpConfigurable.PROXY_PASSWORD_CRYPT.*
-->


## Changes in IntelliJ Platform 2017.3

|  Change | How to deal with it |
|---------|---------------------|
|org.apache.sanselan package removed | Use classes from org.apache.commons.imaging instead |

## Changes in IntelliJ Platform 2016.3

|  Change | How to deal with it |
|---------|---------------------|
| com.intellij.openapi.application.ApplicationListener#afterWriteActionFinished method added | Implement this method or extend com.intellij.openapi.application.ApplicationAdapter class instead of implementing the interface |


## Changes in IntelliJ Platform 2016.2 

|  Change | How to deal with it |
|---------|---------------------|
| com.intellij.util.net.HttpConfigurable#PROXY_LOGIN field removed | Use com.intellij.util.net.HttpConfigurable#getProxyLogin() instead |
| com.intellij.util.net.HttpConfigurable#PROXY_PASSWORD_CRYPT field removed | Use com.intellij.util.net.HttpConfigurable#getPlainProxyPassword() instead |
| org.jetbrains.asm4 package removed | Use classes from org.jetbrains.org.objectweb.asm package instead |

