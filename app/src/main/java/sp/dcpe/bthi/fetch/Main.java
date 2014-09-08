package sp.dcpe.bthi.fetch;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        if(manager.EXTRA_WIFI_STATE.equals(2)) {
            isWifiP2pEnabled = true;
            Log.v(LOG_TAG, "Wifi P2P State is ENABLED");
        }
        else {
            isWifiP2pEnabled = false;
            Log.v(LOG_TAG, "Wifi P2P State is DISABLED");
        }
    }

    public void retrieveButtonClick(View view) {
        List<WifiP2pDevice> peers = WifiDirectBroadcastReceiver.PeerChangeListener.peers;

        for(int i = 0; i < peers.size(); i++) {
            WifiP2pDevice device = (WifiP2pDevice) peers.get(i);
            if (device.deviceName.equals("kLeb") && !isConnected) {

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        // WiFiDirectBroadcastReceiver will notify us. Ignore for now.

                        manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {

                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                                Log.v(LOG_TAG, "AAA");
                                Main.info = wifiP2pInfo;
                            }
                        });
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(Main.this, "Connect failed. Retry.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Intent serviceIntent = new Intent(this, FileTransferService.class);
                serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
                serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, "file://" + Environment.getExternalStorageDirectory() + "/Fetch/3B21TT.jpg");
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, info.groupOwnerAddress.getHostAddress());
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
                this.startService(serviceIntent);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new WifiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
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
