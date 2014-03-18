package net.dean.cyanideviewer.app;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import net.dean.cyanideviewer.app.api.Comic;

import java.util.ArrayList;

/**
 * Created by matthew on 3/16/14, taken from http://stackoverflow.com/a/13671777/1275092
 */
public class ComicPagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<Comic> comics;

    public ComicPagerAdapter(FragmentManager manager, ArrayList<Comic> comics) {
        super(manager);
        this.comics = comics;
    }

//    @Override
//    public Fragment getItem(int position) {
//        Log.i(CyanideViewer.TAG, "Getting item at position " + position);
//        if (comics != null && comics.size() > 0) {
//            position = position % comics.size(); // Use modulo for infinite cycling
//            return ComicStage.newInstance(comics.get(position), g);
//        } else {
//            return ComicStage.newInstance(null);
//        }
//    }


    @Override
    public Fragment getItem(int position) {
        return null;
    }

    public Comic getComic(int position) {
        return comics.get(position);
    }

    @Override
    public int getCount() {
        return comics.size();
    }

    public void addComic(Comic c) {
        comics.add(c);
        notifyDataSetChanged();
    }

    public void prependComic(Comic c) {
        // TODO here
        comics.add(0, c);
        notifyDataSetChanged();
    }
}
