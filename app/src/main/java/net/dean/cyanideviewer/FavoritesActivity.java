package net.dean.cyanideviewer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import net.dean.cyanideviewer.api.Comic;

import java.util.ArrayList;


public class FavoritesActivity extends Activity {
	/** Returned when the user picked a comic */
	public static final int RESULT_OK = 0;

	/** Returned when the user did not pick a comic */
	public static final int RESULT_NONE = 1;

	/** The adapter to give the ListView items */
	private FavoritesAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_favorites);

		ArrayList<Parcelable> comicsData = getIntent().getParcelableArrayListExtra("comics");
		ArrayList<Comic> comics = new ArrayList<>(comicsData.size());
		for (Parcelable p : comicsData) {
			comics.add((Comic) p);
		}

		this.adapter = new FavoritesAdapter(comics);
		ListView listView = (ListView) findViewById(R.id.favorites_list);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent();
				intent.putExtra("comic", (Comic) adapter.getItem(position));
				setResult(RESULT_OK, intent);
				finish();
			}
		});
	}

	@Override
	public void onBackPressed() {
		// Tell the calling activity that no comic was chosen
		setResult(RESULT_NONE);
		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.favorites, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * This class is used to feed the ListView in the FavoritesActivity views (which are FavoriteComicListItems)
	 */
	private class FavoritesAdapter extends BaseAdapter {
		/**
		 * A list of comics that are represented by FavoriteComicListItems
		 */
		private ArrayList<Comic> comics;

		/**
		 * Instantiates a new FavoritesAdapter
		 * @param comics The comics to use
		 */
		public FavoritesAdapter(ArrayList<Comic> comics) {
			this.comics = comics;
		}

		@Override
		public int getCount() {
			return comics.size();
		}

		@Override
		public Object getItem(int position) {
			return comics.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// Create a new view
			return FavoriteComicListItem.newInstance(comics.get(position));
		}
	}
}
