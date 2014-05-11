package net.dean.cyanideviewer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.EditText;

import net.dean.cyanideviewer.api.CyanideApi;

public class NavigationDialog extends DialogFragment {
	public static final long RESULT_CANCEL = -2;
	public static final long RESULT_RANDOM = -3;

	private Callback<Long> callback;
	private long chosenId = RESULT_CANCEL; // Assume no choice

	/**
	 * Instantiates a new NavigationDialog.
	 */
	public NavigationDialog() {

	}


	public void setCallback(Callback<Long> callback) {
		this.callback = callback;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_nav, null);

		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Determine the ID to go to
				switch (v.getId()) {
					case R.id.nav_first:
						// First comic
						chosenId = CyanideApi.instance().getFirstId();
						break;
					case R.id.nav_random:
						// Random comic
						chosenId = RESULT_RANDOM;
						break;
					case R.id.nav_newest:
						// Newest comic
						chosenId = CyanideApi.instance().getNewestId();
						break;
				}

				dismiss();
			}
		};

		// Assign the OnClickListener
		int[] ids = new int[] {R.id.nav_first, R.id.nav_random, R.id.nav_newest};
		for (int id : ids) {
			view.findViewById(id).setOnClickListener(listener);
		}

		final EditText editText = (EditText) view.findViewById(R.id.comic_id);
		// Sample hint: "Comic ID (15 to 3542)"
		editText.setHint(getResources().getString(R.string.nav_hint, CyanideApi.instance().getFirstId(),
				CyanideApi.instance().getNewestId()));

		// Length filter equal to the amount of digits in the maximum ID
		editText.setFilters(new InputFilter[] {
				new InputFilter.LengthFilter(Long.toString(CyanideApi.instance().getNewestId()).length())
		});
		// Only digits 0-9
		editText.setKeyListener(DigitsKeyListener.getInstance());
		// Only numbers
		editText.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER);

		builder.setView(view)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = editText.getText().toString();
						if (text.isEmpty()) {
							// The user pressed "OK" with an empty EditText
							chosenId = RESULT_CANCEL;
							dismiss();
							return;
						}
						chosenId = Long.parseLong(editText.getText().toString());
						dismiss();
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						chosenId = RESULT_CANCEL;
						dismiss();
					}
				}).create();

		return builder.create();
	}

	@Override
	public void dismiss() {
		if (callback != null) {
			callback.onComplete(chosenId);
		}

		super.dismiss();
	}
}
