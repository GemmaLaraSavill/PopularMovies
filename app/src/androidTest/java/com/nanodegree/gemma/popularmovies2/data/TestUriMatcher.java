package com.nanodegree.gemma.popularmovies2.data;

import android.content.UriMatcher;
import android.net.Uri;
import android.test.AndroidTestCase;

/**
 * Created by Gemma S. Lara Savill on 10/11/2015.
 */
public class TestUriMatcher extends AndroidTestCase {

    public static final String LOG = "PopularMovies2_TestUriMatcher";

    // tests OK on 10/11/2015
    public void testUriMatcher() {
        UriMatcher testMatcher = MovieProvider.buildUriMatcher();

        Uri uriQuery = MovieContract.MoviesColumns.buildMoviesUri();

        // order by most popular
        String testOrder = "popularity";
        int expectedMatch = MovieProvider.MOVIES_MOST_POPULAR;
        uriQuery = MovieContract.MoviesColumns.buildMoviesOrderByUri(testOrder);
        System.out.println(uriQuery);
        assertEquals("Error: the ORDER BY popularity URI is matched incorrectly", expectedMatch, testMatcher.match(uriQuery));

        // order by highest rated
        testOrder = "vote_average";
        expectedMatch = MovieProvider.MOVIES_HIGHEST_RATED;
        uriQuery = MovieContract.MoviesColumns.buildMoviesOrderByUri(testOrder);
        assertEquals("Error: the ORDER BY vote_average URI is matched incorrectly", expectedMatch, testMatcher.match(uriQuery));

        // order favorite
        testOrder = "favorite";
        expectedMatch = MovieProvider.MOVIES_FAVORITE;
        uriQuery = MovieContract.MoviesColumns.buildMoviesOrderByUri(testOrder);
        assertEquals("Error: the ORDER BY popularity URI is matched incorrectly", expectedMatch, testMatcher.match(uriQuery));

        String testID = "12345";

        // select movie by id
        expectedMatch = MovieProvider.MOVIE_BY_ID;
        uriQuery = MovieContract.MoviesColumns.buildMovieByIdUri(testID);
//        System.out.println(uriQuery);
        assertEquals("Error: the select movie by ID URI is matched incorrectly", expectedMatch, testMatcher.match(uriQuery));


        // movie trailers
        expectedMatch = MovieProvider.MOVIES_REVIEWS;
        uriQuery = MovieContract.MoviesColumns.buildMovieReviewsUri(testID);
        assertEquals("Error: the get movie reviews URI is matched incorrectly", expectedMatch, testMatcher.match(uriQuery));

        // movie trailers
        expectedMatch = MovieProvider.MOVIE_TRAILERS;
        uriQuery = MovieContract.MoviesColumns.buildMovieTrailersUri(testID);
        assertEquals("Error: the get movie trailers URI is matched incorrectly", expectedMatch, testMatcher.match(uriQuery));

        // toggle movie favorite
        expectedMatch = MovieProvider.MOVIE_TOGGLE_FAVORITE;
        uriQuery = MovieContract.MoviesColumns.buildMovieToggleFavoriteUri(testID);
        assertEquals("Error: the toggle favorite movie URI is matched incorrectly", expectedMatch, testMatcher.match(uriQuery));

        // toggle delete nonfavorite movies
        expectedMatch = MovieProvider.MOVIE_DELETE_NOTFAVORITE;
        uriQuery = MovieContract.MoviesColumns.buildDeleteNonFavMoviesUri();
        assertEquals("Error: the DELETE not favorite movies is matched incorrectly", expectedMatch, testMatcher.match(uriQuery));



    }
}
