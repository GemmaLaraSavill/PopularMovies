package com.nanodegree.gemma.popularmovies2.data;

/**
 * Created by Gemma S. Lara Savill on 09/10/2015.
 * A class with all the constants
 */
public class Constants {

    // to load the poster thumbs thumbUrl + thumbSize + posterUrl
    // url for loading the movie poster thumbs
    public static final String thumbUrl = "http://image.tmdb.org/t/p/";
    // different sizes available for these poster thumbs
    public static final String thumbSize92 = "w92";
    public static final String thumbSize154 = "w154";
    public static final String thumbSize185 = "w185"; // recommended
    public static final String thumbSize342 = "w342";
    public static final String thumbSize500 = "w500";
    public static final String thumbSize780 = "w780";

    // days before I requery the server for fresh data
    public static final int dataCacheInDays = 7;

    // API key for themovieDB.org
	// Go to their website, request your own and paste it here
    public static final String API_KEY = "";
}
