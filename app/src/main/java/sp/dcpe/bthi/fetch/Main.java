package sp.dcpe.bthi.fetch;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class Main extends Activity {

    public static final String LOG_TAG = Main.class.getSimpleName();

    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean isConnected = false;

    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;

    public static WifiP2pInfo info;

    //NFC
    private boolean mResumed = true;
    private boolean mPushMode = false;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private IntentFilter[] mNdefExchangeFilters;

    // Server or Client
    private String vendorCode;
    private String passPhrase;
    public static String mode;

    private String clientDeviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        // Set broadcast receiver intent filter
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Setup Wifip2pManager and Channel for use
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        // Get WifiP2p state
        if(manager.EXTRA_WIFI_STATE.equals(2))
            isWifiP2pEnabled = true;
        else
            isWifiP2pEnabled = false;

        // Setup NFC adapter for use
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Handle all of our received NFC intents in this activity - waiting for intent
        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Intent filters for reading a note from a tag or exchanging over p2p
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefDetected.addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Toast.makeText(Main.this, "Failed to add MIME text/plain into Intent Filter",
                    Toast.LENGTH_SHORT).show();
        }

        // Array of Intent Filter, for our case we only need one that is text/plain
        mNdefExchangeFilters = new IntentFilter[] { ndefDetected };

        // Get preferences setting for database/sharepreference file
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        vendorCode = sp.getString("pref_key_vendorcode", "H123456");
        passPhrase = sp.getString("pref_key_passphrase", "atomic");
        mode = sp.getString("pref_key_client_server", "client");

        Toast.makeText(Main.this, "Mode: " + mode,
                Toast.LENGTH_SHORT).show();

    }

    public void retrieveButtonClick(View view) {

        List<WifiP2pDevice> peers = WifiDirectBroadcastReceiver.PeerChangeListener.peers;

        if(isConnected && mode.equalsIgnoreCase("server")) { // If connected start sending file

            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, "file://" + Environment.getExternalStorageDirectory() + "/Fetch/3B21TT.jpg");
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, info.groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
            this.startService(serviceIntent);
        } else if (!isConnected && peers.size() != 0 && mode.equalsIgnoreCase("server")){ // If not connected

            for(int i = 0; i < peers.size(); i++) {
                WifiP2pDevice device = (WifiP2pDevice) peers.get(i);

                if(!device.deviceName.equals(clientDeviceName))
                    continue;

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(Main.this, "Connecting to " + clientDeviceName ,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(Main.this, "Connect failed. Retry.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else { // Not connected
//            // Write to a tag for as long as the dialog is shown.
//            disableNdefExchangeMode();
            //enablePushMode();

            mNfcAdapter.setNdefPushMessage(buildNdefMessage(), this);


            if(mode.equalsIgnoreCase("client")) {
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(Main.this, "WifiP2p Discovery Initiated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(Main.this, "WifiP2p Discovery Failed : " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new WifiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);

        mResumed = true;

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            NdefMessage[] messages = getNdefMessages(getIntent());
            byte[] payload = messages[0].getRecords()[0].getPayload();
            setIntent(new Intent()); // Consume this intent.
            Log.v(LOG_TAG, new String(payload));
        }

        Log.i("sNFC", "onResume");
        enableNdefExchangeMode();
        //turn off sending Android Beam
        mNfcAdapter.setNdefPushMessage(null, this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // NDEF exchange mode
        if (!mPushMode && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            NdefMessage[] msgs = getNdefMessages(intent);
            String body = new String(msgs[0].getRecords()[0].getPayload());
//            if (body.equals("N"))
//                body = "Nuclear";
//            else
//                body = "Incorrect";

            Log.v(LOG_TAG, body);


            if(mode.equals("server")) {

                String[] info =  body.split("/");

                Toast.makeText(Main.this, info[1],
                        Toast.LENGTH_SHORT).show();

                if(info[1].equalsIgnoreCase("nuclear")) {
                    clientDeviceName = info[2];
                    manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            Toast.makeText(Main.this, "Discovery Initiated",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(int reasonCode) {
                            Toast.makeText(Main.this, "Discovery Failed : " + reasonCode,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }

        if (mPushMode && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            mNfcAdapter.setNdefPushMessage(buildNdefMessage(), this);
            Log.i("sNFC", "onThis");
            mPushMode = false;
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);

        mResumed = false;

        Log.i("sNFC", "onPause");
        disableNdefExchangeMode();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        // etc.
        super.onSaveInstanceState(savedInstanceState);
    }
    //onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        String myString = savedInstanceState.getString("MyString");
    }

    public boolean isWifiP2pEnabled() {
        return isWifiP2pEnabled;
    }

    public void setWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public void formP2pConnection() {

        if(mode.equalsIgnoreCase("client"))
            return;

        if(isConnected)
            return;

        List<WifiP2pDevice> peers = WifiDirectBroadcastReceiver.PeerChangeListener.peers;

//        Log.v(LOG_TAG, "formP2pConnection - ListPeers Size : " + peers.size());
//
//        Toast.makeText(this, "formP2pConnection - ListPeers Size : " + peers.size(),
//                Toast.LENGTH_SHORT).show();


        for(int i = 0; i < peers.size(); i++) {
            WifiP2pDevice device = (WifiP2pDevice) peers.get(i);

            if(device.deviceName.equals(clientDeviceName)) {

                Toast.makeText(this, "Connection to " + clientDeviceName,
                    Toast.LENGTH_SHORT).show();
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                config.groupOwnerIntent = 0;
                manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        // WiFiDirectBroadcastReceiver will notify us. Ignore for now.

                        manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {

                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                                Main.info = wifiP2pInfo;
                                //transferFile();
                            }
                        });

                        Toast.makeText(Main.this, "Connected.",
                                Toast.LENGTH_SHORT).show();
                        setConnected(true);
                        //transferFile();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(Main.this, "Connect failed. Retry.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    public void transferFile() {
        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, "file://" + Environment.getExternalStorageDirectory() + "/Fetch/3B21TT.jpg");
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        this.startService(serviceIntent);
    }

    NdefMessage[] getNdefMessages(Intent intent) {
        // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[] {};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {
                        record
                });
                msgs = new NdefMessage[] {
                        msg
                };
            }
        } else {
            Log.d("sNFC", "Unknown intent.");
            finish();
        }
        return msgs;
    }

    private void enableNdefExchangeMode() {
        //mNfcAdapter.setNdefPushMessage(getNoteAsNdef(), this);
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mNdefExchangeFilters, null);
    }

    private void disableNdefExchangeMode() {
        mNfcAdapter.setNdefPushMessage(null, Main.this);
        //mNfcAdapter.disableForegroundDispatch(this);
    }

    private NdefMessage buildNdefMessage() {
        String msgBuffer;
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        String deviceName = myDevice.getName();
        if(mode.equals("client")) {

            msgBuffer = vendorCode + "/" + passPhrase + "/" + deviceName;
        } else {
            msgBuffer = deviceName;
        }
        byte[] textBytes = msgBuffer.getBytes();
        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain".getBytes(),
                new byte[] {}, textBytes);
        return new NdefMessage(new NdefRecord[] {
                textRecord
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            List<SelectionObject> selectionObjects = new ArrayList<SelectionObject>();
            // Init SelectionObject
            for(int i = 0; i < 8; i++) {
                selectionObjects.add(new SelectionObject(getActivity()));
            }

            SelectionListAdapter adapter = new SelectionListAdapter(getActivity(), selectionObjects);

            ListView listViewForecast = (ListView) rootView.findViewById(R.id.listViewSelection);

            listViewForecast.setAdapter(adapter);

            return rootView;
        }
    }
}
