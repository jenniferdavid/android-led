package in.ac.iitm.led;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * @author ashish
 * 
 */
public class MainActivity extends Activity {
	private Handler handler;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;
	private BluetoothSocket mBluetoothSocket;
	private OutputStream mOutputStream;

	private final int REQUEST_ENABLE_BT = 1;
	private final String MAC_ADDRESS_BTBEE = "00:11:11:31:72:27";
	private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		initializeBluetooth();
		setListeners();

		handler = new Handler();
	}

	@Override
	public void onBackPressed() {
		if (mBluetoothSocket != null) {
			ConnectThread connectThread = new ConnectThread(mBluetoothDevice, mBluetoothSocket);
			connectThread.cancel();
		}
		super.onBackPressed();
	}

	/**
	 * Basic bluetooth setup.
	 */
	private void initializeBluetooth() {
		// Step 1: Get Bluetooth Adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Step 2: Enable Bluetooth Adapter
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		// Step 3: Get Bonded Devices
		Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
		for (BluetoothDevice device : bondedDevices) {
			if (device.getAddress().equals(MAC_ADDRESS_BTBEE)) {
				mBluetoothDevice = device;
			}
		}

		// Step 4: Create RFCOMM Socket
		try {
			mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
		} catch (IOException e) {
		}
	}

	/**
	 * Set various listeners.
	 */
	private void setListeners() {
		// Toggle bluetooth connection to BTBEE.
		CheckBox checkBox_bluetoothOn = (CheckBox) findViewById(R.id.checkBox_bluetoothOn);
		checkBox_bluetoothOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				ConnectThread connectThread = new ConnectThread(mBluetoothDevice, mBluetoothSocket);
				if (isChecked) {
					connectThread.start();
				} else {
					connectThread.cancel();
				}
			}
		});

		// Toggle LED state.
		CheckBox checkBox_LEDOn = (CheckBox) findViewById(R.id.checkBox_LEDOn);
		checkBox_LEDOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				ConnectedThread connectedThread = new ConnectedThread(mBluetoothSocket, mOutputStream);
				if (isChecked) {
					connectedThread.write(new byte[]{(byte) 100});
				} else {
					connectedThread.write(new byte[]{(byte) 0});
				}
			}
		});

		// Change LED Intensity.
		SeekBar seekBar_LEDIntensity = (SeekBar) findViewById(R.id.seekBar_LEDIntensity);
		seekBar_LEDIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				ConnectedThread connectedThread = new ConnectedThread(mBluetoothSocket, mOutputStream);
				connectedThread.write(new byte[]{(byte) progress});
			}
		});

		// See Help.
		Button button_help = (Button) findViewById(R.id.button_help);
		button_help.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Intent intent_help = new Intent(MainActivity.this, HelpActivity.class);
				startActivity(intent_help);
			}
		});

		// See Credits.
		Button button_credits = (Button) findViewById(R.id.button_credits);
		button_credits.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Intent intent_credits = new Intent(MainActivity.this, CreditsActivity.class);
				startActivity(intent_credits);
			}
		});

	}

	/**
	 * @author ashish
	 * 
	 */
	private class ConnectThread extends Thread {
		private final BluetoothDevice mmDevice;
		private final BluetoothSocket mmSocket;

		/**
		 * @param device
		 *            Initialize bluetooth device.
		 * @param socket
		 *            Initialize bluetooth socket with already existing bluetooth socket.
		 */
		public ConnectThread(BluetoothDevice device, BluetoothSocket socket) {
			// Initialize bluetooth device.
			mmDevice = device;

			// Initialize bluetooth socket.
			mmSocket = socket;
		}

		@Override
		public void run() {
			handler.post(new Runnable() {

				public void run() {
					Toast.makeText(MainActivity.this, "Connecting to BTBEE ...", Toast.LENGTH_SHORT).show();
				}
			});

			mBluetoothAdapter.cancelDiscovery();
			try {
				// Call to connect() is a blocking call.
				mmSocket.connect();
				mOutputStream = mmSocket.getOutputStream();
				handler.post(new Runnable() {

					public void run() {
						Toast.makeText(MainActivity.this, "Connected to BTBEE", Toast.LENGTH_SHORT).show();
					}
				});

			} catch (IOException connectException) {
				handler.post(new Runnable() {

					public void run() {
						Toast.makeText(MainActivity.this, "Could not connect to BTBEE", Toast.LENGTH_SHORT).show();
					}
				});

				try {
					mmSocket.close();
				} catch (IOException closeException) {
				}
				return;
			}
		}

		/**
		 * Cancel an in-progress connection, and close the socket.
		 */
		public void cancel() {
			try {
				mmSocket.close();
				Toast.makeText(MainActivity.this, "Disconnected from BTBEE", Toast.LENGTH_SHORT).show();
				initializeBluetooth();
			} catch (IOException e) {
			}
		}

	}

	/**
	 * @author ashish
	 * 
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final OutputStream mmOutputStream;

		/**
		 * @param socket
		 *            Initialize bluetooth socket.
		 * @param oStream
		 *            Initialize output stream with already existing output stream.
		 */
		public ConnectedThread(BluetoothSocket socket, OutputStream oStream) {
			mmSocket = socket;
			mmOutputStream = oStream;
		}

		@Override
		public void run() {

		}

		/**
		 * @param bytes
		 *            Write data to the device.
		 */
		public void write(byte[] bytes) {
			try {
				// Call to write() is a blocking call.
				mmOutputStream.write(bytes);
			} catch (IOException e) {
			}
		}

		/**
		 * Cancel an in-progress connection, and close the socket.
		 */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}
}
