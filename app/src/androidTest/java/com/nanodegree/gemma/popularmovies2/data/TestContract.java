package com.nanodegree.gemma.popularmovies2.data;

import android.net.Uri;
import android.test.AndroidTestCase;

import com.nanodegree.gemma.popularmovies2.data.MovieContract;

/**
 * Created by Gemma S. Lara Savill on 06/10/2015.
 */
public class TestContract extends AndroidTestCase {

    public static final String LOG = "PopularMovies_TestContract";

    /**
     * Testing creation of
     * content://com.nanodegree.gemma.popularmovies1/movies/popularity
     * content://com.nanodegree.gemma.popularmovies1/movies/vote_average
     */
    public void testBuildMoviesOrderByUri() {
        // by popularity
        String testOrder = "popularity";
        Uri testUri = MovieContract.MoviesColumns.buildMoviesOrderByUri(testOrder);
        String expectedUri = "content://com.nanodegree.gemma.popularmovies1/movies/popularity";
        System.out.println(LOG+ " testBuildMoviesOrderByUri: "+expectedUri);
        assertEquals("Order by popularity uri is OK", expectedUri, testUri.toString());

        // by rating
        testOrder = "vote_average";
        testUri = MovieContract.MoviesColumns.buildMoviesOrderByUri(testOrder);
        expectedUri = "content://com.nanodegree.gemma.popularmovies1/movies/vote_average";
        System.out.println(LOG+ " testBuildMoviesOrderByUri: "+expectedUri);
        assertEquals("Order by highest rated uri is OK", expectedUri, testUri.toString());

    }

}
