package com.example.syncrise;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SleepLogProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.syncrise.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/logs");

    public static final String TABLE_NAME = "sleep_logs";
    public static final String COLUMN_DATE = "DATE";
    public static final String COLUMN_CYCLES_COMPLETED = "CYCLES_COMPLETED";
    public static final String COLUMN_SCORE = "SCORE";

    private static final int LOGS = 1;
    private static final int LOG_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "logs", LOGS);
        uriMatcher.addURI(AUTHORITY, "logs/#", LOG_ID);
    }

    private SleepDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new SleepDatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        switch (uriMatcher.match(uri)) {
            case LOGS:
                cursor = db.query(TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case LOG_ID:
                String id = uri.getLastPathSegment();
                String specificSelection = BaseColumns._ID + " = ?";
                String[] specificSelectionArgs = new String[]{id};
                cursor = db.query(TABLE_NAME, projection, specificSelection, specificSelectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case LOGS:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + ".logs";
            case LOG_ID:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + ".logs";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) != LOGS) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(TABLE_NAME, null, values);

        if (id > 0) {
            Uri resultUri = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(resultUri, null);
            return resultUri;
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted;

        switch (uriMatcher.match(uri)) {
            case LOGS:
                rowsDeleted = db.delete(TABLE_NAME, selection, selectionArgs);
                break;
            case LOG_ID:
                String id = uri.getLastPathSegment();
                String specificSelection = BaseColumns._ID + " = ?";
                String[] specificSelectionArgs = new String[]{id};
                rowsDeleted = db.delete(TABLE_NAME, specificSelection, specificSelectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated;

        switch (uriMatcher.match(uri)) {
            case LOGS:
                rowsUpdated = db.update(TABLE_NAME, values, selection, selectionArgs);
                break;
            case LOG_ID:
                String id = uri.getLastPathSegment();
                String specificSelection = BaseColumns._ID + " = ?";
                String[] specificSelectionArgs = new String[]{id};
                rowsUpdated = db.update(TABLE_NAME, values, specificSelection, specificSelectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsUpdated > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    private static class SleepDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "syncrise.db";
        private static final int DATABASE_VERSION = 1;

        public SleepDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                    BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DATE + " TEXT NOT NULL, " +
                    COLUMN_CYCLES_COMPLETED + " INTEGER NOT NULL, " +
                    COLUMN_SCORE + " INTEGER NOT NULL" +
                    ");";
            db.execSQL(SQL_CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }
}