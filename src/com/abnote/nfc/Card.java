package com.abnote.nfc;

import java.io.IOException;

import com.abnote.nfc.util.HexString;

import android.nfc.tech.IsoDep;
import android.util.Log;

public class Card {
	protected static final String TAG = "Card";
	protected IsoDep isoDep;
	protected boolean checkSw = true;
	
	public Card(android.nfc.Tag tag) {
		isoDep = IsoDep.get(tag);
	}
	
	public Card(Card card) {
		this.isoDep = card.isoDep;
	}
	
	public boolean isCheckSw() {
		return checkSw;
	}
	
	public void setCheckSw(boolean checkSw) {
		this.checkSw = checkSw;
	}
	
	public boolean connect() {
		try {
			Log.d(TAG,"Connect");
			isoDep.connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean close() {
		try {
			Log.d(TAG, "Close");
			isoDep.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public class StatusWordException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public StatusWordException(String error) {
			super(error);
		}
	}
	
	public byte[] send(String hexstring) throws StatusWordException, IOException {
		return sendAndCheckSw(HexString.parseHexString(hexstring));
	}
	
	public byte[] send(byte[] apdu) throws StatusWordException, IOException {
		return sendAndCheckSw(apdu);
	}
	
	private byte[] sendAndCheckSw(byte[] apdu) throws StatusWordException, IOException, IllegalStateException {
		Log.d(TAG, "Tx: " + HexString.hexify(apdu));
		byte[] resp = isoDep.transceive(apdu);
		Log.d(TAG, "Rx: " + HexString.hexify(resp));
		if (checkSw) {
			if (resp[resp.length - 2] == (byte)0x90 || resp[resp.length - 2] == 0x61) {
				return resp;
			} else {
				throw new StatusWordException("Status word error : " + HexString.hexify(resp));
			}
		} else {
			return resp;
		}
	}
}
