package com.nanodegree.gemma.popularmovies2;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
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
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Gemma S. Lara Savill on 23/11/2015.
 *
 * Task on a background thread that will connect to the MovieDb server
 * to download the trailers and reviews for this movie
 */
public class FetchTrailersAndReviewsTask extends AsyncTask<String, Void, String> {

    final String MOVIE_INFO_BASE_URL =
            "http://api.themoviedb.org/3/movie/";
    final String URI_TRAILERS = "videos"; // part of the uri to get the trailers
    final String URI_REVIEWS = "reviews"; // part of the uri to get the reviews
    final String API_PARAM = "api_key"; // must be added in every call

    private boolean debug = false;

    public interface FetchTrailersAndReviewsTaskResponse {
        public void onCompleted(String result);
    }

    public FetchTrailersAndReviewsTaskResponse response = null;
    private final String LOG_TAG = MainActivity.LOG_TAG+ " TrailersAsync";
    private final Context mContext;

    /**
     * Constructor to pass in the context of the activity
     * @param context
     */
    public FetchTrailersAndReviewsTask(Context context) {
        mContext = context;
    }

    @Override
    protected String doInBackground(String... params) {

        // If I am calling with no parameters I can get no data, so this ends here
        if (params.length == 0) {
            return null;
        }

        String movieId = params[0];

        // example queries
//        1. trailers
//        http://api.themoviedb.org/3/movie/135397/videos?api_key=my_api_key
//        2. reviews
//        http://api.themoviedb.org/3/movie/135397/reviews?api_key=146c834d0c5d5b5ceca3a365b6aa68c7

        final String MOVIE_INFO_BASE_URL =
                "http://api.themoviedb.org/3/movie/";
        final String URI_TRAILERS = "videos"; // part of the uri to get the trailers
        final String URI_REVIEWS = "reviews"; // part of the uri to get the reviews
        final String API_PARAM = "api_key"; // must be added in every call

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // insert movies to the db using applyBatch
        ArrayList<ContentProviderOperation> dbTransactions = new ArrayList<ContentProviderOperation>();

        // Get reviews for this movie from server
                String reviewData = getReviewsFromServer(movieId);
        if (reviewData != null) {
            ArrayList<ContentValues> reviews = getReviewsDataFromJson(reviewData);
            int reviewsReceived = reviews.size();
            if (debug) {
                Log.v(LOG_TAG, "Received " + reviewsReceived + " REVIEWS for movie " + movieId);
            }
            if (reviewsReceived > 0) {
                Uri deleteReviewsUri = MovieContract.MoviesColumns.buildMovieReviewsUri(movieId);
                String mSelectionClause = MovieContract.MoviesColumns.COL_MOVIE_ID + " = ?";
                String[] mSelectionArgs = {movieId};
                int reviewsDeleted = mContext.getContentResolver().delete(deleteReviewsUri, mSelectionClause, mSelectionArgs);
            }
            Uri reviewUri = MovieContract.MoviesColumns.buildMovieReviewsUri(movieId);
            for (ContentValues review : reviews) {
                review.put(MovieContract.MoviesColumns.COL_MOVIE_ID, movieId);
                // insert this review in DB
                // add to the batch insert
                dbTransactions.add(ContentProviderOperation.newInsert(reviewUri).withValues(review).build());
            }
//         end of reviews for this movie
        }

//         now get trailers for this movie from server
                String trailerData = getTrailersFromServer(movieId);
                if (trailerData != null) {
                    ArrayList<ContentValues> trailers = getTrailerDataFromJson(trailerData);
                    int trailersReceived = trailers.size();
                    if (debug) {
                        Log.v(LOG_TAG, "Received " + trailersReceived + " TRAILERS for movie " + movieId);
                    }
                    if (trailersReceived > 0) {
                        Uri deleteTrailersUri = MovieContract.MoviesColumns.buildMovieTrailersUri(movieId);
                        String mSelectionClause = MovieContract.MoviesColumns.COL_MOVIE_ID +" = ?";
                        String[] mSelectionArgs = {movieId};
                        int trailersDeleted = mContext.getContentResolver().delete(deleteTrailersUri,mSelectionClause,mSelectionArgs);
                    }
                    Uri trailerUri = MovieContract.MoviesColumns.buildMovieTrailersUri(movieId);
                    for (ContentValues trailer : trailers) {
                        trailer.put(MovieContract.MoviesColumns.COL_MOVIE_ID, movieId);
                        // insert this review in DB
                        // add to the batch insert
                        dbTransactions.add(ContentProviderOperation.newInsert(trailerUri).withValues(trailer).build());

                    }
                }// end of videos for this movie

        // bulk insert with batch
        ContentProviderResult[] result = null;
        try {
            if (debug) {
                Log.v(LOG_TAG, "Start of insert server data into DB with applyBatch");
            }
            result = mContext.getContentResolver().applyBatch(MovieContract.CONTENT_AUTHORITY, dbTransactions);
            if (debug) {
                Log.v(LOG_TAG, "End of insert server data into DB with applyBatch");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
        if (debug) {
            Log.d(LOG_TAG, "FetchTrailersAndReviews Complete. Transactions " + result.length);
        }

        return "OK";
    }

    /**
     * Connects to the server and gets the reviews of a movie
     * it comes in JSON format
     * @param movieId
     * @return null if can't get data from server
     */
    private String getReviewsFromServer(String movieId) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Get the REVIEWS info
        String reviewsJsonStr = null;
        String reviewString = null;
        try {
            Uri builtUri = Uri.parse(MOVIE_INFO_BASE_URL)
                    .buildUpon()
                    .appendPath(movieId)
                    .appendPath(URI_REVIEWS)
                    .appendQueryParameter(API_PARAM, Constants.API_KEY)
                    .build();

            URL urlReviews = new URL(builtUri.toString());
            if (debug) {  Log.v(LOG_TAG, "Url requesting reviews: " + urlReviews); }

            // Create the request to the server and open the connection
            urlConnection = (HttpURLConnection) urlReviews.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            int serverResponseCode = urlConnection.getResponseCode();
            String serverResponseMessage = urlConnection.getResponseMessage();
            if (debug) { Log.v(LOG_TAG, "Server response to reviews request: " + serverResponseMessage + " code " + serverResponseCode); }

            if (serverResponseCode == HttpURLConnection.HTTP_OK) {

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    if (debug) { Log.v(LOG_TAG, "Got null inputstream from server :("); }
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
                reviewsJsonStr = buffer.toString();
                if (debug) {  Log.v(LOG_TAG, "Got reviews info:\n" + reviewsJsonStr); }
                return reviewsJsonStr;
            } else {
                return null;
            }
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

    }

    /**
     * Connects to the server and gets the trailers of a movie
     * it comes in JSON format
     * @param movieId
     * @return null if can't get data from server
     */
    private String getTrailersFromServer(String movieId) {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Get the TRAILERS info
        String trailersJsonStr = null;
        String videoString = null;
        try {
            Uri builtUri = Uri.parse(MOVIE_INFO_BASE_URL)
                    .buildUpon()
                    .appendPath(movieId)
                    .appendPath(URI_TRAILERS)
                    .appendQueryParameter(API_PARAM, Constants.API_KEY)
                    .build();

            URL urlTrailers = new URL(builtUri.toString());
            if (debug) {  Log.v(LOG_TAG, "Url requesting trailers: " + urlTrailers); }

            // Create the request to the server and open the connection
            urlConnection = (HttpURLConnection) urlTrailers.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            int serverResponseCode = urlConnection.getResponseCode();
            String serverResponseMessage = urlConnection.getResponseMessage();
            if (debug) { Log.v(LOG_TAG, "Server response to trailer request: " + serverResponseMessage + " code " + serverResponseCode); }

            if (serverResponseCode == HttpURLConnection.HTTP_OK) {
                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    if (debug) {   Log.v(LOG_TAG, "Got null inputstream from server :("); }
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
                trailersJsonStr = buffer.toString();
                if (debug) {  Log.v(LOG_TAG, "Got trailer info:\n" + trailersJsonStr); }
                return trailersJsonStr;
            }   else {
                return null;
            }
        }catch(IOException e){
            Log.e(LOG_TAG, "Error ", e);
            // no data
            return null;
        }finally{
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
    }




    /**
     * Extract the trailer data from the Json format
     * Data extracted: id, key, name, site, type
     * This data will be saved in the local DB via ContentResover > ContentProvider
     * @param trailersJsonStr
     */
    private ArrayList<ContentValues> getTrailerDataFromJson(String trailersJsonStr) {

        // trailers are inside array results
        final String M_RESULTS = "results";
        final String M_ID = "id";
        final String M_KEY = "key";
        final String M_SITE = "site";
        final String M_NAME = "name";
        final String M_TYPE = "type";

        // for testing only
//        String testString = "RECEIVED: ";
        ArrayList<ContentValues> trailers = new ArrayList<ContentValues>();
        try {
            JSONObject moviesJson = new JSONObject(trailersJsonStr);
            JSONArray videosArray = moviesJson.getJSONArray(M_RESULTS);

            if (debug) {  Log.v(LOG_TAG, "Server returned "+videosArray.length()+" videos (trailers)"); }
//            testString = testString.concat(videosArray.length()+" \r\n");

            for(int i = 0; i < videosArray.length(); i++) {
                JSONObject video = videosArray.getJSONObject(i);
                String videoId = video.getString(M_ID);
                String key = video.getString(M_KEY);
                String site = video.getString(M_SITE);
                String name = video.getString(M_NAME);
                String type = video.getString(M_TYPE);

                // for testing only
//                String videoInfo = "\r\nID: "+videoId+"\r\n"
//                        + "key: "+key+"\r\n"
//                        + "site: "+site+"\r\n"
//                        + "name: "+name+"\r\n"
//                        + "type: "+ type+"\r\n";
//                testString = testString.concat(videoInfo);
                // end of testing

                ContentValues movieTrailerValues = new ContentValues();
                movieTrailerValues.put(MovieContract.MoviesColumns.COL_TRAILER_ID, videoId);
                movieTrailerValues.put(MovieContract.MoviesColumns.COL_TRAILER_KEY, key);
                movieTrailerValues.put(MovieContract.MoviesColumns.COL_TRAILER_SITE, site);
                movieTrailerValues.put(MovieContract.MoviesColumns.COL_TRAILER_NAME, name);

                trailers.add(movieTrailerValues);
            }
//            Log.v(LOG_TAG, testString);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

        return trailers;
    }

    /**
     * Extract the review data from the Json format
     * Data extracted:
     * This data will be saved in the local DB via ContentResover > ContentProvider
     * @param reviewsJsonStr
     */
    private ArrayList<ContentValues> getReviewsDataFromJson(String reviewsJsonStr) {

        // trailers are inside array results
        final String M_RESULTS = "results";
        final String M_ID = "id";
        final String M_AUTHOR = "author";
        final String M_REVIEW = "content";

        // for testing only
//        String testString = "RECEIVED: ";

        ArrayList<ContentValues> reviews = new ArrayList<ContentValues>();

        try {
            JSONObject moviesJson = new JSONObject(reviewsJsonStr);
            JSONArray reviewsArray = moviesJson.getJSONArray(M_RESULTS);

            if (debug) {  Log.v(LOG_TAG, "Server returned "+reviewsArray.length()+" reviews"); }
//            testString = testString.concat(reviewsArray.length()+" \r\n");

            for(int i = 0; i < reviewsArray.length(); i++) {

                JSONObject review = reviewsArray.getJSONObject(i);
                String reviewId = review.getString(M_ID);
                String author = review.getString(M_AUTHOR);
                String reviewText = review.getString(M_REVIEW);

                ContentValues movieReviewValues = new ContentValues();
                movieReviewValues.put(MovieContract.MoviesColumns.COL_REVIEW_ID, reviewId);
                movieReviewValues.put(MovieContract.MoviesColumns.COL_REVIEW_AUTHOR, author);
                movieReviewValues.put(MovieContract.MoviesColumns.COL_REVIEW_CONTENT, reviewText);

                reviews.add(movieReviewValues);

                // for testing only
//                String reviewInfo = "\r\nID: "+reviewId+"\r\n"
//                        + "author: "+author+"\r\n"
//                        + "reviewText: "+reviewText+"\r\n";
//                testString = testString.concat(reviewInfo);
                // end of testing


            }
//            Log.v(LOG_TAG, testString);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        // for testing only
//        return testString;
        return reviews;
    }


    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result.equals("OK")) {
            response.onCompleted(result);
        }
    }
}
