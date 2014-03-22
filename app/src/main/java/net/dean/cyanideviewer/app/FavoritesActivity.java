package net.dean.cyanideviewer.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.dean.cyanideviewer.app.api.Comic;

import java.util.ArrayList;


public class FavoritesActivity extends Activity {

	private FavoritesAdapter adapter;
	private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

	    ArrayList<Comic> comics = CyanideViewer.getComicDao().getFavoriteComics();
	    this.adapter = new FavoritesAdapter(comics);
	    this.listView = (ListView) findViewById(R.id.favorites_list);
	    listView.setAdapter(adapter);

	    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
		    @Override
		    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			    // TODO Show MainActivity at a certain comic ID
		    }
	    });
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

	private class FavoritesAdapter extends BaseAdapter {
		private ArrayList<Comic> comics;

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
			LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.favorite_item, null);
			Comic c = comics.get(position);
			((TextView)layout.findViewById(R.id.comic_id)).setText("#" + c.getId());

			return layout;
		}
	}
}
