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
first. Neither the Checker Framework's Nullness Checker nor NullAway flag anything here:

```
cd checker-issue-cause-loop
mvn clean install -Pcheckerframework     # no warnings
mvn clean install -Pnullaway -Dcheckerframework.disable=true   # no warnings
```

Both tools do catch an obviously-bad dereference added to the same file as a sanity
check (see git history for that throwaway edit), so this isn't a case of the tooling
being misconfigured or silently skipped - they are specifically not flagging the
double-evaluated `getCause()` call. Whether this refinement (treating a syntactically
repeated, unannotated method call as returning the same value with nothing proving it's
`@Pure`/`@Deterministic`/`@SideEffectFree`) is an intentional heuristic or an unsoundness
gap is the open question for both projects.