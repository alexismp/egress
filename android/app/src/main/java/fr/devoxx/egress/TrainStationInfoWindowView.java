package fr.devoxx.egress;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;

import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TrainStationInfoWindowView extends LinearLayout {

    @InjectView(R.id.name) TextView nameView;
    @InjectView(R.id.owner) TextView ownerView;

    public TrainStationInfoWindowView(Context context) {
        super(context);
    }

    public TrainStationInfoWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TrainStationInfoWindowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
    }

    public void bind(DataSnapshot dataSnapshot) {
        Map<String, Object> dataValues = (Map<String, Object>) dataSnapshot.getValue();
        nameView.setText((String) dataValues.get("NOM"));
        ownerView.setText((String) dataValues.get("owner"));
    }
}
