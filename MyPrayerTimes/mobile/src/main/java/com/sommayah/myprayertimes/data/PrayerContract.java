package com.sommayah.myprayertimes.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by sommayahsoliman on 3/11/16.
 */
public class PrayerContract {
    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.sommayah.myprayertimes";
    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    // Possible paths (appended to base content URI for possible URI's)
    public static final String PATH_PRAYER = "prayer";

    public static final class PrayerEntry implements BaseColumns{

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PRAYER).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRAYER;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRAYER;
        public static final String TABLE_NAME = "prayer";
        //columns in our table
        public static final String COLUMN_PRAYERNAME = "name";
        public static final String COLUMN_PRAYERTIME = "time";

        public static Uri buildPrayerUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildPrayerWithNameUri(String name){
            return CONTENT_URI.buildUpon().appendPath(name).build();
        }

        public static String getPrayerFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

    }


}
