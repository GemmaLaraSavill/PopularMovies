package com.nanodegree.gemma.popularmovies2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


/**
 * Created by Gemma S. Lara Savill on 08/10/2015.
 * Activity that will load the movie detail fragment
 */
public class MovieDetailActivity extends AppCompatActivity implements MovieDetailFragment.reloadListListener {

    private ActionBar actionBar;
    private boolean debug = false;

    // When showing movies in favorite list mode: if the user removes a movie from the favorites list and returns to the list
    // this movie should not be seen in the favorites list
    // I will pass a flag via activityResult to warn the movie poster activity to refresh the movie list in it's fragment
    // Do refresh movies: RESULT_OK, don't refresh movies: RESULT_CANCELED
    // I have overriden the back button to pass the intent result
    private int activityResult = RESULT_CANCELED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true); // don't forget to add parent activity in manifest (this took me a while)

        if (savedInstanceState == null) { // new
            // data to pass
            Bundle arguments = new Bundle();
            arguments.putParcelable(MovieDetailFragment.MOVIE_URI, getIntent().getData());
            if (debug) { Log.v("Movie Detail Activity", "data "+getIntent().getData()); }

            MovieDetailFragment movieDetailFragment = new MovieDetailFragment();
            movieDetailFragment.setArguments(arguments);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.movie_detail_container, movieDetailFragment)
                    .commit();
        }
    }

    public void actionBarSetTitle(String title) {
        actionBar.setTitle(title);
    }


    /**
     * Passed back by the movie detail fragment when the favorite status of a movie has been changed
     */
    @Override
    public void onFavoritesChanged() {
        //        if (debug) {  Log.v(LOG_TAG, "Detail frag warns that favorite has changed, so reload list"); }
        // warn the Activity holding the movie posters fragment to reload the movie list
        if (SettingsActivity.getPreferredSortOrder(this).equals(getResources().getString(R.string.pref_movies_favorites))) {
            // we are in favorite list mode so we have to refresh the movie list
            activityResult = RESULT_OK;
        }
    }

    /**
     * Used to pass the activity result back to the poster grid activity
     */
    @Override
    public void onBackPressed() {
        Intent mIntent = new Intent();
        setResult(activityResult, mIntent);
        super.onBackPressed();
    }
}
