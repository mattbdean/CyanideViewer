package net.dean.cyanideviewer.app.api;

/**
 * Created by matthew on 3/18/14.
 */
public class RetrievePreviousComicTask extends AbstractComicTask<Long> {

	@Override
	protected Comic doInBackground(Long... params) {
		return CyanideApi.getPrevious(params[0]);
	}
}
