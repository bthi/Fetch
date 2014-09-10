package sp.dcpe.bthi.fetch;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.Settings;
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
    final Context context = this;
    NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
//
//        prefCheckBox = (CheckBox) findViewById(R.id.prefCheckBox);
//        prefEditText = (TextView) findViewById(R.id.prefEditText);

//        loadPref();
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
            Intent goSettings = new Intent(this, sp.dcpe.bthi.fetch.Settings.class);
            startActivity(goSettings);
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

    public void Retrieve (View view){
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        WifiManager Wifi =(WifiManager)getSystemService(Context.WIFI_SERVICE);

        Toast.makeText(getApplicationContext(), "Checking WIFI and NFC...", Toast.LENGTH_SHORT).show();

        if(Wifi.isWifiEnabled() == false){
            Toast.makeText(getApplicationContext(), "Turning on WIFI...", Toast.LENGTH_SHORT).show();
            Wifi.setWifiEnabled(true);
            Toast.makeText(getApplicationContext(), "Your Wifi is enabled.", Toast.LENGTH_SHORT).show();
        }

        else Toast.makeText(getApplicationContext(), "Your Wifi is enabled.", Toast.LENGTH_SHORT).show();

        if(mNfcAdapter.isEnabled() == true)
            Toast.makeText(getApplicationContext(), "Your NFC is enabled.", Toast.LENGTH_SHORT).show();

        else{
            NFCAlert();
        }

        if(mNfcAdapter.isEnabled() == true && Wifi.isWifiEnabled() == true){
            Toast.makeText(getApplicationContext(), "Proceed...", Toast.LENGTH_SHORT).show();

            //CONTINUE WITH NFC
        }
    }

    public void NFCAlert(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);
        // set title
        alertDialogBuilder.setTitle("Enable NFC?");

        // set dialog message
        alertDialogBuilder
                .setMessage("Your NFC is disabled. File(s) cannot be sent without NFC, please enable NFC and click 'Retrieve' again.\n\nDo you want to enable NFC?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }
}
