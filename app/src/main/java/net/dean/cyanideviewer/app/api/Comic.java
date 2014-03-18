package net.dean.cyanideviewer.app.api;

import android.os.Parcel;
import android.os.Parcelable;

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
}
