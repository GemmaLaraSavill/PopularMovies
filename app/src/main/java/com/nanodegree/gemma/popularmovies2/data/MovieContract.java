package com.nanodegree.gemma.popularmovies2.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import com.nanodegree.gemma.popularmovies2.MainActivity;

/**
 * Created by Gemma S. Lara Savill on 02/10/2015.
 *
 * Defines table and column names for the database
 * Has Uri builder functions for communicating with the data provider
 */
public class MovieContract {

    public static final String LOG_TAG = MainActivity.LOG_TAG+" Contract";

    // The "Content authority" is a name for the entire content provider
    // I am using the package name for the app, this will always be unique on the device
    public static final String CONTENT_AUTHORITY = "com.nanodegree.gemma.popularmovies2";

    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_MOVIE = "movies";

    // Path to order queries
    public static final String PATH_ORDER_POPULARITY = "popularity";
    public static final String PATH_ORDER_VOTE_AVERAGE = "vote_average";

    /**
     * Defines the columns of the movie table
     */
    public static final class MoviesColumns implements BaseColumns {

        // types of uris I will resolve:  a collection and an item (list of movies, one movie)
        public static final String COLLECTION_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MOVIE;
        public static final String ONE_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_MOVIE;

        // Table names
        public static final String TABLE_NAME_MOVIES = "movies";
        public static final String TABLE_NAME_MOST_POPULAR = "movies_most_popular";
        public static final String TABLE_NAME_HIGHEST_RATED = "movies_highest_rated";
        public static final String TABLE_NAME_REVIEWS = "reviews";
        public static final String TABLE_NAME_TRAILERS = "trailers";

        // Table columns for table movies
        public static final String COL_ID = "_id";                         // integer, autoincrement
        public static final String COL_MOVIE_ID = "movie_id";             // primary key, integer
        public static final String COL_TITLE = "title";                   // text not null
        public static final String COL_POSTER = "poster";                 // text not null
        public static final String COL_OVERVIEW = "overview";             // text not null
        public static final String COL_RATING = "rating";                 // real not null
        public static final String COL_RELEASE_DATE = "release_date";     // integer not null
        public static final String COL_FAVORITE = "favorite";             // integer 0 not favorite 1 yes favorite

        // Table columns for table reviews
        public static final String COL_REVIEW_ID = "review_id";             // text not null
        public static final String COL_REVIEW_AUTHOR = "author";                   // text not null
        public static final String COL_REVIEW_CONTENT = "content";                 // text not null

        // Table columns for table trailers
        public static final String COL_TRAILER_ID = "trailer_id";             // text not null
        public static final String COL_TRAILER_KEY = "key";                   // text not null
        public static final String COL_TRAILER_SITE = "site";                 // text not null
        public static final String COL_TRAILER_NAME = "name";                 // text not null

        public static Uri buildMoviesUri() {
            return BASE_CONTENT_URI;
        }

        /**
         * Build the URI for queries depending on selected order
         * @param order
         * @return
         */
        public static Uri buildMoviesOrderByUri(String order) {
            Uri uri = null;
            if (order.equals("popularity")) {
                uri = BASE_CONTENT_URI.buildUpon().appendPath(TABLE_NAME_MOST_POPULAR).build();
            } else if (order.equals("vote_average")) {
                uri = BASE_CONTENT_URI.buildUpon().appendPath(TABLE_NAME_HIGHEST_RATED).build();
            } else if (order.equals("favorite")) {
                uri = BASE_CONTENT_URI.buildUpon().appendPath(TABLE_NAME_MOVIES).appendPath(COL_FAVORITE).build();
            }
            return uri;
        }

        /**
         * Build the URI for query by movie ID
         * @param movieID
         * @return
         */
        public static Uri buildMovieByIdUri(String movieID) {
            Uri uri = BASE_CONTENT_URI.buildUpon().appendPath(MoviesColumns.TABLE_NAME_MOVIES).appendPath(movieID).build();
//            Log.v(LOG_TAG, "buildMovieById("+movieID+") = "+uri);
            return uri;
        }

        /**
         * Build the URI for the query that will produce this movies trailers
         * @param movieID
         * @return
         */
        public static Uri buildMovieTrailersUri(String movieID) {
            Uri uri = BASE_CONTENT_URI.buildUpon().appendPath(MoviesColumns.TABLE_NAME_TRAILERS).appendPath(movieID).build();
//            Log.v(LOG_TAG, "buildMovieTrailersUri("+movieID+") = "+uri);
            return uri;
        }

        /**
         * Build the URI for the query that will produce this movies reviews
         * @param movieID
         * @return
         */
        public static Uri buildMovieReviewsUri(String movieID) {
            Uri uri = BASE_CONTENT_URI.buildUpon().appendPath(MoviesColumns.TABLE_NAME_REVIEWS).appendPath(movieID).build();
//            Log.v(LOG_TAG, "buildMovieReviewsUri("+movieID+") = "+uri);
            return uri;
        }

        /**
         * Builds the URI that wil toggle the movie as favorite
         * @param movieID
         * @return
         */
        public static Uri buildMovieToggleFavoriteUri(String movieID, String newValue) {
            Uri uri = BASE_CONTENT_URI.buildUpon().appendPath(MoviesColumns.TABLE_NAME_MOVIES).appendPath(MoviesColumns.COL_FAVORITE).appendPath(movieID).appendPath(newValue).build();
//            Log.v(LOG_TAG, "buildMovieToggleFavoriteUri(" + movieID + ") = " + uri);
            return uri;
        }

        /**
         * Extract movie id from uri format
         * content:// autority /movies/123
         * @param uri
         * @return
         */
        public static String getMovieIdFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Extract movie id from uri format
         * content:// autority /reviews/123
         * @param uri
         * @return
         */
        public static String getMovieIdFromReviewsUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Extract movie id from uri format
         * content:// autority /trailers/123
         * @param uri
         * @return
         */
        public static String getMovieIdFromTrailersUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }



        /**
         * Builds the URI that wil delete the not favorite movies from the database during the data refresh
         * @return
         */
        public static Uri buildDeleteNonFavMoviesUri() {
            Uri uri = BASE_CONTENT_URI.buildUpon().appendPath(MoviesColumns.TABLE_NAME_MOVIES).appendPath("notfavorite").build();
//            Log.v(LOG_TAG, "buildDeleteNonFavMoviesUri() = " + uri);
            return uri;
        }
    }

}
