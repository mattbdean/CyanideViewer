package net.dean.cyanideviewer.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.dean.cyanideviewer.app.api.Comic;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class ComicStage extends LinearLayout {
    private Comic comic;
    private boolean loaded;

    public ComicStage(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.loaded = false;
    }

    public void setComic(Comic comic) {
        this.comic = comic;
        ((TextView) findViewById(R.id.comic_id)).setText("#" + comic.getId());
        new BitmapLoaderTask().execute(this.comic);
    }

    public Comic getComic() {
        return comic;
    }

    public static ComicStage newInstance(Context context, Comic c) {
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ComicStage cs = (ComicStage) li.inflate(R.layout.comic_stage, null);
        cs.setComic(c);
        return cs;
    }

    public class BitmapLoaderTask extends AsyncTask<Comic, Void, Bitmap> {
        public BitmapLoaderTask() {

        }

        @Override
        protected void onPreExecute() {
            // TODO Show indeterminate progress bar/circle
            TextView tv = (TextView) findViewById(R.id.comic_id);
            tv.setText("#" + getComic().getId());
        }

        @Override
        protected Bitmap doInBackground(Comic... params) {
            Comic c = params[0];

            // Adapted from http://stackoverflow.com/a/6621552/1275092

            try {
                URL url = new URL(c.getUrl());

                if (url.getProtocol().equals("file")) {
                    // Local file, no need to make any HTTP requests
                    return BitmapFactory.decodeStream(url.openStream());
                }

                HttpGet request = new HttpGet(url.toURI());
                HttpClient client = new DefaultHttpClient();
                HttpResponse response = client.execute(request);
                BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());

                return BitmapFactory.decodeStream(entity.getContent());
            } catch (URISyntaxException | MalformedURLException e) {
                Log.e(CyanideViewer.TAG, "Malformed URL: " + c.getUrl());
                return null;
            } catch (IOException e) {
                Log.e(CyanideViewer.TAG, "IOException while trying to decode the image from URL " + c.getUrl());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                ImageView imageView = (ImageView) findViewById(R.id.image_view);
                imageView.setImageBitmap(bitmap);
                ProgressBar pb = (ProgressBar) findViewById(R.id.progress_bar);
                // Remove the progress bar once the image is done loading
                ((RelativeLayout) pb.getParent()).removeView(pb);
            }
        }
    }
}
