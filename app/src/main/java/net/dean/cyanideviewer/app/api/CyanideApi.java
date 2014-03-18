package net.dean.cyanideviewer.app.api;

import android.os.AsyncTask;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import net.dean.cyanideviewer.app.CyanideViewer;
import net.dean.cyanideviewer.app.api.impl.SpecialSelection;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * Created by matthew on 3/16/14.
 */
public class CyanideApi {
    /**
     * The base URL for the Cyanide and Happiness comic.
     */
    public static final String BASE_URL = "http://explosm.net/comics/";

    /**
     * The ID of the very first publicly available C&H comic
     */
    public long firstId;

    /**
     * The ID of the newest C&H comic
     */
    public long newestId;

    /**
     * The ID of the last comic that was retrieved
     */
    public long currentId;

    public static CyanideApi instance;

    public CyanideApi() {
        if (instance != null) {
            // Oh my god this is so bad
            throw new IllegalStateException("Only one instance of CyanideApi allowed");
        }
        instance = this;
        this.currentId = -1;
        initIdRanges();
        Log.i(CyanideViewer.TAG, "C&H API instantiated");
    }

    public void initIdRanges() {
        try {
            firstId = new AsyncTask<Void, Void, Long>() {
                @Override
                protected Long doInBackground(Void... params) {
                    return instance.parseId(SpecialSelection.FIRST.getUrl());
                }
            }.execute().get();
            Log.i(CyanideViewer.TAG, "Found first ID: " + firstId);

            newestId = new AsyncTask<Void, Void, Long>() {

                @Override
                protected Long doInBackground(Void... params) {
                    return instance.parseId(SpecialSelection.NEWEST.getUrl());
                }
            }.execute().get();
            Log.i(CyanideViewer.TAG, "Found newest ID: " + newestId);
        } catch (InterruptedException | ExecutionException e) {
            Log.e(CyanideViewer.TAG, "Failed to find the first or newest comic IDs", e);
        }
    }

    private void checkMainThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.e(CyanideViewer.TAG, "Networking on main thread", new NetworkOnMainThreadException());
        }
    }

    public Long parseId(String url) {
        String currentUrl = followRedirect(url);
        if (!currentUrl.contains("explosm.net/comics/")) {
            Log.e(CyanideViewer.TAG, "Cannot load comic: URL not in correct format: " + currentUrl);
            return null;
        }
        return Long.parseLong(currentUrl.substring(currentUrl.lastIndexOf('/', currentUrl.length() - 2)).replace("/", ""));
    }

    /**
     * Follows a given URL and returns the URL that the request was redirected to.
     * @param url The URL to follow
     * @return The URL that the given URL will redirect to
     */
    private String followRedirect(String url) {
        checkMainThread();

        try {
            new URL(url).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            Log.e(CyanideViewer.TAG, "Malformed URL: " + url, e);
            return null;
        }

        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);
            HttpContext context = new BasicHttpContext();
            HttpResponse response = client.execute(get, context);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(response.getStatusLine().toString());
            }
            HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
            HttpHost currentHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            String currentUrl = (currentReq.getURI().isAbsolute() ? currentReq.getURI().toString() : (currentHost.toURI() + currentReq.getURI()));
            Log.i(CyanideViewer.TAG, "Followed " + url + " to " + currentUrl);
            return currentUrl;
        } catch (IOException e) {
            Log.e(CyanideViewer.TAG, "Unable to get the latest comic ID", e);
            return null;
        }
    }

    public String getComicUrl(long id) {
        checkMainThread();

        try {
            Document doc = Jsoup.connect(BASE_URL + id).get();
            Elements images = doc.select("#maincontent img[src]");

            for (Element e : images) {
                if (e.hasAttr("src")) {
                    if (e.attr("src").contains("/db/files/Comics/")) {
                        return e.attr("src");
                    }
                }
            }
        } catch (IOException e) {
            Log.e(CyanideViewer.TAG, "IOException while testing if comic #" + id + " was a comic page or not");
        }

        return null;
    }

    // TODO: getPrevious() and getNext() are pretty much the same method; they can be shortened
    public Comic getPrevious(long id) {
        long newId = id - 1;

        Log.i(CyanideViewer.TAG, "Getting the previous comic relative to #" + id);
        if (newId < firstId) {
            // The ID is less than the first comic's ID
            Log.w(CyanideViewer.TAG, String.format("The given ID (%s) was lower than the minimum ID (%s)", newId, firstId));
            return getComic(firstId);
        }

        if (newId > newestId) {
            // The ID is greater than the newest comic's ID
            Log.w(CyanideViewer.TAG, String.format("The given ID (%s) was greater than the maximum ID (%s)", newId, newestId));
            return getComic(newestId);
        }

        if (getComicUrl(newId) != null) {
            // There is a comic associated with this page
            // TODO: Add an optional param for getComic(), we already know what the comic URL is and
            // getComic() is just going to get it again
            return getComic(newId);
        }

        Log.i(CyanideViewer.TAG, "There was no comic associated with comic #" + newId + ", trying #" + (newId - 1));
        // Go to the previous comic
        return getPrevious(newId);
    }

    public Comic getNext(long id) {
        long newId = id + 1;

        Log.i(CyanideViewer.TAG, "Getting the previous comic relative to #" + id);
        if (newId < firstId) {
            // The ID is less than the first comic's ID
            Log.w(CyanideViewer.TAG, String.format("The given ID (%s) was lower than the minimum ID (%s)", newId, firstId));
            return getComic(firstId);
        }

        if (newId > newestId) {
            // The ID is greater than the newest comic's ID
            Log.w(CyanideViewer.TAG, String.format("The given ID (%s) was greater than the maximum ID (%s)", newId, newestId));
            return getComic(newestId);
        }

        if (getComicUrl(newId) != null) {
            // There is a comic associated with this page
            // TODO: Add an optional param for getComic(), we already know what the comic URL is and
            // getComic() is just going to get it again
            return getComic(newId);
        }

        Log.i(CyanideViewer.TAG, "There was no comic associated with comic #" + newId + ", trying #" + (newId + 1));
        // Go to the previous comic
        return getNext(newId);
    }

    public Comic getNewest() {
        return getComic(newestId);
    }

    public Comic getFirst() {
        return getComic(firstId);
    }

    public Comic getRandom() {
        return getComic(parseId(followRedirect(SpecialSelection.RANDOM.getUrl())));
    }

    public Comic getComic(long id) {
        checkMainThread();

        String img = null;
        long time = System.nanoTime();

        try {
            Document doc = Jsoup.connect(BASE_URL + id).get();
            Elements images = doc.select("#maincontent img[src]");

            // Iterate through the images to find the correct one
            for (Element e : images) {
                if (e.hasAttr("src")) {
                    if (e.attr("src").contains("/db/files/Comics/")) {
                        img = e.attr("src");
                    }
                }
            }

            Log.i(CyanideViewer.TAG, String.format("Retrieved comic #%s in %s milliseconds", id, (System.nanoTime() - time) / 1_000_000));
            this.currentId = id;
            return new Comic(id, img);
        } catch (MalformedURLException e) {
            Log.e(CyanideViewer.TAG, "Malformed URL while trying to get #" + id + ": " + img, e);
            return null;
        } catch (IOException e) {
            Log.e(CyanideViewer.TAG, "Unable to get the image for id of " + id, e);
            return null;
        }
    }


    public long getFirstId() {
        return firstId;
    }

    public long getNewestId() {
        return newestId;
    }
}
