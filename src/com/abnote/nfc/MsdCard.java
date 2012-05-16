package com.abnote.nfc;

import java.io.IOException;
import java.io.Serializable;
import java.util.Hashtable;

import android.nfc.tech.IsoDep;

import com.abnote.nfc.util.HexString;
import com.abnote.nfc.util.Tlv;
import com.abnote.nfc.util.TlvException;

public class MsdCard implements Serializable {
	private static final String TAG = "MsdCard";
	private static final long serialVersionUID = 6514994226419324018L;
	public static final String SELECT_PPSE = "";
	public static final String SELECT_VSDC = "00A4040007A0000000031010";
	public static final String SELECT_MCHIP = "00A4040007A000000004101000";
	public static final String GET_PROCESSING_OPTIONS = "80A8000002830000";
	public static final String READ_0101 = "00B2010C00";
	public byte[] selectResponse;
	public byte[] gpoResponse;
	public byte[] data0101;
	public boolean success = false;
	private static Hashtable<String,String> ht;
	private IsoDep isoDep;
	
	public MsdCard(android.nfc.Tag tag) {
		ht = new Hashtable<String, String>();
		
		ht.put("6F", "FCI Template");
		ht.put("0x84", "AID");
		ht.put("0xA5", "FCI Data");
		ht.put("50", "Application Name");
		ht.put("77", "GPO Response");
		ht.put("82", "AIP");
		ht.put("94", "AFL");
		ht.put("70", "Record content");
		ht.put("9F6C", "9F6C");
		ht.put("9F62", "Track 1 Bitmap ()");
		ht.put("9F63", "Track 1 Bitmap");
		ht.put("9F64", "9F64");
		ht.put("9F65", "Track 2 Bitmap ()");
		ht.put("9F66", "Track 2 Bitmap");
		ht.put("56", "Track 1 Data");
		ht.put("9F6B", "Track 2 Data");
		ht.put("9F67", "9F67");
		ht.put("9F68", "9F68");
		
		isoDep = IsoDep.get(tag);
	}
	
	public boolean connect() {
		try {
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
			isoDep.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean readMsdCardData() {
		try {
			selectResponse = isoDep.transceive(HexString.parseHexString(MsdCard.SELECT_MCHIP));
			gpoResponse = isoDep.transceive(HexString.parseHexString(MsdCard.GET_PROCESSING_OPTIONS));
			data0101 = isoDep.transceive(HexString.parseHexString(MsdCard.READ_0101));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public String getSelectResponse() {
		try {
			Tlv tlv = Tlv.parseTlv(selectResponse);
			return tlv.dump(ht);
		} catch (TlvException e) {
			return "";
		}
	}
	
	public String getGpoResponse() {
		try {
			Tlv tlv = Tlv.parseTlv(gpoResponse);
			return tlv.dump(ht);
		} catch (TlvException e) {
			return "";
		}
	}
	
	public String getData0101() {
		try {
			Tlv tlv = Tlv.parseTlv(data0101);
			return tlv.dump(ht);
		} catch (TlvException e) {
			return "";
		}
	}
	
	public String toString() {
		return getSelectResponse() + "\n" + getGpoResponse() + "\n" + getData0101();
	}
}
