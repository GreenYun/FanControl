package com.greenyun.FanControl;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;

public class ControlActivity extends AppCompatActivity {

	private static final int MSG_DATA_READ = 0x101;
	private static final int MSG_READ_ERROR = 0x102;

	BluetoothSocket bluetoothSocket;

	ActivityHandler mHandler;
	ListeningThread listeningThread;
	SendingThread sendingThread;

	boolean bOut = true;
	int out = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control);
		mHandler = new ActivityHandler(this);
		bluetoothSocket = BluetoothService.getBluetoothSocket();

		Button btnAM = (Button) findViewById(R.id.btnAM);
		btnAM.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				out = '2';
			}
		});
		Button btnUp = (Button) findViewById(R.id.btnUp);
		btnUp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				out = '3';
			}
		});
		Button btnDown = (Button) findViewById(R.id.btnDown);
		btnDown.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				out = '1';
			}
		});

		listeningThread = new ListeningThread();
		listeningThread.start();
		sendingThread = new SendingThread();
		sendingThread.start();
	}

	private static class ActivityHandler extends Handler {

		WeakReference<ControlActivity> activityWeakReference;

		ActivityHandler(ControlActivity controlActivity) {
			activityWeakReference = new WeakReference<>(controlActivity);
		}

		@Override
		public void handleMessage(Message msg) {
			ControlActivity activity = activityWeakReference.get();
			TextView textSpeed = (TextView) activity.findViewById(R.id.textSpeed);
			TextView textTemp = (TextView) activity.findViewById(R.id.textTemp);
			switch (msg.what) {
				case MSG_DATA_READ:
					int[] data = (int[]) msg.obj;
					double temp = data[0] / 100.0;
					textSpeed.setText(String.format(Locale.getDefault(),
						activity.getString(R.string.tips_speed) + "%d r/min", data[1]));
					textTemp.setText(String.format(Locale.getDefault(),
						activity.getString(R.string.tips_temp) + "%.2f℃", temp));
					break;
				case MSG_READ_ERROR:
					break;
				default:
					break;
			}
			super.handleMessage(msg);
		}
	}

	private class ListeningThread extends Thread {
		InputStream inputStream;

		ListeningThread() {
			InputStream tmpIn = null;
			try {
				tmpIn = bluetoothSocket.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			inputStream = tmpIn;
		}

		public void run() {

			final int bufferLength  = 6;
			final int dataLength    = 2;

			int[] buffer = new int[bufferLength];
			int[] data = new int[dataLength];
			int i = 0;
			DataInputStream dataInputStream = new DataInputStream(inputStream);
			while (true) {
				try {
					while (0 == i) {
						if (0xff == buffer[0]) {
							++i;
							break;
						}
						buffer[0] = dataInputStream.readUnsignedByte();
					}
					for (; i < bufferLength; ++i)
						buffer[i] = dataInputStream.readUnsignedByte();
					if ((0xff == buffer[0]) && (0 == buffer[bufferLength - 1])) {
						for (int j = 0; j < dataLength; ++j)
							data[j] = buffer[j * 2 + 1] * 256 + buffer[j * 2 + 2];
						mHandler.obtainMessage(MSG_DATA_READ, -1, -1, data).sendToTarget();
						i = 0;
						buffer[0] = 0;
					}
					else {
						int m;
						for (m = bufferLength - 1; m > 0; --m)
							if (0xff == buffer[m])
								break;
						for (i = 0, buffer[0] = 0; (0 < m) && (m < bufferLength); ++i, ++m)
							buffer[i] = buffer[m];
					}
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
			mHandler.obtainMessage(MSG_READ_ERROR, -1, -1, -1).sendToTarget();
		}
	}

	private class SendingThread extends Thread {
		OutputStream outputStream;

		SendingThread() {
			try {
				outputStream = bluetoothSocket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				while (bOut) {
					outputStream.write(out);
					if (0 != out)
						out = 0;
					sleep(100);
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
