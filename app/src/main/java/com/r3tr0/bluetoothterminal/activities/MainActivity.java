package com.r3tr0.bluetoothterminal.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.r3tr0.bluetoothterminal.R;
import com.r3tr0.bluetoothterminal.adapters.ListAdapter;
import com.r3tr0.bluetoothterminal.communications.BluetoothService;
import com.r3tr0.bluetoothterminal.communications.BluetoothServiceReceiver;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    BluetoothServiceReceiver receiver;
    Intent btIntent;


    ListAdapter adapter;
    RecyclerView terminalRecyclerView;

    TextView statusTextView;
    EditText commandEditText;
    Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    void initialize(){
        terminalRecyclerView = findViewById(R.id.terminalRecyclerView);
        statusTextView = findViewById(R.id.statusTextView);
        sendButton = findViewById(R.id.commandButton);
        commandEditText = findViewById(R.id.commandsEditText);
        adapter = new ListAdapter(this, new ArrayList<String>());
        btIntent = new Intent(this, BluetoothService.class);
        receiver = new BluetoothServiceReceiver(new BluetoothServiceReceiver.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(Object message) {
                Intent intent = (Intent) message;

                String data = intent.getStringExtra("data");
                int status = intent.getIntExtra("status", -1);

                if (data != null){
                    adapter.getStringsList().add("from receiver : " + data);
                    adapter.notifyDataSetChanged();
                }

                if (status != -1){
                    switch (status){
                        case BluetoothService.FLAG_CONNECTED:
                            statusTextView.setText("Status : Connected");
                            break;
                        case BluetoothService.FLAG_NOT_CONNECTED:
                            statusTextView.setText("Status : Not connected");
                            break;
                        case BluetoothService.FLAG_CONNECTION_FAILED:
                            statusTextView.setText("Status : Connection failed");
                            break;
                    }
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(statusTextView.getText().equals("Status : Connected")){
                    btIntent.putExtra("command", BluetoothService.COMMAND_WRITE);
                    btIntent.putExtra("data", commandEditText.getText().toString() + ";");
                    startService(btIntent);
                    btIntent.removeExtra("command");
                    btIntent.removeExtra("data");
                    adapter.getStringsList().add("From you : " + commandEditText.getText().toString());
                    adapter.notifyDataSetChanged();
                }
            }
        });
        terminalRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        terminalRecyclerView.setAdapter(adapter);

        btIntent.putExtra("command", BluetoothService.COMMAND_READ);
        startService(btIntent);
        btIntent.removeExtra("command");
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
        btIntent.putExtra("command", BluetoothService.COMMAND_GET_STATUS);
        startService(btIntent);
        btIntent.removeExtra("command");
    }
}
