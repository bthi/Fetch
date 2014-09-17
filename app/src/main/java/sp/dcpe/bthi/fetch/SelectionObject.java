package sp.dcpe.bthi.fetch;

import android.app.Activity;
import android.graphics.drawable.Drawable;

/**
 * Created by Kevin on 7/9/14.
 */
public class SelectionObject {

    private Drawable _icon;
    private String _title;
    private boolean _checked;

    public static int _listCount;

    public SelectionObject(Activity activity) {
        switch(_listCount) {
            case 0:
                _icon = activity.getResources().getDrawable(R.drawable.ic_launcher);
                _title = "Temperature";
                _checked = false;
                break;
            case 1:
                _icon = activity.getResources().getDrawable(R.drawable.ic_launcher);
                _title = "Bandwith";
                _checked = false;
                break;
            case 2:
                _icon = activity.getResources().getDrawable(R.drawable.ic_launcher);
                _title = "I/O Disk Cycle";
                _checked = false;
                break;
            case 3:
                _icon = activity.getResources().getDrawable(R.drawable.ic_launcher);
                _title = "CPU Load";
                _checked = false;
                break;
            case 4:
                _icon = activity.getResources().getDrawable(R.drawable.ic_launcher);
                _title = "Running Processes";
                _checked = false;
                break;
            case 5:
                _icon = activity.getResources().getDrawable(R.drawable.ic_launcher);
                _title = "Disk Quotas";
                _checked = false;
                break;
            case 6:
                _icon = activity.getResources().getDrawable(R.drawable.ic_launcher);
                _title = "System Logs";
                _checked = false;
                break;
            case 7:
                _icon = activity.getResources().getDrawable(R.drawable.ic_launcher);
                _title = "System Uptime";
                _checked = false;
                break;
        }

        _listCount++;
        if(_listCount == 8)
            _listCount = 0;
    }

    public Drawable get_icon() {
        return _icon;
    }

    public String get_title() {
        return _title;
    }

    public boolean is_checked() {
        return _checked;
    }

    public void set_checked(boolean _checked) {
        this._checked = _checked;
    }
}
