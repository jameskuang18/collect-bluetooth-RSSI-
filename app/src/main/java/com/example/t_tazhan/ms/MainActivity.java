package com.example.t_tazhan.ms;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.example.t_tazhan.ms.room.Constant;
import com.example.t_tazhan.ms.room.FileSave;
import com.example.t_tazhan.ms.taskAdapter.DeviceBsAdapter;
import com.example.t_tazhan.ms.taskAdapter.DumpBsTask;
import com.example.t_tazhan.ms.util.BleUtil;
import com.example.t_tazhan.ms.util.DateUtil;
import com.example.t_tazhan.ms.util.FFT4g;
import com.example.t_tazhan.ms.util.ScannedDevice;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Set;


import static com.example.t_tazhan.ms.room.Constant.map;

public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback,SensorEventListener,TextToSpeech.OnInitListener{

    private static final int AXIS_NUM = 3;
    private static final int MATRIX_SIZE = 16;
    private static final String DUMP_PATH = "/BSScanner/";
    private static final String TAG_LICENSE = "license";
    private static int[] mSampleRates = new int[]{8000, 11025, 22050, 44100};
    int SAMPLING_RATE = 44100;
    int FFT_SIZE = 4096;
    double dB_baseline = Math.pow(2, 15) * FFT_SIZE * Math.sqrt(2);

    double resol = ((SAMPLING_RATE / (double) FFT_SIZE));
    AudioRecord audioRec = null;
    int bufSize;
    Thread fft;
    String fftPath;
    private BluetoothAdapter mBTAdapter;
    private boolean mBackKeyPressed = false;
    private boolean mIsScanning;
    private boolean r2Enabled;
    private CountDownTimer mCountDownTimer;
    private DeviceBsAdapter mDeviceBsAdapter;
    private DumpBsTask mDumpBsTask;
    private EditText white_list_bs;
    private float[] attitude = new float[AXIS_NUM];
    private float[] geomagnetic = new float[AXIS_NUM];
    private float[] gravity = new float[AXIS_NUM];
    private float[] I = new float[MATRIX_SIZE];
    private float[] inR = new float[MATRIX_SIZE];
    private float[] outR = new float[MATRIX_SIZE];
    private float[] orientation = new float[AXIS_NUM];
    private NumberPicker pointNumber, scanRepeat, scanTime;
    private SensorManager mSensorManager;
    private TextToSpeech tts;
    private TextView value, values;
    private Thread thr;
    private ToneGenerator mToneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
    private Runnable runnable =
            new Runnable() {
                @Override
                public void run() {
                    final int scanRepeatValue = (scanRepeat.getValue() > 0) ? scanRepeat.getValue() : 1;
                    final int scanTimeValue = (scanTime.getValue() > 0) ? scanTime.getValue() : 1;
                    speechText(getString(R.string.speech_started));

                    for (int i = 0; i < scanRepeatValue; i++) {
                        if (r2Enabled) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    scanRepeat.setBackgroundColor(Color.RED);
                                    scanTime.setBackgroundColor(Color.RED);
                                }
                            });
                            startBsScan();
                            for (int j = 0; j < scanTimeValue; j++) {
                                sleep(1000L);
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        scanTime.setValue(scanTime.getValue() - 1);
                                    }
                                });
                            }
                            stopBsScan();
                            playTone();
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    scanRepeat.setBackgroundColor(Color.YELLOW);
                                    scanTime.setBackgroundColor(Color.YELLOW);
                                }
                            });
                            audiorec(pointNumber.getValue(), scanTimeValue);
                            speechText(Integer.toString(i));
                        } else {
                            stopBsScan();
                            speechText(getString(R.string.speech_stopped));
                            break;
                        }
                        runOnUiThread(new Runnable() {
                            public void run() {
                                pointNumber.setValue(pointNumber.getValue() + 1);
                                scanRepeat.setValue(scanRepeat.getValue() - 1);
                                scanTime.setValue(scanTimeValue);
                            }
                        });
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            scanRepeat.setValue(scanRepeatValue);
                        }
                    });
                    if (r2Enabled) {
                        speechText(getString(R.string.speech_move));
                    } else {
                        speechText(getString(R.string.speech_move_after_check));
                    }
                    runOnUiThread(new Runnable() {
                        public void run() {
                            scanRepeat.setBackgroundColor(Color.WHITE);
                            scanTime.setBackgroundColor(Color.WHITE);
                        }
                    });
                }
            };

    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT}) {
                for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                    try {
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {

                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return null;
    }

    private void audiorec(final int pointNumber, final int scanTimeVal) {
            audioRec = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufSize * 2);
         audioRec = findAudioRecord();
        audioRec.startRecording();

        byte buf[] = new byte[bufSize * 2];
        final long start = System.currentTimeMillis();
        long th = 1000 * scanTimeVal;

        double[] dbfs = new double[FFT_SIZE / 2];
        Arrays.fill(dbfs, 0);

        long pre_ti = 0;
        while (true) {
            long t = System.currentTimeMillis() - start;
            if (t > th)
                break;
            final int ti = (int) (t / 1000);
            if (ti != pre_ti) {
                pre_ti = ti;
                runOnUiThread(new Runnable() {
                    public void run() {
                        scanTime.setValue(scanTimeVal - ti);
                    }
                });
            }

            audioRec.read(buf, 0, buf.length);

            ByteBuffer bf = ByteBuffer.wrap(buf);
            bf.order(ByteOrder.LITTLE_ENDIAN);
            short[] s = new short[FFT_SIZE];
            for (int i = bf.position(); i < bf.capacity() / 2; i++) {
                s[i] = bf.getShort();
            }

            FFT4g fft = new FFT4g(FFT_SIZE);
            double[] FFTdata = new double[FFT_SIZE];
            for (int i = 0; i < FFT_SIZE; i++) {
                FFTdata[i] = (double) s[i];
            }
            fft.rdft(1, FFTdata);
            for (int i = 0; i < FFT_SIZE; i += 2) {
                final double child = Math.pow(FFTdata[i], 2) + Math.pow(FFTdata[i + 1], 2);
                final double F = Math.sqrt(child) / dB_baseline;
                dbfs[i / 2] += (int) (20 * Math.log10(F));
            }
        }

        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        sb1.append("//f");
        sb2.append("#").append(String.format("%1$05d", pointNumber)).append(",").append(DateUtil.get_yyyyMMddHHmmssSSS(System.currentTimeMillis()));
        for (int i = 0; i < FFT_SIZE; i += 2) {
            sb1.append(",").append(String.format("%5.1f", (resol * i / 2)));
            sb2.append(",").append(String.format("%.3f", (2 * dbfs[i / 2] / FFT_SIZE)));
        }
        sb1.append("\n");
        sb2.append("\n");

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (!mBackKeyPressed) {
                mCountDownTimer.cancel();
                mCountDownTimer.start();

                Toast.makeText(this, getString(R.string.press_again), Toast.LENGTH_SHORT).show();
                mBackKeyPressed = true;
                return false;
            }
            return super.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }
    int x = 10, y = 10;
    private boolean dump() {
        x++;
        y++;
        FileSave.saveFile(sb2.toString(),String.valueOf(x),String.valueOf(y));
        if ((mDumpBsTask != null) && (mDumpBsTask.getStatus() != AsyncTask.Status.FINISHED))
            return true;
        mDumpBsTask = new DumpBsTask(new WeakReference<>(getApplicationContext()));
        mDumpBsTask.execute(mDeviceBsAdapter.getList());
        return false;
    }

    private void init() {
        verifyStoragePermissions(this);
        pointNumber = findViewById(R.id.pointNumber);
        pointNumber.setMaxValue(1000);
        pointNumber.setMinValue(0);
        pointNumber.setValue(0);
        scanRepeat = findViewById(R.id.scanRepeat);
        scanRepeat.setMaxValue(1000);
        scanRepeat.setMinValue(0);
        scanRepeat.setValue(10);
        scanTime = findViewById(R.id.scanTime);
        scanTime.setMaxValue(3600);
        scanTime.setMinValue(0);
        scanTime.setValue(10);
        value = findViewById(R.id.value);
        values = findViewById(R.id.values);
        white_list_bs = findViewById(R.id.white_list_bs);
        white_list_bs.addTextChangedListener(
                new TextWatcher() {
                    public void afterTextChanged(Editable s) {
                        mDeviceBsAdapter.setWhiteList(s.toString());
                    }

                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }
                });
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mCountDownTimer = new CountDownTimer(1000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                mBackKeyPressed = false;
            }
        };

        // BLE check
        if (!BleUtil.isBLESupported(this)) {
            System.out.println("不支持当前设备");
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // BT check
        final BluetoothManager manager = BleUtil.getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            System.out.println("适配器为空");
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        final ListView deviceListView1 = findViewById(R.id.list1);
        mDeviceBsAdapter = new DeviceBsAdapter(this, R.layout.listitem_device,
                new ArrayList<ScannedDevice>());
        mDeviceBsAdapter.setWhiteList(white_list_bs.getText().toString());
        deviceListView1.setAdapter(mDeviceBsAdapter);

        // TTS
        tts = new TextToSpeech(this, this);

        // FFT
        fftPath = Environment.getExternalStorageDirectory().getAbsolutePath() + DUMP_PATH
                + DateUtil.get_nowFftCsvFilename();
        bufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        stopBsScan();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        mToneGenerator.release();

        if (null != tts)
            tts.shutdown();

        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        // TTS初期化
        if (TextToSpeech.SUCCESS != status)
            playTone(1);
    }

    @Override
    public void onLeScan(final BluetoothDevice newDevice, final int newRssi,
                         final byte[] newScanRecord) {
        final int pointNum = pointNumber.getValue();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int whiteListAddressesSize = mDeviceBsAdapter.update(pointNum, newDevice, newRssi, newScanRecord);
                if (whiteListAddressesSize > 0) {
                    List<String> l = new ArrayList<>(Arrays.asList(white_list_bs.getText().toString().split(",")));
                    l.removeAll(Collections.singleton(""));
                    final String summary = "[" + getString(R.string.here) + " #" + Integer.toString(pointNum) + "] " + getString(R.string.white_list) + Integer.toString(whiteListAddressesSize);
                    getActionBar().setSubtitle(summary);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            return true;
        } else if (itemId == R.id.action_scan) {
            r2Enabled = true;
            thr = new Thread(runnable);
            thr.start();
            return true;
        } else if (itemId == R.id.action_stop) {
            r2Enabled = false;
            pointNumber.setValue(pointNumber.getValue() + 1);
            return true;
        } else if (itemId == R.id.action_clear) {
            if ((mDeviceBsAdapter != null) && (mDeviceBsAdapter.getCount() > 0)) {
                mDeviceBsAdapter.clear();
                mDeviceBsAdapter.notifyDataSetChanged();
                getActionBar().setSubtitle("");
            }
            return true;
        } else if (itemId == R.id.action_dump) {
            stopBsScan();
            //
            return dump();
        } else if (itemId == R.id.action_license) {
            showLicense();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopBsScan();

        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsScanning) {
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        } else {
            menu.findItem(R.id.action_scan).setEnabled(true);
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        }
        if ((mBTAdapter == null) || (!mBTAdapter.isEnabled()))
            menu.findItem(R.id.action_scan).setEnabled(false);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if ((mBTAdapter != null) && (!mBTAdapter.isEnabled())) {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu();

            startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
        }

        if (mSensorManager == null)
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetic = event.values.clone();
                break;
            case Sensor.TYPE_ORIENTATION:
                orientation = event.values.clone();
                break;
        }
        if (gravity != null && geomagnetic != null && orientation != null) {
            SensorManager.getRotationMatrix(inR, I, gravity, geomagnetic);
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
            SensorManager.getOrientation(outR, attitude);

            value.setText(String.format("%3.1f", (90.0f + orientation[1])));

            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.attitude));
            sb.append("\n");
            sb.append(getString(R.string.azimuth));
            sb.append(String.format("%3.1f", Math.toDegrees(attitude[0])));
            sb.append("\n");
            sb.append(getString(R.string.pitch));
            sb.append(String.format("%3.1f", Math.toDegrees(attitude[1])));
            sb.append("\n");
            sb.append(getString(R.string.roll));
            sb.append(String.format("%3.1f", Math.toDegrees(attitude[2])));
            sb.append("\n\n");
            sb.append(getString(R.string.orientation));
            sb.append("\n");
            sb.append(getString(R.string.azimuth));
            sb.append(String.format("%3.1f", (orientation[0] > 180.0 ? orientation[0] - 360.0 : orientation[0])));
            sb.append("\n");
            sb.append(getString(R.string.pitch));
            sb.append(String.format("%3.1f", orientation[1]));
            sb.append("\n");
            sb.append(getString(R.string.roll));
            sb.append(String.format("%3.1f", orientation[2]));
            values.setText(sb.toString());
        }
    }

    private void playTone() {
        playTone(0);
    }

    private void playTone(int mode) {
        final int[] toneType = {ToneGenerator.TONE_PROP_BEEP, ToneGenerator.TONE_CDMA_ABBR_ALERT};
        final int[] toneDuration = {35, 200};
        mToneGenerator.startTone(toneType[mode], toneDuration[mode]);
        sleep(toneDuration[mode]);
        mToneGenerator.stopTone();
        sleep(200L);
    }

    private void setTtsListener() {
        if (Build.VERSION.SDK_INT >= 15) {
            int listenerResult = tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                }

                @Override
                public void onError(String utteranceId) {
                }

                @Override
                public void onStart(String utteranceId) {
                }

            });
            if (listenerResult != TextToSpeech.SUCCESS) {
            }
        } else {
            // less than 15th
            int listenerResult = tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                }
            });

            if (listenerResult != TextToSpeech.SUCCESS) {
            }
        }

    }

    private void showLicense() {
        final LicenseDialogFragment dialogFragment = new LicenseDialogFragment();
        dialogFragment.show(getFragmentManager(), TAG_LICENSE);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    private void speechText(String str) {
        if (0 < str.length()) {
            if (tts.isSpeaking()) {
                tts.stop();
                return;
            }

            tts.setSpeechRate(1.0f);
            tts.setPitch(1.0f);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ttsGreater21(str);
            } else {
                ttsUnder20(str);
            }
            setTtsListener();
        }
    }

    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId = this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    int m = 0;
    StringBuilder sb ;
    StringBuilder sb2 = new StringBuilder();
    private void printValue(String s) {
        sb.append(s).append(" ");
    }//被651行调用，输出一行RSSI值
    private void startBsScan() {
        Vector<String> device_key=new Vector<String>();//放入扫描到的信标ID
        m++;
        sb = new StringBuilder();
        Constant.map.clear();
        for (int i=0;i<mDeviceBsAdapter.getList().size();i++) {
            if (Constant.getBeacon(mDeviceBsAdapter.getList().get(i).getDevice().getAddress()) != "mac") {
                Constant.storageValue(Constant.getBeacon(mDeviceBsAdapter.getList().get(i).getDevice().getAddress()), String.valueOf(mDeviceBsAdapter.getList().get(i).getRssi()));
                //   System.out.println("信标设备识别码： " + Constant.getBeacon(mDeviceBsAdapter.getList().get(i).getDevice().getAddress())); //测试取出硬件设备ID
                //  System.out.println("\r");
                if (!device_key.contains(Constant.getBeacon(mDeviceBsAdapter.getList().get(i).getDevice().getAddress())))
                    device_key.add(Constant.getBeacon(mDeviceBsAdapter.getList().get(i).getDevice().getAddress()));//蓝牙设备ID存入容器
                //   device_key.append(Constant.ifConclude());
            }
        }
        Collections.sort(device_key);
        Constant.ifConclude();
        //解决按照vector中顺序输出map.value()
        Iterator<Map.Entry<String,String>> it=map.entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry<String,String> entry=it.next();
            printValue(entry.getValue());
        }
        //查看数据
        //for (String s : device_key) {
        //    System.out.println("容器内数据为: "+s+" "+map.get(s));
        //    System.out.println("\r");
       // }
      // for (String s : map.values()) {
      //     printValue(s);

       //for(int i=0;i<map.size();++i)
      // {
     //      printValue();
      // }
        sb.append("\r");
        System.out.println("当前输出的信标序列为：" + sb.toString());
        sb2.append(sb.toString()).append("\r");
        System.out.println("正在进行的是第" + m + "次采集，设备总数是: " + mDeviceBsAdapter.getList().size());
        mDeviceBsAdapter.setPointNum(pointNumber.getValue());
        if ((mBTAdapter != null) && (!mIsScanning)) {
            mBTAdapter.startLeScan(this);
            mIsScanning = true;
            /**
             *
             */
            Handler handler=new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    setProgressBarIndeterminateVisibility(true);

                }
            });
            invalidateOptionsMenu();
        }
    }

    private void stopBsScan() {
        if (mBTAdapter != null)
            mBTAdapter.stopLeScan(this);
        mIsScanning = false;
        Handler handler=new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                setProgressBarIndeterminateVisibility(false);

            }
        });
        invalidateOptionsMenu();
    }

    public static class LicenseDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setIcon(R.drawable.ic_launcher);
            builder.setTitle(R.string.license_title);
            builder.setPositiveButton(android.R.string.ok, null);

            final LayoutInflater inflater = getActivity().getLayoutInflater();
            final View content = inflater.inflate(R.layout.dialog_license, null);
            final TextView tv = content.findViewById(R.id.text01);
            tv.setText(R.string.license_body);
            builder.setView(content);
            return builder.create();
        }
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }
}
