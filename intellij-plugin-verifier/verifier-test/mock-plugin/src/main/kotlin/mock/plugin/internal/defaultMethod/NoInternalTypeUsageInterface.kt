package mock.plugin.internal.defaultMethod

import internal.defaultMethod.InterfaceWithDefaultMethodUsingInternalAPI

// this interface when compiled with kotlin could have a nested DefaultImpls
// that may refer to internal types, this should not raise warnings.
interface NoInternalTypeUsageInterface : InterfaceWithDefaultMethodUsingInternalAPI {
  // Kotlin should generate a default implementation for the interface
  // default methods. E.g. a `NoInternalTypeUsageInterface$DefaultImpls` class
  // with the default implementations of the interface methods. E.g. :
  //
  //  // access flags 0x9
  //  public static topInternal(Lmock/plugin/internal/defaultMethod/NoInternalTypeUsageInterface;)Linternal/defaultMethod/AnInternalType;
  //  @Lorg/jetbrains/annotations/Nullable;() // invisible
  //    // annotable parameter count: 1 (invisible)
  //    @Lorg/jetbrains/annotations/NotNull;() // invisible, parameter 0
  //   L0
  //    LINENUMBER 5 L0
  //    ALOAD 0
  //    CHECKCAST internal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI
  //    INVOKESTATIC internal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI$DefaultImpls.topInternal (Linternal/defaultMethod/InterfaceWithDefaultMethodUsingInternalAPI;)Linternal/defaultMethod/AnInternalType;
  //   L1
  //    LINENUMBER 11 L1
  //    ARETURN
  //   L2
  //    LOCALVARIABLE $this Lmock/plugin/internal/defaultMethod/NoInternalTypeUsageInterface; L0 L2 0
  //    MAXSTACK = 1
  //    MAXLOCALS = 1
}