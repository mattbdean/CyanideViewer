package net.dean.cyanideviewer;

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

import net.dean.cyanideviewer.api.CyanideApi;
import net.dean.cyanideviewer.api.comic.Comic;

import java.util.ArrayList;

/**
 * The main activity of Cyanide Viewer
 */
public class MainActivity extends FragmentActivity {

	/** An enumeration of all the buttons on this UI whose states are changed manually. */
	public static enum RefreshableButtons {
		/** Represents ${@link #downloadButton} */
		DOWNLOAD,
		/** Represents ${@link #favoriteButton} */
		FAVORITE
	}

	private HistoryManager historyManager;

	/** The ViewPager used to scroll through comics */
	private ViewPager viewPager;

	/** The ComicPagerAdapter used to provide pages to the ViewPager */
	private ComicPagerAdapter pagerAdapter;

	/** The button that, when pressed, downloads the current comic */
	private ImageButton downloadButton;

	/** The button that, when pressed, toggles whether the current comic is a favorite */
	private ToggleButton favoriteButton;

	/**
	 * The view that will be faded in and out when the application is doing work that cannot be done
	 * solely in the background
	 */
	private LinearLayout loadingView;

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
		setContentView(R.layout.activity_main);

		// Trivial attributes
		this.working = false;
		this.hasCreated = false;
		this.historyManager = new HistoryManager(this);

		// Initialize all views
		this.viewPager = (ViewPager) findViewById(R.id.comic_pager);
		this.pagerAdapter = new ComicPagerAdapter();
		this.favoriteButton = (ToggleButton) findViewById(R.id.action_favorite);
		this.downloadButton = (ImageButton) findViewById(R.id.download);
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

		new SetComicTask(this) {
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
	public void showLoading() {
		if (!working) // Only start the animation if it is not showing already
			loadingView.startAnimation(fadeIn);
	}

	/**
	 * Fades the 'loading' pane out and lets the user interact with the main UI
	 */
	public void showStage() {
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
	public void refreshButtons(RefreshableButtons... buttonsToRefresh) {
		if (buttonsToRefresh.length == 0) {
			return;
		}

		for (RefreshableButtons buttonToRefresh : buttonsToRefresh) {
			switch (buttonToRefresh) {
				case DOWNLOAD:
					boolean hasLocal = CyanideApi.instance().hasLocalComic(getCurrentComicId());
					downloadButton.setEnabled(!hasLocal);
					break;
				case FAVORITE:
					favoriteButton.setChecked(getCurrentDbComic().isFavorite());
					break;
				default:
					Log.w(Constants.TAG, "Not refreshing unregistered button " + buttonToRefresh);
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
		Log.v(Constants.TAG, "Current is favorite: " + currentDbComic.isFavorite());

		// Flip it
		currentDbComic.setFavorite(!currentDbComic.isFavorite());
		CyanideViewer.getComicDao().setFavorite(currentDbComic);

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
		Log.i(Constants.TAG, "Latest comic requested");
		new AsyncTask<Void, Void, Long>() {

			@Override
			protected void onPreExecute() {
				showLoading();
			}

			@Override
			protected Long doInBackground(Void... params) {
				CyanideApi.instance().checkForNewComic(new Callback<Boolean>() {

					@Override
					public void onComplete(Boolean result) {
						Log.i(Constants.TAG, "User requested latest comic (newer was " + (result ? "" : "not") + " found)");
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
			case R.id.nav:
				NavigationDialog dialog = new NavigationDialog();
				dialog.setCallback(new Callback<Long>() {
					@Override
					public void onComplete(Long id) {
						if (id == NavigationDialog.RESULT_CANCEL) {
							return;
						} else if (id == NavigationDialog.RESULT_RANDOM) {
							setRandom();
							return;
						}

						// Check the bounds
						if (id < CyanideApi.instance().getFirstId()) {
							id = CyanideApi.instance().getFirstId();
						} else if (id > CyanideApi.instance().getNewestId()) {
							id = CyanideApi.instance().getNewestId();
						}

						setComic(id);
					}
				});
				dialog.show(getFragmentManager(), "navDialog");
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Constants.RESULT_OK) {
			// The user chose a comic to view
			Comic theChosenOne = data.getExtras().getParcelable("comic");
			setComic(theChosenOne.getId());
		}
	}

	@Override
	public void onBackPressed() {
		historyManager.back();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void setRandom() {
		new AbstractComicTask<Void>() {

			@Override
			protected void onPreExecute() {
				showLoading();
				super.onPreExecute();
			}

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
		new SetComicTask(this).execute(id);
	}

	public HistoryManager getHistoryManager() {
		return historyManager;
	}

	public ComicPagerAdapter getPagerAdapter() {
		return pagerAdapter;
	}

	public ViewPager getViewPager() {
		return viewPager;
	}
}
