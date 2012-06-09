//package org.thialfihar.android.apg.provider.blob;
//
//import android.net.Uri;
//import android.provider.BaseColumns;
//
//public class BlobContract {
//
//    interface BlobColumns {
//        String DATA = "data";
//    }
//
//    public static final String CONTENT_AUTHORITY = "org.thialfihar.android.apg";
//
//    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
//
//    public static final String PATH_BLOB = "blob";
//
//    public static class Blobs implements BlobColumns, BaseColumns {
//        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BLOB)
//                .build();
//
//        /** Use if multiple items get returned */
//        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.apg.blob";
//
//        /** Use if a single item is returned */
//        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.apg.blob";
//
//        /** Default "ORDER BY" clause. */
//        public static final String DEFAULT_SORT = BaseColumns._ID + " ASC";
//
//        public static Uri buildUri(String id) {
//            return CONTENT_URI.buildUpon().appendPath(id).build();
//        }
//
//        public static String getId(Uri uri) {
//            return uri.getLastPathSegment();
//        }
//    }
//
//    private BlobContract() {
//    }
//}
