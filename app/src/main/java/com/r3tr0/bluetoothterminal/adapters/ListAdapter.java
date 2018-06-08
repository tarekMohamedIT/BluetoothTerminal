package com.r3tr0.bluetoothterminal.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.r3tr0.bluetoothterminal.interfaces.OnItemClickListener;

import java.util.ArrayList;

/**
 * Created by r3tr0 on 5/25/18.
 */

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ListViewHolder> {
    private Context context;
    private ArrayList<String> stringsList;
    private OnItemClickListener onItemClickListener;

    public ListAdapter(Context context, ArrayList<String> stringsList) {
        this.context = context;
        this.stringsList = stringsList;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public ArrayList<String> getStringsList() {
        return stringsList;
    }

    @NonNull
    @Override
    public ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout layout = new FrameLayout(context);
        layout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView textView = new TextView(context);
        textView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.addView(textView);

        return new ListViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull final ListViewHolder holder, int position) {
        final int pos = position;
        holder.listItemTextView.setText(stringsList.get(position));

        if (onItemClickListener != null)
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onClick(holder.itemView, pos);
                }
            });
    }

    @Override
    public int getItemCount() {
        return stringsList.size();
    }

    public class ListViewHolder extends RecyclerView.ViewHolder{

        TextView listItemTextView;
        public ListViewHolder(View itemView) {
            super(itemView);
            listItemTextView = (TextView) ((FrameLayout)itemView).getChildAt(0);
        }
    }
}
