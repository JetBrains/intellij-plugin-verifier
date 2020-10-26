package mock.plugin.kotlinDefault

import kotlinDefault.KotlinDefault

class KotlinDefaultUser {
  fun problem() {
/*expected(PROBLEM)
Invocation of unresolved method kotlinDefault.KotlinDefault.foo$default(KotlinDefault, int, int, Object) : void

Method mock.plugin.kotlinDefault.KotlinDefaultUser.problem() : void contains an *invokestatic* instruction referencing an unresolved method kotlinDefault.KotlinDefault.foo$default(kotlinDefault.KotlinDefault, int, int, java.lang.Object) : void. This can lead to **NoSuchMethodError** exception at runtime.
*/
    KotlinDefault().foo()

/*expected(PROBLEM)
Invocation of unresolved method kotlinDefault.KotlinDefault.foo(int) : void

Method mock.plugin.kotlinDefault.KotlinDefaultUser.problem() : void contains an *invokevirtual* instruction referencing an unresolved method kotlinDefault.KotlinDefault.foo(int) : void. This can lead to **NoSuchMethodError** exception at runtime.
*/
    KotlinDefault().foo(42)

/*expected(PROBLEM)
Invocation of unresolved method kotlinDefault.KotlinDefault.bar$default(KotlinDefault, int, int, Object) : void

Method mock.plugin.kotlinDefault.KotlinDefaultUser.problem() : void contains an *invokestatic* instruction referencing an unresolved method kotlinDefault.KotlinDefault.bar$default(kotlinDefault.KotlinDefault, int, int, java.lang.Object) : void. This can lead to **NoSuchMethodError** exception at runtime.
*/
    KotlinDefault().bar()

/*expected(PROBLEM)
Invocation of unresolved method kotlinDefault.KotlinDefault.bar(int) : void

Method mock.plugin.kotlinDefault.KotlinDefaultUser.problem() : void contains an *invokevirtual* instruction referencing an unresolved method kotlinDefault.KotlinDefault.bar(int) : void. This can lead to **NoSuchMethodError** exception at runtime.
*/
    KotlinDefault().bar(42)
  }
}