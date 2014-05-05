package net.dean.cyanideviewer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.dean.cyanideviewer.api.comic.Author;

public class AuthorDialog extends DialogFragment {

	public Author author;

	public AuthorDialog(Author author) {
		this.author = author;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Typeface t = Typefaces.get(getActivity().getBaseContext(), "fonts/fontawesome.ttf");
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_author, null);
		Button facebook = (Button) view.findViewById(R.id.facebook);
		facebook.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUri("fb://profile/" + author.getFacebook(), "https://www.facebook.com/" + author.getFacebook());
			}
		});
		facebook.setTypeface(t);

		Button twitter = (Button) view.findViewById(R.id.twitter);
		twitter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUri("twitter://user?screen_name=" + author.getTwitter(), "https://twitter.com/" + author.getTwitter());
			}
		});
		twitter.setTypeface(t);
		((ImageView)view.findViewById(R.id.avatar)).setImageResource(author.getIconResource());
		((TextView)view.findViewById(R.id.name)).setText(author.getName());

		builder.setView(view).setPositiveButton("Done", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
			}
		});

		return builder.create();
	}


	public void openUri(String primary, String fallback) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(primary));
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.i(Constants.TAG, String.format("Primary URI (%s) failed, using backup (%s)", primary, fallback));
			intent.setData(Uri.parse(fallback));
			startActivity(intent);
		}
	}
}
