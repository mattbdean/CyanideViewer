package net.dean.cyanideviewer.app.api;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import net.dean.cyanideviewer.app.CyanideViewer;
import net.dean.cyanideviewer.app.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * Created by matthew on 3/17/14.
 */
public class Comic implements Parcelable {
    private long id;
    private String url;

    public Comic(long id, String url) {
        this.url = url;
        this.id = id;
    }

    public Comic(Parcel in) {
        in.readBundle();
        this.id = in.readLong();
        this.url = in.readString();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(url);
    }

    public static final Creator CREATOR = new Creator() {

        @Override
        public Object createFromParcel(Parcel source) {
            return new Comic(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new Comic[size];
        }
    };

    @Override
    public String toString() {
        return "Comic{" +
                "id=" + id +
                ", url='" + url + '\'' +
                '}';
    }

    public boolean download() {
        try {
            return new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        URL url = new URL(getUrl());
                        InputStream input = url.openStream();
                        // "/sdcard/CyanideViewer"
                        File storagePath = new File(Environment.getExternalStorageDirectory(),
                                CyanideViewer.getContext().getResources().getString(R.string.app_name));
                        storagePath.mkdirs();

                        String ext = getUrl().substring(getUrl().lastIndexOf('.'));
                        File dest = new File(storagePath, getId() + ext);
                        if (!dest.exists()) {
                            // Only download it if the file doesn't already exist
                            OutputStream output = new FileOutputStream(dest);

                            byte[] buffer = new byte[2048];
                            int bytesRead;

                            while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                                output.write(buffer, 0, bytesRead);
                            }

                            output.close();
                            input.close();
                        }

                        Log.i(CyanideViewer.TAG, "Downloaded comic #" + id + " to " + dest.getAbsolutePath());
                        return true;
                    } catch (IOException e) {
                        Log.e(CyanideViewer.TAG, "Failed to download #" + id, e);
                        return false;
                    }
                }
            }.execute().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }
}
