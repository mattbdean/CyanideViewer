package net.dean.cyanideviewer;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class NotificationHelper {
	private static final List<NotificationHelper> REGISTERED_HELPERS = new ArrayList<>();

	private final int notifId;
	private NotificationManager manager;
	private NotificationCompat.Builder builder;

	public static NotificationHelper getInstance(Context context, int notifId) {
		for (NotificationHelper helper : REGISTERED_HELPERS) {
			// Iterate through the registered helpers to see if the notification IDs match.
			// Makes sure there are only as many helpers as there are notifications
			if (helper.notifId == notifId) {
				return helper;
			}
		}

		// The manager wasn't registered yet
		return new NotificationHelper(context, notifId);
	}

	private NotificationHelper(Context context, int notifId) {
		this.notifId = notifId;
		this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		this.builder = new NotificationCompat.Builder(context);
		builder.setSmallIcon(R.drawable.ic_notif);

		REGISTERED_HELPERS.add(this);
	}

	public void notifyManager() {
		manager.notify(notifId, builder.build());
	}

	public NotificationCompat.Builder builder() {
		return builder;
	}
}
