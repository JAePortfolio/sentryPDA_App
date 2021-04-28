package com.example.sentrypda;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.animation.ArgbEvaluator;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.animation.ValueAnimator.REVERSE;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private static final String TAG = "MainActivity";
    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>(); // Holds devices discovered
    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;
    BluetoothConnectionService mBluetoothConnection;
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    BluetoothDevice mBTDevice;

    Button play1;
    Button play2;
    Button btnONOFF;
    Button btnDiscover;
    Button sendButton;
    Button startConnection;
    ImageView sapperIcon;
    MediaPlayer mp;
    ValueAnimator colorAnim;
    ObjectAnimator sapperAnim, ammoAnim, sentryHealthAnim;
    ProgressBar ammoBar, sentryHealthBar;
    int shells = 100, sentryHealth = 100;

    StringBuilder message;

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            // When discovery finds a device
            if(action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)){
                //Get the BluetoothDevice object from the Intent
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1 : State Turning OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onReceive: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1 : State Turning ON");
                        break;
                }
            }
        }
    };
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceiver: ACTION FOUND.");
            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceiver: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 3 cases
                // case 1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcasterReceiver: BOND_BONDED.");
                    lvNewDevices.setVisibility(View.INVISIBLE);
                    mBTDevice = mDevice;
                }
                // case 2: creating bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BroadcasterReceiver: BOND_BONDING.");
                }
                // case 3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BroadcasterReceiver: BOND_NONE.");
                }
            }
        }
    };


    @Override
    protected void onDestroy(){
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sapperIcon = (ImageView)findViewById(R.id.imageView2);

        //final Animation animation = (Animation) AnimationUtils.loadAnimation(this, R.anim.sentryhealthanim);
        sentryHealthBar = (ProgressBar) findViewById(R.id.progressBar1);
        sentryHealthBar.setProgress(sentryHealth);

        ammoBar = (ProgressBar) findViewById(R.id.progressBar2);
        ammoBar.setProgress(shells);

        btnONOFF = (Button)findViewById(R.id.btnONOFF);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        // To receive the incomingMessage from the BluetoothConnectionService class
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String text = intent.getStringExtra("theMessage");
                //message.append(text + "\n");
                if(text.equals("h")){
                    play2.performClick();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        // Broadcasts when bond state changes (ex, pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);
        lvNewDevices.setOnItemClickListener(MainActivity.this);

        startConnection = (Button)findViewById(R.id.startConnection);
        sendButton = (Button)findViewById(R.id.sendButton);

        btnONOFF.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT();
            }
        });

        btnDiscover = (Button)findViewById(R.id.btnDiscover);
        btnDiscover.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Log.d(TAG, "onClick: Discovering devices...");
                btnDiscover(view);
            }
        });

        startConnection.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                startConnection();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                byte[] bytes = "send info test".getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
            }
        });

        play1 = (Button)findViewById(R.id.button1);
        final MediaPlayer mp1 = MediaPlayer.create(this, R.raw.hud_warning);
        play1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //sentryHealthBar.startAnimation(animation);
                String x = "h";
                mBluetoothConnection.write(x.getBytes(Charset.defaultCharset()));
                mp1.setLooping(true);
                mp1.start();
                sentryHealthAnim();
                setIconAnimColor();
                sapperIconAnim();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable(){
                    @Override
                    public void run() {
                        mp1.stop();
                        mp1.prepareAsync();
                    }
                }, 6000);
                //sapperIcon.setBackgroundColor(Color.parseColor("#e73520"));
            }
        });

        play2 = (Button)findViewById(R.id.button2);
        final MediaPlayer mp2 = MediaPlayer.create(this, R.raw.sentry_shoot);
        play2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                sentryShellAnim();
                mp2.setLooping(true);
                mp2.start();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mp2.stop();
                        mp2.prepareAsync();
                    }
                }, 3000);
            }
        });
    }

    //create method for starting connection. Will crash if no pair first
    public void startConnection(){
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }

    /* Start the chat service meth*/
    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCom bluetooth connection.");

        mBluetoothConnection.startClient(device,uuid);
    }

    public void enableDisableBT(){
        if(mBluetoothAdapter == null){
            Log.d(TAG,"enableDisableBT: Does not have BT Capability");
        }
        if(!mBluetoothAdapter.isEnabled()){
            Log.d(TAG,"enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if(mBluetoothAdapter.isEnabled()){
            Log.d(TAG,"enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
    }

    public void btnDiscover(View view){
        Log.d(TAG, "btnDiscover: Looking for unpaired devices. ");

        if(mBluetoothAdapter.isDiscovering()){
            Log.d(TAG, "btnDiscover: Cancelling discovery.");

            // Check BT Permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if(!mBluetoothAdapter.isDiscovering()){
            // Check BT Permissions in manifest
            checkBTPermissions();
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }

    public void checkBTPermissions(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if(permissionCheck != 0){
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},1001);
            }
            else{
                Log.d(TAG, "checkBTPermissions: No need to check permissions, SDK version < LOLLIPOP");
            }
        }
    }

    void setIconAnimColor(){
        int colorOne = Color.parseColor("#e73520");
        int colorTwo = Color.parseColor("#d6cacb");
        colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), colorOne, colorTwo);
        colorAnim.setDuration(500); // milliseconds
        colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                sapperIcon.setBackgroundColor((int) animator.getAnimatedValue());
            }

        });
        colorAnim.setRepeatCount(10);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);
        colorAnim.start();
    }

    void sentryShellAnim(){
        ammoAnim = ObjectAnimator.ofInt(ammoBar, "progress", shells-50);
        ammoAnim.setDuration(3000);
        ammoAnim.start();
        shells -= 50;
    }

    void sapperIconAnim(){
        float ht_px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 113, getResources().getDisplayMetrics());
        sapperAnim = ObjectAnimator.ofFloat(sapperIcon, "translationY", ht_px);
        sapperAnim.setDuration(500);
        sapperAnim.start();
    }

    void sentryHealthAnim(){
        sentryHealthAnim = ObjectAnimator.ofInt(sentryHealthBar, "progress", 0);
        sentryHealthAnim.setDuration(6000);
        sentryHealthAnim.start();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        // first cancel discovery when we start bonding to a device (not necessary to continue
        // search and mem intensive
        mBluetoothAdapter.cancelDiscovery();
        Log.d(TAG, "onItemClick: You clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);



        //create bond
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device: pairedDevices){

                for (ParcelUuid uuid: device.getUuids()){
                    String uuid_string = uuid.toString();
                    Log.d(TAG, "uuid : "+uuid_string);
                }
            }
            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }
    }
}

