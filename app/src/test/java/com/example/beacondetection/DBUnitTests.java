package com.example.beacondetection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;

import com.example.beacondetection.BeaconEntities.BLEDevice;
import com.example.beacondetection.BeaconEntities.IBeacon;
import com.example.beacondetection.DB.FirestoreHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/*
* Por motivos de uso de datos mockeados, para reproducir cada test se recomienda ejecutar por separado cada uno.
* Si se ejecutan todos juntos solo funcionan 2 y, al relanzar los que fallan, 1.
* Finalmente, se comprueba la veracidad de todos
*/
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DBUnitTests {

    private Context context;
    private FirestoreHelper firestoreHelper;

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockCollectionReference;

    @Mock
    private DocumentReference mockDocumentReference;

    @Mock
    private Query mockQuery;

    @Mock
    private ScanResult mockScanResult;

    @Mock
    private BluetoothDevice mockBluetoothDevice;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = mock(Context.class);

        // Configuración del mock de ScanResult para que devuelva un dispositivo Bluetooth válido
        when(mockScanResult.getDevice()).thenReturn(mockBluetoothDevice);
        when(mockBluetoothDevice.getName()).thenReturn("Mock Device");
        when(mockBluetoothDevice.getAddress()).thenReturn("00:11:22:33:44:55");  // Dirección de MAC simulada

        // Simulación de respuestas de FirebaseFirestore
        when(mockFirestore.collection(anyString())).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);
        when(mockCollectionReference.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery); // Encadenado
        when(mockQuery.whereGreaterThan(anyString(), any())).thenReturn(mockQuery); // Encadenado
        when(mockQuery.orderBy(anyString(), any())).thenReturn(mockQuery); // Encadenado
        when(mockQuery.limit(anyLong())).thenReturn(mockQuery); // Encadenado

        // Simulación de la llamada a set()
        Task<Void> mockSetTask = Tasks.forResult(null);  // Simula una tarea de éxito
        when(mockDocumentReference.set(anyMap(), eq(SetOptions.merge()))).thenReturn(mockSetTask);

        // Simulación de la llamada a get()
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockQuery.get()).thenReturn(mockTask);

        FirestoreHelper.setFirestoreInstance(mockFirestore);
        firestoreHelper = FirestoreHelper.getInstance(context);
    }

    @Test
    public void testInsertBLEDevice() {
        BLEDevice bleDevice = new BLEDevice(mockScanResult);
        // Verifica que la dirección MAC no es nula
        assertNotNull(bleDevice.getAddress());
        firestoreHelper.insertBLEDevice(bleDevice);

        // Verifica que se ha invocado document() con la dirección correcta
        verify(mockCollectionReference).document("00:11:22:33:44:55");
        verify(mockDocumentReference).set(anyMap(), eq(SetOptions.merge()));
    }

    @Test
    public void testInsertOrUpdateDevice() {
        IBeacon beacon = new IBeacon(mockScanResult, new byte[30]);
        // Verifica que el UUID no es nulo
        assertNotNull(beacon.getUUID());
        firestoreHelper.insertOrUpdateDevice(beacon);

        // Verifica que se ha invocado document() con el UUID correcto
        verify(mockCollectionReference).document(beacon.getUUID());
        verify(mockDocumentReference).set(anyMap(), eq(SetOptions.merge()));
    }

    @Test
    public void testCountDevicesInRange() {
        firestoreHelper.insertDeviceInteraction("11111111-1111-1111-1111-111111111111", "B0:B2:1C:09:F8:F2", 4.0);
        firestoreHelper.countDevicesInRange("11111111-1111-1111-1111-111111111111", count -> {
            assertEquals(1, (int) count);
            return null;
        });
    }

    @Test
    public void testInsertDeviceInteraction() {
        firestoreHelper.insertDeviceInteraction("11111111-1111-1111-1111-111111111111", "B0:B2:1C:09:F8:F2", 3.0);
        firestoreHelper.countDevicesInRange("11111111-1111-1111-1111-111111111111", count -> {
            assertEquals(1, count.intValue());

            FirebaseFirestore.getInstance()
                    .collection("beacons")
                    .document("11111111-1111-1111-1111-111111111111")
                    .get()
                    .addOnSuccessListener(document -> {
                        assertNotNull(document);
                        assertEquals(1, document.getLong("numDevices").intValue());
                    })
                    .addOnFailureListener(e -> fail("Failed to retrieve the updated beacon data"));
            return null;
        });
    }
}
