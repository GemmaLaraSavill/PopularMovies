package com.nanodegree.gemma.popularmovies2.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.nanodegree.gemma.popularmovies2.MainActivity;

/**
 * Created by Gemma S. Lara Savill on 02/10/2015.
 *
 * Database that will store the movie data downloaded from the
 * themoviedb.org server
 * This is so that the user always has data available even if
 * there is no internet connection
 */
public class MovieDbHelper extends SQLiteOpenHelper {

    private final String LOG_TAG = MainActivity.LOG_TAG+" DB";

    public static final String DATABASE_NAME = "movies.db";
    private static final int DATABASE_VERSION = 1;

    public MovieDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // create table movies
        final String SQL_CREATE_MOVIES_TABLE = "CREATE TABLE " + MovieContract.MoviesColumns.TABLE_NAME_MOVIES+ " ("+
                MovieContract.MoviesColumns.COL_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, " +
                MovieContract.MoviesColumns.COL_MOVIE_ID + " TEXT NOT NULL UNIQUE, " +
                MovieContract.MoviesColumns.COL_TITLE +" TEXT NOT NULL, " +
                MovieContract.MoviesColumns.COL_POSTER +" TEXT NOT NULL, " +
                MovieContract.MoviesColumns.COL_OVERVIEW +" TEXT NOT NULL, " +
                MovieContract.MoviesColumns.COL_RATING +" REAL NOT NULL, " +
                MovieContract.MoviesColumns.COL_RELEASE_DATE +" INTEGER, " +
                MovieContract.MoviesColumns.COL_FAVORITE +"	INTEGER NOT NULL DEFAULT 0" +
        ");";
        Log.v(LOG_TAG, SQL_CREATE_MOVIES_TABLE);
        db.execSQL(SQL_CREATE_MOVIES_TABLE);

        // using movie_id as foreign key with on delete cascade constraint on all other tables
        // this ties the information in the other tables to a movie_id
        // if I delete the movie from the movies table, where all the other foreign keys reference to
        // it will delete the rows with this movie_id in all other tables

        // create table movies_highest_rated
        final String SQL_CREATE_MOST_POPULAR_MOVIE_TABLE = "CREATE TABLE " + MovieContract.MoviesColumns.TABLE_NAME_MOST_POPULAR+ " ("+
                MovieContract.MoviesColumns.COL_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, " +
                MovieContract.MoviesColumns.COL_MOVIE_ID + " TEXT NOT NULL UNIQUE, " +
                " FOREIGN KEY ('"+MovieContract.MoviesColumns.COL_MOVIE_ID+"') REFERENCES "+ MovieContract.MoviesColumns.TABLE_NAME_MOVIES+" ("+ MovieContract.MoviesColumns.COL_MOVIE_ID+") ON DELETE CASCADE);";
        Log.v(LOG_TAG, SQL_CREATE_MOST_POPULAR_MOVIE_TABLE);
        db.execSQL(SQL_CREATE_MOST_POPULAR_MOVIE_TABLE);

        // create table movies_most_popular
        final String SQL_CREATE_HIGHEST_RATED_MOVIE_TABLE = "CREATE TABLE " + MovieContract.MoviesColumns.TABLE_NAME_HIGHEST_RATED+ " ("+
                MovieContract.MoviesColumns.COL_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, " +
                MovieContract.MoviesColumns.COL_MOVIE_ID + " TEXT NOT NULL UNIQUE, " +
                " FOREIGN KEY ('"+MovieContract.MoviesColumns.COL_MOVIE_ID+"') REFERENCES "+ MovieContract.MoviesColumns.TABLE_NAME_MOVIES+" ("+ MovieContract.MoviesColumns.COL_MOVIE_ID+") ON DELETE CASCADE);";
        Log.v(LOG_TAG, SQL_CREATE_HIGHEST_RATED_MOVIE_TABLE);
        db.execSQL(SQL_CREATE_HIGHEST_RATED_MOVIE_TABLE);

        // create table reviews
        final String SQL_CREATE_REVIEWS_TABLE = "CREATE TABLE " + MovieContract.MoviesColumns.TABLE_NAME_REVIEWS+ " ("+
                MovieContract.MoviesColumns.COL_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, " +
                MovieContract.MoviesColumns.COL_MOVIE_ID + " TEXT NOT NULL, " +
                MovieContract.MoviesColumns.COL_REVIEW_ID +" TEXT NOT NULL UNIQUE, " +
                MovieContract.MoviesColumns.COL_REVIEW_AUTHOR +" TEXT, " +
                MovieContract.MoviesColumns.COL_REVIEW_CONTENT +" TEXT NOT NULL, " +
                " FOREIGN KEY ('"+MovieContract.MoviesColumns.COL_MOVIE_ID+"') REFERENCES "+ MovieContract.MoviesColumns.TABLE_NAME_MOVIES+" ("+ MovieContract.MoviesColumns.COL_MOVIE_ID+") ON DELETE CASCADE);";
        Log.v(LOG_TAG, SQL_CREATE_REVIEWS_TABLE);
        db.execSQL(SQL_CREATE_REVIEWS_TABLE);

        // create table trailers
        final String SQL_CREATE_TRAILERS_TABLE = "CREATE TABLE " + MovieContract.MoviesColumns.TABLE_NAME_TRAILERS+ " ("+
                MovieContract.MoviesColumns.COL_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE, " +
                MovieContract.MoviesColumns.COL_MOVIE_ID + " TEXT NOT NULL, " +
                MovieContract.MoviesColumns.COL_TRAILER_ID +" TEXT NOT NULL, " +
                MovieContract.MoviesColumns.COL_TRAILER_KEY +" TEXT NOT NULL, " +
                MovieContract.MoviesColumns.COL_TRAILER_SITE +" TEXT NOT NULL, " +
                MovieContract.MoviesColumns.COL_TRAILER_NAME +" TEXT, " +
                " FOREIGN KEY ('"+MovieContract.MoviesColumns.COL_MOVIE_ID+"') REFERENCES "+ MovieContract.MoviesColumns.TABLE_NAME_MOVIES+" ("+ MovieContract.MoviesColumns.COL_MOVIE_ID+") ON DELETE CASCADE);";
        Log.v(LOG_TAG, SQL_CREATE_TRAILERS_TABLE);
        db.execSQL(SQL_CREATE_TRAILERS_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing to do
    }
}
