package com.r3tr0.bluetoothterminal.communications;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.r3tr0.bluetoothterminal.enums.ServiceCommand;
import com.r3tr0.bluetoothterminal.enums.ServiceFlag;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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

    private ServiceFlag flag = ServiceFlag.disconnected;

    public BluetoothService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        ServiceCommand command = (ServiceCommand) intent.getSerializableExtra("command");

        Log.e("service", "Started with command : " + command);
        if (intent1 == null)
            intent1 = new Intent("com.r3tr0.bluetoothterminal.communications.OBD");

        //commands
        if (command == ServiceCommand.initialize) {
            if (bluetoothSocket == null){
                BluetoothDevice device = intent.getParcelableExtra("device");
                if (device != null)
                    initialize(device);
                else {
                    intent1.putExtra("status", ServiceFlag.noDevice);
                    flag = ServiceFlag.noDevice;
                    sendBroadcast(intent1);
                    intent1.removeExtra("status");
                }
            }
            else {
                intent1.putExtra("status", ServiceFlag.paired);
                flag = ServiceFlag.paired;
                sendBroadcast(intent1);
                intent1.removeExtra("status");
            }
        } else if (command == ServiceCommand.disconnect) {
            if (bluetoothSocket != null)
                try {
                    inputStream = null;
                    outputStream = null;
                    bluetoothSocket.close();
                    bluetoothSocket = null;
                    flag = ServiceFlag.disconnected;
                } catch (IOException e) {
                    e.printStackTrace();
                }

        } else if (command == ServiceCommand.startReading) {
            if (readingThread == null)
                readingThread = new ReadingThread();
            readingThread.start();
        } else if (command == ServiceCommand.stopReading) {
            if (readingThread == null)
                readingThread = new ReadingThread();
            readingThread.stopReading();
        } else if (command == ServiceCommand.write) {
            Log.e("testing service", intent.getStringExtra("data"));
            try {
                write((intent.getStringExtra("data") + "\r").getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (command == ServiceCommand.getPairedDevices) {
            intent1.putExtra("devices", getAllPairedDevices());
            sendBroadcast(intent1);
            intent1.removeExtra("devices");
        } else if (command == ServiceCommand.getStatus) {
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
            intent1.putExtra("status", ServiceFlag.connecting);
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
                intent1.putExtra("status", ServiceFlag.connectionFailed);
                sendBroadcast(intent1);
                intent1.removeExtra("status");
                flag = ServiceFlag.connectionFailed;
            }

            inputStream = tempIn;
            outputStream = tempOut;

            flag = ServiceFlag.connected;
            intent1.putExtra("status", ServiceFlag.connected);
            sendBroadcast(intent1);
            intent1.removeExtra("status");

        } catch (IOException e) {
            e.printStackTrace();
            flag = ServiceFlag.connectionFailed;
            intent1.putExtra("status", ServiceFlag.connectionFailed);
            sendBroadcast(intent1);
            intent1.removeExtra("status");
        }

    }


    public void write(byte[] bytes) throws IOException {
        Log.e("sent data as bytes ", Arrays.toString(bytes));
        outputStream.write(bytes);
        outputStream.flush();
    }

    public ArrayList<BluetoothDevice> getAllPairedDevices(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.e("bt", "size of paired : " + mBluetoothAdapter.getBondedDevices().size());
        return new ArrayList<>(mBluetoothAdapter.getBondedDevices());
    }

    private class ReadingThread extends Thread{
        private volatile boolean isStop = true;

        @Override
        public synchronized void start() {
            super.start();
            isStop = true;
        }

        @Override
        public void run() {
            super.run();

            char c;
            while(isStop) {
                try {
                    byte b = 0;
                    StringBuilder res = new StringBuilder();
                    while (((b = (byte) inputStream.read()) > -1)) {
                        c = (char) b;
                        if (c == '>') // read until '>' arrives
                        {
                            break;
                        }
                        Log.e("Data", c + "");
                        res.append(c);
                        if (res.toString().matches("TDA[0-9]+\\sV[0-9]+\\.[0-9]+\\s"))
                            break;
                    }

                    String result = res.toString().replaceAll("SEARCHING\\.+", "");
                    result = result.replaceAll("\\s", "");
                    result = result.replaceAll("(BUS INIT)|(BUSINIT)|(\\.)", "");

                    if (result.length() > 0) {
                        Log.e("test thread", "read");
                        intent1.putExtra("data", result);
                        sendBroadcast(intent1);
                        intent1.removeExtra("data");
                    } else {
                        Log.e("test thread", "read");
                        intent1.putExtra("data", "");
                        sendBroadcast(intent1);
                        intent1.removeExtra("data");
                    }
                    //bytes = inputStream.read(buffer);
                    //tempMsg = new String(buffer,0,bytes);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stopReading(){
            this.isStop = false;
        }
    }


}
