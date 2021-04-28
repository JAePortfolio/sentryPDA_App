package com.example.sentrypda;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";

    private static final String appName = "MYAPP";

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    private class AcceptThread extends Thread { // Always sitting listening for connections
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                // Setting up listening server socket
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOEXCEPTION: " + e.getMessage());
            }

            Log.d(TAG, "AcceptThread: Setting up server using" + MY_UUID_INSECURE);
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "run: AcceptThread Running.");
            BluetoothSocket socket = null;
            // Accept thread hangs here until connection happens, or exception - Blocking call
            try {
                Log.d(TAG, "run: RFCOM server socket start....");
                socket = mmServerSocket.accept();
                Log.d(TAG, "run: RFCOM server socket acception connection.");
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: IOEXCEPTION: " + e.getMessage());
            }

            if (socket != null) {
                connected(socket, mmDevice);
            }
            Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread ");
            //Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        + MY_UUID_INSECURE);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Always cancel discovery mode because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Accept thread hangs here until connection happens, or exception - Blocking call
                mmSocket.connect();
                mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                // if gets passed blocking call
                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e) {
                // close the socket
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE);
            }
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed " + e.getMessage());
            }
        }
    }

    // Start the chat service. Specifically start AcceptThread to begin a session in listening
    // (server) mode. Callled by activity onResume()
    public synchronized void start() {
        Log.d(TAG, "start");
        if (mConnectThread != null) { // If exists, cancel and create new one
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) { // If accept thread is null, create new one
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start(); // Not THIS start, thread class start
        }
    }

    // Start the AcceptThread and sits and waits for a connection. Then ConnectThread starts
    // and attempts to make a connection with the other devices AcceptThread
    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");
        //initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please wait..."
                , true);
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //Dismiss the progressdialog when connection is established
            try {
                mProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            try{
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch(IOException e){
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
    }

        public void run(){
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes; // bytes returned from read()
            while(true){
                //Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG,"InputStream: " + incomingMessage);
                    // Below is so we can make use of the incoming data in the MainActivity
                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage", incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading InputStream: " + e.getMessage());
                    break;
                }
            }
        }

        //Call this from the main activity to send data to the remote device
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream " + text);
            try{
                mmOutStream.write(bytes);
            } catch(IOException e){
                Log.e(TAG, "write: Error writing to outputstream. " + e.getMessage());
            }
        }

        // Call from main activity to shutdown the connection
        public void cancel(){
            try{
                mmSocket.close();
            }catch (IOException e){

            }
        }
    }
    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG, "connected: Starting.");
        //Start thread to manage connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    //Write to the ConnectedTh0 in an unsync manner
    // out is the Bytes to write
    public void write(byte[] out){
        ConnectedThread r;
        //Sync a copy of the ConnectedThread
        Log.d(TAG, "write: Write called.");
        // begin write
        mConnectedThread.write(out);
    }
}