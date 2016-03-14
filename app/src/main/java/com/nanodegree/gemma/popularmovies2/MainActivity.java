package com.nanodegree.gemma.popularmovies2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Popular Movies
 * An app that shows movies from themovieDB.org
 * User can sort movies by most popular, highest rated or a selection of favorites the user can keep in the local memory via the favorite star button
 * Each movie shows details, user reviews and videos such as trailers or teasers, when available
 * To minimize data usage the app will refresh data from the server only when user changes sort order or 7 days have gone by since last data refresh. Favorite movies will always be available on local storage.
 * By Gemma S. Lara Savill November/December 2015 as a task for my Udacity Android Developer Nanodegree
 *
 */
public class MainActivity extends AppCompatActivity implements MoviePostersFragment.OnMovieSelectedListener, MovieDetailFragment.reloadListListener {

    // screen layout: one or two fragments
    private boolean mTwoPane;
    // Tag for detail fragment
    public static final String MOVIE_DETAIL_FRAG_TAG = "DETAILTAG";
    // movies sort order selected by user
    private String mSortOrder;

    // for debugging
    public final static String LOG_TAG = "PopMov2";
    private boolean debug = false;

    // Request code used when this Activity opens MovieDetailActivity
    // This is to listen in one pane mode if the user has removed a movie from favorites
    // If in favorite listing mode we should update the favorites list
    static final int OPEN_MOVIE_DETAIL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSortOrder = SettingsActivity.getPreferredSortOrder(this);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.movie_detail_container) != null) {
            // system has loaded the dual pane layout: two fragments
            mTwoPane = true;
            if (debug) {
                Log.v(LOG_TAG, "onCreate I have TWO fragments");
            }
            // in the two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a fragment transaction
            if (savedInstanceState == null) {
                if (debug) {
                    Log.v(LOG_TAG, "onCreate loading detail fragment");
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.movie_detail_container, new MovieDetailFragment(), MOVIE_DETAIL_FRAG_TAG)
                        .commit();
            } else {
                // let the system restore the state
            }

        } else {
            // only one fragment
            mTwoPane = false;
            if (debug) {
                Log.v(LOG_TAG, "onCreate I have ONE fragment");
            }
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * If the user loads favorite movies with no favorite movies saved, here I will remove the detail fragment
     * as it would be empty
     */
    public void removeDetailFragment() {
        if (mTwoPane) { // only in two pane mode
            if (getSupportFragmentManager().findFragmentByTag(MOVIE_DETAIL_FRAG_TAG) != null) {
                getSupportFragmentManager().beginTransaction().
                        remove(getSupportFragmentManager().findFragmentById(R.id.movie_detail_container)).commit();
            }
        }
    }

    /**
     * When a movie is selected
     * Passed from MoviePostersFragment via interface
     * @param movieUri
     */
    @Override
    public void onMovieSelected(Uri movieUri) {
        if (movieUri == null) {
            // no movie detail to load, so lets unload the detail fragment layout
            removeDetailFragment();
        } else {
            if (mTwoPane) {
                Bundle args = new Bundle();
                args.putParcelable(MovieDetailFragment.MOVIE_URI, movieUri);

                MovieDetailFragment fragment = new MovieDetailFragment();
                fragment.setArguments(args); // important pass the content uri

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.movie_detail_container, fragment, MOVIE_DETAIL_FRAG_TAG)
                        .commit();
            } else {
                Intent intent = new Intent(this, MovieDetailActivity.class).setData(movieUri);
                startActivityForResult(intent, OPEN_MOVIE_DETAIL);
            }
        }
    }

    /**
     * Used by the movie poster grid fragment to alert that the movie data is loaded
     * It passes in the uri of the default movie to load in the detail fragment
     * Called from MoviePostersFragment via interface
     * @param movieUri
     */
    @Override
    public void loadDefaultMovie(Uri movieUri) {
        if (debug) {
            Log.v(LOG_TAG, "loadDefaultMovie " + movieUri);
        }
        if (mTwoPane) { // only if I have a detail fragment on the screen
            if (getSupportFragmentManager().findFragmentByTag(MOVIE_DETAIL_FRAG_TAG) != null) {
                // detail fragment is already loaded, so I will replace it passing the movie Uri
                MovieDetailFragment movieDetailFragment = (MovieDetailFragment) getSupportFragmentManager().findFragmentByTag(MOVIE_DETAIL_FRAG_TAG);
                Bundle args = new Bundle();
                args.putParcelable(MovieDetailFragment.MOVIE_URI, movieUri);

                MovieDetailFragment fragment = new MovieDetailFragment();
                fragment.setArguments(args); // important pass the content uri
                if (debug) {
                    Log.v(LOG_TAG, "replace detail frag");
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.movie_detail_container, fragment, MOVIE_DETAIL_FRAG_TAG)
                        .commitAllowingStateLoss();
            } else {

                if (debug) {
                    Log.v(LOG_TAG, "add detail frag with uri "+movieUri);
                }
                Bundle args = new Bundle();
                args.putParcelable(MovieDetailFragment.MOVIE_URI, movieUri);

                MovieDetailFragment fragment = new MovieDetailFragment();
                fragment.setArguments(args); // important pass the content uri

                getSupportFragmentManager().beginTransaction()
                        .add(R.id.movie_detail_container, fragment, MOVIE_DETAIL_FRAG_TAG)
                        .commit();
            }
        }
    }

    /**
     * Passed back by the movie detail fragment when the favorite status of a movie has been changed
     * When in favorite list mode we must reload the movie list
     */
    @Override
    public void onFavoritesChanged() {
//        if (debug) {  Log.v(LOG_TAG, "Detail frag warns that favorite has changed, so reload list"); }
        MoviePostersFragment frag = (MoviePostersFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_poster_grid);
        if (frag!=null) {
            frag.loadMovies();
        }
    }

    /**
     * Here I will listen for the result of the Movie Detail Activity
     * This is only for onePane mode, so that the two actitivies (movie list and movie detail) can communicate
     * If we are showing the favorites list and the user opens a movie and removes it from the favorite list
     * I will listen back here to refresh the favorite movie list
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == OPEN_MOVIE_DETAIL) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // the user has removed a movie from favorites in favorite list mode, so we have to refresh the movie list
                    onFavoritesChanged();
            }
        }
    }
}
