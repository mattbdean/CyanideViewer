package net.dean.cyanideviewer;

import android.os.AsyncTask;
import android.support.v4.view.ViewPager;
import android.util.Log;

import net.dean.cyanideviewer.api.CyanideApi;
import net.dean.cyanideviewer.api.comic.Comic;
import net.dean.cyanideviewer.ui.ComicPagerAdapter;
import net.dean.cyanideviewer.ui.ComicStage;
import net.dean.cyanideviewer.ui.MainActivity;

/**
 * This class is used to set the current comic being displayed to the user to a specific comic
 */
public class SetComicTask extends AsyncTask<Long, Void, Integer> {
	private final boolean showStagePostExecute;
	private final MainActivity activity;
	private final HistoryManager historyManager;
	private final ComicPagerAdapter pagerAdapter;
	private final ViewPager viewPager;

	public SetComicTask(MainActivity activity) {
		this(activity, true);
	}

	public SetComicTask(MainActivity activity, boolean showStagePostExecute) {
		this.showStagePostExecute = showStagePostExecute;
		this.activity = activity;
		this.historyManager = activity.getHistoryManager();
		this.pagerAdapter = activity.getPagerAdapter();
		this.viewPager = activity.getViewPager();
	}

	@Override
	protected void onPreExecute() {
		// Show the loading screen
		activity.showLoading();

		activity.getViewPager().setAdapter(null);
	}

	@Override
	protected Integer doInBackground(Long... params) {
		Log.d(Constants.TAG, "SetComicTask invoked for #" + params[0]);


		long id = params[0];

		// Fix the ID range
		if (id > CyanideApi.instance().getNewestId()) {
			id = CyanideApi.instance().getNewestId();
		} else if (id < CyanideApi.instance().getFirstId()) {
			id = CyanideApi.instance().getFirstId();
		}

		Comic c = CyanideApi.instance().getComic(id);
		while (c == null) {
			c = CyanideApi.instance().getComic(++id);
		}

		historyManager.add(c.getId());

		int curComicPagerIndex = -1;
		Comic prevComic = CyanideApi.instance().getPrevious(id);
		Comic nextComic = CyanideApi.instance().getNext(id);

		// Reuse all the views to be more efficient
		if (pagerAdapter.getCount() != 0) {
			for (int i = 0; i < pagerAdapter.getCount(); i++) {
				ComicStage stage = pagerAdapter.getComicStage(i);
				if (stage.getComicIdToLoad() == id) {
					// ComicStage already exists, return it's id
					return i;
				}
			}
			// Not the first time this has been called
			int midway = pagerAdapter.getCount() / 2;
			if (id == CyanideApi.instance().getNewestId()) {
				// Make midway all the way to the right so that there are no views to the right of it
				midway = pagerAdapter.getCount() - 1;
			} else if (id == CyanideApi.instance().getFirstId()) {
				// Shift it all the way to the left
				midway = 0;
			}
			pagerAdapter.getComicStage(midway).setComic(id);
			curComicPagerIndex = midway;
			// From the midway to the beginning
			for (int i = midway - 1; i >= 0; i--) {
				pagerAdapter.getComicStage(i).setComic(prevComic.getId());
				prevComic = CyanideApi.instance().getPrevious(prevComic.getId());
			}

			// From the midway to the end
			for (int i = midway + 1; i < pagerAdapter.getCount(); i++) {
				pagerAdapter.getComicStage(i).setComic(nextComic.getId());
				nextComic = CyanideApi.instance().getNext(nextComic.getId());
			}

			// Load the comics and remove the loading screen
			pagerAdapter.getComicStage(curComicPagerIndex).loadComic(new Callback<Void>() {
				@Override
				public void onComplete(Void result) {
					activity.showStage();
				}
			});

			ComicStage before = pagerAdapter.getComicStage(curComicPagerIndex - 1);
			if (before != null)
				before.loadComic();
			ComicStage after = pagerAdapter.getComicStage(curComicPagerIndex + 1);
			if (after != null)
				after.loadComic();
		} else {
			// First time setup
			if (prevComic != null) {
				// Add the previous comic if it exists
				pagerAdapter.addView(prevComic);
			}

			// Current comic
			Comic curComic = CyanideApi.instance().getComic(id);
			if (curComic != null) { // Current might be a short
				curComicPagerIndex = pagerAdapter.addView(curComic);
				pagerAdapter.getComicStage(curComicPagerIndex).loadComic();
			}

			if (nextComic != null) {
				// Add the next comic if it exists
				pagerAdapter.addView(nextComic);
			}
		}

		return curComicPagerIndex;
	}

	@Override
	protected void onPostExecute(Integer currentItemIndex) {
		viewPager.setAdapter(pagerAdapter);
		viewPager.setCurrentItem(currentItemIndex);

		if (showStagePostExecute) {
			activity.showStage();
		}
	}
}
