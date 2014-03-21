package net.dean.cyanideviewer.app;

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

import net.dean.cyanideviewer.app.api.Comic;
import net.dean.cyanideviewer.app.api.CyanideApi;
import net.dean.cyanideviewer.app.api.RetrievePreviousComicTask;

import java.util.concurrent.ExecutionException;

/**
 * The main activity of Cyanide Viewer
 */
public class MainActivity extends FragmentActivity {

	/** The ViewPager used to scroll through comics */
	private ViewPager viewPager;

	/** The ComicPagerAdapter used to provide pages to the ViewPager */
	private ComicPagerAdapter pagerAdapter;

	/** The button that, when pressed, downloads the current comic */
	private ImageButton downloadButton;

	/** The button that, when pressed, toggles whether the current comic is a favorite */
	private ToggleButton favoriteButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Instantiate a ViewPager and a PagerAdapter
		this.viewPager = (ViewPager) findViewById(R.id.comic_pager);

		this.pagerAdapter = new ComicPagerAdapter();
		viewPager.setAdapter(pagerAdapter);

		try {
			new AsyncTask<Void, Void, Void>() {

				@Override
				protected Void doInBackground(Void... params) {
					// Add the newest
					pagerAdapter.addView(CyanideApi.getNewest());

					// Add the second newest
					ComicStage cs = pagerAdapter.getComicStage(pagerAdapter.getCount() - 1);
					pagerAdapter.addView(CyanideApi.getPrevious(cs.getComic().getId()), 0);

					return null;
				}
			}.execute().get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		viewPager.setCurrentItem(1);

		this.favoriteButton = (ToggleButton) findViewById(R.id.action_favorite);
		this.downloadButton = (ImageButton) findViewById(R.id.download);
		// Refresh button states
		refreshDownloadButtonState();
		refreshFavoriteButtonState();

		viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

			@Override
			public void onPageSelected(int position) {
				if (position == 0) { // At the very left
					try {
						Comic comicToShowNext = new RetrievePreviousComicTask()
								.execute(getCurrentComic().getId()).get();
						pagerAdapter.addView(comicToShowNext, 0);
					} catch (InterruptedException | ExecutionException e) {
						Log.e(CyanideViewer.TAG, "Failed to get the previous comic", e);
					}
				}

				// Refresh button states
				refreshDownloadButtonState();
				refreshFavoriteButtonState();
			}

			@Override
			public void onPageScrollStateChanged(int state) { }
		});
	}

	public void refreshDownloadButtonState() {
		boolean hasLocal = CyanideApi.hasLocal(getCurrentComic().getId());
		downloadButton.setEnabled(!hasLocal);
	}

	private Comic getCurrentComic() {
		return pagerAdapter.getComicStage(viewPager.getCurrentItem()).getComic();
	}

	/** Called when the 'Favorite' button is clicked */
	public void onFavoriteClicked(View view) {
		Comic currentDbComic = getCurrentDbComic();
		// Flip it
		Log.v(CyanideViewer.TAG, "Current is favorite: " + currentDbComic.isFavorite());
		currentDbComic.setFavorite(!currentDbComic.isFavorite());
		CyanideViewer.getComicDao().updateComicAsFavorite(currentDbComic);

		refreshFavoriteButtonState();
	}

	private Comic getCurrentDbComic() {
		return CyanideViewer.getComicDao().getComic(getCurrentComic().getId());
	}

	private void refreshFavoriteButtonState() {
		favoriteButton.setChecked(getCurrentDbComic().isFavorite());
	}

	/**  Called when the 'Download' button is clicked */
	public void onDownloadClicked(View view) {
		// Download the comic at the current ID
		pagerAdapter.getComicStage(viewPager.getCurrentItem()).getComic().download(this);

		refreshDownloadButtonState();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		return id == R.id.action_settings || super.onOptionsItemSelected(item);

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
	 * @param v The view that contained the 'Random' button
	 */
	public void onRandomClicked(View v) {
		//new RetrieveRandomComicTask().execute();
	}
}
