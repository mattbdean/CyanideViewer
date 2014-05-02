package net.dean.cyanideviewer.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import net.dean.cyanideviewer.CyanideViewer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * An abstract class that controls most of the CRUD operations on the database pertaining to a certain
 * object type that extends Model. This class uses the {@link net.dean.cyanideviewer.db.DatabaseField}
 * annotation to pair a key string to a field of a class to construct an array of table columns.
 *
 * This class has default implementations of inserting, creating, updating, and deleting rows that will
 * work for most classes out of the box. The only methods that inheriting classes are required to
 * implement is ${@link #parse(android.database.Cursor)}.
 *
 * @param <T> The type of Model that will be used
 */
public abstract class BaseDao<T extends Model> {
	/** A list of all table names being operated on by all subclasses of this class ever constructed */
	static List<String> tableNames = new ArrayList<>();

	/** The database that will be used to perform CRUD operations on */
	protected SQLiteDatabase db;

	/** The name of the table to operate on */
	protected final String tableName;

	/** The names of the columns, with the first element being 'id' */
	protected final String[] columns;

	protected final boolean writable;

	/**
	 * Instantiates a new BaseDao
	 * @param helper The helper object to obtain a writable database from
	 * @param tableName The name of the table to operate on
	 * @param modelClass The class (which extends model) that will be searched for fields with the
	 *                   {@link net.dean.cyanideviewer.db.DatabaseField} annotation
	 */
	public BaseDao(CyanideDatabaseHelper helper, String tableName, Class<T> modelClass) {
		this(helper, tableName, modelClass, true);
	}

	/**
	 * Instantiates a new BaseDao
	 * @param helper The helper object to obtain a database from
	 * @param tableName The name of the table to operate on
	 * @param modelClass The class (which extends model) that will be searched for fields with the
	 *                   {@link net.dean.cyanideviewer.db.DatabaseField} annotation
	 * @param writable Whether or not the database is writable
	 */
	public BaseDao(CyanideDatabaseHelper helper, String tableName, Class<T> modelClass, boolean writable) {
		this.db = writable ? helper.getWritableDatabase() : helper.getReadableDatabase();
		this.writable = writable;
		this.tableName = tableName;

		// List<String> to array
		List<String> fieldNames = Model.getDatabaseFieldNames(modelClass);
		this.columns = fieldNames.toArray(new String[fieldNames.size()]);

		// Record of all table names
		if (!tableNames.contains(tableName)) {
			tableNames.add(tableName);
		} else {
			Log.w(CyanideViewer.TAG, "Creating an extra BaseDao object for table \"" + tableName + "\"");
		}
	}

	/**
	 * Adds an object to the database
	 * @param model The model to add
	 * @return True if the row was inserted successfully, false if else
	 */
	public boolean add(T model) {
		if (!isValid(model)) {
			Log.w(CyanideViewer.TAG, "Model not valid: " + model);
			return false;
		}

		checkWritable();

		ContentValues values = new ContentValues();

		Map<String, Object> fields = model.getDatabaseFields();
		for (Map.Entry<String, Object> entry : fields.entrySet()) {
			addTo(entry.getKey(), entry.getValue(), values);
		}

		Log.v(CyanideViewer.TAG, "Adding model to the \"" + tableName + "\" table: " + model.toString());
		return db.insertOrThrow(tableName, null, values) != -1;
	}

	/**
	 * Determines the type of an object and adds it to a ContentValues. All object representations of
	 * primitive types (java.lang.Boolean, java.lang.Integer, etc.) are added via ContentValue's put()
	 * methods. ${@link java.util.Date} is added as a long based on it's Unix timestamp ({@link java.util.Date#getTime()}.
	 * If the object is an instance of Model, then then the value of it's ID is added as a Long.
	 * @param key The name of the column
	 * @param o The object to add
	 * @param values The ContentValues to add to
	 */
	protected void addTo(String key, Object o, ContentValues values) {
		if (o == null) {
			values.putNull(key);
			return;
		}

		switch (o.getClass().getName()) {
			case "java.lang.Boolean":
				values.put(key, (Boolean) o);
				return;
			case "java.lang.Byte":
				values.put(key, (Byte) o);
				return;
			case "java.lang.Double":
				values.put(key, (Double) o);
				return;
			case "java.lang.Float":
				values.put(key, (Float) o);
				return;
			case "java.lang.Integer":
				values.put(key, (Integer) o);
				return;
			case "java.lang.Long":
				values.put(key, (Long) o);
				return;
			case "java.lang.Short":
				values.put(key, (Short) o);
				return;
			case "java.lang.String":
				values.put(key, (String) o);
				return;
			case "java.net.URL":
				values.put(key, ((URL)o).toExternalForm());
				return;
			case "java.util.Date":
				values.put(key, ((Date)o).getTime());
				return;
		}

		// Object is a Model, use it's ID
		if (o instanceof Model) {
			Model m = (Model) o;
			values.put(key, m.getId());
			return;
		}

		Log.i(CyanideViewer.TAG, "Could not determine object type of " + o + " (key was \"" + key + "\")");
	}

	/**
	 * Gets an object with a given ID
	 * @param id The ID to find
	 * @return A model, if it exists in the table. Null if it does not.
	 */
	public T get(long id) {
		// SELECT * FROM comics WHERE id=$id
		Cursor cursor = db.query(true, tableName, columns, columns[0] + "=?",
				new String[] {Long.toString(id)}, null, null, null, null);
		cursor.moveToFirst();

		T model = parse(cursor);
		cursor.close();
		return model;
	}

	public boolean exists(long id) {
		return get(id) != null;
	}

	/**
	 * Gets a list of all models of type T in the database
	 */
	public List<T> getAll() {
		Cursor cursor = db.query(tableName, columns,
				null, null, null, null, null);
		cursor.moveToFirst();

		List<T> models = new ArrayList<>();

		while (!cursor.isAfterLast()) {
			models.add(parse(cursor));
			cursor.moveToNext();
		}
		cursor.close();

		return models;
	}

	/**
	 * Updates the values in the database to match the given object
	 * @param model The object to use
	 */
	public int update(T model) {
		checkWritable();

		ContentValues values = new ContentValues();

		Map<String, Object> fields = model.getDatabaseFieldsUpdate();
		for (Map.Entry<String, Object> entry : fields.entrySet())
			if (!entry.getKey().equals("is_favorite"))
				addTo(entry.getKey(), entry.getValue(), values);

		Log.v(CyanideViewer.TAG, "Updated model in table \"" + tableName + "\": " + model.toString());
		return db.update(tableName, values, columns[0] + "=?", new String[] {Long.toString(model.getId())});
	}

	/**
	 * Deletes a comic from the database
	 * @param id The id of the object to delete
	 * @return True if only one row was affected, false if else
	 */
	public boolean delete(long id) {
		checkWritable();

		Log.v(CyanideViewer.TAG, "Deleting model (id=" + id + ") from the \"" + tableName + "\" table.");
		return db.delete(tableName, columns[0] + "=?", new String[] {Long.toString(id)}) == 1;
	}

	/**
	 * Deletes all the objects from the database and then vacuums the database
	 */
	public void deleteAll() {
		checkWritable();
		// http://stackoverflow.com/a/6835115
		Log.v(CyanideViewer.TAG, "Deleting all records in table \"" + tableName + "\".");
		db.execSQL("DELETE FROM " + tableName);
		db.execSQL("VACUUM");
	}

	/**
	 * Parses a model given a Cursor from the table of the database
	 * @param c A new Model parsed from the values of the row at which the cursor is pointing
	 * @return A new model from a cursor
	 */
	protected abstract T parse(Cursor c);

	/**
	 * Checks if the model is valid enough to be put into the database.
	 * @param model The model to check
	 * @return True, if the model is valid. False if else.
	 */
	protected abstract boolean isValid(T model);

	protected void checkWritable() {
		if (!writable) {
			throw new UnsupportedOperationException("This database is read-only");
		}
	}

	/**
	 * Gets a list of table names that all instances of this class has used
	 */
	static List<String> getTableNames() {
		return tableNames;
	}
}
