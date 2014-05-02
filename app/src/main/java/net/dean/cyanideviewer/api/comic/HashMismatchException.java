package net.dean.cyanideviewer.api.comic;

/**
 * Used when two checksums do not match
 */
public class HashMismatchException extends Exception {
	public HashMismatchException(String expected, String given) {
		super(String.format("Checksum mismatch. Expected: \"%s\", given: \"%s\"", expected, given));
	}
}
