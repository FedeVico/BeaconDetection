package com.example.beacondetection;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import java.util.Collection;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {
    protected static final String TAG = "BeaconsMonitoring";
    private BeaconManager beaconManager;
    private Button scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanButton = findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBeaconScan();
            }
        });

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.bind(this);
    }

    private void startBeaconScan() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    for (Beacon beacon : beacons) {
                        Log.d(TAG, "Beacon detected: " + beacon.getId1() + " - " + beacon.getDistance());
                    }
                } else {
                    Log.d(TAG, "No beacons detected");
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        // Aquí es donde deberías configurar tus detectores de beacons y comenzar a monitorear o escanear
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
}
