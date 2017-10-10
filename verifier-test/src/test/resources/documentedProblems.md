---
title: Incompatible Changes in IntelliJ Platform and Plugins API
---

## Changes

|  Change | How to deal with it |
|---------|---------------------|
|`com.example.deletedPackage` package removed | Use classes from `org.apache.commons.imaging` instead |
| [`com.example.Faz.newAbstractMethod`](upsource:///platform/core-api/src/com/intellij/openapi/application/ApplicationListener.java) abstract method added | Implement this method or extend [`com.intellij.openapi.application.ApplicationAdapter`](upsource:////platform/core-api/src/com/intellij/openapi/application/ApplicationAdapter.java) class instead of implementing the interface |
| `com.example.Baz.REMOVED_FIELD` field removed | Use [`com.intellij.util.net.HttpConfigurable.getProxyLogin()`](upsource:///platform/platform-api/src/com/intellij/util/net/HttpConfigurable.java) instead |

## More changes
|  Change | How to deal with it |
|---------|---------------------|
| `com.example.Foo` class removed | Use [`com.intellij.util.net.HttpConfigurable.getPlainProxyPassword()`](upsource:///platform/platform-api/src/com/intellij/util/net/HttpConfigurable.java) instead |
| `com.example.Bar.removedMethod` method removed | Use classes from `org.jetbrains.org.objectweb.asm` package instead |
| `com.example.Baf` class moved to package `com.another` | Use the moved classes |