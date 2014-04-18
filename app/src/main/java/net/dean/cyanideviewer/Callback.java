package net.dean.cyanideviewer;

/**
 * Represents a callback, sort of like jQuery
 * @param <T> The return type of the callback
 */
public interface Callback<T> {
	/**
	 * Executes when the method is complete
	 * @param result The result of the method
	 */
	public void onComplete(T result);
}
