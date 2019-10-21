package com.zmm.usbserialforandroidtest;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class MAlertDialog extends AlertDialog implements AdapterView.OnItemClickListener {
    State state;
    public MAlertDialog(@NonNull Context context,State state) {
        super(context);
        this.state=state;
        ListView listView=new ListView(context);
        listView.setAdapter(new BA());
        listView.setOnItemClickListener(this);

        setView(listView);
      /*  setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });*/
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        state.open(adapterView.getItemAtPosition(i).toString());
        dismiss();
    }

    public interface State{
        void open(String file);
    }
    class BA extends BaseAdapter{
        String[]strings=new String[0];
        public BA(){
            strings=FileUtils.getfilesFromAssets(getContext(),"");
        }
        @Override
        public int getCount() {
            return strings.length;
        }

        @Override
        public Object getItem(int i) {
            return strings[i];
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TextView textView=new TextView(getContext());
            textView.setTextSize(22);
            textView.setText(strings[i]);
            return textView;
        }
    }

}
