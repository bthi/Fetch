package sp.dcpe.bthi.fetch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kevin on 8/9/14.
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    public static final String LOG_TAG = WifiDirectBroadcastReceiver.class.getSimpleName();

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private Main activity;

    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, Main activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();

        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(intentAction)) {

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

            if(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                activity.setWifiP2pEnabled(true);
                Log.v(LOG_TAG, "Wifi P2P State is ENABLED");
            }
            else {
                activity.setWifiP2pEnabled(false);
                Log.v(LOG_TAG, "Wifi P2P State is DISABLED");
            }
        } else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(intentAction)) {
            // if peers is available
            if(manager != null) {
                manager.requestPeers(channel, (WifiP2pManager.PeerListListener) new PeerChangeListener(activity));
            }
            Log.v(LOG_TAG, "Peers Discovery Changed");
        } else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(intentAction)) {

            if(manager == null) {
                return;
            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if(networkInfo.isConnected()) {
                Log.v(LOG_TAG, "Connected");
                activity.setConnected(true);

                if(activity.mode.equalsIgnoreCase("client")) {
                    manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {

                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                            Main.info = wifiP2pInfo;

                            if (wifiP2pInfo.isGroupOwner) {
                                new FileTransferServer(activity).execute();
                            }
                        }
                    });
                } else {
                    manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {

                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                            Main.info = wifiP2pInfo;

                            activity.transferFile();
                        }
                    });
                }
            }
        }
    }

    public static class PeerChangeListener implements WifiP2pManager.PeerListListener{

        public static List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

        private Main activity;

        public PeerChangeListener(Main activity) {
            this.activity = activity;
        }

        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            //peers.clear();
            peers.addAll(wifiP2pDeviceList.getDeviceList());
            activity.formP2pConnection();
        }

        public List getPeers() {
            return peers;
        }
    }

    public class FileTransferServer extends AsyncTask<Void, Void, String> {

        public final String LOG_TAG = FileTransferServer.class.getSimpleName();

        private Context context;

        public FileTransferServer(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {

                /**
                 * Create a server socket and wait for client connections. This
                 * call blocks until a connection is accepted from a client
                 */
                ServerSocket serverSocket = new ServerSocket(8988);
                Socket client = serverSocket.accept();

                /**
                 * If this code is reached, a client has connected and transferred data
                 * Save the input stream from the client as a JPEG file
                 */
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                return null;
            }
        }
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(LOG_TAG, e.toString());
            return false;
        }
        return true;
    }
}
