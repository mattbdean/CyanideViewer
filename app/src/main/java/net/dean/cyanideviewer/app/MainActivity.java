package net.dean.cyanideviewer.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import net.dean.cyanideviewer.app.api.Comic;
import net.dean.cyanideviewer.app.api.CyanideApi;
import net.dean.cyanideviewer.app.api.impl.CyanideApiImpl;

import java.util.concurrent.ExecutionException;


public class MainActivity extends FragmentActivity {
    private CyanideApiImpl api;

    private ViewPager viewPager;
    private ComicPagerAdapter2 pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.api = new CyanideApiImpl(this);

        // Instantiate a ViewPager and a PagerAdapter
        viewPager = (ViewPager) findViewById(R.id.comicPager);

        //pagerAdapter = new ComicPagerAdapter(getSupportFragmentManager(), initialComics);
        pagerAdapter = new ComicPagerAdapter2(getApplicationContext());
        viewPager.setAdapter(pagerAdapter);

        try {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    // Add the newest
                    pagerAdapter.addView(CyanideApi.instance.getNewest());

                    // Add the second newest
                    ComicStage cs = (ComicStage) pagerAdapter.getComic(pagerAdapter.getCount() - 1);
                    pagerAdapter.addView(CyanideApi.instance.getPrevious(cs.getComic().getId()), 0);

                    return null;
                }
            }.execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }


        viewPager.setCurrentItem(1);
        // There are currently two items in the adapter. Set it to the right-most one. (the newest comic)

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                Log.v(CyanideViewer.TAG, "At position: " + position);

                if (position == 0) { // At the very left
                    Comic comicToShow = ((ComicStage)pagerAdapter.getComic(position)).getComic();
                    Comic comicToShowNext = api.getComicPrevious(comicToShow.getId());
                    pagerAdapter.addView(comicToShowNext, 0);
                    System.out.println();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
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

    public ComicPagerAdapter2 getPagerAdapter() {
        return pagerAdapter;
    }

    public CyanideApiImpl getApi() {
        return api;
    }
}
