# Issue 6671

To build

`mvn clean install -Pcheckerframework`


https://github.com/typetools/checker-framework/issues/6671

The following seems to happen if a method does the sneak throw technique enclosed in a sealed interface :

```java
		@SuppressWarnings("unchecked")
		private static <E extends Throwable> void sneakyThrow(final Throwable x) throws E {
			throw (E) x;
		}
```

```
[ERROR] COMPILATION ERROR :
[INFO] -------------------------------------------------------------
[ERROR] /Users/agent/projects/checker-issues/checker-issue-6671/src/main/java/io/jstach/checker/issue6671/Issue6671.java:[28,15] error: [type.arguments.not.inferred] Could not infer type arguments for PropertyFunction.sneakyThrow
  unsatisfiable constraint: @UnknownInitialization @Nullable RuntimeException</*Type args not initialized*/> <: @Initialized @NonNull Throwable
[INFO] 1 error
[INFO] ----------
```

If we add a witness to that line of code

```
				PropertyFunction.<RuntimeException>sneakyThrow(e);
```

The error goes away but not in my other projects. Also notice the witness is not needed if the enclosing class is not sealed.

In the other project we get a much worse error:

```
[ERROR] error: StructuralEqualityComparer: unexpected combination:  type: [DECLARED class org.checkerframework.framework.type.AnnotatedTypeMirror$AnnotatedDeclaredType] Object  supertype: [TYPEVAR class org.checkerframework.framework.type.AnnotatedTypeMirror$AnnotatedTypeVariable] R extends Object
```

# Issue 6689

cd to directory and run

``mvn clean install -Pcheckerframework`

# getCause() loop (no issue number yet)

`checker-issue-cause-loop` - reduced from real code that walks a `Throwable` cause
chain to find the root cause:

```java
Throwable cause = e;
while (cause.getCause() != null) {
	cause = cause.getCause();
}
```

`Throwable.getCause()` is modeled as `@Nullable` by both tools. `cause.getCause()` is
called twice per iteration: once in the while-condition, and again (a distinct call
expression) in the body, to compute the value re-assigned into `cause`.

Eclipse's null analysis flags the assignment as a potential null pointer problem - it
does not assume the second call returns the same already-proven-non-null result as the
first. Neither the Checker Framework's Nullness Checker, NullAway, nor EISOP (the
"reference implementation" fork, see below) flag anything here:

```
cd checker-issue-cause-loop
mvn clean install -Pcheckerframework                            # no warnings
mvn clean install -Pnullaway -Dcheckerframework.disable=true    # no warnings
mvn clean install -Peisop -Dcheckerframework.disable=true       # no warnings
```

All three tools do catch an obviously-bad dereference added to the same file as a
sanity check (see git history for that throwaway edit), so this isn't a case of the
tooling being misconfigured or silently skipped - they are specifically not flagging
the double-evaluated `getCause()` call.

Turns out this isn't really a heuristic gap for the Checker Framework (or EISOP, which
ships the identical annotated JDK): `checker-<version>.jar`'s
`annotated-jdk/src/java.base/share/classes/java/lang/Throwable.java` stub has

```java
@Pure
@Nullable
public synchronized Throwable getCause(@GuardSatisfied Throwable this);
```

`@Pure` (`org.checkerframework.dataflow.qual.Pure`) is an explicit promise that repeat
calls with nothing in between are safe to cache/reuse - so CF/EISOP's refinement here is
sound *conditioned on that stub annotation being correct*, which for `Throwable` it is.
NullAway doesn't have (or need) a purity annotation for this: it has its own
`com.uber.nullaway.dataflow.AccessPath`/`AccessPathNullnessPropagation` machinery that
treats zero-arg method calls as trackable pseudo-fields and refines them flow-sensitively
without any purity promise - a more aggressive, NullAway-specific heuristic than CF's.

JSpecify itself has no vocabulary for any of this (`@Nullable`/`@NonNull`/`@NullMarked`
only, no purity concept), so nothing here is guaranteed by the spec these tools jointly
target - it's each tool's own added machinery carrying the day. The portable/correct
rewrite that needs none of it:

```java
Throwable cause = e;
Throwable tmp = cause.getCause();
while (tmp != null) {
	cause = tmp;
	tmp = tmp.getCause();
}
```

which, as a bonus, also halves the number of `getCause()` calls - and since
`getCause()` is `synchronized` in the real JDK (not just the stub), that's one fewer
monitor acquisition per iteration too, not just a call the JIT might elide.