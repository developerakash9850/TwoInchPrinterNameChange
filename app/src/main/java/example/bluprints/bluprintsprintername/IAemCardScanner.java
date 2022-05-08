package example.bluprints.bluprintsprintername;

import example.bluprints.bluprintsprintername.CardReader.CARD_TRACK;

public interface IAemCardScanner {

	public void onScanMSR(String buffer, CARD_TRACK cardtrack);

	public void onScanDLCard(String buffer);
	
	public void onScanRCCard(String buffer);
	
	public void onScanRFD(String buffer);
	
	public void onScanPacket(String buffer);
}
