package net.dean.cyanideviewer.api.comic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.R;
import net.dean.cyanideviewer.db.DatabaseField;
import net.dean.cyanideviewer.db.Model;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class Author extends Model implements Parcelable {
	private static final Author[] AUTHORS;

	private static final HashMap<Integer, Bitmap> iconCache = new HashMap<>();

	@DatabaseField(columnName = "name")
	private String name;
	@DatabaseField(columnName = "twitter")
	private String twitter;
	@DatabaseField(columnName = "facebook")
	private String facebook;

	public Author(long id, String name, String twitter, String facebook) {
		super(id);
		this.name = name;
		this.twitter = twitter;
		this.facebook = facebook;
	}

	public Author(Parcel in) {
		super(in.readLong());
		this.name = in.readString();
		this.facebook = in.readString();
		this.twitter = in.readString();
	}

	public String getName() {
		return name;
	}

	public String getTwitter() {
		return twitter;
	}

	public String getFacebook() {
		return facebook;
	}

	public Bitmap getIcon() {
		int resId = -1;

		// switch (firstName.toLowerCase())
		switch (name.substring(0, name.indexOf(' ')).toLowerCase()) {
			case "kris":
				resId = R.drawable.ic_author_kris;
				break;
			case "rob":
				resId = R.drawable.ic_author_rob;
				break;
			case "matt":
				resId = R.drawable.ic_author_matt;
				break;
			case "dave":
				resId = R.drawable.ic_author_dave;
				break;
		}

		if (resId == -1) {
			return null;
		}

		if (iconCache.containsKey(resId)) {
			return iconCache.get(resId);
		}

		Bitmap bmp = BitmapFactory.decodeResource(CyanideViewer.getContext().getResources(), resId);
		iconCache.put(resId, bmp);
		return bmp;
	}

	public static final Creator CREATOR = new Creator() {
		@Nullable
		@Override
		public Object createFromParcel(Parcel source) {
			return new Author(source);
		}

		@Override
		public Object[] newArray(int size) {
			return new Author[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(name);
		dest.writeString(facebook);
		dest.writeString(twitter);
	}

	@Override
	public String toString() {
		return "Author {" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", twitter=" + twitter +
				", facebook=" + facebook +
				'}';
	}

	public static Author getByName(String name) {
		for (Author author : AUTHORS) {
			if (author.getName().equals(name)) {
				return author;
			}
		}

		return null;
	}

	static {
		List<Author> allAuthors = CyanideViewer.getAuthorDao().getAll();
		AUTHORS = allAuthors.toArray(new Author[allAuthors.size()]);
	}
}
