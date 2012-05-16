package com.abnote.nfc;

import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Random;

import android.util.Log;

import com.abnote.nfc.Card.StatusWordException;
import com.abnote.nfc.util.HexString;
import com.abnote.nfc.util.Tlv;
import com.abnote.nfc.util.TlvException;

public class Terminal {
	public static final String TAG = "Terminal";
	public static final String READ_DATA = "00B2020C00";
	
	private Hashtable<String, String> terminalSettings = new Hashtable<String, String>();
	private static Hashtable<String, String> tagNameMap = new Hashtable<String, String>();
	private byte[] selectPpse;
	private byte[] paymentAid;
	private byte[] selectPaymentApp;
	private byte[] gpoResponse;
	private byte[] _0101;
	private byte[] pdolData = new byte[0];
	private EmvCard card;
	
	private Random rand = new Random();
	
	static {
		tagNameMap.put("6F", "Template");
		tagNameMap.put("84", "DF");
		tagNameMap.put("A5", "FCI Proprietary Template");
		tagNameMap.put("BF0C", "FCI Issuer Discretionary Data");
		tagNameMap.put("61", "Application Template");
		tagNameMap.put("4F", "AID - card");
		tagNameMap.put("50", "Application Label");
		tagNameMap.put("87", "Application Priority Indicator");
		tagNameMap.put("9F12", "Application Preferred Name");
		tagNameMap.put("9F11", "Issuer Code Table Index");
		tagNameMap.put("5F2D", "Language Preference");
		tagNameMap.put("9F38", "PDOL");
		tagNameMap.put("77", "Response Message Template 2");
		tagNameMap.put("9F10", "Issuer Application Data");
		tagNameMap.put("5F20", "Cardholder Name");
		tagNameMap.put("57", "Track 2 Equivalent Data");
		tagNameMap.put("5F34", "PAN Sequence Number");
		tagNameMap.put("82", "Application Interchange Profile");
		tagNameMap.put("9F36", "Application Transaction Counter");
		tagNameMap.put("9F26", "Application Cryptogram");
		tagNameMap.put("70", "Read Record Template");
		tagNameMap.put("9F1F", "Track 1 Discretionary Data");
	}

	public Terminal(Card card) {
		byte[] r = new byte[4];
		rand.nextBytes(r);
		Date today = new Date();
		Format f = new SimpleDateFormat("yyMMdd");
		
		this.card = new EmvCard(card);
		terminalSettings.put("9F66", "D6400000");			// Visa Terminal Transaction Qualifiers
		terminalSettings.put("9F02", "000000000010");		// Amt Authorised
		terminalSettings.put("9F37", HexString.hexify(r));	// Terminal Random
		terminalSettings.put("5F2A", "0036");				// Country Code AU
		terminalSettings.put("9A", f.format(today));		// Transaction Date YYMMDD
	}
	
	public Terminal(Card card, Hashtable<String, String> terminalSettings) {		
		this.card = new EmvCard(card);
		this.terminalSettings = terminalSettings;
	}
	
	public static Hashtable<String, String> getTagNameMap() {
		return tagNameMap;
	}
	
	public byte[] getSelectPpseResponse() {
		return selectPpse;
	}
	
	public byte[] getSelectPaymentAppResponse() {
		return selectPaymentApp;
	}
	
	public byte[] getGpoResponse() {
		return gpoResponse;
	}
	
	public byte[] get0101() {
		return _0101;
	}
	
	public String getPAN() {
		if (_0101 == null) {
			return "";
		} else {
			String search = "70|57";
			String pan;
			try {
				Tlv tlv = Tlv.searchTlv(_0101, search);
				pan = HexString.hexify(tlv.getValue());
				pan = pan.substring(0, pan.indexOf("D"));
			} catch (TlvException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "";
			}
			return pan;
		}
	}
	
	public Card getCard() {
		return card;
	}
	
	public void setCard(Card card) {
		this.card = new EmvCard(card);
	}
	
	public boolean connect() {
		return card.connect();
	}
	
	public boolean disconnect() {
		return card.close();
	}
	
	public void selectPpse() throws TlvException, StatusWordException, IOException {
		selectPpse = card.selectPpse();
		String search = "6F|A5|BF0C|61|4F";
		
		Tlv tlv = Tlv.searchTlv(selectPpse, search);
		if (tlv != null) {
			paymentAid = tlv.getValue();
		} else { paymentAid = null; }
	}
	
	public void selectPaymentApp() throws StatusWordException, IOException, TlvException {
		selectPaymentApp = card.select(paymentAid);
		
		// prepare PDOL data
		String search = "6F|A5|9F38";
		String pdolString = "";
		
		Tlv tlv = Tlv.searchTlv(selectPaymentApp, search);
		if (tlv != null) {
			ArrayList<String> list = Tlv.getTagLengthMap(tlv.getValue());
			Log.d(TAG, "List size is: " + list.size());
			for (int i = 0; i < list.size(); i++) {
				Log.d(TAG, list.get(i));
				if (terminalSettings.containsKey(list.get(i))) {
					pdolString += terminalSettings.get(list.get(i));
				} else {
					Log.d(TAG, "Terminal data does not contain " + list.get(i));
				}
			}
			Log.d(TAG, "Terminal data is: " + pdolString);
			pdolData = HexString.parseHexString(pdolString);
		}
	}
	
	public void getProcessingOptions() throws TlvException, StatusWordException, IOException {
		gpoResponse = card.getProcessingOptions(pdolData);
	}
	
	public void getMSD() throws StatusWordException, IOException {
		_0101 = card.send(READ_DATA);
	}
	
	public void readRecord(int sfi, int record) {
		
	}
	
	public void skimCard() throws StatusWordException, IOException, TlvException {
		selectPpse();
		selectPaymentApp();
		getProcessingOptions();
		_0101 = card.send(READ_DATA);
	}
}
