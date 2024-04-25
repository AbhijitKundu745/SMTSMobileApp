package com.psllab.smtsmobileapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.psllab.smtsmobileapp.InOutActivityNew;
import com.psllab.smtsmobileapp.R;
import com.psllab.smtsmobileapp.databases.InOutAssets;
import com.psllab.smtsmobileapp.databases.InventoryMaster;
import com.psllab.smtsmobileapp.helper.AppConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InOutAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    public ArrayList<HashMap<String, String>> tagList;
    private Context mContext;
    public int CURRENT_INDEX = -1;

    public InOutAdapter(Context context, ArrayList<HashMap<String, String>> tagList) {
        this.mInflater = LayoutInflater.from(context);
        this.tagList = tagList;
        this.mContext = context;
    }
    @Override
    public int getCount() {
        return tagList.size();
    }

    @Override
    public Object getItem(int i) {
        return tagList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.inout_adapter_layout, null);
            holder.textId = (TextView) convertView.findViewById(R.id.textId);
            holder.textKitId = (TextView) convertView.findViewById(R.id.textKitId);
            holder.imgStatus = (ImageView) convertView.findViewById(R.id.imgStatus);

            convertView.setTag(holder);
        } else {
            holder = (InOutAdapter.ViewHolder) convertView.getTag();
        }

        int id = position+1;
        holder.textId.setText(""+id);
        holder.textKitId.setText(tagList.get(position).get("assetName"));
        if (position%2!=0) {
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.red3));
        }
        else {
            convertView.setBackgroundColor(mContext.getResources().getColor(R.color.green1));
        }
        holder.imgStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CURRENT_INDEX = position;
                ((InOutActivityNew) mContext).onListItemClicked(tagList.get(position));
            }
        });

        return convertView;
    }
    public final class ViewHolder {
        public TextView textId;
        public TextView textKitId;
        public ImageView imgStatus;

    }
}
