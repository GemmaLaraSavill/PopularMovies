package com.nanodegree.gemma.popularmovies2;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.nanodegree.gemma.popularmovies2.data.Constants;
import com.nanodegree.gemma.popularmovies2.data.MovieContract;
import com.squareup.picasso.Picasso;

/**
 * Created by Gemma S. Lara Savill on 05/10/2015.
 * Adapter used to show the movie posters in a grid view
 * Uses Picasso to load the thumbs
 */
public class MoviesAdapter extends CursorAdapter {

    private Context context;
    private final String LOG_TAG = MainActivity.LOG_TAG+" Adapter";


    public MoviesAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        this.context = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        String posterUrl = cursor.getString(cursor.getColumnIndex(MovieContract.MoviesColumns.COL_POSTER));
        String movieId = cursor.getString(cursor.getColumnIndex(MovieContract.MoviesColumns.COL_MOVIE_ID));
        String posterServerUrl = Constants.thumbUrl+Constants.thumbSize185+posterUrl;
//        Log.v(LOG_TAG, "getting poster from " + posterServerUrl);
//        Picasso.with(context).setIndicatorsEnabled(true);
//        Picasso.with(context).setLoggingEnabled(true);
        Picasso.with(context)
                .load(posterServerUrl)
//                .networkPolicy(NetworkPolicy.OFFLINE)
                                .into(viewHolder.posterView)
        ;
//        viewHolder.posterView.setText(posterUrl+" "+movieId);
    }

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView posterView;

        public ViewHolder(View view) {
            posterView = (ImageView) view.findViewById(R.id.grid_item_poster);
        }
    }
}
