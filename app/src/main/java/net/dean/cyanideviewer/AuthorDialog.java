package net.dean.cyanideviewer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_author, null);
		view.findViewById(R.id.facebook).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUri("fb://profile/" + author.getFacebook(), "https://www.facebook.com/" + author.getFacebook());
			}
		});
		view.findViewById(R.id.twitter).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openUri("twitter://user?screen_name=" + author.getTwitter(), "https://twitter.com/" + author.getTwitter());
			}
		});
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
		} catch (Exception e) {
			Log.e(Constants.TAG, String.format("Primary URI (%s) failed, using backup (%s)", primary, fallback), e);
			intent.setData(Uri.parse(fallback));
			startActivity(intent);
		}
	}
}
