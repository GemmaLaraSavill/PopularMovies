package com.nanodegree.gemma.popularmovies2.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.nanodegree.gemma.popularmovies2.MainActivity;

/**
 * Created by Gemma S. Lara Savill on 02/10/2015.
 *
 * Middleman between the SQL database and the content resolver
 */
public class MovieProvider extends ContentProvider {

    private final String LOG_TAG = MainActivity.LOG_TAG+" Provider";
    private boolean debug = false;

    // database helper
    private MovieDbHelper mDbHelper;

    // constants for every type of query
    static final int MOVIES_HIGHEST_RATED = 1;      // content://authority/movies_highest_rated
    static final int MOVIES_MOST_POPULAR = 2;       // content://authority/movies_most_popular
    static final int MOVIES_FAVORITE = 3;           // content://authority/movies/favorite
    static final int MOVIE_TRAILERS = 4;            // content://authority/trailers/#
    static final int MOVIES_REVIEWS = 5;            // content://authority/reviews/#
    static final int MOVIE_BY_ID = 6;               // content://authority/movies/#
    static final int MOVIE_TOGGLE_FAVORITE = 7;     // content://authority/movies/favorite/#
    static final int MOVIE_DELETE_NOTFAVORITE = 8;  // content://authority/movies/nonfavorite


    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    /**
     * Uses a simple expression syntax to help us match uri's for a ContentProvider
     * "PATH" matches "PATH" exactly
     * "PATH/#" = "PATH" followed by a NUMBER
     * "PATH/*" = "PATH" followed by a STRING
     * You can concat these:
     * "PATH/#/OTHER/*" = "PATH" followed by a NUMBER followed by "OTHER" followed by a STRING
     * @return
     */
    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        // highest rated movies
        matcher.addURI(MovieContract.CONTENT_AUTHORITY, "movies_highest_rated", MOVIES_HIGHEST_RATED);
        // most popular movies
        matcher.addURI(MovieContract.CONTENT_AUTHORITY, "movies_most_popular", MOVIES_MOST_POPULAR);
        // favorite movies
        matcher.addURI(MovieContract.CONTENT_AUTHORITY, "movies/favorite", MOVIES_FAVORITE);
        // trailers of a movie
        matcher.addURI(MovieContract.CONTENT_AUTHORITY, "trailers/#", MOVIE_TRAILERS);
        // reviews of a movie
        matcher.addURI(MovieContract.CONTENT_AUTHORITY, "reviews/#", MOVIES_REVIEWS);
        // a movie by it's id
        matcher.addURI(MovieContract.CONTENT_AUTHORITY, "movies/#", MOVIE_BY_ID);
        // movie toggle favorite
        matcher.addURI(MovieContract.CONTENT_AUTHORITY, "movies/favorite/#/#", MOVIE_TOGGLE_FAVORITE);
        // movie delete non-favorites
        matcher.addURI(MovieContract.CONTENT_AUTHORITY, "movies/notfavorite", MOVIE_DELETE_NOTFAVORITE);
        return matcher;
    }


    /**
     * Create the db helper
     * @return
     */
    @Override
    public boolean onCreate() {
        mDbHelper = new MovieDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (debug) {
        Log.v(LOG_TAG, "Movie provider query with uri "+uri);
        }

        // open the database for reading only
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // find which operation through the uri
        final int match = sUriMatcher.match(uri);

        // Maybe setup the movie selection with the order already, another URIs
        Cursor retCursor;
        switch (match) {
            case MOVIES_HIGHEST_RATED:
                // SELECT * FROM movies INNER JOIN movies_most_popular ON movies.movie_id = movies_most_popular.movie_id
                String highestRatedTableJoin = MovieContract.MoviesColumns.TABLE_NAME_MOVIES
                        +" INNER JOIN "+ MovieContract.MoviesColumns.TABLE_NAME_HIGHEST_RATED
                        +" ON "+MovieContract.MoviesColumns.TABLE_NAME_MOVIES+"."+MovieContract.MoviesColumns.COL_MOVIE_ID
                        +" = "
                        +MovieContract.MoviesColumns.TABLE_NAME_HIGHEST_RATED+"."+MovieContract.MoviesColumns.COL_MOVIE_ID;
                if (debug) { Log.v(LOG_TAG, "Movies most popular table join: "+highestRatedTableJoin); }
                sortOrder = MovieContract.MoviesColumns.TABLE_NAME_HIGHEST_RATED+"."+MovieContract.MoviesColumns.COL_ID + " ASC";
                retCursor = db.query(
                        highestRatedTableJoin,
                        null,
                        null,
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;
             case MOVIES_MOST_POPULAR:
                // SELECT * FROM movies INNER JOIN movies_most_popular ON movies.movie_id = movies_most_popular.movie_id
                String mostPopularTableJoin = MovieContract.MoviesColumns.TABLE_NAME_MOVIES
                        +" INNER JOIN "+ MovieContract.MoviesColumns.TABLE_NAME_MOST_POPULAR
                        +" ON "+MovieContract.MoviesColumns.TABLE_NAME_MOVIES+"."+MovieContract.MoviesColumns.COL_MOVIE_ID
                        +" = "
                        +MovieContract.MoviesColumns.TABLE_NAME_MOST_POPULAR+"."+MovieContract.MoviesColumns.COL_MOVIE_ID;
                 if (debug) { Log.v(LOG_TAG, "Movies most popular table join: "+mostPopularTableJoin); }
                sortOrder = MovieContract.MoviesColumns.TABLE_NAME_MOST_POPULAR+"."+MovieContract.MoviesColumns.COL_ID + " ASC";
                retCursor = db.query(
                        mostPopularTableJoin,
                        null,
                        null,
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;
            case MOVIES_FAVORITE:
                // SELECT * FROM movies WHERE favorite=1
                sortOrder = MovieContract.MoviesColumns.TABLE_NAME_MOVIES+"."+MovieContract.MoviesColumns.COL_ID + " ASC";
                retCursor = db.query(
                        MovieContract.MoviesColumns.TABLE_NAME_MOVIES,  // table
                        null, // all columns
                        MovieContract.MoviesColumns.COL_FAVORITE+" = ?",   // Columns for the "where" clause
                        new String[]{"1"},  // Values for the "where" clause
                        null,
                        null,
                        sortOrder
                );
                break;
            case MOVIE_BY_ID:
                String movieId = MovieContract.MoviesColumns.getMovieIdFromUri(uri);
                if (debug) { Log.v(LOG_TAG, "Movie id received: "+movieId); }
                retCursor = db.query(
                        MovieContract.MoviesColumns.TABLE_NAME_MOVIES,      // table
                        null,                                               // all columns
                        MovieContract.MoviesColumns.COL_MOVIE_ID+" = ?",    // Columns for the "where" clause
                        new String[]{movieId},                              // Values for the "where" clause
                        null,                                               // columns to group by
                        null,                                               // columns to filter by row groups
                        null                                                // sort order
                );

                break;
            case MOVIES_REVIEWS:
                movieId = MovieContract.MoviesColumns.getMovieIdFromUri(uri);
                if (debug) {
                    Log.v(LOG_TAG, "Query get reviews for movie: " + movieId);
                }
                retCursor = db.query(
                        MovieContract.MoviesColumns.TABLE_NAME_REVIEWS,      // table
                        null,                                               // all columns
                        MovieContract.MoviesColumns.COL_MOVIE_ID+" = ?",    // Columns for the "where" clause
                        new String[]{movieId},                              // Values for the "where" clause
                        null,                                               // columns to group by
                        null,                                               // columns to filter by row groups
                        null                                                // sort order
                );
                break;
            case MOVIE_TRAILERS:
                movieId = MovieContract.MoviesColumns.getMovieIdFromUri(uri);
                if (debug) {
                    Log.v(LOG_TAG, "Query get trailers for movie: " + movieId);
                }
                retCursor = db.query(
                        MovieContract.MoviesColumns.TABLE_NAME_TRAILERS,      // table
                        null,                                               // all columns
                        MovieContract.MoviesColumns.COL_MOVIE_ID+" = ?",    // Columns for the "where" clause
                        new String[]{movieId},                              // Values for the "where" clause
                        null,                                               // columns to group by
                        null,                                               // columns to filter by row groups
                        null                                                // sort order
                );
                break;
             default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);

            }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }



    @Nullable
    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case MOVIES_HIGHEST_RATED:
                return MovieContract.MoviesColumns.COLLECTION_TYPE; // a dir
            case MOVIES_MOST_POPULAR:
                return MovieContract.MoviesColumns.COLLECTION_TYPE; // a dir
            case MOVIES_FAVORITE:
                return MovieContract.MoviesColumns.COLLECTION_TYPE; // a dir
            case MOVIE_TRAILERS:
                return MovieContract.MoviesColumns.COLLECTION_TYPE; // a dir
            case MOVIES_REVIEWS:
                return MovieContract.MoviesColumns.COLLECTION_TYPE; // a dir
            case MOVIE_BY_ID:
                return MovieContract.MoviesColumns.ONE_ITEM_TYPE; // one item
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }


    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (debug) {
        Log.v(LOG_TAG, "Movie provider INSERT with uri "+uri);
        }

        // open the database for writing
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // find which operation through the uri
        final int match = sUriMatcher.match(uri);

        Uri returnUri = null;
        long db_id = -1;
        switch (match) {
            case MOVIES_MOST_POPULAR:
                db_id = db.insert(MovieContract.MoviesColumns.TABLE_NAME_MOVIES, null, values);
                if (db_id > 0) {
                    if (debug) {
                    Log.v(LOG_TAG, "Inserted into MOVIES_MOST_POPULAR "+db_id);
                    }
                    // now insert movie as most popular
                    ContentValues mostPopularValues = new ContentValues();
                    mostPopularValues.put(MovieContract.MoviesColumns.COL_ID, db_id);
                    mostPopularValues.put(MovieContract.MoviesColumns.COL_MOVIE_ID, values.get(MovieContract.MoviesColumns.COL_MOVIE_ID).toString());
                    long popular_id = db.insert(MovieContract.MoviesColumns.TABLE_NAME_MOST_POPULAR, null,mostPopularValues);
                    returnUri = MovieContract.MoviesColumns.buildMoviesOrderByUri(MovieContract.PATH_ORDER_POPULARITY);
                    if (debug) {
                        Log.v(LOG_TAG, "returnUri " + returnUri);
                    }
                } else {
                    Log.e(LOG_TAG, "Error inserting into database");
                    throw new android.database.SQLException("Failed to insert row into "+uri);
                }
                break;
            case MOVIES_HIGHEST_RATED:
               db_id = db.insert(MovieContract.MoviesColumns.TABLE_NAME_MOVIES, null, values);
                if (db_id > 0) {
                    if (debug) {
                    Log.v(LOG_TAG, "Inserted into MOVIES_HIGHEST_RATED "+db_id);
                    }
                    // now insert movie as highest rated
                    ContentValues highestRatedMovies = new ContentValues();
                    highestRatedMovies.put(MovieContract.MoviesColumns.COL_ID, db_id);
                    highestRatedMovies.put(MovieContract.MoviesColumns.COL_MOVIE_ID, values.get(MovieContract.MoviesColumns.COL_MOVIE_ID).toString());
                    long popular_id = db.insert(MovieContract.MoviesColumns.TABLE_NAME_HIGHEST_RATED, null,highestRatedMovies);
                    returnUri = MovieContract.MoviesColumns.buildMoviesOrderByUri(MovieContract.PATH_ORDER_VOTE_AVERAGE);
                    if (debug) {
                    Log.v(LOG_TAG, "returnUri "+returnUri);
                    }
                } else {
                    Log.e(LOG_TAG, "Error inserting into database");
                    throw new android.database.SQLException("Failed to insert row into "+uri);
                }
                break;
            case MOVIES_REVIEWS:
                // insert review
                db_id = db.insert(MovieContract.MoviesColumns.TABLE_NAME_REVIEWS, null, values);
                if (db_id > 0) {
                    returnUri = MovieContract.MoviesColumns.buildMovieReviewsUri(String.valueOf(db_id));
                    if (debug) {
                         Log.v(LOG_TAG, "returnUri "+returnUri);
                    }
                } else {
                    Log.e(LOG_TAG, "Error inserting REVIEW into database");
                    throw new android.database.SQLException("Failed to insert row into "+uri);
                }
                break;
            case MOVIE_TRAILERS:
                // insert review
                db_id = db.insert(MovieContract.MoviesColumns.TABLE_NAME_TRAILERS, null, values);
                if (db_id > 0) {
                    returnUri = MovieContract.MoviesColumns.buildMovieTrailersUri(String.valueOf(db_id));
                    if (debug) {
                         Log.v(LOG_TAG, "returnUri "+returnUri);
                    }
                } else {
                    Log.e(LOG_TAG, "Error inserting TRAILER into database");
                    throw new android.database.SQLException("Failed to insert row into "+uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs) {
        if (debug) {
        Log.v(LOG_TAG, "Movie provider UPDATE with uri "+uri);
        }
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int updatedRecords = 0;
        long db_id = -1;
        switch (match) {
            case MOVIES_MOST_POPULAR:
                db_id = db.update(MovieContract.MoviesColumns.TABLE_NAME_MOVIES, values, whereClause, whereArgs);
                if (db_id > 0) {
                    updatedRecords = (int) db_id;
                } else {
                    Log.e(LOG_TAG, "Error updating database");
                    Log.e(LOG_TAG, "values " + values.toString());
                    Log.e(LOG_TAG, "whereClause "+whereClause);
                    Log.e(LOG_TAG, "whereArgs "+whereArgs[0]);
                    throw new android.database.SQLException("Failed to update row with "+uri);
                }
                break;
            case MOVIES_HIGHEST_RATED:
                db_id = db.update(MovieContract.MoviesColumns.TABLE_NAME_MOVIES, values, whereClause, whereArgs);
                if (db_id > 0) {
                    updatedRecords = (int) db_id;
                } else {
                    Log.e(LOG_TAG, "Error updating database");
                    throw new android.database.SQLException("Failed to update row with "+uri);
                }
                break;
            case MOVIE_TOGGLE_FAVORITE:
                if (debug) {
                    Log.v(LOG_TAG, "Values " + values);
                    Log.v(LOG_TAG, "whereClause " + whereClause);
                    Log.v(LOG_TAG, "whereArgs " + whereArgs);
                }
                db_id = db.update(MovieContract.MoviesColumns.TABLE_NAME_MOVIES, values, whereClause, whereArgs);
                if (db_id > 0) {
                    updatedRecords = (int) db_id;
                } else {
                    Log.e(LOG_TAG, "Error updating database");
                    throw new android.database.SQLException("Failed to update row with "+uri);
                }
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        return updatedRecords;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (debug) {
            Log.v(LOG_TAG, "Movie provider DELETE with uri " + uri);
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.execSQL("PRAGMA foreign_keys = ON;"); // important for on delete cascade
        final int match = sUriMatcher.match(uri);
        int deletedRecords = 0;
        if (null == selection) {selection = "1"; } // this makes delete all rows return the number of rows deleted
        switch (match) {
            case MOVIE_DELETE_NOTFAVORITE:
                // delete not favorite movies from the database
                if (debug) {
                Log.v(LOG_TAG, "Deleted selection "+selection);
                Log.v(LOG_TAG, "Deleted selectionArgs "+selectionArgs[0]);
                }

                deletedRecords = db.delete(
                        MovieContract.MoviesColumns.TABLE_NAME_MOVIES,
                        selection,
                        selectionArgs);

                break;
            case MOVIE_TRAILERS:
                // delete trailers for this movie
                if (debug) {
                    Log.v(LOG_TAG, "Deleted selection " + selection);
                    Log.v(LOG_TAG, "Deleted selectionArgs " + selectionArgs[0]);
                }

                deletedRecords = db.delete(
                        MovieContract.MoviesColumns.TABLE_NAME_TRAILERS,
                        selection,
                        selectionArgs);
                break;
            case MOVIES_REVIEWS:
                // delete reviews for this movie
                if (debug) {
                    Log.v(LOG_TAG, "Deleted selection " + selection);
                    Log.v(LOG_TAG, "Deleted selectionArgs " + selectionArgs[0]);
                }

                deletedRecords = db.delete(
                        MovieContract.MoviesColumns.TABLE_NAME_REVIEWS,
                        selection,
                        selectionArgs);
                break;

        }
        if (debug) {
            Log.v(LOG_TAG, "Deleted " + deletedRecords + " from database");
        }
            if (deletedRecords > 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
            // return the actual rows deleted
            return deletedRecords;
    }



}
