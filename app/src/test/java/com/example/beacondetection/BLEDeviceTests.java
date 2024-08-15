package com.example.beacondetection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

import com.example.beacondetection.BeaconEntities.BLEDevice;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BLEDeviceTests {

    private ScanResult mockScanResult;
    private BluetoothDevice mockBluetoothDevice;
    private BLEDevice bleDevice;

    @Before
    public void setUp() {
        // Crear los mocks
        mockBluetoothDevice = Mockito.mock(BluetoothDevice.class);
        mockScanResult = Mockito.mock(ScanResult.class);

        // Configurar los valores de retorno de los mocks
        Mockito.when(mockBluetoothDevice.getName()).thenReturn("Test Device");
        Mockito.when(mockBluetoothDevice.getAddress()).thenReturn("00:11:22:33:44:55");
        Mockito.when(mockScanResult.getDevice()).thenReturn(mockBluetoothDevice);
        Mockito.when(mockScanResult.getRssi()).thenReturn(-65);

        // Crear una instancia de BLEDevice con el ScanResult mockeado
        bleDevice = new BLEDevice(mockScanResult);
    }

    @Test
    public void testGetAddress() {
        // Verificar que la dirección es la esperada
        assertEquals("00:11:22:33:44:55", bleDevice.getAddress());
    }

    @Test
    public void testAddRssi() {
        // Añadir valores RSSI y verificar el cálculo promedio
        bleDevice.addRssi(-70);
        bleDevice.addRssi(-75);
        assertEquals(-70, bleDevice.calculateRssi());
    }

    @Test
    public void testCalculateRssi() {
        // Añadir valores RSSI y verificar el cálculo promedio
        bleDevice.addRssi(-70);
        bleDevice.addRssi(-60);
        int expectedAverageRssi = (int) Math.round(Arrays.asList(-65, -70, -60).stream().mapToInt(Integer::intValue).average().getAsDouble());
        assertEquals(expectedAverageRssi, bleDevice.calculateRssi());
    }

    @Test
    public void testGetDistance1() {
        // Añadir valores RSSI y verificar el cálculo de la distancia usando getDistance1
        bleDevice.addRssi(-70);
        bleDevice.addRssi(-60);
        double expectedDistance = Math.pow(10, (-59 - bleDevice.calculateRssi()) / (10 * 2.0));
        assertEquals(expectedDistance, bleDevice.getDistance1(), 0.01);
    }

    @Test
    public void testGetDistance() {
        // Añadir valores RSSI y verificar el cálculo de la distancia usando getDistance
        bleDevice.addRssi(-70);
        bleDevice.addRssi(-60);
        double ratio = bleDevice.calculateRssi() / -53.0;
        double expectedDistance = Math.exp(1.203420305) * Math.pow(ratio, 6.170094565) + -0.203420305;
        assertEquals(expectedDistance, bleDevice.getDistance(), 0.01);
    }

    @Test
    public void testRssiListSizeLimit() {
        // Añadir más valores RSSI de los permitidos y verificar que se mantenga el límite del tamaño de la lista
        bleDevice.addRssi(-70);
        bleDevice.addRssi(-60);
        bleDevice.addRssi(-65);
        bleDevice.addRssi(-55);
        bleDevice.addRssi(-50);
        bleDevice.addRssi(-45); // Esto debería eliminar el primer valor (-65)
        int expectedRssi = (int) Math.round(Arrays.asList(-60, -65, -55, -50, -45).stream().mapToInt(Integer::intValue).average().getAsDouble());
        assertEquals(expectedRssi, bleDevice.calculateRssi());
    }
}
