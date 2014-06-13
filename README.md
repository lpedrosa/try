# Java 8 Try<T>

This is a Java 8 Try<T> implementation, based on the <a href="http://www.scala-lang.org/api/current/#scala.util.Try">scala.util.Try</a> implementation. Since Java does not support pattern matching, much like java.util.Optional, this class does not use the subclass case approach.

This means, unlike scala.util.Try, there are no Success or Failure classes that extend this class.

Try is a monad that represents the value of computations that might fail. In other words, it might contain the value of the computation, if this is successful, or it might contain the exception thrown by the computation.

The original Try class was created by twitter, in result of a desire to pass a computation result between thread, even if this was a failure. With the addition of java.util.CompletableFuture in Java SE 8, this class might not be as useful as it seems.

Still, much like Optional, the ability to express this behaviour using Java's type system is always helpful.
