package com.nanodegree.gemma.popularmovies2.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.HashSet;

/**
 * Created by Gemma S. Lara Savill on 07/10/2015.
 */
public class TestDatabase extends AndroidTestCase {

    public static final String LOG = "PopularMovies_TestDatabase";

    // set up test values
    private String movieId = "135397";
    private String movieTitle = "Jurassic World";
    private String poster = "/hTKME3PUzdS3ezqK5BZcytXLCUl.jpg";
    private String overview = "Twenty-two years after the events of Jurassic Park, Isla Nublar now features a fully functioning dinosaur theme park, Jurassic World, as originally envisioned by John Hammond.";
    private double rating = 7.0;
    private String release_date = "2015-06-12";

    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(MovieDbHelper.DATABASE_NAME);
    }

    public void setUp() {
        deleteTheDatabase();
    }

    public void testCreateDb() throws Throwable {
        // build a HashSet of all of the table names we wish to look for
        // Note that there will be another table in the DB that stores the
        // Android metadata (db version information)
        final HashSet<String> tableNameHashSet = new HashSet<String>();
        tableNameHashSet.add(MovieContract.MoviesColumns.TABLE_NAME_MOVIES);
        tableNameHashSet.add(MovieContract.MoviesColumns.TABLE_NAME_HIGHEST_RATED);
        tableNameHashSet.add(MovieContract.MoviesColumns.TABLE_NAME_MOST_POPULAR);
        tableNameHashSet.add(MovieContract.MoviesColumns.TABLE_NAME_REVIEWS);
        tableNameHashSet.add(MovieContract.MoviesColumns.TABLE_NAME_TRAILERS);


        mContext.deleteDatabase(MovieDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new MovieDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that the tables haven't been created
        assertTrue("Error: Your database was created all the tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        // movie table column check
        c = db.rawQuery("PRAGMA table_info(" + MovieContract.MoviesColumns.TABLE_NAME_MOVIES + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for in table movies
        final HashSet<String> moviesColumnHashSet = new HashSet<String>();
        moviesColumnHashSet.add(MovieContract.MoviesColumns.COL_ID);
        moviesColumnHashSet.add(MovieContract.MoviesColumns.COL_MOVIE_ID);
        moviesColumnHashSet.add(MovieContract.MoviesColumns.COL_TITLE);
        moviesColumnHashSet.add(MovieContract.MoviesColumns.COL_POSTER);
        moviesColumnHashSet.add(MovieContract.MoviesColumns.COL_OVERVIEW);
        moviesColumnHashSet.add(MovieContract.MoviesColumns.COL_RATING);
        moviesColumnHashSet.add(MovieContract.MoviesColumns.COL_RELEASE_DATE);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            moviesColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your table doesn't contain all of the required columns
        assertTrue("Error: The table doesn't contain all of the required columns",
                moviesColumnHashSet.isEmpty());
        // end of movie table column check

        // movies_most_popular table column check
        c = db.rawQuery("PRAGMA table_info(" + MovieContract.MoviesColumns.TABLE_NAME_MOST_POPULAR + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for in table movies
        final HashSet<String> mostPopularColumnHashSet = new HashSet<String>();
        mostPopularColumnHashSet.add(MovieContract.MoviesColumns.COL_ID);
        mostPopularColumnHashSet.add(MovieContract.MoviesColumns.COL_MOVIE_ID);

        columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            mostPopularColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your table doesn't contain all of the required columns
        assertTrue("Error: The table doesn't contain all of the required columns",
                mostPopularColumnHashSet.isEmpty());
        // end of movies_most_popular table column check

        // movies_highest_rated table column check
        c = db.rawQuery("PRAGMA table_info(" + MovieContract.MoviesColumns.TABLE_NAME_HIGHEST_RATED + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for in table movies
        final HashSet<String> highestRatedColumnHashSet = new HashSet<String>();
        highestRatedColumnHashSet.add(MovieContract.MoviesColumns.COL_ID);
        highestRatedColumnHashSet.add(MovieContract.MoviesColumns.COL_MOVIE_ID);

        columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            highestRatedColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your table doesn't contain all of the required columns
        assertTrue("Error: The table doesn't contain all of the required columns",
                highestRatedColumnHashSet.isEmpty());
        // end of movies_highest_rated table column check
        db.close();
    }

    /**
     * Test inserting a movie in the movie table
     */
    public void testMoviesTable() {
        MovieDbHelper dbHelper = new MovieDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // insert and get row id
        long insertedId = insertMovieInDB(db);

        // make sure it has been inserted
        assertTrue(insertedId != -1);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // Query the database and receive a Cursor back
        Cursor cursor = db.query(
                MovieContract.MoviesColumns.TABLE_NAME_MOVIES,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );

        // Move the cursor to a valid database row and check to see if we got any records back
        // from the query
        assertTrue("Error: No Records returned from query", cursor.moveToFirst());

        // Validate data in resulting Cursor with the original test data
        String returnedMovieId = cursor.getString(cursor.getColumnIndex(MovieContract.MoviesColumns.COL_MOVIE_ID));
        assertEquals("Value movie_id " + movieId + " did not match expected " + returnedMovieId, movieId, returnedMovieId);

        String returnedMovieTitle = cursor.getString(cursor.getColumnIndex(MovieContract.MoviesColumns.COL_TITLE));
        assertEquals("Value movie_id "+movieTitle+" did not match expected "+returnedMovieTitle, movieTitle, returnedMovieTitle);

        String returnedMoviePoster = cursor.getString(cursor.getColumnIndex(MovieContract.MoviesColumns.COL_POSTER));
        assertEquals("Value movie_id "+poster+" did not match expected "+returnedMoviePoster, poster, returnedMoviePoster);

        String returnedMovieOverview = cursor.getString(cursor.getColumnIndex(MovieContract.MoviesColumns.COL_OVERVIEW));
        assertEquals("Value movie_id "+overview+" did not match expected "+returnedMovieOverview, overview, returnedMovieOverview);

        double returnedMovieRating = cursor.getDouble(cursor.getColumnIndex(MovieContract.MoviesColumns.COL_RATING));
        assertEquals("Value movie_id "+rating+" did not match expected "+returnedMovieRating, rating, returnedMovieRating);

        String returnedMovieReleaseDate = cursor.getString(cursor.getColumnIndex(MovieContract.MoviesColumns.COL_RELEASE_DATE));
        assertEquals("Value movie_id "+release_date+" did not match expected "+returnedMovieReleaseDate, release_date, returnedMovieReleaseDate);

        // Move the cursor to demonstrate that there is only one record in the database
        assertFalse("Error: More than one record returned from inserting movie query", cursor.moveToNext());

        // close the cursor and database
        cursor.close();
        db.close();
    }

    /**
     * Inserts a movie in the Database
     */
    private long insertMovieInDB(SQLiteDatabase db) {

        ContentValues movieValues = new ContentValues();
        movieValues.put(MovieContract.MoviesColumns.COL_MOVIE_ID, movieId);
        movieValues.put(MovieContract.MoviesColumns.COL_TITLE, movieTitle);
        movieValues.put(MovieContract.MoviesColumns.COL_POSTER, poster);
        movieValues.put(MovieContract.MoviesColumns.COL_OVERVIEW, overview);
        movieValues.put(MovieContract.MoviesColumns.COL_RATING, rating);
        movieValues.put(MovieContract.MoviesColumns.COL_RELEASE_DATE, release_date);

        // insert and get row id
        long insertedId;
        insertedId = db.insert(MovieContract.MoviesColumns.TABLE_NAME_MOVIES, null, movieValues);
        return insertedId;

    }

    /**
     * Test deleting non-favorite movies from the database
     */
    public void testDeletingMovies() {

        MovieDbHelper dbHelper = new MovieDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("PRAGMA foreign_keys = ON;"); // important for on delete cascade

        // insert and get row id
        long insertedId = insertMovieInDB(db);
        // make sure it has been inserted
        assertTrue(insertedId != -1);
        if (insertedId > 0) {
            // now insert movie as most popular
            ContentValues mostPopularValues = new ContentValues();
            mostPopularValues.put(MovieContract.MoviesColumns.COL_ID, insertedId);
            mostPopularValues.put(MovieContract.MoviesColumns.COL_MOVIE_ID, movieId);
            long popular_id = db.insert(MovieContract.MoviesColumns.TABLE_NAME_MOST_POPULAR, null,mostPopularValues);
            // make sure it has been inserted
            assertTrue(popular_id != -1);
        }

        // see how many movies I have in TABLE_NAME_MOVIES
        // Query the database and receive a Cursor back
        Cursor cursor = db.query(
                MovieContract.MoviesColumns.TABLE_NAME_MOVIES,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );
        int howManyInMoviesTable = cursor.getCount();
        System.out.println("I have "+howManyInMoviesTable+" movies in MOVIES table");
        cursor.close();

        // delete from tables movie, deletes from all other movies linked with external key with on delete cascade
        String mSelectionClause = MovieContract.MoviesColumns.COL_FAVORITE +" = ?";
        String[] mSelectionArgs = {"0"};
        int howManyDeleted = db.delete(MovieContract.MoviesColumns.TABLE_NAME_MOVIES, mSelectionClause, mSelectionArgs);
        assertEquals("Deleted more " + howManyDeleted + " movie from Movies table out of " + howManyInMoviesTable, howManyInMoviesTable, howManyDeleted);

        // see how many movies I have in TABLE_NAME_MOST_POPULAR
        cursor = db.query(
                MovieContract.MoviesColumns.TABLE_NAME_MOST_POPULAR,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null, // columns to group by
                null, // columns to filter by row groups
                null // sort order
        );
        int howManyInMostPopularTable = cursor.getCount();
        System.out.println("I have "+howManyInMostPopularTable+" movies in MOST_POPULAR table");
        cursor.close();

        assertEquals("Deleted also from MOST POPULAR table", 0, howManyInMostPopularTable);
        db.close();
    }



}
