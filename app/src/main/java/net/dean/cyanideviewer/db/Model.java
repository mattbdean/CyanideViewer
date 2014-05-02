package net.dean.cyanideviewer.db;

import android.util.Log;

import net.dean.cyanideviewer.Constants;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Model {
	@DatabaseField(columnName = "id")
	protected long id;

	public Model(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Map<String, Object> getDatabaseFields() {
		Map<String, Object> fields = new HashMap<>();

		Class clazz = getClass();

		// Iterate through all the class' and its superclass' fields
		while (clazz != null) {
			for (Field f : clazz.getDeclaredFields()) {
				boolean accessible = f.isAccessible();
				// Set it accessible if it's not
				if (!accessible) {
					f.setAccessible(true);
				}

				if (f.isAnnotationPresent(DatabaseField.class)) {
					try {
						fields.put(f.getAnnotation(DatabaseField.class).columnName(), f.get(this));
					} catch (IllegalAccessException e) {
						Log.e(Constants.TAG, "Unable to access field " + f.getName() + " in class " + f.getClass().getName());
					}

				}

				// Return it to it's previous state
				if (!accessible) {
					f.setAccessible(false);
				}
			}
			clazz = clazz.getSuperclass();
		}

		return fields;
	}

	public Map<String, Object> getDatabaseFieldsUpdate() {
		Map<String, Object> fields = new HashMap<>();

		for (Map.Entry<String, String> entry : Model.getDatabaseFieldNamesUpdate(getClass()).entrySet()) {
			String fieldName = entry.getKey();
			Field field = null;
			try {
				field = getClass().getDeclaredField(fieldName);
				boolean accessible = field.isAccessible();
				// Set it accessible if it's not
				if (!accessible) {
					field.setAccessible(true);
				}

				if (field.isAnnotationPresent(DatabaseField.class)) {
					// put(field.name, field.value)
					fields.put(field.getAnnotation(DatabaseField.class).columnName(), field.get(this));
				}
				// Return it to it's previous state
				if (!accessible) {
					field.setAccessible(false);
				}
			} catch (IllegalAccessException e) {
				Log.e(Constants.TAG, "Unable to access field " + field.getName() + " in class " + field.getClass().getName());
			} catch (NoSuchFieldException e) {
				Log.e(Constants.TAG, "No such field " + getClass().getName() + "." + fieldName + " (this really shouldn't happen)", e);
			}
		}

		return fields;
	}

	public static List<String> getDatabaseFieldNames(Class<? extends Model> modelClass) {
		List<String> fields = new ArrayList<>();

		for (Field field : modelClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(DatabaseField.class)) {
				fields.add(field.getAnnotation(DatabaseField.class).columnName());
			}
		}

		// Remove "id" if it exists
		if (fields.contains("id")) {
			fields.remove("id");
		}

		// Sort the normal keys alphabetically
		Collections.sort(fields);

		// Add the "id" key back
		fields.add(0, "id");

		return fields;
	}

	/**
	 * Returns a map relating the field name (key) to the database column name (value)
	 * @param modelClass The class to observe
	 * @return A map of fields
	 */
	public static Map<String, String> getDatabaseFieldNamesUpdate(Class<? extends Model> modelClass) {
		Map<String, String> fields = new HashMap<>();

		for (Field field : modelClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(DatabaseField.class)) {
				DatabaseField dbField = field.getAnnotation(DatabaseField.class);
				if (dbField.doesUpdate()) {
					fields.put(field.getName(), field.getAnnotation(DatabaseField.class).columnName());
				}
			}
		}

		return fields;
	}
}
