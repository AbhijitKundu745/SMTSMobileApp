package com.psllab.smtsmobileapp.adapters;


import static com.psllab.smtsmobileapp.helper.AppConstants.ASSET_NAME;
import static com.psllab.smtsmobileapp.helper.AppConstants.ASSET_STATUS;
import static com.psllab.smtsmobileapp.helper.AppConstants.ID;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.psllab.smtsmobileapp.InOutActivityNew;
import com.psllab.smtsmobileapp.KitDataUploadActivity;
import com.psllab.smtsmobileapp.R;
import com.psllab.smtsmobileapp.databases.InventoryMaster;
import com.psllab.smtsmobileapp.helper.AppConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class InventoryAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    public ArrayList<HashMap<String, String>> tagList;
    private Context mContext;
    public int KIT_INDEX = -1;

    public InventoryAdapter(Context context, ArrayList<HashMap<String, String>> tagList) {
        this.mInflater = LayoutInflater.from(context);
        this.tagList = tagList;
        this.mContext = context;
    }
    public int getCount() {
        // TODO Auto-generated method stub
        return tagList.size();
    }

    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return tagList.get(arg0);
    }
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return arg0;
    }
    public View getView(int position, View convertView, ViewGroup parent) {
        View view2;
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            view2 = this.mInflater.inflate(R.layout.inventory_adapter_layout, (ViewGroup) null);
            viewHolder.textId = (TextView) view2.findViewById(R.id.textId);
            viewHolder.textKitId = (TextView) view2.findViewById(R.id.textKitId);
            viewHolder.imgStatus = (ImageView) view2.findViewById(R.id.imgStatus);
            view2.setTag(viewHolder);
        } else {
            view2 = convertView;
            viewHolder = (ViewHolder) convertView.getTag();
        }
        int id = position+1;
        viewHolder.textId.setText(""+id);
        viewHolder.textKitId.setText((String) this.tagList.get(position).get(AppConstants.ASSET_NAME));
        if (position % 2 != 0) {
            view2.setBackgroundColor(this.mContext.getResources().getColor(R.color.red3));
        } else {
            view2.setBackgroundColor(this.mContext.getResources().getColor(R.color.green1));
        }
        viewHolder.imgStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KIT_INDEX = position;
                ((KitDataUploadActivity) mContext).onListItemClicked(tagList.get(position));
            }
        });
        return view2;
    }
    public  void setSelectItem(int select) {
        if(selectItem==select){
            selectItem=-1;

        }else {
            selectItem = select;

        }

    }
    private int  selectItem=-1;
    public final class ViewHolder {
        public TextView textId;
        public TextView textKitId;
        public ImageView imgStatus;

    }

}
