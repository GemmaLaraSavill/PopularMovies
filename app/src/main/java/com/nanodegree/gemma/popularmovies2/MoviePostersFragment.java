package com.nanodegree.gemma.popularmovies2;


import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.nanodegree.gemma.popularmovies2.data.Constants;
import com.nanodegree.gemma.popularmovies2.data.MovieContract;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Gemma S. Lara Savill on 05/10/2015.
 * A fragment that will show a grid of movie posters
 * The movies will be loaded using the themovieDB.org API
 * and stored in the local database via content provider
 */
public class MoviePostersFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, FetchMovieData.FetchMoviesTaskResponse {

    private static final int POSTERS_LOADER = 0; // loader key
    private static final String SELECTED_KEY = "selected_movie"; // selected movie key
    private static final String DATA_SOURCE = "data_source"; // data source key, local or server
    private MoviesAdapter mMoviesAdapter;
    private GridView mGridView;
    private String mSortOrder;
    private int mPosition;
    private MoviePostersFragment mCallback;
    private TextView mEmptyView;

    private String mDataSource = "local"; // local or server, let's try and provide local data when possible
    private boolean loadDefaultMovieFlag = false; // flag to load a default movie when user changes order in setting in dual pane, this is used so that the detail fragment always has data to show when no movie has been selected by user

    private boolean debug = false;
    private final String LOG_TAG = MainActivity.LOG_TAG+" PosterFrag";



    public MoviePostersFragment() { }

    @Override
    public void onCompleted() {
        getLoaderManager().restartLoader(POSTERS_LOADER, null, mCallback); // restarts the loader
    }


    // Container Activity must implement this interface
    public interface OnMovieSelectedListener {
        public void onMovieSelected(Uri movieUri); // user has selected a movie
        public void loadDefaultMovie(Uri movieUri); // movie list has changed, load default movie
    }

    // Container Activity must implement this interface
    public interface OnDataLoadedListener {
        public void loadDefaultMovie(Uri movieUri);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Added this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(POSTERS_LOADER, null, this); // start movie data loader
        mCallback = this;
        super.onActivityCreated(savedInstanceState);
    }


    /**
     * Called when the movie list data has been loaded
     * We pass in the id of the movie that should be loaded by default
     * into the detail fragment if we are in two pane mode
     * We pass this to the Activity so it can decide what to do
     * @param movieId
     */
    public void loadDefaultMovie(String movieId) {
        // warn the Activity to load default movie if we are in two pane mode
        Uri movieUri = MovieContract.MoviesColumns.buildMovieByIdUri(movieId);
        if (debug) {
            Log.v(LOG_TAG, "sending loadDefaultMovie "+movieId);
        }
        ((OnMovieSelectedListener) getActivity()).loadDefaultMovie(movieUri);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // get sort parameter from preferences
        mSortOrder = SettingsActivity.getPreferredSortOrder(getActivity());
        if (debug) {
            Log.v(LOG_TAG, "onCreateView showing movies by sort order " + mSortOrder);
        }

        mMoviesAdapter = new MoviesAdapter(getActivity(), null, 0);
        View rootView = inflater.inflate(R.layout.fragment_movie_posters, container, false);

        // Get a reference to the GridView, and attach adapter to it.
        mGridView = (GridView) rootView.findViewById(R.id.grid_posters);
        mGridView.setAdapter(mMoviesAdapter);
        // empty view for when the list is empty
        mEmptyView = (TextView)rootView.findViewById(R.id.noMoviesFound);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                String movieId = cursor.getString(cursor.getColumnIndex(MovieContract.MoviesColumns.COL_MOVIE_ID));
                Uri movieUri = MovieContract.MoviesColumns.buildMovieByIdUri(movieId);
                ((OnMovieSelectedListener) getActivity()).onMovieSelected(movieUri);
                mPosition = position;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // the GridView probably hasn't even been populated yet. Actually perform the
            // the swapout in loadFinished
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
            loadDefaultMovieFlag = false;
        } else {
            loadDefaultMovieFlag = true;
        }

        // let's get some movies
        loadMovies();

        return rootView;
    }

    /**
     * Loads the movies
     * If more than 7 days have gone by it will query the server for fresh data
     * through fetchMovieDataFromServer()
     * If not it will load the cursor from the DB
     */
    public void loadMovies() {
        if (mSortOrder.equals(getActivity().getResources().getString(R.string.pref_movies_favorites))) {
            // favorite movies always come from the local data source (DB via content provider)
            mDataSource = "local";
            getLoaderManager().restartLoader(POSTERS_LOADER, null, this);
            if (debug) {
                Log.v(LOG_TAG, "Loading favorite movies from local data");
            }
        } else {
            // get data fresh data from server every 7 days
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
                fetchMovieDataFromServer();
                // save last refresh date
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("lastupdate", nowDate.getTime());
                editor.commit();
                mDataSource = "server";
            } else {
                // will use local data to save battery
                mDataSource = "local";
                getLoaderManager().restartLoader(POSTERS_LOADER, null, this);
            }
            if (debug) {
                Log.v(LOG_TAG, "Serving non-favorite movies from " + mDataSource);
            }
        }
    }



    @Override
    public void onSaveInstanceState(Bundle outState) {
        // when tablets rotate, the currently selected list item needs to be saved
        // when no click is selected mPosition will be set to ListView.INVALID_POSITION
        // so check that before restoring
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        outState.putString(DATA_SOURCE, mDataSource);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mDataSource = savedInstanceState.getString(DATA_SOURCE);
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }
        super.onViewStateRestored(savedInstanceState);
    }



    /**
     * Starts the background task to go and fetch the movie data from the server
     * When the task is finished it will the cursor will load the data into the UI
     */
    private void fetchMovieDataFromServer() {
//        Toast.makeText(getActivity(), "Querying server for movies data", Toast.LENGTH_LONG).show();
        if (debug) {
            Log.v(LOG_TAG, "fetchMovieDataFromServer starting AsyncTask FetchMovieData");
        }
        FetchMovieData getMovieDataTask = new FetchMovieData(getActivity());
        // implemented interface for when data refresh has finished:
        // when finished task will call onCompleted and will reload the cursor
        getMovieDataTask.response = this;
        getMovieDataTask.execute(mSortOrder);

    }

    /**
     * If the user has come back from settings after changing the movies he wants from server
     */
    @Override
    public void onResume() {
        super.onResume();
        String sortOrder = SettingsActivity.getPreferredSortOrder(getActivity());
        if (sortOrder != mSortOrder) {
            loadDefaultMovieFlag = true; // will swap the movie in the detail fragment
            mSortOrder = sortOrder;
            Log.v(LOG_TAG, "onResume with new sortOrder " + sortOrder);
            if (mSortOrder.equals(getActivity().getResources().getString(R.string.pref_movies_favorites))) {
                loadMovies();
            } else {
                if (debug) { Log.v(LOG_TAG, "onResume fetchMovieDataFromServer()"); }
//                // if not favorite get them from server
                fetchMovieDataFromServer();
            }
        }
        if (mPosition != ListView.INVALID_POSITION) {
            mGridView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (debug) {  Log.v(LOG_TAG, "In onCreateLoader"); }
        // get data
        Uri uriQuery = MovieContract.MoviesColumns.buildMoviesOrderByUri(mSortOrder);
        Log.v(LOG_TAG, "uriQuery: "+uriQuery);
            return new CursorLoader(
                    getActivity(),
                    uriQuery,
                    null,
                    null,
                    null,
                    null
            );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor moviesCursor) {
        if (debug) { Log.v(LOG_TAG, "In onLoadFinished, " + loader.getId() + " received cursor of " + moviesCursor.getCount()); }
        if(loader.getId() == POSTERS_LOADER) {
            if (moviesCursor.getCount() == 0) { // no movies found!!
                if (mSortOrder.equals(getActivity().getResources().getString(R.string.pref_movies_favorites))) {
                    mEmptyView.setText(R.string.noFavMoviesFound);
                    mEmptyView.setVisibility(View.VISIBLE);
                    mGridView.setVisibility(View.GONE);
                    // interesting, have to use runnable here for a small delay to ensure that the cursor is loaded
                    // Source StackOverflow
                    // http://stackoverflow.com/questions/21983163/performitemclick-on-first-listview-item-after-onloadfinished?lq=1
                    // http://stackoverflow.com/questions/14005936/listview-requires-2-setselections-to-scroll-to-item
                    mGridView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ((OnMovieSelectedListener) getActivity()).onMovieSelected(null);
                        }
                    }, 200);
                } else {
                        mEmptyView.setText(R.string.noMoviesFound);
                        mEmptyView.setVisibility(View.VISIBLE);
                        mGridView.setVisibility(View.GONE);
                        // use of runnable to unload empty detail page
                        mGridView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ((OnMovieSelectedListener) getActivity()).onMovieSelected(null);
                            }
                        }, 200);
                    }
            } else { // I have some movies to show
                mMoviesAdapter.swapCursor(moviesCursor);
                if (debug) {
                    Log.v(LOG_TAG, "In onLoadFinished, swapCursor");
                }
                if (moviesCursor.getCount() > 0) {
                    if (loadDefaultMovieFlag) { // if I want the default movie loaded, when list changes
                        loadDefaultMovieFlag = false;
                        moviesCursor.moveToFirst();
                        final String firstMovieId = moviesCursor.getString(moviesCursor.getColumnIndex(MovieContract.MoviesColumns.COL_MOVIE_ID));
                        // interesting, have to use runnable here for a small delay to ensure that the cursor is loaded
                        // Source StackOverflow
                        // http://stackoverflow.com/questions/21983163/performitemclick-on-first-listview-item-after-onloadfinished?lq=1
                        // http://stackoverflow.com/questions/14005936/listview-requires-2-setselections-to-scroll-to-item
                        mGridView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loadDefaultMovie(firstMovieId);
                            }
                        }, 500);
                    }

                }
                // just in case we are returning from an empty favorites list
                mGridView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
                if (mPosition != ListView.INVALID_POSITION) {
                    mGridView.smoothScrollToPosition(mPosition);
                }


            }
        }

    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
//        Log.v(LOG_TAG, "In onLoaderReset");
        mMoviesAdapter.swapCursor(null);
    }


}
