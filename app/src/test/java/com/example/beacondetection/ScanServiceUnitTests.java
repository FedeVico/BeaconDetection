package com.example.beacondetection;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.*;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.Context;

import com.example.beacondetection.Adapters.DeviceListAdapter;
import com.example.beacondetection.BeaconEntities.IBeacon;
import com.example.beacondetection.Services.ScanService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;


/*
* Pasan todos los tests relacionados con el ScanService, el más importante = testCheckDeviceExists
* */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 23)
public class ScanServiceUnitTests {

    @Mock
    private BluetoothAdapter mockBluetoothAdapter;

    // Implementación falsa para DeviceListAdapter
    private static class FakeDeviceListAdapter extends DeviceListAdapter {
        public FakeDeviceListAdapter(ArrayList<Object> deviceList) {
            super(deviceList);
        }
    }

    private BluetoothDevice mockBluetoothDevice;
    private ScanResult mockScanResult;
    private Context context;
    private ArrayList<Object> deviceList;
    private ScanService scanService;
    private DeviceListAdapter fakeAdapter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Simula el contexto
        context = mock(Context.class);

        // Simula el BluetoothManager, BluetoothAdapter, y BluetoothLeScanner
        BluetoothManager mockBluetoothManager = mock(BluetoothManager.class);
        mockBluetoothAdapter = mock(BluetoothAdapter.class);
        BluetoothLeScanner mockBluetoothLeScanner = mock(BluetoothLeScanner.class);
        NotificationManager mockNotificationManager = mock(NotificationManager.class);

        // Asegura que el contexto devuelva los servicios simulados
        when(context.getSystemService(BluetoothManager.class)).thenReturn(mockBluetoothManager);
        when(mockBluetoothManager.getAdapter()).thenReturn(mockBluetoothAdapter);
        when(mockBluetoothAdapter.getBluetoothLeScanner()).thenReturn(mockBluetoothLeScanner);
        when(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);

        // Inicializa la lista de dispositivos y el adaptador falso
        deviceList = new ArrayList<>();
        fakeAdapter = new FakeDeviceListAdapter(deviceList);

        // Inicializa el ScanService
        scanService = new ScanService(context, deviceList, fakeAdapter);
        scanService.initScanner();

        // Simula BluetoothDevice y ScanResult
        mockBluetoothDevice = mock(BluetoothDevice.class);
        mockScanResult = mock(ScanResult.class);
        when(mockScanResult.getDevice()).thenReturn(mockBluetoothDevice);
        when(mockBluetoothDevice.getAddress()).thenReturn("00:11:22:33:44:55");
    }

    @Test
    public void testIsBluetoothEnabled() {
        when(mockBluetoothAdapter.isEnabled()).thenReturn(true);
        assertTrue(scanService.isBluetoothEnabled());

        when(mockBluetoothAdapter.isEnabled()).thenReturn(false);
        assertFalse(scanService.isBluetoothEnabled());
    }

    @Test
    public void testStartBLEScan() {
        when(mockBluetoothAdapter.isEnabled()).thenReturn(true);
        scanService.startBLEScan(context);
        assertTrue(scanService.isScanning());
    }

    @Test
    public void testStopBLEScan() {
        when(mockBluetoothAdapter.isEnabled()).thenReturn(true);
        scanService.startBLEScan(context);
        scanService.stopBLEScan(context);
        assertFalse(scanService.isScanning());
    }

    @Test
    public void testCheckDeviceExists() {
        // Simula que ScanResult devuelve la dirección correcta del dispositivo
        when(mockScanResult.getDevice().getAddress()).thenReturn("00:11:22:33:44:55");

        // Crea una instancia real de IBeacon con el ScanResult simulado
        byte[] data = new byte[30];  // Suponiendo que estos son los datos necesarios para IBeacon
        IBeacon beacon = new IBeacon(mockScanResult, data);

        deviceList.add(beacon);

        // Prueba si el dispositivo existe
        int index = scanService.checkDeviceExists(mockScanResult);
        assertEquals(0, index);

        // Cambia el mock para devolver una dirección diferente y prueba nuevamente
        when(mockScanResult.getDevice().getAddress()).thenReturn("11:22:33:44:55:66");
        index = scanService.checkDeviceExists(mockScanResult);
        assertEquals(-1, index);
    }
}
