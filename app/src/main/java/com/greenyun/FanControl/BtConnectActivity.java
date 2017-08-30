package com.greenyun.FanControl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import static java.lang.String.format;

public class BtConnectActivity extends AppCompatActivity {

	private static final int MSG_CONNECTED          = 0x101;
	private static final int MSG_CONNECTION_FAILED  = 0x102;

	private static final int REQUEST_ENABLE_BLUETOOTH   = 0x1001;

	private static final UUID SPP_UUID
		= UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	ArrayList<BluetoothDevice> bluetoothDeviceList = new ArrayList<>();
	BluetoothAdapter bluetoothAdapter;
	BluetoothDevice bluetoothDevice;
	BluetoothSocket bluetoothSocket;

	private ActivityHandler mHandler = new ActivityHandler(this);
	private ActivityBroadcastReceiver mBroadcastReceiver = new ActivityBroadcastReceiver(this);
	private ConnectThread connectThread;

	private boolean bForceStop = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (null == bluetoothAdapter) {
			new AlertDialog.Builder(this)
				.setTitle(R.string.bluetooth_device_error_title)
				.setMessage(R.string.bluetooth_device_error_msg)
				.setPositiveButton(R.string.btn_ok, null)
				.show();
			startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
			bForceStop = true;
			finish();
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bt_connect);

		registerReceiver(mBroadcastReceiver,
			new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		registerReceiver(mBroadcastReceiver,
			new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
		registerReceiver(mBroadcastReceiver,
			new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		registerReceiver(mBroadcastReceiver,
			new IntentFilter(BluetoothDevice.ACTION_FOUND));

		final Button btnRefresh = (Button) findViewById(R.id.btnRefresh);
		btnRefresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startBluetoothDiscovery();
			}
		});

		final ListView listView = (ListView) findViewById(R.id.listBluetoothDevice);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				BluetoothDevice device = bluetoothDeviceList.get(position);
				String name = device.getName();
				TextView textDeviceTips = (TextView) findViewById(R.id.textDeviceTips);

				textDeviceTips.setText(String.format(Locale.getDefault(),
					getString(R.string.text_device_selected), name));
				bluetoothDevice = device;
				BluetoothService.setBluetoothDevice(bluetoothDevice);
				connectBluetoothDevice();
			}
		});

		final SwitchCompat btSwitch = (SwitchCompat) findViewById(R.id.switchBluetooth);
		btSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (null != bluetoothAdapter) {
					int bluetoothState = bluetoothAdapter.getState();
					if (isChecked)
						if (BluetoothAdapter.STATE_OFF == bluetoothState)
							startActivityForResult(
								new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
								REQUEST_ENABLE_BLUETOOTH);
					if (!isChecked)
						if (BluetoothAdapter.STATE_ON == bluetoothState)
							if (!bluetoothAdapter.disable())
								startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
				}
				else {
					btSwitch.setChecked(false);
				}
			}
		});

		if (null != bluetoothAdapter) {
			BluetoothService.setBluetoothAdapter(bluetoothAdapter);
			int bluetoothState = bluetoothAdapter.getState();
			onBluetoothStateChanged(bluetoothState);
			if (BluetoothAdapter.STATE_OFF == bluetoothState)
				startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
					REQUEST_ENABLE_BLUETOOTH);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_ENABLE_BLUETOOTH:
				if (AppCompatActivity.RESULT_CANCELED == resultCode) {
					onBluetoothStateChanged(BluetoothAdapter.STATE_OFF);
					new AlertDialog.Builder(this)
						.setTitle(R.string.bluetooth_required_title)
						.setMessage(R.string.bluetooth_required_msg)
						.setPositiveButton(R.string.btn_ok, null)
						.show();
				}
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
				break;
		}
	}

	@Override
	protected void onDestroy() {
		if (null != connectThread) {
			connectThread.disconnect(bluetoothSocket);
			connectThread = null;
		}
		if (null != bluetoothSocket) {
			try {
				bluetoothSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		unregisterReceiver(mBroadcastReceiver);
		super.onDestroy();
		if (bForceStop)
			System.exit(0);
	}

	void connectBluetoothDevice() {
		if (null != connectThread) {
			connectThread.disconnect(bluetoothSocket);
			connectThread = null;
		}
		if (null != bluetoothSocket) {
			try {
				bluetoothSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		connectThread = new ConnectThread();
		connectThread.start();
	}

	void onBluetoothStateChanged(int state) {
		Button btRefresh = (Button) findViewById(R.id.btnRefresh);
		SwitchCompat btSwitch = (SwitchCompat) findViewById(R.id.switchBluetooth);
		TextView textDeviceList = (TextView) findViewById(R.id.textDeviceList);
		TextView textDeviceTips = (TextView) findViewById(R.id.textDeviceTips);
		switch (state) {
			case BluetoothAdapter.STATE_OFF:
				btRefresh.setVisibility(View.INVISIBLE);
				btSwitch.setEnabled(true);
				btSwitch.setChecked(false);
				bluetoothDeviceList.clear();
				refreshDeviceListView();
				textDeviceList.setVisibility(View.INVISIBLE);
				textDeviceTips.setText(R.string.text_bt_off);
				break;
			case BluetoothAdapter.STATE_ON:
				btSwitch.setEnabled(true);
				btSwitch.setChecked(true);
				textDeviceList.setVisibility(View.VISIBLE);
				textDeviceTips.setText("");
				startBluetoothDiscovery();
				break;
			case BluetoothAdapter.STATE_TURNING_OFF:
			case BluetoothAdapter.STATE_TURNING_ON:
				btSwitch.setEnabled(false);
				break;
			default:
				break;
		}
	}

	private void refreshDeviceListView() {
		ListView listBluetoothDevice
			= (ListView) findViewById(R.id.listBluetoothDevice);
		ArrayList<String> bluetoothDeviceShowList =
			new ArrayList<>();
		for (BluetoothDevice device:bluetoothDeviceList) {
			String tmpStr
				= format(Locale.getDefault(), "%s", device.getName());
			bluetoothDeviceShowList.add(tmpStr);
		}
		listBluetoothDevice.setAdapter(new ArrayAdapter<>(this,
			R.layout.array_adaptor, bluetoothDeviceShowList));
	}

	void startBluetoothDiscovery() {
		if (!bluetoothAdapter.isDiscovering()) {
			bluetoothDeviceList.clear();
			refreshDeviceListView();
			bluetoothAdapter.startDiscovery();
		}
	}

	void startControlActivity() {
		BluetoothService.setBluetoothSocket(bluetoothSocket);
		Intent intent = new Intent(BtConnectActivity.this, ControlActivity.class);
		startActivity(intent);
	}

	private static final class ActivityBroadcastReceiver extends BroadcastReceiver {

		WeakReference<BtConnectActivity> activityWeakReference;

		ActivityBroadcastReceiver(BtConnectActivity activity) {
			activityWeakReference = new WeakReference<>(activity);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			BtConnectActivity activity = activityWeakReference.get();
			Button btnRefresh
				= (Button) activity.findViewById(R.id.btnRefresh);
			String intentAction = intent.getAction();
			TextView textScanning
				= (TextView) activity.findViewById(R.id.textScanning);
			switch (intentAction) {
				case BluetoothAdapter.ACTION_STATE_CHANGED:
					//String prevStateExtra = BluetoothAdapter.EXTRA_PREVIOUS_STATE;
					//int prevState = intent.getIntExtra(prevStateExtra, -1);
					int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
					activity.onBluetoothStateChanged(bluetoothState);
					break;
				case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
					btnRefresh.setVisibility(View.INVISIBLE);
					textScanning.setVisibility(View.VISIBLE);
					break;
				case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
					btnRefresh.setVisibility(View.VISIBLE);
					textScanning.setVisibility(View.INVISIBLE);
					break;
				case BluetoothDevice.ACTION_FOUND:
					BluetoothDevice bluetoothDevice =
						intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					if (!activity.bluetoothDeviceList.contains(bluetoothDevice)) {
						activity.bluetoothDeviceList.add(bluetoothDevice);
					}
					activity.refreshDeviceListView();
					break;
				default:
					break;
			}
		}
	}

	private static class ActivityHandler extends Handler {

		WeakReference<BtConnectActivity> activityWeakReference;

		ActivityHandler(BtConnectActivity activity) {
			activityWeakReference = new WeakReference<>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			BtConnectActivity activity = activityWeakReference.get();
			switch (msg.what) {
				case MSG_CONNECTED:
					activity.startControlActivity();
					break;
				case MSG_CONNECTION_FAILED:
					new AlertDialog.Builder(activity)
						.setTitle(R.string.connection_failed_title)
						.setMessage(R.string.connection_failed_msg)
						.setPositiveButton(R.string.btn_ok, null)
						.show();
					break;
				default:
					super.handleMessage(msg);
					break;
			}
		}
	}

	private class ConnectThread extends Thread {

		BluetoothSocket socket;

		ConnectThread() {
			try {
				socket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
			} catch (IOException e) {
				e.printStackTrace();
				socket = null;
			}
		}

		@Override
		public void run() {
			if (null == socket) {
				failure();
				return;
			}
			try {
				socket.connect();
			} catch (IOException e) {
				e.printStackTrace();
				disconnect(socket);
				socket = null;
				failure();
				return;
			}
			bluetoothSocket = socket;
			mHandler.obtainMessage(MSG_CONNECTED).sendToTarget();
			synchronized (BtConnectActivity.this) {
				connectThread = null;
			}
		}

		void disconnect(BluetoothSocket bSocket) {
			if (null != bSocket) {
				try {
					bSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		void failure() {
			mHandler.obtainMessage(MSG_CONNECTION_FAILED).sendToTarget();
			synchronized (BtConnectActivity.this) {
				connectThread = null;
			}
		}
	}
}
