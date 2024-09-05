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