package com.example.testmessage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class BroadcastReceiverActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener {

    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    private String name;
    private WifiP2pDeviceList peers;//new ArrayList();
    private ListView mListView;
    private ArrayAdapter<String> WifiP2parrayAdapter;
    private WifiP2pDevice ConnectedPartner;
    private int PORT = 8888;
    private String TAG = "##BoadcastReceiverAct";

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener(){
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            Log.d("INPeerListListener", "Works");
            // Out with the old, in with the new.
            ArrayList<WifiP2pDevice> peersNameFixed = new ArrayList<WifiP2pDevice>();

            for (WifiP2pDevice peer : peerList.getDeviceList()) {
                String newDeviceName = peer.deviceName.replace("[Phone]","");
                peer.deviceName = newDeviceName;
            }
            peers = new WifiP2pDeviceList(peerList);

            WifiP2parrayAdapter.clear();
            for (WifiP2pDevice peer : peerList.getDeviceList()) {

                WifiP2parrayAdapter.add(peer.deviceName); //+ "\n" + peer.deviceAddress
                Log.d("INPeerListListenerNAME:", peer.deviceName);
            }
        }
    };

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_receiver);

        View bv = findViewById(R.id.broadcastActivity);
        bv.setBackgroundColor(getResources().getColor(R.color.colorLightGrey));



        // get name entered by user in MainActivity
        Bundle extras = getIntent().getExtras();
        name = extras.getString("nameText");

        TextView youAreLoggedInTextView = (TextView) findViewById(R.id.loggedIn);
        youAreLoggedInTextView.setText("You are logged in as " +  name);

        getSupportActionBar().setTitle("New Chat");

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiBroadcastReceiver(mManager, mChannel, this, peerListListener);  //Setting up Wifi Receiver

        if (mManager != null && mChannel != null) {
            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && mManager != null && mChannel != null) {
                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        try {
            Method m = mManager.getClass().getMethod("setDeviceName", new Class[]{WifiP2pManager.Channel.class, String.class,
                    WifiP2pManager.ActionListener.class});
            m.invoke(mManager, mChannel, name, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.d(TAG, "Name change successful.");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "name change failed: " + reason);
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "No such method");
        }

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //will not provide info about who it discovered
            }

            @Override
            public void onFailure(int reasonCode) {

            }
        });
        mListView = (ListView) findViewById(R.id.ListView);
        TextView emptyText = (TextView)findViewById(android.R.id.empty);
        mListView.setEmptyView(emptyText);
        WifiP2parrayAdapter = new ArrayAdapter<String>(this, R.layout.fragment_peer, R.id.textView);

        mListView.setAdapter(WifiP2parrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                Log.d(TAG, "item clicked");
                //Get string from textview
                TextView tv = ((LinearLayout) arg1).findViewById(R.id.textView);
                WifiP2pDevice device = null;
                for(WifiP2pDevice wd : peers.getDeviceList())
                {
                    if(wd.deviceName.equals(tv.getText()))
                        device = wd;
                }
                if(device != null)
                {
                    Log.d(TAG, " calling connectToPeer");
                    //Connect to selected peer
                    connectToPeer(device);

                }
                else
                {

                }

            }

        });
        receiveConnectRequest.execute();
    }
    public void connectToPeer(final WifiP2pDevice wifiPeer)
    {
        this.ConnectedPartner = wifiPeer;
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = wifiPeer.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener()  {
            public void onSuccess() {

            }

            public void onFailure(int reason) {

                     }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(TAG, "pre connect " + Boolean.toString(info.groupFormed));
        if (info.groupFormed) {
            Intent intent = new Intent(BroadcastReceiverActivity.this, ChatActivity.class);
            intent.putExtra("info", info);
            intent.putExtra("name",name);
            startActivityForResult(intent, 1);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (mManager != null && mChannel != null) {
                mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup group) {
                        if (group != null && mManager != null && mChannel != null) {
                            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "removeGroup onSuccess2 -");
                                }

                                @Override
                                public void onFailure(int reason) {
                                    Log.d(TAG, "removeGroup onFailure2 -" + reason);
                                }
                            });
                        }
                    }
                });
            }
        }
    }
    public void onRefresh(View view) {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //will not provide info about who it discovered
            }

            @Override
            public void onFailure(int reasonCode) {

            }
        });
    }

    AsyncTask<Void, Void, Void> receiveConnectRequest = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                ServerSocket server = new ServerSocket();
                Socket client = server.accept();
                BufferedReader dataIn = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter dataOut = new PrintWriter(client.getOutputStream(), true);
                String in;
                while (true) {
                    if ((in = dataIn.readLine()) != null) {
                        String request;
                        String name;
                        try {
                            JSONObject json = new JSONObject(in);
                            request = json.getString("request");
                            name = json.getString("name");
                        } catch (JSONException e) {
                            request = "";
                            name = "";
                        }
                        if (request.equals("connection request")) {
                            //TODO: *name* wants to connect to you. (Accept/Decline)
                            //For now is accepts automatically
                            String ack = "";
                            try {
                                ack = new JSONObject()
                                        .put("type", "ack").toString();
                            } catch (JSONException e) {
                                Log.d(TAG, "creating ack failed :" + e.getMessage());
                            }
                            dataOut.println(ack);
                            Intent intent = new Intent(BroadcastReceiverActivity.this, ChatActivity.class);
                            //TODO: Give necessary info to intent.
                            startActivity(intent);
                            Log.d(TAG, "Transitioning to Chat Activity");
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    };

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
