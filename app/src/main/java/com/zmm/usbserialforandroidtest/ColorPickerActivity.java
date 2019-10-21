package com.zmm.usbserialforandroidtest;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.zmm.usbserialforandroidtest.fftpack.RealDoubleFFT;

import net.margaritov.preference.colorpicker.ColorPickerView;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import static java.lang.Thread.sleep;

public class ColorPickerActivity extends Activity
        implements
        ColorPickerView.OnColorChangedListener,
        UsbService.MHandler, View.OnClickListener {

    private static final String TAG = "ColorPickerActivity";

    final static byte LIGHT_OFF = 0;
    final static byte LIGHT_ON = 1;
    final static byte LIGHT_SET_COLOER = 2;
    final static byte LIGHT_PWM_BREATH = 3;
    final static byte LIGHT_MIC_LIGHT = 4;
    final static int BYTE_START_FF = 255;
    final static byte BYTE_START_00 = 0;
    int soundBufferSize = 100;
    //    LinkedHashMap<Long, Double> soundBufferMap = new LinkedHashMap<>();
    int frequency = 8000;
    int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    RealDoubleFFT fftTrans;
    int blockSize = 256;
    RecordAudioTask recordAudioTask;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;

    private UsbService usbService;
    ColorPickerDome colorPicker;
    Switch lightSwitch, lightBreath, lightMusic, soundWave;
    private Button burnButton;
    LinearLayout brightnessLayout, sleepTimeLayout;
    ImageView musicView;
    TextView textView;

    static SeekBar sleepTime, brightness;
    private static byte BYTE_UART_SEND[] = {(byte) BYTE_START_FF, BYTE_START_00, 0, 0, 0, 0, 0, 0};
    private Activity MyActivity;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyActivity = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.color_picker_view);
        textView = findViewById(R.id.text_byte);

        colorPicker = findViewById(R.id.colorPicker);
        burnButton = findViewById(R.id.burn_button);
        burnButton.setOnClickListener(this);

        brightnessLayout = findViewById(R.id.brightness_layout);
        sleepTimeLayout = findViewById(R.id.sleep_layout);
        musicView = findViewById(R.id.music_view);
        sleepTimeLayout.setVisibility(View.GONE);

        brightness = findViewById(R.id.brightness);
        sleepTime = findViewById(R.id.sleep);

        lightSwitch = findViewById(R.id.light_switch);
        lightBreath = findViewById(R.id.light_breath);
        lightMusic = findViewById(R.id.light_music);
//        soundWave = findViewById(R.id.sound_wave);


        lightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    fillCommandHead(LIGHT_SET_COLOER);
                    fillCommandParameter((byte) brightness.getProgress());
                    brightnessLayout.setVisibility(View.VISIBLE);
                    fillColorBytes(colorPicker.getColor());
                } else {
                    fillCommandHead(LIGHT_OFF);
                    lightBreath.setChecked(false);
                    lightMusic.setChecked(false);
                    brightnessLayout.setVisibility(View.GONE);
                }

                countBytesSum();
                send(BYTE_UART_SEND);
            }
        });
        lightBreath.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    fillColorBytes(colorPicker.getColor());
                    lightSwitch.setChecked(true);
                    brightnessLayout.setVisibility(View.GONE);
                    sleepTimeLayout.setVisibility(View.VISIBLE);
                    fillCommandHead(LIGHT_PWM_BREATH);
                    fillCommandParameter((byte) sleepTime.getProgress());
                } else {
                    sleepTimeLayout.setVisibility(View.GONE);
                    if (lightSwitch.isChecked()) {
                        brightnessLayout.setVisibility(View.VISIBLE);
                        fillCommandParameter((byte) brightness.getProgress());
                    } else {
                        fillCommandHead(LIGHT_OFF);
                    }
                }
                countBytesSum();
                send(BYTE_UART_SEND);
            }
        });
        lightMusic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MyActivity, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                        lightMusic.setChecked(false);
                    } else {
                        fillColorBytes(colorPicker.getColor());
                        lightSwitch.setChecked(true);
                        musicView.setVisibility(View.VISIBLE);
                        recordAudioTask = new RecordAudioTask();
                        recordAudioTask.execute();
                        fillCommandHead(LIGHT_MIC_LIGHT);
                        fillRedColor((byte) 0);
                        fillGreenColor((byte) 0);
                        fillBlueColor((byte) 0);
                    }
                } else {
                    recordAudioTask.cancel(true);
                    musicView.setVisibility(View.GONE);
                }
                countBytesSum();
                send(BYTE_UART_SEND);
            }
        });

        fftTrans = new RealDoubleFFT(blockSize);

        bitmap = Bitmap.createBitmap(blockSize, 100, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.YELLOW);
        musicView.setImageBitmap(bitmap);

        lightMusic.setChecked(true);

        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                fillCommandParameter((byte) i);
                if (!lightSwitch.isChecked()) {
                    lightSwitch.setChecked(true);
                }
                send(BYTE_UART_SEND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sleepTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                fillCommandParameter((byte) i);
                send(BYTE_UART_SEND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        colorPicker.setOnColorChangedListener(this);
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
            usbService.setHandler(ColorPickerActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };


    public void send(byte[] bytes) {
        if (usbService != null) {
            usbService.write(bytes);
        }
//        Toast.makeText(getApplicationContext(), ByteToString(bytes), Toast.LENGTH_SHORT).show();
    }

    private void fillCommandHead(byte command) {
        BYTE_UART_SEND[2] = command;
    }

    private void fillCommandParameter(byte parameter) {
        BYTE_UART_SEND[3] = parameter;
    }

    public void fillColorBytes(int pixel) {
        int a = Color.alpha(pixel);
        BYTE_UART_SEND[4] = (byte) Color.red(pixel);
        BYTE_UART_SEND[5] = (byte) Color.green(pixel);
        BYTE_UART_SEND[6] = (byte) Color.blue(pixel);
    }

    public void fillRedColor(byte red) {
        BYTE_UART_SEND[4] = red;
    }

    public void fillGreenColor(byte green) {
        BYTE_UART_SEND[5] = green;
    }

    public void fillBlueColor(byte blue) {
        BYTE_UART_SEND[6] = blue;
    }

    private void countBytesSum() {
        BYTE_UART_SEND[7] = (byte) (BYTE_UART_SEND[2] + BYTE_UART_SEND[3] + BYTE_UART_SEND[4] + BYTE_UART_SEND[5] + BYTE_UART_SEND[6]);

    }

    public String ByteToString(byte[] data) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < data.length; i++)
            stringBuffer.append(data[i] + " ");
        return stringBuffer.toString();
    }

    boolean mv = true;
    @Override
    public void obtainMessage(int what, Object obj) {
        final byte[] data = (byte[]) obj;
        final StringBuilder string = new StringBuilder();
        boolean flag =false;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0xff && data[i + 1] == 0x00) {
               flag = true;
            }
            if (flag) {
                string.append(data[i]);
                if (mv){
                    mv = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.append(string);
                        }
                    });
                }
            }
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

    @Override
    public void onColorChanged(int color) {
        if (!lightSwitch.isChecked()) {
            lightSwitch.setChecked(true);
        }
        fillColorBytes(color);
        send(BYTE_UART_SEND);
    }

    @Override
    public void onClick(View view) {
        Intent burnIntent = new Intent(this, BurnActivity.class);
        startActivity(burnIntent);
    }

    private class RecordAudioTask extends AsyncTask<Void, double[], Void> {

        int count = 0;
        double record_RedMax = 0, record_RedMin, second_RedMax = 0, second_RedMin = 0;
        double redMax = 0, greenMax = 0, blueMax = 0;
        double Multiple = 1;
        int recordTime = 0, typeRecordTime;
        double light_red = 0;
        int soundLeapSum, FirstDrums_leap = 0, SecondDrums_leap = 0, rest_type = 0, drumsLeap_type = 0, restLeap_type = 0, restroy_drumsLeap_type = 0, restroy_restLeap_type = 0;
        double threshold = 0.01;
        double[] soundArray = new double[soundBufferSize];

        ArrayList[] typeCountRecord = new ArrayList[5];

        @Override
        protected Void doInBackground(Void... params) {
            try {
                int bufferSize = AudioRecord.getMinBufferSize(frequency,
                        channelConfig, audioFormat);
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency,
                        channelConfig, audioFormat, bufferSize);

                short[] audioBuffer = new short[blockSize];
                double[] toTrans = new double[blockSize];

                audioRecord.startRecording();

                while (lightMusic.isChecked()) {
                    int result = audioRecord.read(audioBuffer, 0, blockSize);

                    soundLeapSum = 0;
                    for (int i = 0; i < blockSize && i < result; i++) {
                        toTrans[i] = (double) audioBuffer[i] / Short.MAX_VALUE;
                    }

                    fftTrans.ft(toTrans);
                    publishProgress(toTrans);

                    double Multiple;
                    for (int i = 1; i < 11; i++) {
                        Multiple = Math.abs(toTrans[i]) / Math.abs(toTrans[i - 1]);
                        if (Multiple < 1.1 && Multiple > 0.9) {
//                            Log.w(TAG, "doInBackground: Multiple" + Multiple);
                            soundLeapSum += Multiple;
                        }

                    }


                    redMax = greenMax = blueMax = 0;

                    soundArray[count % soundBufferSize] = soundLeapSum;
                    count++;
                    if (count % soundBufferSize > recordTime) {
                        recordTime++;
                        record_RedMax = 0;
                        record_RedMin = Double.MAX_VALUE;
//                        typeList = new ArrayList<>();
                        for (int leap = 1; leap < soundBufferSize; leap++) {
                            double comp = 0;
                            double sum = 0;
                            for (int i = 0; i < soundBufferSize - leap; i += leap) {
                                comp += Math.abs(soundArray[i] - soundArray[i + leap]);               //获取相邻声音差值
                                sum += soundArray[i] + soundArray[i + leap];
//                                if (soundArray[i] > record_RedMax) {
//                                    record_RedMax = soundArray[i];
//                                    max_type = i;
//                                }
//                                if (soundArray[i] < record_RedMin) {
//                                    record_RedMin = soundArray[i];
//                                    min_type = i;
//                                }
                            }
                            double valueDouble = sum / comp;
                            if (valueDouble < record_RedMin) {
                                record_RedMin = valueDouble;
                                rest_type = leap;
                            }
                            if (valueDouble > record_RedMax) {
                                record_RedMax = valueDouble;
                                SecondDrums_leap = FirstDrums_leap;
                                FirstDrums_leap = leap;
                            }
                        }
                    }


//                    if (record_RedMax == 0 ){
//                        light_red = 255;
//                        record_RedMax = 1;
//                    }else {
//                        Multiple = 255 / record_RedMax;
//                        light_red = soundLeapSum * Multiple;
//                    }
//                    if (light_red > 255) {
//                        light_red = 255;
//                    }

                    int gcd1 = get_gcd(FirstDrums_leap, SecondDrums_leap);
                    restroy_drumsLeap_type = drumsLeap_type;
                    restroy_restLeap_type = restLeap_type;
                    if (gcd1 > 20) {
                        restLeap_type = gcd1;
                        drumsLeap_type = FirstDrums_leap > SecondDrums_leap ? SecondDrums_leap : FirstDrums_leap;
                    } else {
                        restLeap_type = FirstDrums_leap;
                        drumsLeap_type = SecondDrums_leap;
                        if (FirstDrums_leap > SecondDrums_leap) {
                            restLeap_type = SecondDrums_leap;
                            drumsLeap_type = FirstDrums_leap;
                        }
                    }
                    if (restroy_drumsLeap_type != 0 && restroy_restLeap_type != 0) {
                        restLeap_type -= (restLeap_type - restroy_restLeap_type) / 2;
                        drumsLeap_type -= (drumsLeap_type - restroy_drumsLeap_type) / 2;
                    }
                    if (drumsLeap_type != 0 && restLeap_type != 0) {
                        if (count % drumsLeap_type == 0) {
                            fillRedColor((byte) (255));
                        } else if (count % restLeap_type == 0) {
                            fillRedColor((byte) (100));
                        } else {
                            fillRedColor((byte) (0));
                        }
//                        if (count % gcdRest_type == 0) {
////                                Log.w(TAG, "onProgressUpdate: " + "当前鼓点 i :" + leap_type + " 大小： " + light_red + " 比例： " + Multiple);
//                            fillRedColor((byte) (0));
//                        }
                    }

                    send(BYTE_UART_SEND);
                    sleep(10);
                }
                audioRecord.stop();
            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording failed");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(double[]... values) {
            canvas.drawColor(Color.BLACK);
            double beca10_values;
            for (int i = 0; i < values[0].length; i++) {
                beca10_values = values[0][i] * 10;
                int downy = (int) (100 - beca10_values);
                int upy = 100;
                canvas.drawLine(i, downy, i, upy, paint);
                if (i < values[0].length / 2) {
                    if (greenMax < values[0][i]) {
                        greenMax = values[0][i];
                    }
                } else {
                    if (blueMax < values[0][i]) {
                        blueMax = values[0][i];
                    }
                }

            }

//            fillGreenColor((byte) (greenMax * 2.55));
//            fillBlueColor((byte) (blueMax * 2.55));
//            send(BYTE_UART_SEND);
            musicView.invalidate();

        }

    }

    public static int get_gcd(int a, int b) {       //最大公约数
        int max, min;
        max = (a > b) ? a : b;
        min = (a < b) ? a : b;

        if (min == 0) {
            Log.w(TAG, "get_gcd: done!: " + max);
            return 0;
        }

        if (max % min != 0) {
            return get_gcd(min, max % min);
        } else
            return min;

    }

    public static int get_lcm(int a, int b) {       //最小公倍数
        if (a == 0) {
            return b;
        }
        if (b == 0) {
            return a;
        }
        return a * b / get_gcd(a, b);
    }


}
