package gps.fake;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FakeBluetoothGPS {
    private static final UUID GPS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final DateFormat timeFormat = new SimpleDateFormat("HHmmss");
    private final DateFormat dateFormat = new SimpleDateFormat("ddMMyy");

    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ConnectedThread connectedThread;
    private AcceptThread acceptThread;

    private LatLng lastLatLng;
    private LatLng currentLatLng;

    private StringBuffer gprmc = new StringBuffer();

    public void start() {
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                acceptThread = new AcceptThread(device.getAddress());
                acceptThread.run();
            }
        }

        ScheduledExecutorService scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                sendPositions();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
    }

    public boolean isConnected() {
        return connectedThread != null;
    }

    public void move(LatLng target) {
        lastLatLng = currentLatLng;
        currentLatLng = target;
    }

    private void sendPositions() {
        try {
            if (connectedThread != null && currentLatLng != null) {
                final Date date = new Date();

                gprmc.setLength(0);

                gprmc.append("$GPRMC,"); // Recommended Minimum sentence
                gprmc.append(",");
                gprmc.append(timeFormat.format(date)); // Fix time
                gprmc.append(",");
                gprmc.append("A"); // Status A=active or V=Void
                gprmc.append(",");
                gprmc.append(getComponents(currentLatLng.latitude)); // Latitude
                gprmc.append(",");
                gprmc.append(getNorthSouth()); // N or S (Latitude)
                gprmc.append(",");
                gprmc.append(getComponents(currentLatLng.longitude)); // Latitude
                gprmc.append(",");
                gprmc.append(getEastWest()); // E or W (Longitude)
                gprmc.append(",");
                gprmc.append("0"); // Speed over the ground in knots
                gprmc.append(",");
                gprmc.append(String.valueOf(findBearing())); // Track angle in degrees
                gprmc.append(",");
                gprmc.append(dateFormat.format(date)); // Date
                gprmc.append(",");
                gprmc.append("000.0"); // Magnetic Variation
                gprmc.append(",");
                gprmc.append("W"); // E or W (Magnetic variation)
                gprmc.append("*6A"); // checksum (always begins with *)
                gprmc.append("\n"); // Carriage return

                connectedThread.write(gprmc.toString().getBytes());
            }
        } catch (Throwable e) {
            Log.e(FakeBluetoothGPS.class.getName(), "Error writing on socket");
        }
    }

    private double findBearing() {
        double bearing = 0.0f;

        if (currentLatLng != null && lastLatLng != null) {
            bearing = SphericalUtil.computeHeading(lastLatLng, currentLatLng);
        }

        return Math.round(((bearing * 60) % 60) * 100) / 100.0;
    }

    private String getEastWest() {
        String output = "E";

        if (currentLatLng.latitude < 0) {
            output = "W";
        }

        return output;
    }

    private String getNorthSouth() {
        String output = "N";

        if (currentLatLng.longitude < 0) {
            output = "S";
        }

        return output;
    }

    private String getComponents(double value) {
        value = Math.abs(value);

        String d = "" + (int) value;
        String m = "" + Math.round(((value * 60) % 60) * 100) / 100.0;

        return d + m;
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread(String name) {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(name, GPS_UUID);
            } catch (IOException e) {
                Log.e(FakeBluetoothGPS.class.getName(), "Error listening on socket");
            }
            mServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;
            while (true) {
                try {
                    socket = mServerSocket.accept();
                } catch (IOException e) {
                    Log.e(FakeBluetoothGPS.class.getName(), "Error accepting on socket");
                    break;
                }
                if (socket != null) {
                    try {
                        mServerSocket.close();

                        connectedThread = new ConnectedThread(socket);
                        connectedThread.run();

                        break;
                    } catch (IOException e) {
                        Log.e(FakeBluetoothGPS.class.getName(), "Error connecting");
                    }
                }
            }
        }

        public void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(FakeBluetoothGPS.class.getName(), "Error closing socket");
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mSocket = socket;
            OutputStream tmpOut = null;

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(FakeBluetoothGPS.class.getName(), "Error getting output stream");
            }

            mOutStream = tmpOut;
        }

        public void write(byte[] bytes) {
            try {
                mOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(FakeBluetoothGPS.class.getName(), "Error writing bytes");
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(FakeBluetoothGPS.class.getName(), "Error closing socket");
            }
        }
    }
}
