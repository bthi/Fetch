package sp.dcpe.bthi.fetch;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by TERENCE on 10/9/2014.
 */
public class PrefsFragment extends PreferenceFragment{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
