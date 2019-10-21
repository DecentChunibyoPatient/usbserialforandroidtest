package com.zmm.usbserialforandroidtest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.BreakIterator;
import java.util.Set;

public class BurnActivity extends AppCompatActivity implements MAlertDialog.State,UsbService.MHandler, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "BurnActivity";
    private UsbService usbService;
    ListView listView;
    EditText editText;
    TextView fileName;
    static MBaseAdapter mBaseAdapter;
    static Activity activity;
    BurnThread burnThread;
    Switch aSwitch;
    byte[]fileData;
    boolean Handshake=false;
    Button chooseFile;
    TextView startText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity=this;
        setContentView(R.layout.burn_activity);
        (aSwitch = findViewById(R.id.start)).setOnCheckedChangeListener(this);
        findViewById(R.id.send).setOnClickListener(this);
        findViewById(R.id.burnFile).setOnClickListener(this);
        (fileName=findViewById(R.id.fileName)).setOnClickListener(this);
        startText=findViewById(R.id.startText);
        chooseFile=findViewById(R.id.chooseFile);
        chooseFile.setOnClickListener(this);
        editText = findViewById(R.id.editText);
        editText.setFocusable(false);
        listView = findViewById(R.id.listview);
        listView.setAdapter(mBaseAdapter = new MBaseAdapter(getApplicationContext()));
        mAlertDialog=new MAlertDialog(this, this);
        //open("Slow.bin");
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();
        startService(UsbService.class, usbConnection, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }


    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(BurnActivity.this);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Handshake=false;
            usbService = null;
            burnThread = null;
        }
    };

    @Override
    public void obtainMessage(int what, Object obj) {
        final byte[] data = (byte[]) obj;
        if (data.length>0){
            if (burnThread!=null){
                burnThread.write(data);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBaseAdapter.add(HexUtils.byte2HexStr(data, data.length));
                }
            });
        }
    }

    @Override
    public void obtainMessage(final int what) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                switch (what) {
                    case UsbService.CTS_CHANGE:
                        Toast.makeText(getApplicationContext(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                        break;
                    case UsbService.DSR_CHANGE:
                        Toast.makeText(getApplicationContext(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
    }
    MAlertDialog mAlertDialog;
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.send: {
                if (usbService!=null){
                    usbService.write(editText.getText().toString().getBytes());
                }
            }break;
            case R.id.chooseFile: {
                if (!mAlertDialog.isShowing()){
                    mAlertDialog.show();
                }
            }break;
            case R.id.burnFile: {
                if (Handshake){
                    if (fileData!=null){
                        startText.setText("烧录开始！");
                        Toast.makeText(getApplicationContext(),"烧录开始",Toast.LENGTH_SHORT).show();
                        burnThread=new BurnThread(usbService, editData(fileData), 1, new BurnThread.State() {
                            @Override
                            public void end() {
                                System.out.println("BurnFileData end");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        startText.setText("烧录完成！");
                                        Toast.makeText(getApplicationContext(),"烧录完成！",Toast.LENGTH_SHORT).show();
                                        Handshake=false;
                                    }
                                });
                            }
                        });
                        burnThread.start();
                    }else {
                        Toast.makeText(getApplicationContext(),"没有数据，请确认文件！",Toast.LENGTH_SHORT).show();
                    }

                }else {
                    startText.setText("没有握手的设备");
                    Toast.makeText(getApplicationContext(),"没有握手的设备",Toast.LENGTH_SHORT).show();
                }

            }break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.start: {
                if (b) {
                    if ( usbService != null) {
                        if (burnThread!=null){
                            if (burnThread.isRun())
                                burnThread.Stop();
                        }
                        burnThread = new BurnThread(usbService,BurnData.BurnData1, 9,new BurnThread.State() {
                            @Override
                            public void end() {
                                BurnActivity.w("BurnData1 end");
                                burnThread=new BurnThread(usbService, BurnData.BurnData2, 1, new BurnThread.State() {
                                    @Override
                                    public void end() {
                                        BurnActivity.w("BurnData2 end");
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                aSwitch.setChecked(false);
                                                Handshake=true;
                                                startText.setText("牵手成功");
                                                Toast.makeText(getApplicationContext(),"牵手成功",Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                    }
                                });
                                burnThread.start();
                            }
                        });
                        if (!burnThread.isRun()) {
                            burnThread.start();
                        } else {
                            startText.setText("正在握手");
                            Toast.makeText(getApplicationContext(), "正在握手", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        startText.setText("没有连接的USB设备");
                        Toast.makeText(getApplicationContext(), "没有连接的USB设备", Toast.LENGTH_SHORT).show();
                        aSwitch.setChecked(false);
                    }
                } else {
                    burnThread.Stop();
                }

            }
            break;
        }
    }

    static public void w(final Object o){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBaseAdapter.add(o.toString());
            }
        });
    }
    static public void w(final String TAG, final Object o){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBaseAdapter.add(TAG+":"+o);
            }
        });
    }

    @Override
    public void open(String file) {
        fileData=FileUtils.readStringFromAssets(getApplicationContext(),file);
        fileName.setText(file+" 长度："+HexUtils.byte2HexStr(HexUtils.TotalToBytes(fileData.length,256,4),4));
        byte[][] bytes=editData(fileData);
        for (byte[]bytes1:bytes){
            System.out.println(HexUtils.byte2HexStr(bytes1,bytes1.length));
        }
    }
    public byte[][] editData(byte[] data){
        if (data==null)return null;
        if (data.length<=0)return null;
        int s=(data.length-48)/56+2;
        int l=(data.length-48)%56==0?s:s+1;
        byte[][]newData=new byte[l][64];
        newData[0][0]= (byte) 0xA4;
        newData[0][8]= (byte) 0x01;
        newData[1][0]= (byte) 0xA0;
        System.arraycopy(HexUtils.TotalToBytes(fileData.length,256,4),0,newData[1],12,4);
        System.arraycopy(data,0,newData[1],16,48);
        for (int i=2;i<newData.length-1;i++){
            System.arraycopy(data,(i-2)*56+48,newData[i],8,56);
        }
        if ((data.length-48)%56==0){
            System.arraycopy(data,data.length-56,newData[newData.length-1],8,56);
        }else {
            System.arraycopy(data,data.length-((data.length-48)%56),newData[newData.length-1],8,(data.length-48)%56);
        }
        return newData;
    }
}
