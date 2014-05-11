package net.dean.cyanideviewer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import net.dean.cyanideviewer.R;
import net.dean.cyanideviewer.Typefaces;

/**
 * http://stackoverflow.com/a/7197867/1275092
 */
public class TextViewPlus extends TextView {
	public TextViewPlus(Context context) {
		super(context);
	}

	public TextViewPlus(Context context, AttributeSet attrs) {
		super(context, attrs);
		setCustomFont(context, attrs);
	}

	public TextViewPlus(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setCustomFont(context, attrs);
	}

	private void setCustomFont(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextViewPlus);
		String fontFileName = a.getString(R.styleable.TextViewPlus_fontFile);
		setCustomFont(context, fontFileName);

		a.recycle();
	}

	private void setCustomFont(Context context, String fontFile) {
		setTypeface(Typefaces.get(context, "fonts/" + fontFile));
	}
}
