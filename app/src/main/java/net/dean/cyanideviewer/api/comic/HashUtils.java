package net.dean.cyanideviewer.api.comic;

import android.util.Log;

import net.dean.cyanideviewer.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class used for generating and checking hashes of files. Based off of
 * <a href="http://stackoverflow.com/a/304275/1275092">this</a> StackOverflow question.
 */
public final class HashUtils {
	/**
	 * The algorithm to use.
	 */
	private static final String ALGO = "MD5";


	/**
	 * Generates a hash of a given file.
	 *
	 * @param f The file to create a hash for
	 * @return A String representation of the file's hash, or null, if the algorithm "${@value #ALGO}
	 * could not be found.
	 * @throws FileNotFoundException If the file does not exist
	 */
	public static String getChecksum(File f) throws FileNotFoundException {
		FileInputStream inputStream = new FileInputStream(f);
		byte[] buffer = new byte[1024];
		MessageDigest complete;
		try {
			complete = MessageDigest.getInstance(ALGO);
		} catch (NoSuchAlgorithmException e) {
			Log.e(Constants.TAG_API, "Could not find algorithm " + ALGO);
			return null;
		}

		int numRead;

		try {
			do {
				numRead = inputStream.read(buffer);
				if (numRead > 0) {
					complete.update(buffer, 0, numRead);
				}
			} while (numRead != -1);

		} catch (IOException e) {
			Log.e(Constants.TAG_API, "Could not create a digest for InputStream " + inputStream);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				Log.e(Constants.TAG_API, "Unable to close FileInputStream", e);
			}
		}

		byte[] digestBytes = complete.digest();

		StringBuilder result = new StringBuilder();
		for (byte b : digestBytes) {
			result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
		}

		return result.toString();
	}

	/**
	 * Compares a hash of a given file to another hash.
	 *
	 * @param f The file to use to create the second hash
	 * @param expected The expected hash
	 * @throws FileNotFoundException If the file could not be found
	 * @throws HashMismatchException If the two hashes were different.
	 */
	public static void check(File f, String expected) throws FileNotFoundException, HashMismatchException {
		String hash = getChecksum(f);
		if (!hash.equals(expected)) {
			throw new HashMismatchException(expected, hash);
		}
	}

}
