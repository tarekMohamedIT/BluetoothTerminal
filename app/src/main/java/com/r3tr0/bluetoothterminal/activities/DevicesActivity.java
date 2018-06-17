package com.r3tr0.bluetoothterminal.activities;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.r3tr0.bluetoothterminal.R;
import com.r3tr0.bluetoothterminal.adapters.ListAdapter;
import com.r3tr0.bluetoothterminal.communications.BluetoothService;
import com.r3tr0.bluetoothterminal.communications.BluetoothServiceReceiver;
import com.r3tr0.bluetoothterminal.enums.ServiceCommand;
import com.r3tr0.bluetoothterminal.enums.ServiceFlag;
import com.r3tr0.bluetoothterminal.interfaces.OnItemClickListener;

import java.util.ArrayList;

public class DevicesActivity extends AppCompatActivity {

    Button pairedButton;
    Button nextButton;
    RecyclerView recyclerView;
    ListAdapter adapter;
    Intent btServiceIntent;
    BluetoothServiceReceiver receiver;
    ArrayList<BluetoothDevice> bluetoothDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        init();
    }

    public void init(){
        pairedButton = findViewById(R.id.getPairedButton);
        nextButton = findViewById(R.id.nextButton);
        recyclerView = findViewById(R.id.devicesRecyclerView);
        adapter = new ListAdapter(this, new ArrayList<String>());
        btServiceIntent = new Intent(this, BluetoothService.class);
        receiver = new BluetoothServiceReceiver(new BluetoothServiceReceiver.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(Object message) {

                Log.e("receiver", "message received");

                Object data = ((Intent)message).getSerializableExtra("devices");
                Log.e("extra", "status : " + ((Intent)message).getStringExtra("status"));
                ServiceFlag connectionFlag = (ServiceFlag) ((Intent) message).getSerializableExtra("status");

                if (data != null) {
                    bluetoothDevices = (ArrayList<BluetoothDevice>) data;
                    adapter.getStringsList().clear();
                    for (BluetoothDevice device : bluetoothDevices){
                        adapter.getStringsList().add(device.getName() + "\n" + device.getAddress());
                    }
                    adapter.notifyDataSetChanged();
                }

                if (connectionFlag != null) {
                    switch (connectionFlag) {
                        case connected:
                            nextButton.setText("Connected, click me to continue!");
                            nextButton.setEnabled(true);
                            break;

                        case connectionFailed:
                            nextButton.setText("Failed to connect to device!");
                            nextButton.setEnabled(false);
                            break;

                        case connecting:
                            nextButton.setText("Connecting to device ...");
                            nextButton.setEnabled(false);
                            break;
                        case noDevice:
                            nextButton.setText("No device was sent!");
                            nextButton.setEnabled(false);
                            break;
                        case paired:
                            nextButton.setText("A device already paired");
                            nextButton.setEnabled(false);
                            break;
                    }
                }
            }
        });

        pairedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btServiceIntent.putExtra("command", ServiceCommand.getPairedDevices);
                startService(btServiceIntent);
                btServiceIntent.removeExtra("command");
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DevicesActivity.this, MainActivity.class));
            }
        });

        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.e("list", "Clicked");
                btServiceIntent.putExtra("command", ServiceCommand.initialize);
                btServiceIntent.putExtra("device", bluetoothDevices.get(position));
                startService(btServiceIntent);
                btServiceIntent.removeExtra("command");
                btServiceIntent.removeExtra("device");
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(BluetoothService.RECEIVER_ACTION));
    }
}
