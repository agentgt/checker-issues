package io.jstach.checker.causeloop;

/**
 * Reduced from real code (a test helper walking a {@link Throwable} cause chain to find
 * the root cause):
 *
 * <pre>{@code
 * Throwable cause = e;
 * while (cause.getCause() != null) {
 * 	cause = cause.getCause();
 * }
 * }</pre>
 *
 * <p>
 * {@link Throwable#getCause()} is modeled as returning a nullable {@code Throwable} by
 * both the Checker Framework's annotated JDK and NullAway's built-in library models.
 * {@code cause.getCause()} is called <strong>twice</strong> here: once in the
 * while-condition, and again - a syntactically identical but distinct call expression -
 * in the loop body to compute the value re-assigned into {@code cause}.
 *
 * <p>
 * Eclipse's null analysis flags the assignment {@code cause = cause.getCause();} as a
 * potential null pointer problem (and, transitively, the following iteration's
 * {@code cause.getCause()} as a potential null dereference on {@code cause} itself),
 * since it does not assume the second call returns the same (already proven non-null)
 * result as the first. Neither the Checker Framework's Nullness Checker
 * ({@code mvn clean install -Pcheckerframework}) nor NullAway
 * ({@code mvn clean install -Pnullaway}) report anything here - both appear to refine the
 * second call using the non-null fact already established by the first, and carry that
 * refinement through the assignment. That refinement is only sound if
 * {@link Throwable#getCause()} is guaranteed to return the same value across repeated
 * calls with nothing observable happening in between - true for {@code Throwable} in
 * practice, but not something either tool requires a purity annotation
 * ({@code @Pure}/{@code @Deterministic}/{@code @SideEffectFree}) to rely on here.
 */
public final class CauseLoop {

	private CauseLoop() {
	}

	/**
	 * Walks {@code e}'s cause chain and returns the root cause.
	 * @param e throwable to walk, itself non-null.
	 * @return the root cause (possibly {@code e} itself if it has no cause).
	 */
	public static Throwable rootCause(Throwable e) {
		Throwable cause = e;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		return cause;
	}

}
