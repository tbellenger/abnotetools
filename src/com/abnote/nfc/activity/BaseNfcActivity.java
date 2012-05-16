package com.abnote.nfc.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;

public abstract class BaseNfcActivity extends Activity {
	protected static final String TAG = "BaseNfcActivity";
	protected static final String STATE_TAG = "com.abnote.nfc.activity.BaseNfcActivity.TAG";
	private NfcAdapter mAdapter;
	private PendingIntent mPendingIntent;
	private String[][] mTechLists;
	private IntentFilter[] mFilters;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mAdapter = NfcAdapter.getDefaultAdapter(this);

		// Create a generic PendingIntent that will be deliver to this activity.
		// The NFC stack will fill in the intent with the details of the discovered tag before
		// delivering to this activity.
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// Setup an intent filter for all MIME based dispatches
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		try {
			ndef.addDataType("*/*");
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("fail", e);
		}
		mFilters = new IntentFilter[] { ndef, };

		// Setup a tech list for all NfcF tags
		mTechLists = new String[][] { new String[] { IsoDep.class.getName() } };
	}
	
	public void setFilters(IntentFilter[] filter) {
		mFilters = filter;
	}
	
	public void setTechLists(String[][] techLists) {
		mTechLists = techLists;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists);
	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.i(TAG, "Discovered tag with intent: " + intent);

		String action = intent.getAction();

		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			// check we have card then start transaction
			if (tag != null) {
				Log.i(TAG, "Tag in extras. Calling onNfcDiscovered");
				onNfcDiscovered(tag);
			}
		}
		
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mAdapter != null) mAdapter.disableForegroundDispatch(this);
	}
	
	abstract public void onNfcDiscovered(Tag tag);
}
