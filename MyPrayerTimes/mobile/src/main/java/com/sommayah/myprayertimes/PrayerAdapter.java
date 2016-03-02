package com.sommayah.myprayertimes;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by sommayahsoliman on 2/25/16.
 */
public class PrayerAdapter extends RecyclerView.Adapter<PrayerAdapter.PrayerAdapterViewHolder> {
    private static final int VIEW_TYPE_NEXT = 0; //representing the next prayer
    private static final int VIEW_TYPE_ALL = 1;  //representing all other prayers

    private boolean mUseNextPrayerLayout = true;

    private Cursor mCursor;
    final private Context mContext;
    final private PrayerAdapterOnClickHandler mClickHandler;
    final private View mEmptyView;
    ArrayList<String> prayerTimes;

    PrayerAdapter(Context context, PrayerAdapterOnClickHandler ch, View emptyView){
        mContext = context;
        mClickHandler = ch;
        mEmptyView = emptyView;

    }

    public PrayerAdapter(ArrayList<String> array,Context context, PrayerAdapterOnClickHandler ch, View emptyView) {
        mContext = context;
        mClickHandler = ch;
        mEmptyView = emptyView;
        prayerTimes = new ArrayList<>();
        for(int i=0 ; i<array.size();i++){
            prayerTimes.add(array.get(i));
        }


    }

    @Override
    public PrayerAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if ( parent instanceof RecyclerView ) {
            int layoutId = -1;
            switch (viewType) {
                case VIEW_TYPE_NEXT: {
                    layoutId = R.layout.prayer_list_item_big;
                    break;
                }
                case VIEW_TYPE_ALL: {
                    layoutId = R.layout.prayer_list_item;
                    break;
                }
            }
            View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            view.setFocusable(true);
            return new PrayerAdapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }

    @Override
    public void onBindViewHolder(PrayerAdapterViewHolder holder, int position) {

        String name = "";
        switch (position){
            case 0:
                name = "Fajr";
                break;
            case 1:
                name = "SunRise";
                break;
            case 2:
                name = "Zuhr";
                break;
            case 3:
                name = "Asr";
                break;
            case 4:
                name = "Maghrib";
                break;
            case 5:
                name = "Isha";
                break;
            default:
                name = "error";
                break;

        }
        holder.mPrayerName.setText(name);
        holder.mPrayerTime.setText(prayerTimes.get(position));

    }

    @Override
    public int getItemCount() {
        return prayerTimes.size();
    }

    public void setUseNextPrayerLayout(boolean useBigLayout) {
        mUseNextPrayerLayout = useBigLayout;
    }


    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseNextPrayerLayout) ? VIEW_TYPE_NEXT : VIEW_TYPE_ALL;
    }

    public class PrayerAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final TextView mPrayerName;
        public final TextView mPrayerTime;
        public final TextView mTimeRemaining;

        public PrayerAdapterViewHolder(View view) {
            super(view);
            mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
            mPrayerName = (TextView) view.findViewById(R.id.prayer_name);
            mPrayerTime = (TextView) view.findViewById(R.id.prayer_time);
            mTimeRemaining = (TextView) view.findViewById(R.id.prayer_time_remaining);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

        }
    }

    public static interface PrayerAdapterOnClickHandler{
        void onClick(int prayerPos, PrayerAdapterViewHolder vh);
    }
}
