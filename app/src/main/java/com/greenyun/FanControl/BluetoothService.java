package com.greenyun.FanControl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

class BluetoothService{

	static private BluetoothAdapter bluetoothAdapter;
	static private BluetoothDevice bluetoothDevice;
	static private BluetoothSocket bluetoothSocket;

	public static BluetoothAdapter getBluetoothAdapter() {
		return bluetoothAdapter;
	}

	public static BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}

	static BluetoothSocket getBluetoothSocket() {
		return bluetoothSocket;
	}

	static void setBluetoothAdapter(BluetoothAdapter bluetoothAdapter) {
		BluetoothService.bluetoothAdapter = bluetoothAdapter;
	}

	static void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
		BluetoothService.bluetoothDevice = bluetoothDevice;
	}

	static void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
		BluetoothService.bluetoothSocket = bluetoothSocket;
	}

//	private ConnectThread connectThread;
//
//	private Handler sHandler;
//
//	private static class ServiceHandler extends Handler {
//
//		WeakReference<BluetoothService> serviceWeakReference;
//
//		ServiceHandler(BluetoothService service) {
//			serviceWeakReference = new WeakReference<>(service);
//		}
//
//		@Override
//		public void handleMessage(Message msg) {
//			BluetoothService service = serviceWeakReference.get();
//			switch (msg.what) {
//				case MSG_CONNECTED:
//					service.sHandler.obtainMessage(MSG_CONNECTED).sendToTarget();
//					break;
//				case MSG_CONNECTION_FAILED:
//					service.sHandler.obtainMessage(MSG_CONNECTION_FAILED).sendToTarget();
//					break;
//				default:
//					break;
//			}
//		}
//	}
//
//	private ServiceHandler mHandler = new ServiceHandler(this);
//
////	private int errno;
//
//	BluetoothService(Handler handler) {
//		sHandler = handler;
//		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//	}
//
//	BluetoothAdapter getBluetoothAdapter() {
//		return bluetoothAdapter;
//	}
//
//
//	void connect() {
//		if (null != connectThread) {
//			connectThread.disconnect(bluetoothSocket);
//			connectThread = null;
//		}
//		connectThread = new ConnectThread();
//		connectThread.start();
//	}
//
//	private class ConnectThread extends Thread {
//
//		BluetoothSocket socket;
//
//		ConnectThread() {
//			try {
//				socket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
//			} catch (IOException e) {
//				e.printStackTrace();
//				socket = null;
//			}
//		}
//
//		@Override
//		public void run() {
//			if (null == socket) {
//				failure();
//				return;
//			}
//			try {
//				//disconnect(socket);
//				socket.connect();
//			} catch (IOException e) {
//				e.printStackTrace();
//				disconnect(socket);
//				socket = null;
//				failure();
//				return;
//			}
//			bluetoothSocket = socket;
//			mHandler.obtainMessage(MSG_CONNECTED).sendToTarget();
//			synchronized (BluetoothService.this) {
//				connectThread = null;
//			}
//		}
//
//		void disconnect(BluetoothSocket bSocket) {
//			if (null != bSocket) {
//				try {
//					bSocket.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		void failure() {
//			mHandler.obtainMessage(MSG_CONNECTION_FAILED).sendToTarget();
//			synchronized (BluetoothService.this) {
//				connectThread = null;
//			}
//		}
//	}
}
