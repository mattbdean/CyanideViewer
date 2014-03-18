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
import java.net.URISyntaxException;

/**
 * Represents the main component of this application: where the comic is presented. It contains three
 * main parts: an ImageView to hold the image, a ProgressBar to tell the user when the comic is
 * still loading, and a TextView to show the user the ID of the comic they are viewing.
 */
public class ComicStage extends LinearLayout {

    /**
     * Creates a new instance of a ComicStage and its comic to the one given.
     * @param c The comic to use
     * @return A new ComicStage
     */
    public static ComicStage newInstance(Comic c) {
        LayoutInflater li = (LayoutInflater) CyanideViewer.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ComicStage cs = (ComicStage) li.inflate(R.layout.comic_stage, null);
        cs.setComic(c);
        return cs;
    }

    /** The comic that used to fill out this ComicStage */
    private Comic comic;

    /** Instantiates a new ComicStage */
    public ComicStage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** Sets the comic of this ComicStage. Will adjust the TextView and ImageView accordingly. */
    public void setComic(Comic comic) {
        this.comic = comic;
        ((TextView) findViewById(R.id.comic_id)).setText("#" + comic.getId());
        new BitmapLoaderTask().execute(this.comic);
    }

    /** Gets the Comic associated with this ComicStage */
    public Comic getComic() {
        return comic;
    }

    /** Represents the task of loading a Comic's URL into a Bitmap usable by an ImageView */
    public class BitmapLoaderTask extends AsyncTask<Comic, Void, Bitmap> {

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
                if (c.getUrl().getProtocol().equals("file")) {
                    // Local file, no need to make any HTTP requests
                    return BitmapFactory.decodeStream(c.getUrl().openStream());
                }

                HttpGet request = new HttpGet(c.getUrl().toURI());
                HttpClient client = new DefaultHttpClient();
                HttpResponse response = client.execute(request);
                BufferedHttpEntity entity = new BufferedHttpEntity(response.getEntity());

                return BitmapFactory.decodeStream(entity.getContent());
            } catch (URISyntaxException e) {
                Log.e(CyanideViewer.TAG, "URISyntaxException: " + c.getUrl(), e);
                return null;
            } catch (IOException e) {
                Log.e(CyanideViewer.TAG, "IOException while trying to decode the image from URL " + c.getUrl(), e);
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
