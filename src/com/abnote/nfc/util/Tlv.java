package com.abnote.nfc.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.Hashtable;

public class Tlv {
	private static final String TAG = "Tlv";
	private byte[] mBuffer;
	private byte[] mTag;
	private int mLength;
	private byte[] mLengthValue;
	private byte[] mValue;
	private Hashtable<String, String> tagNameMap = null;

	public void setTagNameMap(Hashtable<String, String> map) {
		tagNameMap = map;
	}

	public byte[] getBuffer() {
		return mBuffer;
	}

	public byte[] getTag() {
		return mTag;
	}

	public int length() {
		return mLength;
	}

	public byte[] getValue() {
		return mValue;
	}

	public byte[] getLengthValue() {
		if (mLengthValue == null) {
			mLengthValue = new byte[mBuffer.length - mTag.length];
			System.arraycopy(mBuffer, mTag.length, mLengthValue, 0, mBuffer.length - mTag.length);
		}
		return mLengthValue;
	}

	public int getTLLength() {
		return mBuffer.length - mValue.length;
	}

	private Tlv() {
	}

	private static byte[] concat(byte[] a, byte[] b) {
		byte[] temp = new byte[a.length + b.length];
		System.arraycopy(a, 0, temp, 0, a.length);
		System.arraycopy(b, 0, temp, a.length, b.length);
		return temp;
	}

	public static Tlv createTlv(byte[] tag, byte[] value) throws TlvException {
		byte[] length;
		byte[] tlvBuffer;

		if (value.length < 0x7F) {
			length = new byte[1];
			length[0] = (byte) value.length;
		} else if (value.length < 0x7F00) {
			byte[] temp = HexString.parseHexString(HexString.hexifyShort((short) value.length));
			length = Tlv.concat(new byte[] { (byte) 0x82 }, temp);
		} else {
			throw new TlvException("TLV with length greater than 0x7F00");
		}

		tlvBuffer = Tlv.concat(Tlv.concat(tag, length), value);

		Tlv tlv = new Tlv();
		tlv.mBuffer = tlvBuffer;
		tlv.mTag = tag;
		tlv.mLength = value.length;
		tlv.mValue = value;

		return tlv;
	}

	public String dump(Hashtable<String, String> tagNameMap) {
		return dump(this.getBuffer(), 0, 0, tagNameMap);
	}

	public String dump() {
		return dump(this.getBuffer(), 0, 0, tagNameMap);
	}

	private static String dump(byte[] buffer, int offset, int depth, Hashtable<String, String> tagNameMap) {
		Tlv parent;
		String out = "";
		String tabs = "";

		try {
			parent = Tlv.parseTlv(buffer, offset);

			for (int i = 0; i < depth; i++) {
				tabs += "\t";
			}
			if (tagNameMap != null && tagNameMap.containsKey(HexString.hexify(parent.getTag()))) {
				out = "\n" + tabs + "TAG: " + tagNameMap.get(HexString.hexify(parent.getTag())) + " {";
			} else {
				out = "\n" + tabs + "TAG: " + HexString.hexify(parent.getTag()) + " {";
			}
			out += "\n" + tabs + "\tLENGTH: " + HexString.hexify(parent.getValue().length);
			out += "\n" + tabs + "\tVALUE: " + HexString.dump(parent.getValue());

			// get child
			out += dump(buffer, parent.getTLLength() + offset, depth + 1, tagNameMap);
			// get sibling
			while (buffer.length - parent.getBuffer().length > 0) {
				out += dump(buffer, parent.getBuffer().length + offset, depth + 1, tagNameMap);
			}
			out += "\n" + tabs + "}";
		} catch (TlvException e) {
			// the value does not contain
		}
		return out;
	}

	public static Tlv parseTlv(byte[] buff) throws TlvException {
		return parseTlv(buff, 0);
	}

	public static Tlv parseTlv(byte[] buff, int offset) throws TlvException {
		// parse tlv and push on to stack while working
		// convert stack to array
		// remove stack array length from buff length
		int lengthTag = 0;
		int lengthLength = 0;
		int lengthData = 0;
		int count = 0;

		// try {
		// Parse the tag
		if (buff.length < 2) {
			throw new TlvException("Buffer must contain at least 2 bytes to be TLV");
		}

		if ((buff[offset] & 0x1F) == 0x1F) {
			for (int i = 1;; i++) {
				if ((buff[offset + i] & 0x80) != 0x80) {
					lengthTag = i + 1;
					break;
				}
				if (i > 4) {
					throw new TlvException("TLV Parsing - maximum tag length of 4");
				}
			}
		} else {
			lengthTag = 1;
		}

		if (buff.length <= lengthTag) {
			throw new TlvException("Invalid TLV data");
		}

		// Parse length
		byte[] len = new byte[4];
		if ((buff[offset + lengthTag] & 0x80) == 0x80) { // high-bit set - multi length
			if (buff.length - offset < lengthTag + 2) {
				throw new TlvException("Buffer does not contain enough bytes");
			}
			lengthLength = (buff[offset + lengthTag] & 0x7F) + 1;
			Log.d(TAG, "Length length is :" + lengthLength);
			count = (buff[offset + lengthTag] & 0x7F);
			if (count == 0x00) {
				throw new TlvException("Dynamic length TLV can not be parsed with this tool");
			}
			for (int i = count, j = 0; i > 0; i--, j++) {
				if (j > 3 || lengthTag + i > buff.length - offset) {
					throw new TlvException("Error in length of length parsing");
				}
				len[j] = buff[offset + lengthTag + j];
			}
			lengthData = Integer.parseInt(HexString.hexify(len), 16);
			// lengthData = System.BitConverter.ToInt32(len, 0);
			Log.d(TAG, "Length Data : " + lengthData);
		} else {
			lengthLength = 1;
			lengthData = buff[offset + lengthTag];
		}

		// check that length of expected data 'L' is not greater than data provided in buffer
		if (lengthTag + lengthLength + lengthData > buff.length - offset) {
			throw new TlvException("Invalid TLV byte array");
		}

		// check that length is not equal to zero
		// if (lengthData == 0) {
		// throw new TlvException("TLV value should not be zero length");
		// }

		// copy into internal array and class attribs

		Tlv tlv = new Tlv();
		tlv.mBuffer = new byte[lengthTag + lengthLength + lengthData];
		// Array.Copy(buff, tlv.Buffer, lengthTag + lengthLength + lengthData);
		System.arraycopy(buff, offset, tlv.mBuffer, 0, lengthTag + lengthLength + lengthData);
		tlv.mTag = new byte[lengthTag];
		// Array.Copy(buff, 0, tlv.Tag, 0, lengthTag);
		System.arraycopy(buff, offset, tlv.mTag, 0, lengthTag);
		tlv.mLength = lengthData;
		tlv.mValue = new byte[lengthData];
		// Array.Copy(buff, lengthTag + lengthLength, tlv.Value,0, lengthData);
		System.arraycopy(buff, offset + lengthTag + lengthLength, tlv.mValue, 0, lengthData);

		// remove from incoming buffer
		byte[] temp = new byte[buff.length - (lengthTag + lengthLength + lengthData)];
		// Array.Copy(buff, lengthTag + lengthLength + lengthData, temp, 0, buff.Length - (lengthTag + lengthLength + lengthData));
		System.arraycopy(buff, lengthTag + lengthLength + lengthData, temp, 0, buff.length - (lengthTag + lengthLength + lengthData));

		return tlv;

		// } catch (Exception e) {
		// throw new TlvException(e.getMessage());
		// }

	}

	public static Tlv searchTlv(byte[] tlvData, String search) throws TlvException {
		return searchTlv(tlvData, 0, search);
	}

	public static Tlv searchTlv(byte[] tlvData, int offset, String search) throws TlvException {
		String[] searchPath = search.toUpperCase().split("\\|");
		Tlv tlv = Tlv.parseTlv(tlvData, offset);

		if (searchPath[0].equals(HexString.hexify(tlv.getTag()))) {
			// first tag matches so continue search downwards
			if (searchPath.length > 1) {
				// make new TLV from the value
				String newSearch = search.substring(search.indexOf('|') + 1);
				return searchTlv(tlvData, tlv.getTLLength() + offset, newSearch);
			} else {
				Log.d(TAG, "Search completed successfully");
				// this was the last tag to match so we have found the value requested
				return tlv;
			}
		} else {
			// search across the remaining data to find any other TLV's
			if (tlvData.length - (tlv.getBuffer().length + offset) > 1) {
				return searchTlv(tlvData, tlv.getBuffer().length + offset, search);
			} else {
				// no data remaining so quit - unsuccessful search
				return null;
			}
		}
	}

	public static ArrayList<String> getTagLengthMap(byte[] buff) throws TlvException {
		// parse tlv and push on to stack while working
		// convert stack to array
		// remove stack array length from buff length
		int lengthTag = 0;
		int lengthLength = 0;
		int cursor = 0;
		ArrayList<String> list = new ArrayList<String>();

		// Parse the tag
		if (buff.length < 2) {
			throw new TlvException("Buffer must contain at least 2 bytes to be Tag Length Map");
		}

		Log.d(TAG, "Getting TagLength Map " + HexString.hexify(buff));

		while (cursor < buff.length) {
			if ((buff[cursor] & 0x1F) == 0x1F) {
				for (int i = 1;; i++) {
					if ((buff[cursor + i] & 0x80) != 0x80) {
						lengthTag = i + 1;
						break;
					}
					if (i > 4) {
						throw new TlvException("Tag Length Map Parsing - maximum tag length of 4");
					}
				}
			} else {
				lengthTag = 1;
			}

			// Parse length
			if ((buff[cursor + lengthTag] & 0x80) == 0x80) { // high-bit set - multi length
				lengthLength = (buff[cursor + lengthTag] & 0x7F) + 1;
			} else {
				lengthLength = 1;
			}

			list.add(HexString.hexify(buff, cursor, lengthTag));
			cursor += lengthTag + lengthLength;
		}
		Log.d(TAG, "Finished parse. List contains: " + list.size() + " elements");
		return list;
	}

	@Override
	public String toString() {
		return HexString.hexify(mTag) + "\n\t" + HexString.hexify(mLength) + "\n\t\t" + HexString.hexify(mValue);
	}
}
