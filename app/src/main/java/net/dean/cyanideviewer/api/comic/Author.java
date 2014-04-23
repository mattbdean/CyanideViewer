package net.dean.cyanideviewer.api.comic;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.db.DatabaseField;
import net.dean.cyanideviewer.db.Model;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Author extends Model implements Parcelable {
	public static final Author[] AUTHORS;

	@DatabaseField(columnName = "name")
	private String name;
	@DatabaseField(columnName = "twitter")
	private String twitter;
	@DatabaseField(columnName = "facebook")
	private String facebook;
	private Bitmap icon;

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

	public String getFirstName() {
		return name.substring(0, name.indexOf(' '));
	}

	public String getLastName() {
		return name.substring(name.indexOf(' ') + 1);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTwitter() {
		return twitter;
	}

	public void setTwitter(String twitter) {
		this.twitter = twitter;
	}

	public String getFacebook() {
		return facebook;
	}

	public void setFacebook(String facebook) {
		this.facebook = facebook;
	}

	public Bitmap getIcon() {
		return icon;
	}

	public void setIcon(Bitmap icon) {
		this.icon = icon;
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
				", icon=" + icon +
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
