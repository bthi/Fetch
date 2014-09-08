package sp.dcpe.bthi.fetch;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Kevin on 7/9/14.
 */
public class SelectionListAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private Activity activity;
    private List<SelectionObject> selectionObjects;

    public SelectionListAdapter(Activity activity, List<SelectionObject> selectionObjects) {
        this.activity = activity;
        this.selectionObjects = selectionObjects;
        this.inflater = activity.getLayoutInflater();
    }

    @Override
    public int getCount() {
        return selectionObjects.size();
    }

    @Override
    public Object getItem(int position) {
        return selectionObjects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View singleListView, ViewGroup parent) {

        singleListView = this.inflater.inflate(R.layout.listitem_selection, null);

        ImageView selectionIconIV = (ImageView) singleListView.findViewById(R.id.selectionIconIV);
        TextView selectionTV = (TextView) singleListView.findViewById(R.id.selectionTV);
        CheckBox selectionCB = (CheckBox) singleListView.findViewById(R.id.selectionCB);

        selectionIconIV.setImageDrawable(selectionObjects.get(position).get_icon());
        selectionTV.setText(selectionObjects.get(position).get_title());
        selectionCB.setChecked(selectionObjects.get(position).is_checked());

        selectionCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                selectionObjects.get(position).set_checked(isChecked);
            }
        });
        return singleListView;
    }
}
