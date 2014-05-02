package net.dean.cyanideviewer;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.crashlytics.android.Crashlytics;

import net.dean.cyanideviewer.api.CyanideApi;
import net.dean.cyanideviewer.api.comic.Comic;

import java.util.ArrayList;

/**
 * The main activity of Cyanide Viewer
 */
public class MainActivity extends FragmentActivity {

	/** An enumeration of all the buttons on this UI whose states are changed manually. */
	private static enum RefreshableButtons {
		/** Represents ${@link #downloadButton} */
		DOWNLOAD,
		/** Represents ${@link #firstComicButton} */
		FIRST,
		/** Represents ${@link #newestComicButton} */
		NEWEST,
		/** Represents ${@link #favoriteButton} */
		FAVORITE
	}

	/** The ViewPager used to scroll through comics */
	private ViewPager viewPager;

	/** The ComicPagerAdapter used to provide pages to the ViewPager */
	private ComicPagerAdapter pagerAdapter;

	/** The button that, when pressed, downloads the current comic */
	private ImageButton downloadButton;

	/** The button that, when pressed, shows the user the first available comic */
	private ImageButton firstComicButton;

	/** The button that, when pressed, shows the user the newest available comic */
	private ImageButton newestComicButton;

	/** The button that, when pressed, toggles whether the current comic is a favorite */
	private ToggleButton favoriteButton;

	/**
	 * The view that will be faded in and out when the application is doing work that cannot be done
	 * solely in the background
	 */
	private LinearLayout loadingView;

	/**
	 * The FragmentManager used to instantiate {@link #pagerAdapter} and AuthorDialogs
	 */
	private FragmentManager fragmentManager;

	/** The animation used to fade the loading view in */
	private Animation fadeIn;
	/** The animation used to fade the loading view out */
	private Animation fadeOut;

	/** True if the onCrated method has completed, false if else */
	private boolean hasCreated;

	/** True if the app is doing work and the loading view must be shown, false if else */
	private boolean working;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Crashlytics.start(this);
		setContentView(R.layout.activity_main);

		// Trivial attributes
		this.working = false;
		this.hasCreated = false;

		// Initialize all views
		this.viewPager = (ViewPager) findViewById(R.id.comic_pager);
		this.fragmentManager = getFragmentManager();
		this.pagerAdapter = new ComicPagerAdapter(fragmentManager);
		this.favoriteButton = (ToggleButton) findViewById(R.id.action_favorite);
		this.downloadButton = (ImageButton) findViewById(R.id.download);
		this.firstComicButton = (ImageButton) findViewById(R.id.first_comic);
		this.newestComicButton = (ImageButton) findViewById(R.id.newest_comic);
		this.loadingView = (LinearLayout) findViewById(R.id.loading_view);
		this.fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		fadeIn.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				working = true;
				if (loadingView.getVisibility() != View.VISIBLE) {
					loadingView.setVisibility(View.VISIBLE);
				}
			}

			@Override public void onAnimationEnd(Animation animation) { }
			@Override public void onAnimationRepeat(Animation animation) { }
		});
		this.fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
		fadeOut.setAnimationListener(new Animation.AnimationListener() {
			@Override public void onAnimationStart(Animation animation) { }
			@Override
			public void onAnimationEnd(Animation animation) {
				loadingView.setVisibility(View.INVISIBLE);
				working = false;
			}
			@Override public void onAnimationRepeat(Animation animation) {}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				showLoading();
			}

			@Override
			protected Void doInBackground(Void... params) {
				CyanideApi.init();
				return null;
			}

			@Override
			protected void onPostExecute(Void v) {
				onFinishedApiSetup();
				hasCreated = true;
				setLastComic();
			}
		}.execute();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (hasCreated) {
			setLastComic();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
		editor.putLong("lastId", getCurrentComicId());
		editor.apply();
	}

	/**
	 * Called when the CyanideApi class has finished setting up its ID ranges. Sets {@link #viewPager}'s
	 * ${@link android.support.v4.view.ViewPager.OnPageChangeListener}
	 */
	private void onFinishedApiSetup() {
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

				refreshButtons();
			}

			@Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
			@Override public void onPageScrollStateChanged(int state) {}

		});
	}

	/**
	 * Reads the value of the last ID from the SharedPreferences and sets the current comic to that ID
	 */
	private void setLastComic() {
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		long lastId = prefs.getLong("lastId", CyanideApi.instance().getNewestId());

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

				refreshButtons(RefreshableButtons.DOWNLOAD, RefreshableButtons.FAVORITE);
			}
		}.execute(lastId);
	}

	/**
	 * Shows the 'loading' pane over the main one (where most of the UI elements are)
	 */
	private void showLoading() {
		if (!working) // Only start the animation if it is not showing already
			loadingView.startAnimation(fadeIn);
	}

	/**
	 * Fades the 'loading' pane out and lets the user interact with the main UI
	 */
	private void showStage() {
		if (working)
			loadingView.startAnimation(fadeOut);
	}

	/**
	 * Refreshes all of the possible buttons
	 */
	private void refreshButtons() {
		refreshButtons(RefreshableButtons.values());
	}

	/**
	 * Refreshes each of the buttons assigned to the values of {@link net.dean.cyanideviewer.MainActivity.RefreshableButtons}.
	 * @param buttonsToRefresh The representation of the buttons to refresh.
	 */
	private void refreshButtons(RefreshableButtons... buttonsToRefresh) {
		if (buttonsToRefresh.length == 0) {
			return;
		}
		long currComicId = -2; // Need a unique value because it could be -1

		for (RefreshableButtons buttonToRefresh : buttonsToRefresh) {
			switch (buttonToRefresh) {
				case DOWNLOAD:
					boolean hasLocal = CyanideApi.instance().hasLocalComic(getCurrentComicId());
					downloadButton.setEnabled(!hasLocal);
					break;
				case FAVORITE:
					favoriteButton.setChecked(getCurrentDbComic().isFavorite());
					break;
				case FIRST:
					if (currComicId == -2) { // Only get it if we need it
						currComicId = pagerAdapter.getComicStage(viewPager.getCurrentItem()).getComicIdToLoad();
					}
					newestComicButton.setEnabled(currComicId != CyanideApi.instance().getNewestId());
					break;
				case NEWEST:
					if (currComicId == -2) { // Only get it if we need it
						currComicId = pagerAdapter.getComicStage(viewPager.getCurrentItem()).getComicIdToLoad();
					}
					firstComicButton.setEnabled(currComicId != CyanideApi.instance().getFirstId());
					break;
				default:
					Log.w(CyanideViewer.TAG, "Not refreshing unregistered button " + buttonToRefresh);
			}
		}
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
		CyanideViewer.getComicDao().update(currentDbComic);

		pagerAdapter.getComicStage(viewPager.getCurrentItem()).getComic().downloadIcon();

		refreshButtons(RefreshableButtons.FAVORITE, RefreshableButtons.DOWNLOAD);
	}

	/**
	 * Gets what the database knows about the current comic being shown to the user
	 * @return A comic from the database
	 */
	private Comic getCurrentDbComic() {
		return CyanideViewer.getComicDao().get(getCurrentComicId());
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

	/**
	 * Called when the user wants the very newest comic. Calls the CyanideApi instance and checks
	 * for a new ID and then sets the current comic to the latest one available.
	 */
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

	public void onFirstComicButtonPressed(View view) {
		setComic(CyanideApi.instance().getFirstId());
	}

	public void onNewestComicButtonPressed(View view) {
		setComic(CyanideApi.instance().getNewestId());
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
				pagerAdapter.getComicStage(midway).setComic(id, fragmentManager);
				curComicPagerIndex = midway;
				// From the midway to the beginning
				for (int i = midway - 1; i >= 0; i--) {
					pagerAdapter.getComicStage(i).setComic(prevComic.getId(), fragmentManager);
					prevComic = CyanideApi.instance().getPrevious(prevComic.getId());
				}

				// From the midway to the end
				for (int i = midway + 1; i < pagerAdapter.getCount(); i++) {
					pagerAdapter.getComicStage(i).setComic(nextComic.getId(), fragmentManager);
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

			refreshButtons(RefreshableButtons.FIRST, RefreshableButtons.NEWEST);
		}
	}
}
