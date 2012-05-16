package com.abnote.nfc;

import java.io.IOException;

import com.abnote.nfc.util.HexString;
import com.abnote.nfc.util.Tlv;
import com.abnote.nfc.util.TlvException;

public class EmvCard extends Card {
	public static final String PPSE = "325041592E5359532E4444463031";
	public static final String SELECT = "00A40400";
	public static final String GET_PROCESSING_OPTIONS = "80A80000";
	public static final String READ_DATA = "00B2010C00";
	public byte[] selectPpse;
	public byte[] selectResponse;
	public byte[] gpoResponse;
	public byte[] data;
	public String paymentAid;
	public boolean success = false;
	
	public EmvCard(android.nfc.Tag tag) {
		super(tag);
	}
	
	public EmvCard(Card card) {
		super(card);
	}
	
	public byte[] select(String aid) throws StatusWordException, IOException {
		return select(HexString.parseHexString(aid));
	}
	
	public byte[] select(byte[] aid) throws StatusWordException, IOException {
		return send(SELECT + HexString.hexify(aid.length) + HexString.hexify(aid) + "00");
	}
	
	public byte[] selectPpse() throws StatusWordException, IOException {
		return select(EmvCard.PPSE);
	}
	
	public byte[] getProcessingOptions(byte[] pdol) throws TlvException, StatusWordException, IOException {
		Tlv tlv = Tlv.createTlv(new byte[] {(byte)0x83}, pdol);
		return send(EmvCard.GET_PROCESSING_OPTIONS + HexString.hexify(tlv.getBuffer().length) + HexString.hexify(tlv.getBuffer()) + "00");
	}
}
