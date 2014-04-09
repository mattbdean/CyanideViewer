package net.dean.cyanideviewer;

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
import android.widget.Toast;
import android.widget.ToggleButton;

import net.dean.cyanideviewer.api.Comic;
import net.dean.cyanideviewer.api.CyanideApi;

import java.util.ArrayList;
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
			public void onPageSelected(final int position) {

				if (position == 0) {
					// At the very left
					new AbstractComicTask<Long>() {
						@Override
						protected Comic doInBackground(Long... params) {
							return CyanideApi.instance().getPrevious(params[0]); }

						@Override
						protected void onPostExecute(Comic comic) {
							if (comic != null) {
								int index = pagerAdapter.addView(comic, 0);
								pagerAdapter.getComicStage(index).loadComic();
							}
						}
					}.execute(getCurrentComicId());

				} else if (position == pagerAdapter.getCount() - 1) {
					// At the very right
					new AbstractComicTask<Long>() {
						@Override
						protected Comic doInBackground(Long... params) { return CyanideApi.instance().getNext(params[0]); }

						@Override
						protected void onPostExecute(Comic comic) {
							if (comic != null) {
								int index = pagerAdapter.addView(comic);
								pagerAdapter.getComicStage(index).loadComic();
							}

						}
					}.execute(getCurrentComicId());
				}

				// Refresh button states
				refreshDownloadButtonState();
				refreshFavoriteButtonState();
			}

			@Override
			public void onPageScrollStateChanged(int state) { }
		});

		setComic(CyanideApi.instance().getNewestId());
		viewPager.setCurrentItem(pagerAdapter.getCount() - 1);
		// Load second newest comic
		pagerAdapter.getComicStage(viewPager.getCurrentItem() - 1).loadComic();

		// Refresh button states
		refreshDownloadButtonState();
		refreshFavoriteButtonState();
	}

	/**
	 * Shows a comic with the given ID to the user
	 * @param id The ID to use
	 */
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

	/**
	 * Refreshes the state of the download button based on if the comic exists on the file system
	 */
	public void refreshDownloadButtonState() {
		boolean hasLocal = CyanideApi.instance().hasLocalComic(getCurrentComicId());
		downloadButton.setEnabled(!hasLocal);
	}

	/**
	 * Refreshes the state of the favorite button based on whether the database thinks the current
	 * comic is a favorite or not
	 */
	private void refreshFavoriteButtonState() {
		favoriteButton.setChecked(getCurrentDbComic().isFavorite());
	}

	/**
	 * Gets the ID of the comic currently being shown to the user
	 * @return
	 */
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

		pagerAdapter.getComicStage(viewPager.getCurrentItem()).getComic().downloadIcon();

		refreshFavoriteButtonState();
	}

	/**
	 * Gets what the database knows about the current comic being shown to the user
	 * @return A comic from the database
	 */
	private Comic getCurrentDbComic() {
		return CyanideViewer.getComicDao().getComic(getCurrentComicId());
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
				startActivity(new Intent(this, SettingsActivity.class));
				return true;
			case R.id.menu_favorites:
				ArrayList<Comic> comics = CyanideViewer.getComicDao().getFavoriteComics();
				if (comics.isEmpty()) {
					// Show a Toast instead of showing an empty activity
					Toast.makeText(getApplicationContext(), "You don\'t have any favorites yet!",
							Toast.LENGTH_SHORT).show();
				} else {
					Intent intent = new Intent(this, FavoritesActivity.class);
					intent.putExtra("comics", comics);
					startActivityForResult(intent, 0);
				}

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
		new AbstractComicTask<Void>() {

			@Override
			protected Comic doInBackground(Void... params) {
				return CyanideApi.instance().getRandom();
			}

			@Override
			protected void onPostExecute(Comic comic) {
				setComic(comic.getId());
			}
		}.execute();
	}

	/**
	 * This class is used to set the current comic being displayed to the user to a specific comic
	 */
	private class SetComicTask extends AsyncTask<Long, Void, Integer> {

		@Override
		protected Integer doInBackground(Long... params) {
			long id = params[0];

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
			} else {
				// First time setup
				if (prevComic != null) {
					// Add the previous comic if it exists
					pagerAdapter.addView(prevComic);
				}
				// Current comic
				curComicPagerIndex = pagerAdapter.addView(CyanideApi.instance().getComic(id));
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
