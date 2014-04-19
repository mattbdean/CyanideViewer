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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import net.dean.cyanideviewer.api.CyanideApi;
import net.dean.cyanideviewer.api.comic.Comic;

import java.util.ArrayList;

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

	private LinearLayout loadingView;

	private Animation fadeIn;
	private Animation fadeOut;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.viewPager = (ViewPager) findViewById(R.id.comic_pager);
		this.pagerAdapter = new ComicPagerAdapter();
		this.favoriteButton = (ToggleButton) findViewById(R.id.action_favorite);
		this.downloadButton = (ImageButton) findViewById(R.id.download);
		this.loadingView = (LinearLayout) findViewById(R.id.loading_view);
		this.fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		fadeIn.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				if (loadingView.getVisibility() != View.VISIBLE) {
					loadingView.setVisibility(View.VISIBLE);
				}
			}

			@Override public void onAnimationEnd(Animation animation) { }
			@Override public void onAnimationRepeat(Animation animation) { }
		});
		this.fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
		fadeOut.setAnimationListener(new Animation.AnimationListener() {
			@Override public void onAnimationStart(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) {
				loadingView.setVisibility(View.INVISIBLE);
			}
			@Override public void onAnimationRepeat(Animation animation) {}
		});

		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(final int position) {
				if (position == 0) {
					// At the very left
					new AbstractComicTask<Long>() {
						@Override
						protected Comic doInBackground(Long... params) {
							return CyanideApi.instance().getPrevious(params[0]);
						}

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

				refreshButtonStates();
			}

			@Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
			@Override public void onPageScrollStateChanged(int state) {}

		});

		new SetComicTask() {
			@Override
			protected void onPostExecute(Integer currentItemIndex) {
				super.onPostExecute(currentItemIndex);
				// Load second newest comic
				ComicStage before = pagerAdapter.getComicStage(currentItemIndex - 1);
				if (before != null)
					before.loadComic();

				ComicStage after = pagerAdapter.getComicStage(currentItemIndex + 1);
				if (after != null)
					after.loadComic();

				refreshButtonStates();
			}
		}.execute(CyanideApi.instance().getNewestId());
	}

	private void showLoading() {
		loadingView.startAnimation(fadeIn);
	}

	private void showStage() {
		loadingView.startAnimation(fadeOut);
	}


	/**
	 * Refreshes the state of the download button based on if the comic exists on the file system
	 */
	private void refreshDownloadButtonState() {
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
	 * Refreshes the download button and favorite button states
	 */
	private void refreshButtonStates() {
		refreshDownloadButtonState();
		refreshFavoriteButtonState();
	}

	/**
	 * Gets the ID of the comic currently being shown to the user
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
		final Comic c = pagerAdapter.getComicStage(viewPager.getCurrentItem()).getComic();
		// Use a callback approach because the user might have pressed the button before the bitmap
		// was loaded, causing a NullPointerException
		c.setOnBitmapLoaded(new Callback<Void>() {
			@Override
			public void onComplete(Void result) {
				c.download(downloadButton);
			}
		});
	}

	public void onLatestRequested() {
		Log.i(CyanideViewer.TAG, "Latest comic requested");
		new AsyncTask<Void, Void, Long>() {

			@Override
			protected Long doInBackground(Void... params) {
				CyanideApi.instance().checkForNewComic(new Callback<Boolean>() {

					@Override
					public void onComplete(Boolean result) {
						Log.i(CyanideViewer.TAG, "User requested latest comic (newer was " + (result ? "" : "not") + " found)");
					}
				});

				return CyanideApi.instance().getNewestId();
			}

			@Override
			protected void onPostExecute(Long id) {
				setComic(id);
			}
		}.execute();
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
			case R.id.menu_go_to_latest:
				onLatestRequested();
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

	@Override
	protected void onResume() {
		super.onResume();
//		refreshButtonStates();
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
	 * Shows a comic with the given ID to the user
	 * @param id The ID to use
	 */
	public void setComic(final long id) {
		new SetComicTask().execute(id);
	}

	/**
	 * This class is used to set the current comic being displayed to the user to a specific comic
	 */
	private class SetComicTask extends AsyncTask<Long, Void, Integer> {
		private boolean showStagePostExecute;

		public SetComicTask() {
			this(true);
		}

		public SetComicTask(boolean showStagePostExecute) {
			this.showStagePostExecute = showStagePostExecute;
		}

		@Override
		protected void onPreExecute() {
			// Show the loading screen
			showLoading();

			viewPager.setAdapter(null);
		}

		@Override
		protected Integer doInBackground(Long... params) {
			Log.d(CyanideViewer.TAG, "SetComicTask invoked for #" + params[0]);
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
						showStage();
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
				showStage();
			}

		}
	}
}
