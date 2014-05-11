package net.dean.cyanideviewer.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import net.dean.cyanideviewer.Callback;

/**
 * This class allows for simple yes/no decisions through the use of a dialog
 */
public class SimpleDialog extends DialogFragment {

	/**
	 * The callback that is called if the user confirms the action
	 */
	private Callback<Void> onConfirmed;

	public static SimpleDialog newInstance(String title, String body, String positiveButtonText,
	                                       Callback<Void> onConfirmed) {
		SimpleDialog d = new SimpleDialog();
		d.setOnConfirmed(onConfirmed);

		Bundle args = new Bundle();
		args.putString("title", title);
		args.putString("body", body);
		args.putString("positiveButtonText", positiveButtonText);

		d.setArguments(args);

		return d;
	}

	public SimpleDialog() {
		// Default constructor
	}

	public void setOnConfirmed(Callback<Void> onConfirmed) {
		this.onConfirmed = onConfirmed;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(args.getString("title"))
				.setMessage(args.getString("body"))
				.setPositiveButton(args.getString("positiveButtonText"), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Confirmed
						onConfirmed.onComplete(null);
						dismiss();
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Canceled
						dismiss();
					}
				});
		return builder.create();
	}
}
