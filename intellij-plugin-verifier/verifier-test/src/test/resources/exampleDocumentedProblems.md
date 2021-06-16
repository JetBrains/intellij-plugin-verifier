---
title: Incompatible Changes in IntelliJ Platform and Plugins API
---

<!--
To document a new incompatible change you have to fill a row in a table so that
the first column is a problem pattern and the second column is a human-readable description.

The following problem patterns are supported:
<package name> package removed
<class name> class removed

<class name>.<method name> method removed
<class name>.<method name> method moved to the superclass
<class name>.<method name> method return type changed from <before> to <after>
<class name>.<method name> method parameter type changed from <before> to <after>
<class name>.<method name> method visibility changed from <before> to <after>

<class name>.<field name> field removed
<class name>.<field name> field moved to the superclass
<class name>.<field name> field type changed from <before> to <after>
<class name>.<field name> field visibility changed from <before> to <after>

<class name>.<method name> abstract method added
<class name>.<method name> marked abstract
<class name> class moved to package <package name>

where <class name> is a fully-qualified name of the class, e.g. com.intellij.openapi.actionSystem.AnAction$InnerClass.

NOTE: You are allowed to prettify the pattern using markdown-features:
 1) code quotes: `org.example.Foo.methodName`
 2) links [org.example.Foo](upsource:///platform/core-api/src/org/example/Foo)
 3) both code quotes and links: [`org.example.Foo`](upsource:///platform/core-api/src/org/example/Foo)
-->

<style>
  table {
    width:100%;
  }
  th, tr, td {
    width:50%;
  }
</style>

## Changes

`com.example.deletedPackage` package removed 
: Use classes from `org.apache.commons.imaging` instead
[`com.example.Faz.newAbstractMethod`](upsource:///platform/core-api/src/com/intellij/openapi/application/ApplicationListener.java) abstract method added 
: Implement this method or extend [`com.intellij.openapi.application.ApplicationAdapter`](upsource:////platform/core-api/src/com/intellij/openapi/application/ApplicationAdapter.java) class instead of implementing the interface
`com.example.Baz.REMOVED_FIELD` field removed 
: Use [`com.intellij.util.net.HttpConfigurable.getProxyLogin()`](upsource:///platform/platform-api/src/com/intellij/util/net/HttpConfigurable.java) instead
`com.example.Baz.MOVED_FIELD` field moved to the superclass
: Recompile the client code
`com.example.Inner.Class` class removed 
: Use other class instead
`com.example.SomeClass.abstractMethodWithParams(int, boolean)` abstract method added 
: Implement the method in implementations
`com.example.RemovedConstructorWithParams(OneParam, TwoParam)` constructor removed 
: Use something else
`com.example.MethodHolder#methodMarkedFinal(SomeParams)` method marked final 
: Override something else
`com.example.MethodHolder#abstractMethodSeparatedWithDash(SomeParams)` abstract method added 
: Implement it

## More changes

`com.example.Foo` class removed 
: Use [`com.intellij.util.net.HttpConfigurable.getPlainProxyPassword()`](upsource:///platform/platform-api/src/com/intellij/util/net/HttpConfigurable.java) instead
`com.example.SomeInterface` interface removed 
: reason
`com.example.SomeAnnotation` annotation removed 
: reason
`com.example.SomeEnum` enum removed 
: reason
`com.example.Bar.removedMethod` method removed 
: Use classes from `org.jetbrains.org.objectweb.asm` package instead
`com.example.Bar.movedMethod` method moved to the superclass
: Recompile the client code
`com.example.Baf` class moved to package `com.another` 
: Use the moved classes
`com.example.MI` interface moved to package `com.another` 
: Use the moved classes
`com.example.MA` annotation moved to package `com.another` 
: Use the moved classes
`com.example.ME` enum moved to package `com.another` 
: Use the moved classes

##Even more changes

`com.example.Foo$InnerClass.changedReturnType(int, String)` method return type changed from `One` to `Another` 
: It is a comment why
`com.example.Bar.changedParameterType(SomeClass)` method parameter type changed from `SomeClass` to `Object` 
: It is a comment why
`com.example.Bar.methodVisibilityChanged()` method visibility changed from `public` to `private` 
: It is a comment why
`com.example.Foo.fieldTypeChanged` field type changed from `String` to `int` 
: It is a comment why
`com.example.Foo.fieldVisibilityChanged` field visibility changed from `public` to `private` 
: It is a comment why
`com.example.Foo.foo` method parameter `int` removed 
: It is a comment why
`com.example.Baz` constructor removed 
: It is a comment why
`com.example.Baf` constructor parameter `ParamType` removed 
: It is a comment why
`com.example.Bam` constructor parameter `ParamType` type changed 
: It is a comment why
`com.example.Bam` class renamed to `com.example.NewBam` 
: It is a comment why
`com.example.Iface` interface renamed to `com.example.NewIface` 
: It is a comment why
`com.example.Annotation` annotation renamed to `com.example.NewAnnotation` 
: It is a comment why
`com.example.Enum` enum renamed to `com.example.NewEnum` 
: It is a comment why
`com.some.Class` class now extends `com.some.other.Class` and inherits its final method `methodName(int, String)`
: It is a comment why
`com.some.Class` class now implements `com.some.other.Interface` and inherits its abstract method `abstractMethod(int, String)`
: It is a comment why
`com.some.Class.someMarkedMethod` method `com.some.Parameter` parameter marked @`com.some.Annotation`
: It is a comment why
`com.some.Class` class type parameter T added
: It is a comment why
`com.some.Class` superclass changed from `com.some.old.Super` to `com.some.new.Super`
: It is a comment why
`com.some.Interface` superinterface changed from `com.some.old.SuperIface` to `com.some.new.NewSuperIface`
: It is a comment why
`some.property.name` property removed from resource bundle messages.Bundle
: It is a comment why
Java code migrated to use `TYPE_USE` nullability annotations
: It is a comment why
`com.some.Class.markedAbstractMethod` marked abstract 
: Implement the method in implementations