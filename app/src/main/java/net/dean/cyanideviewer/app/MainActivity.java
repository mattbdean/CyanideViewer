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
import android.widget.Toast;

import net.dean.cyanideviewer.app.api.Comic;
import net.dean.cyanideviewer.app.api.CyanideApi;
import net.dean.cyanideviewer.app.api.RetrievePreviousComicTask;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate a ViewPager and a PagerAdapter
        viewPager = (ViewPager) findViewById(R.id.comic_pager);

        pagerAdapter = new ComicPagerAdapter();
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
        // Disable the Download button if the file already exists
        Comic currentComic = pagerAdapter.getComicStage(viewPager.getCurrentItem()).getComic();
        boolean hasLocal = CyanideApi.hasLocal(currentComic.getId());
        ((ImageButton) findViewById(R.id.download)).setEnabled(!hasLocal);

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                Log.v(CyanideViewer.TAG, "At position: " + position);
                Comic currentComic = pagerAdapter.getComicStage(position).getComic();

                if (position == 0) { // At the very left
                    try {
                        Comic comicToShowNext = new RetrievePreviousComicTask().execute(currentComic.getId()).get();
                        pagerAdapter.addView(comicToShowNext, 0);
                    } catch (InterruptedException | ExecutionException e) {
                        Log.e(CyanideViewer.TAG, "Failed to get the previous comic", e);
                    }
                }

                // Disable the Download button if the file already exists
                boolean hasLocal = CyanideApi.hasLocal(currentComic.getId());
                ((ImageButton) findViewById(R.id.download)).setEnabled(!hasLocal);
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        (findViewById(R.id.download)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Download the comic at the current ID
                boolean succeeded = pagerAdapter.getComicStage(viewPager.getCurrentItem())
                        .getComic().download();

                String toastText;
                if (succeeded) {
                    toastText = "Comic downloaded";
                } else {
                    toastText = "Comic failed to download!";
                }

                Toast.makeText(CyanideViewer.getContext(), toastText, Toast.LENGTH_SHORT).show();
            }
        });
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
