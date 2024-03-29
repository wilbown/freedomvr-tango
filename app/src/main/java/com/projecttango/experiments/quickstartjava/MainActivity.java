/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.projecttango.experiments.quickstartjava;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.quickstartjava.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;


/**
 * Main Activity for the Tango Java Quickstart. Demonstrates establishing a
 * connection to the {@link Tango} service and printing the
 * data to the LogCat. Also demonstrates Tango lifecycle management through
 * {@link TangoConfig}.
 */
public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();


	private static final UUID FVR_UUID = UUID.fromString("AEDBD263-E6EC-467D-8461-746329DE6754");
	private BTManager mBTManager;

	private static final String sTranslationFormat = "Translation: %f, %f, %f";
	private static final String sRotationFormat = "Rotation: %f, %f, %f, %f";

	private TextView mTranslationTextView;
	private TextView mRotationTextView;

	private Tango mTango;
	private TangoConfig mConfig;
	private boolean mIsTangoServiceConnected;
	private boolean mIsProcessing = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mTranslationTextView = (TextView) findViewById(R.id.translation_text_view);
		mRotationTextView = (TextView) findViewById(R.id.rotation_text_view);

		// Instantiate Tango client
		mTango = new Tango(this);

		// Set up Tango configuration for motion tracking
		// If you want to use other APIs, add more appropriate to the config
		// like: mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true)
		mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
		mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);


		//Log.d(TAG, "UUID: " +UUID.randomUUID().toString().toUpperCase());
		mBTManager = new BTManager(this, FVR_UUID, false, true);
		mBTManager.Enable();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Lock the Tango configuration and reconnect to the service each time
		// the app
		// is brought to the foreground.
		super.onResume();
		if (!mIsTangoServiceConnected) {
			startActivityForResult(Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING), Tango.TANGO_INTENT_ACTIVITYCODE);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		int BTresult = mBTManager.onActivityResult(requestCode, resultCode, data);

		// Check which request we're responding to
		if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
			// Make sure the request was successful
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "This app requires Motion Tracking permission!", Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			try {
				setTangoListeners();
			} catch (TangoErrorException e) {
				Toast.makeText(this, "Tango Error! Restart the app!", Toast.LENGTH_SHORT).show();
			}
			try {
				mTango.connect(mConfig);
				mIsTangoServiceConnected = true;
			} catch (TangoOutOfDateException e) {
				Toast.makeText(getApplicationContext(), "Tango Service out of date!", Toast.LENGTH_SHORT).show();
			} catch (TangoErrorException e) {
				Toast.makeText(getApplicationContext(), "Tango Error! Restart the app!", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// When the app is pushed to the background, unlock the Tango
		// configuration and disconnect
		// from the service so that other apps will behave properly.
		try {
			mTango.disconnect();
			mIsTangoServiceConnected = false;
		} catch (TangoErrorException e) {
			Toast.makeText(getApplicationContext(), "Tango Error!", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mBTManager.Close();
	}

	private void setTangoListeners() {
		// Select coordinate frame pairs
		ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
		framePairs.add(new TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE, TangoPoseData.COORDINATE_FRAME_DEVICE));

		// Add a listener for Tango pose data
		mTango.connectListener(framePairs, new OnTangoUpdateListener() {

			@SuppressLint("DefaultLocale")
			@Override
			public void onPoseAvailable(TangoPoseData pose) {
				if (mBTManager.socketReady) {
					//pose.translation[0]
					byte[] bytes = new byte[12];
					ByteBuffer poseB = ByteBuffer.wrap(bytes);
					poseB.putFloat(0, (float)pose.translation[0]);
					poseB.putFloat(4, (float)pose.translation[1]);
					poseB.putFloat(8, (float)pose.translation[2]);

					try {
//						mBTManager.mmOutStream.write(BTManager.toByteArray(pose.translation[0]));
//						mBTManager.mmOutStream.write(BTManager.toByteArray(pose.translation[1]));
//						mBTManager.mmOutStream.write(BTManager.toByteArray(pose.translation[2]));
						mBTManager.mmOutStream.write(bytes);
						mBTManager.mmOutStream.flush();
						//Log.d("BluetoothDataManager", "Pose Written to socket");
					} catch (IOException e) {
						Log.e(TAG, "EXCEPTION " + e.getMessage());
					}
				}

//				if (mIsProcessing) {
//					Log.i(TAG, "Processing UI");
//					return;
//				}
//				mIsProcessing = true;
//
//				// Format Translation and Rotation data
//				final String translationMsg = String.format(sTranslationFormat, pose.translation[0], pose.translation[1], pose.translation[2]);
//				final String rotationMsg = String.format(sRotationFormat, pose.rotation[0], pose.rotation[1], pose.rotation[2], pose.rotation[3]);
//
//				// Output to LogCat
//				String logMsg = translationMsg + " | " + rotationMsg;
//				Log.i(TAG, logMsg);
//
//				// Display data in TextViews. This must be done inside a runOnUiThread call because
//				// it affects the UI, which will cause an error if performed from the Tango service thread
//				runOnUiThread(new Runnable() {
//					@Override
//					public void run() {
//						mTranslationTextView.setText(translationMsg);
//						mRotationTextView.setText(rotationMsg);
//						mIsProcessing = false;
//					}
//				});
			}

			@Override
			public void onXyzIjAvailable(TangoXyzIjData arg0) {
				// Ignoring XyzIj data
			}

			@Override
			public void onTangoEvent(TangoEvent arg0) {
				// Ignoring TangoEvents
			}

			@Override
			public void onFrameAvailable(int arg0) {
				// Ignoring onFrameAvailable Events

			}

		});
	}

}
