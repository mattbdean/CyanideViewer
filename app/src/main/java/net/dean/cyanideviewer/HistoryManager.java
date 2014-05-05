package net.dean.cyanideviewer;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the specific history of the user's comic browsing. Comic IDs should only
 * be added to the record if the ID is <i>specific</i>, (e.g. when ${@link net.dean.cyanideviewer.MainActivity#setComic(long)} is called
 * or a ${@link net.dean.cyanideviewer.MainActivity.SetComicTask} is executed.
 */
public class HistoryManager {
	/**
	 * A list of the comic IDs the user has. The newest element will be removed from this list
	 * once the 'Back' button is pressed.
	 */
	private List<Long> specificHistory;

	private MainActivity activity;

	/**
	 * Instantiates a new HistoryManager
	 *
	 * @param activity The MainActivity to use
	 */
	public HistoryManager(MainActivity activity) {
		this.specificHistory = new ArrayList<>();
		this.activity = activity;
	}

	/**
	 * Adds an ID to the history
	 * @param id The ID to use
	 */
	public void add(Long id) {
		if (specificHistory.size() > 0) {
			if ((specificHistory.get(specificHistory.size() - 1)).equals(id)) {
				return;
			}
		}
		specificHistory.add(id);
	}

	/**
	 * Pops the latest element in the history and sets the new latest element as the current comic.
	 * If there is only one element in the history, the default implementation of <code>onBackPressed()</code>
	 * will be called.
	 *
	 * @throws IndexOutOfBoundsException If the history is empty. ${@link #add(Long)}
	 *         needs to be called before this method.
	 */
	public void back() {
		if (specificHistory.isEmpty()) {
			Log.e(Constants.TAG, "History is empty", new IndexOutOfBoundsException());
		}
		if (specificHistory.size() == 1) {
			activity.onBackPressed();
			return;
		}

		int removeIndex = specificHistory.size() - 1;
		long id = specificHistory.get(removeIndex - 1);
		specificHistory.remove(removeIndex);

		activity.setComic(id);
	}
}
