package com.example.beacondetection;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import java.util.Collection;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {
    protected static final String TAG = "BeaconsMonitoring";
    private BeaconManager beaconManager = null;
    private Region beaconRegion = null;
    private Button startButton;
    private Button stopButton;
    private static final String ALTBEACON_LAYOUT = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
    private void showAlert(final String title, final String message) {
        runOnUiThread(() -> {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle(title);
            alertDialog.setMessage(message);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    (dialog, which) -> {
                        dialog.dismiss();
                    });
            alertDialog.show();
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.scanButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        startButton.setOnClickListener((v) ->{startBeaconMonitoring();});
        stopButton.setOnClickListener((v) ->{stopBeaconMonitoring();});
        checkBluetoothPermissions();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(ALTBEACON_LAYOUT));
        beaconManager.bind(this);
    }
    private static final int REQUEST_PERMISSION_BLUETOOTH = 1;

    @RequiresApi(api = Build.VERSION_CODES.S)
    private boolean checkBluetoothPermissions() {
        boolean permissionsGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int bluetoothScanPermissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN);
            if (bluetoothScanPermissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN},
                        1004);
                permissionsGranted = false;
            }
        }
        int bluetoothPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH);
        int bluetoothAdminPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_ADMIN);
        int coarseLocationPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (bluetoothPermissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH},
                    1001);
            permissionsGranted = false;
        }

        if (bluetoothAdminPermissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    1002);
            permissionsGranted = false;
        }

        if (coarseLocationPermissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1003);
            permissionsGranted = false;
        }

        return permissionsGranted;
    }


    private Boolean entryMessage = false;
    private Boolean exitMessage = false;
    private Boolean rangingMessage = false;

    private void stopBeaconMonitoring() {
        try {
            beaconManager.stopMonitoringBeaconsInRegion(beaconRegion);
            beaconManager.stopRangingBeaconsInRegion(beaconRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void startBeaconMonitoring() {
        Log.d(TAG, "startBeaconMonitoring called");
        try {
            beaconRegion = new Region("MyBeacons", Identifier.parse("4d6fc88b-be75-6698-da48-6866a36ec78e"),
                    Identifier.parse("4"), Identifier.parse("200"));
            beaconManager.startMonitoringBeaconsInRegion(beaconRegion);
            beaconManager.startRangingBeaconsInRegion(beaconRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect called");
        beaconManager.setMonitorNotifier(new MonitorNotifier() {


            public void didEnterRegion(Region region) {
                if (!entryMessage) {
                    showAlert("didEnterRegion", "Entering region " + region.getUniqueId() +
                            " Beacon detected UUID/major/minor: " + region.getId1() +
                            "/" + region.getId2() + "/" + region.getId3());
                    entryMessage = true;
                }
            }


            public void didExitRegion(Region region) {
                if (!exitMessage) {
                    showAlert("didExitRegion", "Exiting region " + region.getUniqueId() +
                            " Beacon detected UVID/major/minor: " + region.getId1() +
                            "/" + region.getId2() + "/" + region.getId3());
                    exitMessage = true;
                }
            }

            public void didDetermineStateForRegion(int i, Region region) {
            }
        });

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override

            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (!rangingMessage && beacons != null && !beacons.isEmpty()) {
                    for (Beacon beacon : beacons) {
                        showAlert("didExitRegion", "Ranging region" + region.getUniqueId() +
                                " Beacon detected UUID/major/minor: " + beacon.getId1() +
                                "/" + beacon.getId2() + "/" + beacon.getId3());

                    }
                    rangingMessage = true;
                }
            }
        });
    }
}

