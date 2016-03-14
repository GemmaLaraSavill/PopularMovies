package com.nanodegree.gemma.popularmovies2;


import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nanodegree.gemma.popularmovies2.data.Constants;
import com.nanodegree.gemma.popularmovies2.data.MovieContract;
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * Created by Gemma S. Lara Savill on 08/10/2015.
 * A fragment that will show all the details of a movie
 * It will attempt to contact the server from another thread to retrieve further information
 * on the movie, like user reviews and video trailers
 */
public class MovieDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, FetchTrailersAndReviewsTask.FetchTrailersAndReviewsTaskResponse {

    // keys
    public static final String MOVIE_URI = "movieUri";
    private static final int DETAIL_LOADER = 0;
    private static final int REVIEWS_LOADER = 1;
    private static final int VIDEOS_LOADER = 2;

    private Uri mMovieUri;
    private TextView mTitleView;
    private ImageView mPosterView;
    private TextView mDateView;
    private TextView mOverviewView;
    private TextView mRatingView;
    private String movieId;
    private TextView mReviews;
    private LinearLayout mTrailersLayout;
    private Uri videoUri;
    private String favToggle;
    private Menu mMenu;
    private String favorite;

    private String mDataSource = "local"; // local or server, always try local first
    private TextView mVideoHeader;
    private TextView mReviewsHeader;

    private final String LOG_TAG = MainActivity.LOG_TAG+" DetailFrag";
    private boolean debug = false;

    // used to share the first video of this movie
    private ShareActionProvider mShareActionProvider;
    private String mVideoToShare;

    // Container Activity must implement this interface
    public interface reloadListListener {
        public void onFavoritesChanged();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment MovieDetailFragment.
     */
    public static MovieDetailFragment newInstance(String param1) {
        MovieDetailFragment fragment = new MovieDetailFragment();
        Bundle args = new Bundle();
        args.putString(MOVIE_URI, param1);
        fragment.setArguments(args);
        return fragment;
    }

    public MovieDetailFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // more than one loader here: one for details, one for trailers, one for reviews
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        getLoaderManager().initLoader(REVIEWS_LOADER, null, this);
        getLoaderManager().initLoader(VIDEOS_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // read the arguments the fragment was initialized with
        if (getArguments() != null) {
            mMovieUri = getArguments().getParcelable(MOVIE_URI);
            if (debug) {
                Log.v(LOG_TAG, "OnCreate mMovieUri is " + mMovieUri);
            }
        }

    }

    /**
     * Used to load different icons in menu to match movie favorite state
     * @param menu
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
            MenuItem favItem = menu.findItem(R.id.action_favorite);
            // set your desired icon here based on a flag
            if (favorite.equals("0")) {
                favItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_not_favorite));
            } else {
                favItem.setIcon(getResources().getDrawable(R.drawable.ic_menu_favorite));
            }
            MenuItem shareItem = menu.findItem(R.id.action_share);
            // decide if I show the share first video link
        if (mVideoToShare != null) {
            // I have a video link to share, show the share button
            shareItem.setVisible(true);
        } else {
            // this movie has no first video link to share, don't show user the button
            shareItem.setVisible(false);
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.mMenu = menu;
        inflater.inflate(R.menu.movie_menu, menu);

        // Retrieve the share menu item
        MenuItem shareItem = menu.findItem(R.id.action_share);
        shareItem.setVisible(false); // let's hide it from user until we have something to share

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

        // with loader, the loader loads this when done
        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mVideoToShare != null) {
            mShareActionProvider.setShareIntent(createShareFirstVideoIntent());
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_favorite) {
            toggleFavorite();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // read the arguments the fragment was initialized with
        Bundle arguments = getArguments();
        if (arguments != null) {
            mMovieUri = arguments.getParcelable(MOVIE_URI);
            if (mMovieUri != null) {
                // get movieId from the URI
                movieId = MovieContract.MoviesColumns.getMovieIdFromUri(mMovieUri);
            }
        }

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        // load the views
        mTitleView = (TextView) rootView.findViewById(R.id.title);
        mPosterView = (ImageView) rootView.findViewById(R.id.poster);
        mDateView = (TextView) rootView.findViewById(R.id.date);
        mOverviewView = (TextView) rootView.findViewById(R.id.overview);
        mRatingView = (TextView) rootView.findViewById(R.id.rating);
        // their data loaded with a Loader

        // reviews
        mReviewsHeader = (TextView) rootView.findViewById(R.id.reviewsHeader);
        mReviews = (TextView) rootView.findViewById(R.id.reviews);

        // videos
        mVideoHeader = (TextView) rootView.findViewById(R.id.videoHeader);
        mTrailersLayout = (LinearLayout) rootView.findViewById(R.id.trailers);

        // Favorite button
        favorite = "0";
//        mVideoToShare = getResources().getString(R.string.noVideoToShare);
        mVideoToShare = null;

        return rootView;
    }

    /**
     * Checks to see if the data I have in the DB is more than 7 days old
     * @return
     */
    private void getFreshData() {
        // get data from server every 7 days
        // get last data refresh date/time
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Date lastDataRefresh = new Date(prefs.getLong("lastupdate", 0));
//        Log.v("Data referesh", "last data collected "+lastDataRefresh);

        // get now date/time
        Date nowDate = new Date(System.currentTimeMillis()); //or simply new Date();
//        Log.v("Data refresh", "now is " + nowDate);

        // 7 days in miliseconds
        long refreshInterval = TimeUnit.MILLISECONDS.convert(Constants.dataCacheInDays, TimeUnit.DAYS);

        // calculate difference between last data refresh and now
        long diff = nowDate.getTime() - lastDataRefresh.getTime();
//        Log.v("Data refresh", "difference " + diff);
//        Log.v("Data refresh", "refreshInterval " + refreshInterval);

        // if it is time to refresh the data
        if (diff >= refreshInterval) {
            // get fresh data from server
            fetchMoreInfo();
            // save last refresh date
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("lastupdate", nowDate.getTime());
            editor.commit();
            mDataSource = "server";
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mMovieUri != null) {
            if (debug) { Log.v(LOG_TAG, "onSaveInstanceState "+mMovieUri); }
            outState.putParcelable(MOVIE_URI, mMovieUri);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mMovieUri = savedInstanceState.getParcelable(MOVIE_URI);
            if (debug) {
                Log.v(LOG_TAG, "onViewStateRestored " + mMovieUri);
            }
        }
        super.onViewStateRestored(savedInstanceState);
    }

    /**
     * Starts the background task to go and fetch the movie trailers and reviews from the server
     */
    private void fetchMoreInfo() {
//        Toast.makeText(getActivity(), "Querying server for more data on this movie", Toast.LENGTH_LONG).show();
        FetchTrailersAndReviewsTask moreInfoTask = new FetchTrailersAndReviewsTask(getActivity());
        moreInfoTask.response = this;
        moreInfoTask.execute(movieId);
        mDataSource = "server";

    }

    @Override
    public void onCompleted(String result) {
        // Update the UI with trailers and videos
        if (isAdded()) { // only execute if fragment is attached to the activity
            // if we are in tablet mode and the user rotates the device and the background task
            // calls onCompleted from postExecute we have to check that the fragment is added to
            // the activity before we refresh the cursor
            getLoaderManager().restartLoader(REVIEWS_LOADER, null, this); // restarts the loader
        }
    }




    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//        Log.v(LOG_TAG, "In onCreateLoader");
        if ( null != mMovieUri ) {
            switch (id) {
                case DETAIL_LOADER:
                    if (debug) { Log.v(LOG_TAG, "Detail Loader"); }
                    CursorLoader movieInfo = new CursorLoader(getActivity(), mMovieUri ,null, null, null, null);
                    return movieInfo;
                case REVIEWS_LOADER:
                    Uri reviewsUri = MovieContract.MoviesColumns.buildMovieReviewsUri(movieId);
                    CursorLoader reviews = new CursorLoader(getActivity(), reviewsUri, null, null, null, null);
                    return reviews;
                case VIDEOS_LOADER:
                    Uri videosUri = MovieContract.MoviesColumns.buildMovieTrailersUri(movieId);
                    CursorLoader videos = new CursorLoader(getActivity(), videosUri, null, null, null, null);
                    return videos;
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor movieInfoCursor) {
//        Log.v(LOG_TAG, "In onLoadFinished");
        // updates the UI when the different cursors are loaded
        switch (loader.getId()) {
            case DETAIL_LOADER:
            if (movieInfoCursor != null && movieInfoCursor.moveToFirst()) {
                // title
                String movieTitle = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_TITLE));
                if (getActivity().getActionBar() != null) {
                getActivity().getActionBar().setTitle(movieTitle);
                }
                mTitleView.setText(movieTitle.toUpperCase());

                // poster
                String posterUrl = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_POSTER));
                String posterServerUrl = Constants.thumbUrl + Constants.thumbSize185 + posterUrl;
//        Log.v(LOG_TAG, "getting poster from " + posterServerUrl);
                Picasso.with(getActivity()).load(posterServerUrl).into(mPosterView);

                // release date
                String date = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_RELEASE_DATE));
                mDateView.setText(date);

                // overview
                String overview = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_OVERVIEW));
                mOverviewView.setText(overview);

                // rating
                String rating = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_RATING));
                mRatingView.setText(rating + " / 10");

                // favorite button
                favorite = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_FAVORITE));

                if (favorite.equals("0")) {
                    favToggle = "1";
//                    Log.v(LOG_TAG, "Movie loaded NOT favorite");
                } else {
                    favToggle = "0";
//                    log.v(log_tag, "movie loaded is favorite");
                }
                // load the proper icon in the action bar: favorite or not
                getActivity().invalidateOptionsMenu();
            }
                break;
            case REVIEWS_LOADER:
//                Log.v(LOG_TAG, "Reviews cursor loaded");
                if (movieInfoCursor != null) {
                    if (debug) { Log.v(LOG_TAG, "Received "+movieInfoCursor.getCount()+" reviews from DB"); }
                    if (movieInfoCursor.getCount() > 0) {
                        // I have reviews in DB
                        mReviewsHeader.setVisibility(View.VISIBLE);
                        // see if more than 7 days have gone by since data refresh, if so try and get new data
                        getFreshData();
                        String reviewsText = "";
//                        movieInfoCursor.moveToFirst();
                        while (movieInfoCursor.moveToNext()) {
                            String author = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_REVIEW_AUTHOR));

                            String content = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_REVIEW_CONTENT));
                            reviewsText = reviewsText.concat(content + "\nBy:" + author + "\n\n");
                        }
                        // some reviews have a href links, tried using Html.fromHtml() but as the HTML is wrong, I get bad results
                        mReviews.setText(reviewsText);
                    } else {
                        // no reviews for this movie in the local storage
                        if (mDataSource.equals("local")) {
                            // try and get some from the server
                            fetchMoreInfo();
                        }
                    }
                }
                break;
            case VIDEOS_LOADER:
                if (debug) { Log.v(LOG_TAG, "Videos cursor loaded"); }
                if (movieInfoCursor != null) {
                    if (debug) { Log.v(LOG_TAG, "Received " + movieInfoCursor.getCount() + " videos from DB"); }
                    if (movieInfoCursor.getCount() > 0) {
                        mTrailersLayout.removeAllViews();
                        mVideoHeader.setVisibility(View.VISIBLE);
                        int counter = 1; // counter to retrieve first video for sharing
                        // I have videos in DB
                        while (movieInfoCursor.moveToNext()) {
                            // get layout inflating xml
                            View videoRowLayout = getActivity().getLayoutInflater().inflate(R.layout.video_row, null);
                            String id = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_TRAILER_ID));
                            final String key = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_TRAILER_KEY));
                            String name = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_TRAILER_NAME));
                            if (debug) { Log.v(LOG_TAG, "Video name "+name); }
                            final String site = movieInfoCursor.getString(movieInfoCursor.getColumnIndex(MovieContract.MoviesColumns.COL_TRAILER_SITE));

                            if (site.equals("YouTube")) {
                                videoUri = Uri.parse("https://www.youtube.com/watch").buildUpon()
                                        .appendQueryParameter("v", key)
                                        .build();
                            }
                            TextView videoName = (TextView)videoRowLayout.findViewById(R.id.videoTitle);
                            videoName.setText(name);
                            videoRowLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube://" + key));
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);

                                    } catch (ActivityNotFoundException e) {
                                        // youtube app is not installed on the device. The system will provide a list of compatible apps and the user can choose
                                        Intent i = new Intent(Intent.ACTION_VIEW, videoUri);
                                        startActivity(i);
                                    }
                                }
                            });
                            mTrailersLayout.addView(videoRowLayout);
                            if (counter == 1) {
                                // first video for share intent
                                mVideoToShare = name + " "+videoUri;
                                // If onCreateOptionsMenu has already happened, we need to update the share intent now.
                                if (mShareActionProvider != null) {
                                    mShareActionProvider.setShareIntent(createShareFirstVideoIntent());
                                }
                                // reload the action bar menu to display share icon now I have a video link to share
                                getActivity().invalidateOptionsMenu();
                                counter++;
                            }
                        }
                    } else {
                        // no videos for this movie in the local storage
                        if (mDataSource.equals("local")) {
                            // try and get some from the server
                            fetchMoreInfo();
                        }
                    }
                }
                break;
        }

    }

    /**
     * Toggles movie as Favorite
     */
    private void toggleFavorite() {

        Uri favUri = MovieContract.MoviesColumns.buildMovieToggleFavoriteUri(movieId, favToggle);
        ContentValues cv = new ContentValues();
        cv.put(MovieContract.MoviesColumns.COL_FAVORITE, favToggle);
        int update = getActivity().getContentResolver().update(favUri, cv, MovieContract.MoviesColumns.COL_MOVIE_ID + " = ?", new String[]{movieId});
        if (update > 0) {
            if (favToggle.equals("0")) {
                // movie removed from favorite
                mMenu.findItem(R.id.action_favorite).setIcon(R.drawable.ic_menu_not_favorite);
                if (debug) { Log.v(LOG_TAG, "toggleFavorite from fav to no fav"); }
                // if in favorite list mode warn the poster fragment or it's parent activity to refresh its data
                // you don't want to see a non favorited movie in your favorites list!
                String sortOrder = SettingsActivity.getPreferredSortOrder(getActivity());
                if (sortOrder.equals(getActivity().getResources().getString(R.string.pref_movies_favorites))) {
                        ((reloadListListener) getActivity()).onFavoritesChanged();
                }

            } else {
                // movie added as favorite
                mMenu.findItem(R.id.action_favorite).setIcon(R.drawable.ic_menu_favorite);
                if (debug) {  Log.v(LOG_TAG, "toggleFavorite from NOTfav to fav"); }
                // if in favorite list mode warn the poster fragment or it's parent activity to refresh its data
                String sortOrder = SettingsActivity.getPreferredSortOrder(getActivity());
                if (sortOrder.equals(getActivity().getResources().getString(R.string.pref_movies_favorites))) {
                        ((reloadListListener) getActivity()).onFavoritesChanged();
                }
            }
            getActivity().invalidateOptionsMenu();
            // just restart the loader
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this); // restarts the loader

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
            // nothing to do
//        Log.v(LOG_TAG, "onLoaderReset");
        loader = null;
    }

    /**
     * Activity tells us what default movie to load
     * @param newMovieUri
     */
    public void loadDefaultMovie(Uri newMovieUri) {
        // if I have no Uri from restoring the state load this one
        if (mMovieUri == null) {
            if (null != newMovieUri) {
                if (debug) {
                    Log.v(LOG_TAG, "loadDefaultMovie received from Activiy with new uri " + newMovieUri);
                }
                // get movieId from the URI
                movieId = MovieContract.MoviesColumns.getMovieIdFromUri(newMovieUri);
                mMovieUri = newMovieUri;
                getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
            }
        }
    }

    /**
     * Creates the intent to share the first video link of this movie
     * A share button will appear in the action bar if there is a link to share
     * @return
     */
    private Intent createShareFirstVideoIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mVideoToShare);
        return shareIntent;
    }


}
