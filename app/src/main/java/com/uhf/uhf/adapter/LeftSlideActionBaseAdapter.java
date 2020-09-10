package com.uhf.uhf.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.uhf.uhf.R;

public abstract class LeftSlideActionBaseAdapter extends BaseAdapter {
    protected Context mContext;
    public OnItemActionListener mListener;

    public LeftSlideActionBaseAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public final View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.view_left_slide_action, parent, false);

            holder = new ViewHolder();
            holder.viewContent = (RelativeLayout) convertView.findViewById(R.id.view_content);
            holder.tvRmove = (TextView) convertView.findViewById(R.id.tv_remove);
            convertView.setTag(holder);

            // viewChild是实际的界面
            holder.viewChild = getSubView(position, null, parent);
            holder.viewContent.addView(holder.viewChild);
        } else {
            holder = (ViewHolder) convertView.getTag();
            getSubView(position, holder.viewChild, parent);
        }
        holder.tvRmove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemRemove(position);
                    notifyDataSetChanged();
                }
            }
        });
        return convertView;
    }

    public abstract View getSubView(int position, View convertView, ViewGroup parent);

    private static class ViewHolder {
        RelativeLayout viewContent;
        View viewChild;
        View tvRmove;
    }

    public interface OnItemActionListener {
         void onItemRemove(int position);
    }
}