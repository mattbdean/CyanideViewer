package net.dean.cyanideviewer.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.dean.cyanideviewer.app.api.Comic;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLDecoder;

/**
 * Represents the main component of this application: where the comic is presented. It contains three
 * main parts: an ImageView to hold the image, a ProgressBar to tell the user when the comic is
 * still loading, and a TextView to show the user the ID of the comic they are viewing.
 *
 * Panning/Zooming adapted from http://stackoverflow.com/a/6650484/1275092
 */
public class ComicStage extends LinearLayout implements View.OnTouchListener {
	private static final float MIN_ZOOM = 1f, MAX_ZOOM = 1f;

	/**
	 * Creates a new instance of a ComicStage and its comic to the one given.
	 * @param c The comic to use
	 * @return A new ComicStage
	 */
	public static ComicStage newInstance(Comic c) {
		LayoutInflater li = (LayoutInflater) CyanideViewer.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ComicStage cs = (ComicStage) li.inflate(R.layout.comic_stage, null);
		cs.setComic(c);
		return cs;
	}

	/** The comic that used to fill out this ComicStage */
	private Comic comic;

	private Matrix matrix;
	private Matrix savedMatrix;
	private ZoomState mode;
	private PointF start;
	private PointF mid;
	private float oldDist;

	/** Instantiates a new ComicStage */
	public ComicStage(Context context, AttributeSet attrs) {
		super(context, attrs);

		this.matrix = new Matrix();
		this.savedMatrix = new Matrix();
		this.mode = ZoomState.NONE;

		this.start = new PointF();
		this.mid = new PointF();
		this.oldDist = 1f;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		findViewById(R.id.image_view).setOnTouchListener(this);
	}

	/** Sets the comic of this ComicStage. Will adjust the TextView and ImageView accordingly. */
	public void setComic(Comic comic) {
		this.comic = comic;
		((TextView) findViewById(R.id.comic_id)).setText("#" + comic.getId());
		new BitmapLoaderTask().execute(this.comic);
	}

	/** Gets the Comic associated with this ComicStage */
	public Comic getComic() {
		return comic;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		ImageView view = (ImageView) v;
		view.setScaleType(ImageView.ScaleType.MATRIX);
		float scale;

		dumpEvent(event);

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			// First finger down only
			case MotionEvent.ACTION_DOWN:
				savedMatrix.set(matrix);
				start.set(event.getX(), event.getY());
				Log.d(CyanideViewer.TAG, "mode=DRAG"); // write to LogCat
				mode = ZoomState.DRAG;
				break;
			case MotionEvent.ACTION_UP:
				// First finger lifted
			case MotionEvent.ACTION_POINTER_UP:
				// Second finger lifted
				mode = ZoomState.NONE;
				Log.d(CyanideViewer.TAG, "mode=NONE");
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				// First and second finger down
				oldDist = spacing(event);
				Log.d(CyanideViewer.TAG, "oldDist=" + oldDist);
				if (oldDist > 5f) {
					savedMatrix.set(matrix);
					midPoint(mid, event);
					mode = ZoomState.ZOOM;
					Log.d(CyanideViewer.TAG, "mode=ZOOM");
				}
				break;

			case MotionEvent.ACTION_MOVE:
				if (mode == ZoomState.DRAG) {
					matrix.set(savedMatrix);
					matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); // create the transformation in the matrix  of points
				} else if (mode == ZoomState.ZOOM) {
					// pinch zooming
					float newDist = spacing(event);
					Log.d(CyanideViewer.TAG, "newDist=" + newDist);
					if (newDist > 5f) {
						matrix.set(savedMatrix);
						scale = newDist / oldDist;
						// If scale > 1: zoom in; < 1: zoom out
						matrix.postScale(scale, scale, mid.x, mid.y);
					}
				}
				break;
		}

		view.setImageMatrix(matrix); // display the transformation on screen

		return true; // indicate event was handled
	}

	/**
	 * Gets the spacing between the two fingers on a touch event
	 * @param event The event to evaluate
	 * @return The disatnce between the two fingers
	 */
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	/**
	 * Calculates the midpoint of the two fingers using a MotionEvent and the midpoint formula.
	 * @param point The point to update
	 * @param event The event to use
	 */
	private void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	/** Writes a string representation of a MotionEvent to Logcat */
	private void dumpEvent(MotionEvent event) {
		String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE","POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;
		sb.append("event ACTION_").append(names[actionCode]);

		if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP) {
			sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			sb.append(")");
		}

		sb.append("[");
		for (int i = 0; i < event.getPointerCount(); i++) {
			sb.append("#").append(i);
			sb.append("(pid ").append(event.getPointerId(i));
			sb.append(")=").append((int) event.getX(i));
			sb.append(",").append((int) event.getY(i));
			if (i + 1 < event.getPointerCount())
				sb.append(";");
		}

		sb.append("]");
		Log.d(CyanideViewer.TAG, sb.toString());
	}

	/** Represents the task of loading a Comic's URL into a Bitmap usable by an ImageView */
	public class BitmapLoaderTask extends AsyncTask<Comic, Void, Bitmap> {

		@Override
		protected void onPreExecute() {
			// TODO Show indeterminate progress bar/circle
			TextView tv = (TextView) findViewById(R.id.comic_id);
			tv.setText("#" + getComic().getId());
		}

		@Override
		protected Bitmap doInBackground(Comic... params) {
			Comic c = params[0];

			// Adapted from http://stackoverflow.com/a/6621552/1275092

			try {
				if (c.getUrl().getProtocol().equals("file")) {
					// Local file, no need to make any HTTP requests
					return BitmapFactory.decodeFile(URLDecoder.decode(c.getUrl().getPath(), "UTF-8"));
				}

				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet(c.getUrl().toURI());
				HttpResponse response = client.execute(request);
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					Log.w(CyanideViewer.TAG, "Failed to fetch comic at " + c.getUrl().toExternalForm());
					return null;
				}
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					InputStream is = null;
					try {
						is = entity.getContent();
						return BitmapFactory.decodeStream(is);
					} finally {
						if (is != null) {
							is.close();
						}
						entity.consumeContent();
					}
				}

			} catch (URISyntaxException e) {
				Log.e(CyanideViewer.TAG, "URISyntaxException: " + c.getUrl(), e);
				return null;
			} catch (IOException e) {
				Log.e(CyanideViewer.TAG, "IOException while trying to decode the image from URL " + c.getUrl(), e);
				return null;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				ImageView imageView = (ImageView) findViewById(R.id.image_view);
				imageView.setImageBitmap(bitmap);
				ProgressBar pb = (ProgressBar) findViewById(R.id.progress_bar);
				// Remove the progress bar once the image is done loading
				((RelativeLayout) pb.getParent()).removeView(pb);
			}
		}
	}

	private enum ZoomState {
		NONE,
		DRAG,
		ZOOM
	}
}
