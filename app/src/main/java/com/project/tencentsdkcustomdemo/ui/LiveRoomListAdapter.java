package com.project.tencentsdkcustomdemo.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.project.tencentsdkcustomdemo.R;

import java.util.List;

public class LiveRoomListAdapter extends BaseAdapter {

    private List<String> mRoomList;
    private Context mContext;

    public LiveRoomListAdapter(Context context, List<String> roomList) {
        mRoomList = roomList;
        mContext = context;
    }

    @Override
    public int getCount() {
        if (mRoomList == null) {
            return 0;
        }
        return mRoomList.size();
    }

    @Override
    public Object getItem(int position) {
        if (mRoomList == null) {
            return null;
        }
        return mRoomList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.live_room_item, parent, false);
            holder = new ViewHolder();
            holder.roomIdText = convertView.findViewById(R.id.tv_room_id);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.roomIdText.setText("直播间ID：" + mRoomList.get(position));
        return convertView;
    }

    static class ViewHolder {
        TextView roomIdText;
    }

}

