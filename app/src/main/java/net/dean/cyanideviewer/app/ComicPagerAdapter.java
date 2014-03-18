package net.dean.cyanideviewer.app;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import net.dean.cyanideviewer.app.api.Comic;

import java.util.ArrayList;

/**
 * http://stackoverflow.com/a/13671777/1275092
 */
public class ComicPagerAdapter extends PagerAdapter {
    private final Context context;
    private ArrayList<View> views = new ArrayList<View>();

    public ComicPagerAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getItemPosition(Object object) {
        int index = views.indexOf(object);
        if (index == -1) {
            return POSITION_NONE;
        } else {
            return index;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View v = views.get(position);
        container.addView(v);
        return v;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(views.get(position));
    }

    @Override
    public int getCount() {
        return views.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public int addView(Comic c) {
        return addView(c, views.size());
    }

    public int addView(Comic c, int position) {
        views.add(position, ComicStage.newInstance(this.context, c));
        notifyDataSetChanged();
        return position;
    }

    public int removeView(ViewPager pager, View v) {
        return removeView(pager, views.indexOf(v));
    }

    public int removeView(ViewPager pager, int position) {
        pager.setAdapter(null);
        views.remove(position);
        pager.setAdapter(this);

        return position;
    }

    public View getComic(int position) {
        return views.get(position);
    }
}