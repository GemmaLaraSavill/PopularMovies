package com.nanodegree.gemma.popularmovies2;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

import com.nanodegree.gemma.popularmovies2.data.Constants;
import com.nanodegree.gemma.popularmovies2.data.MovieContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Gemma S. Lara Savill on 06/11/2015.
 *
 * Task on a background thread that will connect to the MovieDb server
 * to download the movies data and insert the new data in batch mode to preserve the flash memory
 * of the device
 *
 * Initial intent was to download new movies with trailers and reviews
 * Server does not accept so many connections, so trailers and reviews will be loaded elsewhere
 */
public class FetchMovieData extends AsyncTask<String, Void, String> {


    public interface FetchMoviesTaskResponse {
        void onCompleted();
    }

    public FetchMoviesTaskResponse response = null;
    private final String LOG_TAG = MainActivity.LOG_TAG+ " FetchMvData";
    private final Context mContext;
    private String mSortOrder;

    final String MOVIE_INFO_BASE_URL =
            "http://api.themoviedb.org/3/movie/";
    final String URI_TRAILERS = "videos"; // part of the uri to get the trailers
    final String URI_REVIEWS = "reviews"; // part of the uri to get the reviews
    final String API_PARAM = "api_key"; // must be added in every call

    private boolean debug = false;

    /**
     * Constructor to pass in the context of the activity
     * @param context
     */
    public FetchMovieData(Context context) {
        mContext = context;
    }


    @Override
    protected String doInBackground(String... params) {

        if (debug) {
            Log.v(LOG_TAG, "starting AsyncTask FetchMovieData");
        }

        // If I am calling with no parameters I can get no data, so this ends here
        if (params.length == 0) {
            Log.i(LOG_TAG, "No sort order selected. End of data task.");
            return null;
        }

        // example queries
//        1. most popular
//        http://api.themoviedb.org/3/discover/movie?sort_by=popularity.desc&api_key=my_api_key
//        2. highest rated
//        http://api.themoviedb.org/3/discover/movie?sort_by=vote_average.desc&api_key=my_api_key

        // extract the parameters
        mSortOrder = params[0];
//        if (mSortOrder.equals(mContext.getResources().getString(R.string.pref_movies_favorites))) {
//            Log.v(LOG_TAG, "Trying to get favorites from server.");
//            return null;
//        }
        String sortBy = mSortOrder.concat(".desc");

        final String MOVIES_BASE_URL =
                "http://api.themoviedb.org/3/discover/movie?";
        final String SORT_PARAM = "sort_by"; // selection of movies

        // build the uri
        Uri builtUri = Uri.parse(MOVIES_BASE_URL).buildUpon()
                    .appendQueryParameter(SORT_PARAM, sortBy)
                    .appendQueryParameter(API_PARAM, Constants.API_KEY)
                    .build();
        String popularMovies = getServerData(builtUri);
        if (popularMovies != null) {
            getMovieDataFromJsonToDB(popularMovies);
        }
        if (debug) {
            Log.i(LOG_TAG, "finished AsyncTask FetchMovieData");
        }
//        Log.v(LOG_TAG, "testString "+testString);
        return null;
    }

    /**
     * Connect to server and retreive the data in Json format
     * @param builtUri
     * @return String with Json data
     */
    private String getServerData(Uri builtUri) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        // Will contain the raw JSON response as a string.
        String moviesJsonStr = null;
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (debug) {
            Log.v(LOG_TAG, "getServerData with uri: " + url);
        }

        try {
            // Create the request to the server and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            moviesJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // no data
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return moviesJsonStr;
    }

    /**
     * Extract the data from the returned Json String
     * Data extracted: title, movie poster image, synopsis, user rating, release date
     * This data will be saved in the local DB via ContentResover > ContentProvider
     * @param dataStr
     */
    private String getMovieDataFromJsonToDB(String dataStr) {

//        Log.v(LOG_TAG, "getMovieDataFromJsonToDB with dataStr: " + dataStr);

        final String M_ID = "id";
        final String M_TITLE = "original_title";
        final String M_POSTER = "poster_path";
        final String M_OVERVIEW = "overview";
        final String M_USER_RATING = "vote_average";
        final String M_RELEASE_DATE = "release_date";

        // the movies are inside array results
        final String M_RESULTS = "results";

        String testString = "RECEIVED ";

        try {
            JSONObject moviesJson = new JSONObject(dataStr);
            JSONArray movieArray = moviesJson.getJSONArray(M_RESULTS);
            if (debug) {
                Log.v(LOG_TAG, "Server returned " + movieArray.length() + " movies");
            }

            // delete all movies from database, EXCEPT favorite movies
            if (movieArray.length() > 0) {
                // got new data, lets erase the old movies that aren't favorites
                Uri deleteMoviesUri = MovieContract.MoviesColumns.buildDeleteNonFavMoviesUri();
                String mSelectionClause = MovieContract.MoviesColumns.COL_FAVORITE +" = ?";
                String[] mSelectionArgs = {"0"};
                int movieDeleted = mContext.getContentResolver().delete(deleteMoviesUri,mSelectionClause,mSelectionArgs);

            }

            // insert movies to the db using applyBatch
            ArrayList<ContentProviderOperation> dbTransactions = new ArrayList<ContentProviderOperation>();
            // insert movie Uri
            Uri thisMovieUri = MovieContract.MoviesColumns.buildMoviesOrderByUri(mSortOrder);

            for(int i = 0; i < movieArray.length(); i++) { // for each movie received

                JSONObject movie = movieArray.getJSONObject(i);
                String movieId = movie.getString(M_ID);
                String movieTitle = movie.getString(M_TITLE);
                String poster = movie.getString(M_POSTER);
                String overview = movie.getString(M_OVERVIEW);
                double rating = movie.getDouble(M_USER_RATING);
                String release_date = movie.getString(M_RELEASE_DATE);

                ContentValues movieValues = new ContentValues();
                movieValues.put(MovieContract.MoviesColumns.COL_MOVIE_ID, movieId);
                movieValues.put(MovieContract.MoviesColumns.COL_TITLE, movieTitle);
                movieValues.put(MovieContract.MoviesColumns.COL_POSTER, poster);
                movieValues.put(MovieContract.MoviesColumns.COL_OVERVIEW, overview);
                movieValues.put(MovieContract.MoviesColumns.COL_RATING, rating);
                movieValues.put(MovieContract.MoviesColumns.COL_RELEASE_DATE, release_date);

                // see if movie is already in database
                // we will refresh this movie data with an update
                // if not we will insert the new movie
                Uri movieUri = MovieContract.MoviesColumns.buildMovieByIdUri(movieId);
                Cursor movieCursor = mContext.getContentResolver().query(movieUri, null, null, null, null);
                if (movieCursor.getCount() > 0) {
                    movieCursor.moveToFirst();
                    // update the movie data
                    String idInDatabase = movieCursor.getString(movieCursor.getColumnIndex(MovieContract.MoviesColumns._ID));
//                    Log.v(LOG_TAG, "Movie " + movieTitle + "[" + movieId + "] already in database: update it");
                    dbTransactions.add(ContentProviderOperation
                            .newUpdate(thisMovieUri)
                            .withValues(movieValues)
                            .withSelection(MovieContract.MoviesColumns._ID + " = ?", new String[]{idInDatabase})
                            .build());

                } else {
//                    Log.v(LOG_TAG, "Movie "+movieTitle+ "["+movieId+"] NOT in database: insert it whith uri "+thisMovieUri);
                    // if movie not in database: insert
                    // add to the batch insert
                    dbTransactions.add(ContentProviderOperation.newInsert(thisMovieUri).withValues(movieValues).build());
                }
                movieCursor.close();

            } // end of for each movie received

            // bulk insert with batch
            ContentProviderResult[] result = null;
            try {
//                Log.v(LOG_TAG, "Start of insert server data into DB with applyBatch");
                result = mContext.getContentResolver().applyBatch(MovieContract.CONTENT_AUTHORITY, dbTransactions);
//                Log.v(LOG_TAG, "End of insert server data into DB with applyBatch");
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
//
//
//            Log.d(LOG_TAG, "FetchMoviesTask Complete. Transactions " + result.length);
//            for(ContentProviderResult res : result) {
            ////http://developer.android.com/intl/es/reference/android/content/ContentProviderResult.html#describeContents()
//                Log.v(LOG_TAG, "Transactions " + res.toString());
//            }


        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return testString;

    }




    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        // call the loader that will refresh the UI
//        getLoaderManager().restartLoader(POSTERS_LOADER, null, mCallback); // restarts the loader
        response.onCompleted();
    }
}
