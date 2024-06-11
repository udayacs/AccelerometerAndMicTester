package com.zebra.cpg.myaccelerometer;

import static android.Manifest.permission.READ_MEDIA_AUDIO;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity  implements SensorEventListener{
    private static final String TAG = "LosAlamosTester";
    SensorManager sensorManager;
    Sensor accelerometer;
    TextView tvx;
    TextView tvy;
    private Button accStart;
    private Button accStop;
    private boolean smListnerRegistered = false;

    private MediaRecorder recorder;
    private Button recStart;
    private Button recStop;

    private MediaPlayer player;
    private Button play;

    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 38058;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
    }

    private void resumeOnCreate(){
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        note("All permission granted.", false);

        if(accelerometer == null){
            note("Accelerometer is null.", false);
        }
        else {
            tvx = findViewById(R.id.tvx);
            tvy = findViewById(R.id.tvy);
            accStart = findViewById(R.id.btnStartAcc);
            accStart.setOnClickListener(view -> {
                if(!smListnerRegistered) {
                    smListnerRegistered = sensorManager.registerListener(this, accelerometer,
                            SensorManager.SENSOR_DELAY_NORMAL);
                    if(smListnerRegistered){
                        note("Accelerometer started.", false);
                    }
                    else {
                        note("Accelerometer not started.", true);
                    }
                }
            });
            accStop = findViewById(R.id.btnStopAcc);
            accStop.setOnClickListener(view -> {
                sensorManager.unregisterListener(this);
                smListnerRegistered = false;
                note("Accelerometer stopped.", false);
            });
        }

        recorder = new MediaRecorder();
        // Specify the audio source (microphone)
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // Set the output format for the recording (e.g., AAC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        // Set the audio encoder used for compression (e.g., AAC_ADTS)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        String filename =  getExternalFilesDir(Environment.DIRECTORY_MUSIC) + "/" + "recording.aac";
        recorder.setOutputFile(filename);
        Log.d(TAG, "Recording file name: " + filename);

        recStart = findViewById(R.id.btnRec);
        recStart.setOnClickListener(view -> {

            try {
                recorder.prepare();
                recorder.start();
                note("Recording started.", false);
            } catch (Exception e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
        });

        recStop = findViewById(R.id.btnStopRec);
        recStop.setOnClickListener(view -> {

            try {
                recorder.stop();
                recorder.release();
                note("Recording stopped.", false);
            } catch (Exception e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
        });

        play = findViewById(R.id.btnPlay);
        play.setOnClickListener(view -> {

            try {
                player = new MediaPlayer();
                player.setDataSource(filename);
                player.prepare();
                player.start();
                note("Playback started.", false);
                player.setOnCompletionListener(mediaPlayer -> {
                    player.release();
                    note("Playback stopped.", false);
                });
            } catch (Exception e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }
        });
    }

    private void checkPermission(){
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            //Get Permissions
            String[] requestedPermissions = packageInfo.requestedPermissions;
            List<String> neededPermissions = new ArrayList<>();
            if (requestedPermissions == null) {
                resumeOnCreate();
                return;
            }
            for (String requestedPermission : requestedPermissions) {
                if (checkSelfPermission(requestedPermission) == PackageManager.PERMISSION_DENIED) {
                    if(requestedPermission.equals(READ_MEDIA_AUDIO) && Build.VERSION_CODES.TIRAMISU > Build.VERSION.SDK_INT) {
                        Log.d(TAG, "permissions: " + requestedPermission + " : not related");
                        continue;
                    }
                    neededPermissions.add(requestedPermission);
                    Log.d(TAG, "permissions: " + requestedPermission + " : denied");
                } else {
                    Log.d(TAG, "permissions: " + requestedPermission + " : granted");
                }
            }
            if (neededPermissions.size() == 0) {
                resumeOnCreate();
                return;
            }
            requestPermissions(neededPermissions.toArray(new String[0]), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        } catch (Exception ex) {
            Log.e(TAG, Objects.requireNonNull(ex.getMessage()));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allPermissionsGranted = true;
        if(REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS == requestCode) {
            for (int i = 0; i < permissions.length; i++) {
                if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                    allPermissionsGranted = false;
                    break;
                }
            }
        }

        if(allPermissionsGranted){
            resumeOnCreate();
        }
        else {
            note("All permission not granted. App will not work", true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(sensorManager != null && smListnerRegistered) {
            sensorManager.unregisterListener(this);
            smListnerRegistered = false;
            note("Stopping Accelerometer", false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            // Do something with the accelerometer data (x, y, z)
            tvx.setText(String.valueOf(x));
            tvy.setText(String.valueOf(y));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void note(String msg, boolean isError){
        if(isError){
            Log.e(TAG, msg);
        }
        else {
            Log.d(TAG, msg);
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}