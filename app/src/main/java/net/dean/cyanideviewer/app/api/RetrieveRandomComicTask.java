package net.dean.cyanideviewer.app.api;

/**
 * Represents a task to retrieve a random comic
 */
public class RetrieveRandomComicTask extends AbstractComicTask<Void> {

	/** Instantiates a new RetrieveRandomComicTask */
	public RetrieveRandomComicTask() {
		super();
	}

	@Override
	protected Comic doInBackground(Void... params) {
		return CyanideApi.getRandom();
	}
}
