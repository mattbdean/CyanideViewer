package net.dean.cyanideviewer.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.dean.cyanideviewer.app.api.impl.CyanideApiImpl;


public class MainActivity extends Activity {
    private CyanideApiImpl api;
    private TextView comicIdLabel;
    private ImageView comicViewer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the latest comic and display it
        this.comicViewer = (ImageView) findViewById(R.id.viewer);
        this.comicIdLabel = (TextView) findViewById(R.id.currentId);
        this.api = new CyanideApiImpl(comicViewer, comicIdLabel);
        api.setComicNewest();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void onNewestClicked(View v) {
        api.setComicNewest();
    }

    public void onFirstClicked(View v) {
        api.setComicFirst();
    }

    public void onRandomClicked(View v) {
        api.setComicRandom();
    }

    public void onNextClicked(View v) {
        api.setComicNext();
    }

    public void onPreviousClicked(View v) {
        api.setComicPrevious();
    }
}
