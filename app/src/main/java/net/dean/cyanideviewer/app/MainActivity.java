package net.dean.cyanideviewer.app;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import net.dean.cyanideviewer.app.api.AbstractComicTask;
import net.dean.cyanideviewer.app.api.Comic;
import net.dean.cyanideviewer.app.api.CyanideApi;

import java.util.concurrent.ExecutionException;

/**
 * The main activity of Cyanide Viewer
 */
public class MainActivity extends FragmentActivity {

	/**
	 * The ViewPager used to scroll through comics
	 */
	private ViewPager viewPager;

	/**
	 * The ComicPagerAdapter used to provide pages to the ViewPager
	 */
	private ComicPagerAdapter pagerAdapter;

	/**
	 * The button that, when pressed, downloads the current comic
	 */
	private ImageButton downloadButton;

	/**
	 * The button that, when pressed, toggles whether the current comic is a favorite
	 */
	private ToggleButton favoriteButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.viewPager = (ViewPager) findViewById(R.id.comic_pager);
		this.pagerAdapter = new ComicPagerAdapter();
		this.favoriteButton = (ToggleButton) findViewById(R.id.action_favorite);
		this.downloadButton = (ImageButton) findViewById(R.id.download);

		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

			@Override
			public void onPageSelected(int position) {
				if (position == 0) { // At the very left
					new RetrievePreviousComicTask().execute(getCurrentComicId());
				} else if (position == pagerAdapter.getCount() - 1) {
					new RetrieveNextComicTask().execute(getCurrentComicId());
				}

				// It's been selected, start loading
				pagerAdapter.getComicStage(position).loadComic();

				// Refresh button states
				refreshDownloadButtonState();
				refreshFavoriteButtonState();
			}

			@Override
			public void onPageScrollStateChanged(int state) { }
		});

		setComic(CyanideApi.getNewestId());
		viewPager.setCurrentItem(pagerAdapter.getCount() - 1);

		// Refresh button states
		refreshDownloadButtonState();
		refreshFavoriteButtonState();
	}

	public void setComic(final long id) {
		try {
			viewPager.setAdapter(null);
			int currentItemIndex = new SetComicTask().execute(id).get();
			viewPager.setAdapter(pagerAdapter);
			viewPager.setCurrentItem(currentItemIndex);
		} catch (InterruptedException e) {
			Log.e(CyanideViewer.TAG, "Interrupted trying to set the new comic to #" + id, e);
		} catch (ExecutionException e) {
			Log.e(CyanideViewer.TAG, "An error occurred while trying to set the new comic to #" + id, e);
		}
	}

	public void refreshDownloadButtonState() {
		boolean hasLocal = CyanideApi.hasLocal(getCurrentComicId());
		downloadButton.setEnabled(!hasLocal);
	}

	private long getCurrentComicId() {
		return pagerAdapter.getComicStage(viewPager.getCurrentItem()).getComicIdToLoad();
	}

	/**
	 * Called when the 'Favorite' button is clicked
	 */
	public void onFavoriteClicked(View view) {
		Comic currentDbComic = getCurrentDbComic();
		Log.v(CyanideViewer.TAG, "Current is favorite: " + currentDbComic.isFavorite());

		// Flip it
		currentDbComic.setFavorite(!currentDbComic.isFavorite());
		CyanideViewer.getComicDao().updateComicAsFavorite(currentDbComic);

		refreshFavoriteButtonState();
	}

	private Comic getCurrentDbComic() {
		return CyanideViewer.getComicDao().getComic(getCurrentComicId());
	}

	private void refreshFavoriteButtonState() {
		favoriteButton.setChecked(getCurrentDbComic().isFavorite());
	}

	/**
	 * Called when the 'Download' button is clicked
	 */
	public void onDownloadClicked(View view) {
		// Download the comic at the current ID
		pagerAdapter.getComicStage(viewPager.getCurrentItem()).getComic().download(downloadButton);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		switch (id) {
			case R.id.action_settings:
				// Show settings
				return true;
			case R.id.menu_favorites:
				startActivityForResult(new Intent(this, FavoritesActivity.class), 0);
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == FavoritesActivity.RESULT_OK) {
			// The user chose a comic to view
			Comic theChosenOne = data.getExtras().getParcelable("comic");
			setComic(theChosenOne.getId());
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		// TODO: Go to last viewed
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Called when the 'Random' button is called. Shows a random comic to the user.
	 *
	 * @param v The view that contained the 'Random' button
	 */
	public void onRandomClicked(View v) {
		//new RetrieveRandomComicTask().execute();
	}

	public class RetrievePreviousComicTask extends AbstractComicTask<Long> {

		@Override
		protected Comic doInBackground(Long... params) {
			return CyanideApi.getPrevious(params[0]);
		}

		@Override
		protected void onPostExecute(Comic comic) {
			if (comic != null) pagerAdapter.addView(comic, 0);
		}
	}

	private class RetrieveNextComicTask extends AbstractComicTask<Long> {
		@Override
		protected Comic doInBackground(Long... params) {
			return CyanideApi.getNext(params[0]);
		}

		@Override
		protected void onPostExecute(Comic comic) {
			if (comic != null) pagerAdapter.addView(comic);
		}
	}

	private class SetComicTask extends AsyncTask<Long, Void, Integer> {

		@Override
		protected Integer doInBackground(Long... params) {
			long id = params[0];

			int curComicPagerIndex = -1;
			Comic prevComic = CyanideApi.getPrevious(id);
			Comic nextComic = CyanideApi.getNext(id);

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
				pagerAdapter.getComicStage(midway).setComic(id);
				curComicPagerIndex = midway;
				// From the midway to the beginning
				for (int i = midway - 1; i >= 0; i--) {
					pagerAdapter.getComicStage(i).setComic(prevComic.getId());
					prevComic = CyanideApi.getPrevious(prevComic.getId());
				}

				// From the midway to the end
				for (int i = midway + 1; i < pagerAdapter.getCount(); i++) {
					pagerAdapter.getComicStage(i).setComic(nextComic.getId());
					nextComic = CyanideApi.getNext(nextComic.getId());
				}
			} else {
				// First time setup
				if (prevComic != null) {
					// Add the previous comic if it exists
					pagerAdapter.addView(prevComic);
				}
				// Current comic
				curComicPagerIndex = pagerAdapter.addView(CyanideApi.getComic(id));
				pagerAdapter.getComicStage(curComicPagerIndex).loadComic();

				if (nextComic != null) {
					// Add the next comic if it exists
					pagerAdapter.addView(nextComic);
				}
			}

			return curComicPagerIndex;
		}
	}
}
