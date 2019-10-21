package com.zmm.usbserialforandroidtest;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import static android.os.Looper.getMainLooper;

class MBaseAdapter extends BaseAdapter {
    ArrayList<String> arrayList = new ArrayList<>();
    public Context context;

    public MBaseAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int i) {
        return arrayList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TextView text = new TextView(context);
        text.setText(arrayList.get(i));
        text.setTextColor(Color.GREEN);
        return text;
    }
    public void add(String text) {
        arrayList.add(0, text);
        while (arrayList.size() > 100) {
            arrayList.remove(100);
        }
        if (getMainLooper() == Looper.myLooper()) {
            notifyDataSetChanged();
        } else {
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }
}