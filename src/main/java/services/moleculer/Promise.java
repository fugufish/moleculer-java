package services.moleculer;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.datatree.Tree;

/**
 * ES6-like Promise based on the Java8's CompletableFuture API. A Promise is an
 * object that may produce a single value some time in the future: either a
 * resolved value, or a reason that it's not resolved (e.g., a network error
 * occurred). Promise users can attach callbacks to handle the fulfilled value
 * or the reason for rejection.
 */
public class Promise {

	// --- INTERNAL COMPLETABLE FUTURE ---

	/**
	 * An internal CompletableFuture, which does the working logic of this
	 * Promise.
	 */
	protected final CompletableFuture<Tree> future;

	// --- PARENT PROMISE ---

	/**
	 * Previous Promise in the invocation chain.
	 */
	protected Promise parent;

	// --- STATIC CONSTRUCTORS ---

	/**
	 * Returns a Promise object that is resolved with {@code null} value.
	 * 
	 * @return new RESOLVED/COMPLETED Promise
	 */
	public static final Promise resolve() {
		return new Promise((Tree) null, (Throwable) null);
	}

	/**
	 * Returns a Promise object that is resolved with the given value.
	 * 
	 * @param value
	 *            value of the new Promise
	 * 
	 * @return new RESOLVED/COMPLETED Promise
	 */
	public static final Promise resolve(Tree value) {
		if (value == null) {
			value = new Tree().setObject(null);
		}
		return new Promise(value, null);
	}

	/**
	 * Returns a Promise object that is rejected with the given reason.
	 * 
	 * @param error
	 *            error state of the new Promise
	 * 
	 * @return new REJECTED/COMPLETED Promise
	 */
	public static final Promise reject(Throwable error) {
		return new Promise(null, error);
	}

	// --- PUBLIC CONSTRUCTOR ---

	/**
	 * Creates an empty PENDING/UNCOMPLETED Promise.
	 */
	public Promise() {
		this((Tree) null, (Throwable) null);
	}

	/**
	 * Creates a Promise with an asynchronous initializer. Sample code:<br>
	 * <br>
	 * <b>return new Promise((r) -> {</b><br>
	 * Tree value = new Tree();<br>
	 * value.put("a.b.c", 3);<br>
	 * r.resolve(value);<br>
	 * <b>});</b>
	 */
	public Promise(Initializer initializer) {
		this((Tree) null, (Throwable) null);
		initializer.init(new Resolver(future));
	}

	@FunctionalInterface
	public interface Initializer {

		void init(Resolver resolver);

	}

	public static final class Resolver {

		private final CompletableFuture<Tree> future;

		private Resolver(CompletableFuture<Tree> future) {
			this.future = future;
		}

		public final void resolve(Tree value) {
			future.complete(value);
		}

		public final void reject(Throwable error) {
			future.completeExceptionally(error);
		}

	}

	// --- PROTECTED CONSTRUCTORS ---

	protected Promise(Tree value, Throwable error) {
		if (error != null) {
			future = new CompletableFuture<>();
			future.completeExceptionally(error);
		} else if (value == null) {
			future = new CompletableFuture<>();
		} else {
			future = CompletableFuture.completedFuture(value);
		}
	}

	protected Promise(CompletableFuture<Tree> future, Promise parent) {
		this.future = future;
		this.parent = parent;
	}

	// --- WATERFALL FUNCTION ---

	/**
	 * Promises can be used to unnest asynchronous functions and allows one to
	 * chain multiple functions together - increasing readability and making
	 * individual functions, within the chain, more reusable. Sample code:<br>
	 * <br>
	 * <b>Promise.resolve().then((value) -> {</b><br>
	 * // ...do something...<br>
	 * return value;<br>
	 * <b>}).then((value) -> {</b><br>
	 * // ...do something...<br>
	 * return value;<br>
	 * <b>}).Catch((error) -> {</b><br>
	 * // ...error handling...<br>
	 * return value;<br>
	 * <b>});</b>
	 * 
	 * @param action
	 *            next action in the invocation chain
	 * 
	 * @return output Promise
	 */
	public Promise then(Function<Tree, Tree> action) {
		return new Promise(future.thenApply(action), this);
	}

	// --- ERROR HANDLER METHODS ---

	/**
	 * The Catch() method returns a Promise and deals with rejected cases only.
	 * 
	 * @param action
	 *            error handler of the previous "next" handlers
	 * 
	 * @return output Promise
	 */
	public Promise Catch(Function<Throwable, Tree> action) {
		return new Promise(catchAllPreviousThen(action), null);
	}

	protected CompletableFuture<Tree> catchAllPreviousThen(Function<Throwable, Tree> action) {
		CompletableFuture<Tree> f = future.exceptionally((error) -> {
			return action.apply(error);
		});
		if (parent != null) {
			parent.catchAllPreviousThen(action);
		}
		return f;
	}

	// --- COMPLETE UNRESOLVED / UNCOMPLETED PROMISE ---

	/**
	 * If not already completed, sets the value to the given value. Sample code:<br>
	 * <br>
	 * Promise p = new Promise();<br>
	 * // Listener:<br>
	 * p.next((value) -> {<br>
	 * System.out.println("Received: " + value);<br>
	 * return value;<br>
	 * });<br>
	 * // Invoke chain:<br>
	 * Tree t = new Tree().put("a", "b");<br>
	 * p.complete(t);
	 * 
	 * @param value
	 *            the result value
	 * 
	 * @return {@code true} if this invocation caused this Promise to transition
	 *         to a completed state, else {@code false}
	 */
	public boolean complete(Tree value) {
		return future.complete(value);
	}

	/**
	 * If not already completed, sets the exception state to the given
	 * exception. Sample code:<br>
	 * <br>
	 * Promise p = new Promise();<br>
	 * // Listener:<br>
	 * p.Catch((error) -> {<br>
	 * System.out.println("Received: " + error);<br>
	 * return null;<br>
	 * });<br>
	 * // Invoke chain:<br>
	 * p.complete(new Exception("Foo!"));
	 *
	 * @param error
	 *            the exception
	 * 
	 * @return {@code true} if this invocation caused this Promise to transition
	 *         to a completed state, else {@code false}
	 */
	public boolean complete(Throwable error) {
		return future.completeExceptionally(error);
	}

	// --- STATUS ---

	/**
	 * Returns {@code true} if this Promise completed exceptionally, in any way.
	 *
	 * @return {@code true} if this Promise completed exceptionally
	 */
	public boolean isRejected() {
		return future.isCompletedExceptionally();
	}

	/**
	 * Returns {@code true} if this Promise completed normally, in any way.
	 *
	 * @return {@code true} if this Promise completed normally
	 */
	public boolean isResolved() {
		return future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled();
	}

	/**
	 * Returns {@code true} if this Promise completed in any fashion: normally,
	 * exceptionally, or via cancellation.
	 *
	 * @return {@code true} if completed
	 */
	public boolean isDone() {
		return future.isDone();
	}

	// --- GET THE INTERNAL COMPLETABLE FUTURE ---

	/**
	 * Returns the internal CompletableFuture.
	 * 
	 * @return internal CompletableFuture
	 */
	public CompletableFuture<Tree> toFuture() {
		return future;
	}

	// --- PARALLEL ALL / ALLOF FUNCTION ---

	/**
	 * Returns a new Promise that is completed when all of the given Promise
	 * complete. If any of the given Promise complete exceptionally, then the
	 * returned Promise also does so, with a Promise holding this exception as
	 * its cause.
	 * 
	 * @param promises
	 *            array of Promises
	 * 
	 * @return a new Promise that is completed when all of the given Promises
	 *         complete
	 */
	public static Promise all(Promise... promises) {

		@SuppressWarnings("unchecked")
		CompletableFuture<Tree>[] futures = new CompletableFuture[promises.length];
		for (int i = 0; i < promises.length; i++) {
			futures[i] = promises[i].future;
		}
		CompletableFuture<Void> all = CompletableFuture.allOf(futures);
		return new Promise((r) -> {
			all.whenComplete((Void, error) -> {
				try {
					if (error != null) {
						r.reject(error);
						return;
					}
					Tree array = new Tree().putList("array");
					for (int i = 0; i < futures.length; i++) {
						array.addObject(futures[i].get());
					}
					r.resolve(array);
				} catch (Throwable cause) {
					r.reject(cause);
				}
			});
		});
	}

	// --- PARALLEL RACE / ANYOF FUNCTION ---

	/**
	 * Returns a new Promise that is completed when any of the given Promises
	 * complete, with the same result. Otherwise, if it completed exceptionally,
	 * the returned Promise also does so, with a CompletionException holding
	 * this exception as its cause.
	 * 
	 * @param promises
	 *            array of Promises
	 * 
	 * @return a new Promise that is completed with the result or exception of
	 *         any of the given Promises when one completes
	 */
	public static Promise race(Promise... promises) {

		@SuppressWarnings("unchecked")
		CompletableFuture<Tree>[] futures = new CompletableFuture[promises.length];
		for (int i = 0; i < promises.length; i++) {
			futures[i] = promises[i].future;
		}
		CompletableFuture<Object> any = CompletableFuture.anyOf(futures);
		return new Promise((r) -> {
			any.whenComplete((object, error) -> {
				try {
					if (error != null) {
						r.reject(error);
						return;
					}
					r.resolve((Tree) object);
				} catch (Throwable cause) {
					r.reject(cause);
				}
			});
		});
	}

}