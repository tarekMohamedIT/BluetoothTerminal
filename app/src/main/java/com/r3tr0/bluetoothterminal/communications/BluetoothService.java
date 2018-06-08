package com.r3tr0.bluetoothterminal.communications;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothService extends Service {
    public static final String App_Name = "btChat";
    public static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static final String RECEIVER_ACTION = "com.r3tr0.bluetoothterminal.communications.OBD";

    private Intent intent1;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private ReadingThread readingThread;

    private int flag = 1;

    public static final int FLAG_PAIRED = 5;
    public static final int FLAG_NO_DEVICE = 4;
    public static final int FLAG_CONNECTION_FAILED = 3;
    public static final int FLAG_CONNECTED = 2;
    public static final int FLAG_NOT_CONNECTED = 1;
    public static final int FLAG_CONNECTING = 0;

    public static final int COMMAND_INITIALIZE = 0;
    public static final int COMMAND_DISCONNECT = 1;
    public static final int COMMAND_READ = 2;
    public static final int COMMAND_WRITE = 3;
    public static final int COMMAND_GET_PAIRED_DEVICES = 4;
    public static final int COMMAND_GET_STATUS = 5;

    public BluetoothService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int command = intent.getIntExtra("command", -1);

        Log.e("service", "Started with command : " + command);
        if (intent1 == null)
            intent1 = new Intent("com.r3tr0.bluetoothterminal.communications.OBD");

        //commands
        if (command == COMMAND_INITIALIZE) {
            if (bluetoothSocket == null){
                BluetoothDevice device = intent.getParcelableExtra("device");
                if (device != null)
                    initialize(device);
                else {
                    intent1.putExtra("status", FLAG_NO_DEVICE);
                    flag = FLAG_NO_DEVICE;
                    sendBroadcast(intent1);
                    intent1.removeExtra("status");
                }
            }
            else {
                intent1.putExtra("status", FLAG_PAIRED);
                flag = FLAG_PAIRED;
                sendBroadcast(intent1);
                intent1.removeExtra("status");
            }
        }

        else if (command == COMMAND_DISCONNECT) {
            if (bluetoothSocket != null)
                try {
                    inputStream = null;
                    outputStream = null;
                    bluetoothSocket.close();
                    bluetoothSocket = null;
                    flag = FLAG_NOT_CONNECTED;
                } catch (IOException e) {
                    e.printStackTrace();
                }

        } else if (command == COMMAND_READ) {
            if (readingThread == null)
                readingThread = new ReadingThread();
            read();
        }
        else if (command == COMMAND_WRITE) {
            Log.e("testing service", intent.getStringExtra("data"));
            try {
                write(intent.getStringExtra("data").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if (command == COMMAND_GET_PAIRED_DEVICES){
            intent1.putExtra("devices", getAllPairedDevices());
            sendBroadcast(intent1);
            intent1.removeExtra("devices");
        }

        else if (command == COMMAND_GET_STATUS){
            intent1.putExtra("status", flag);
            sendBroadcast(intent1);
            intent1.removeExtra("status");
        }

        return START_NOT_STICKY;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("test", "created");
    }

    public void initialize(BluetoothDevice device) {
        //Server socket initialized
        try {
            intent1.putExtra("status", FLAG_CONNECTING);
            sendBroadcast(intent1);
            intent1.removeExtra("status");
            bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            bluetoothSocket.connect();
            if (!bluetoothSocket.isConnected()){
                bluetoothSocket = null;
                throw new IOException("Failed to connect to this device");
            }
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                intent1.putExtra("status", FLAG_CONNECTION_FAILED);
                sendBroadcast(intent1);
                intent1.removeExtra("status");
                flag = FLAG_CONNECTION_FAILED;
            }

            inputStream = tempIn;
            outputStream = tempOut;

            flag = FLAG_CONNECTED;
            intent1.putExtra("status", FLAG_CONNECTED);
            sendBroadcast(intent1);
            intent1.removeExtra("status");

        } catch (IOException e) {
            e.printStackTrace();
            flag = FLAG_CONNECTION_FAILED;
            intent1.putExtra("status", FLAG_CONNECTION_FAILED);
            sendBroadcast(intent1);
            intent1.removeExtra("status");
        }

    }



    public void read() {
        readingThread.start();
    }

    public void write(byte[] bytes) throws IOException {
        outputStream.write(bytes);
        outputStream.flush();
    }

    public ArrayList<BluetoothDevice> getAllPairedDevices(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.e("bt", "size of paired : " + mBluetoothAdapter.getBondedDevices().size());
        return new ArrayList<>(mBluetoothAdapter.getBondedDevices());
    }

    private class ReadingThread extends Thread{

        @Override
        public void run() {
            super.run();

            byte[] buffer = new byte[1024];
            while(true) {
                int bytes = -1;
                try {
                    bytes = inputStream.read(buffer);
                    //tempMsg = new String(buffer,0,bytes);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (bytes != -1) {
                    Log.e("test thread", "read");
                    intent1.putExtra("data", new String(buffer,0,bytes));
                    sendBroadcast(intent1);
                    intent1.removeExtra("data");
                } else {
                    intent1.putExtra("data", "");
                    sendBroadcast(intent1);
                    intent1.removeExtra("data");
                }
            }
        }
    }
}
