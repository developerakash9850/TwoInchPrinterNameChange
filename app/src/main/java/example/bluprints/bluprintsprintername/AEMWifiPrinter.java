package example.bluprints.bluprintsprintername;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import java.io.IOException;
import example.bluprints.bluprintsprintername.PrintRasterImage.dither;

public class AEMWifiPrinter
{

		    public AEMWifiPrinter() {
		        
		    }
		   
	private final byte BARCODE_TYPE_UPCA 	= 	0x41;//'A'
	private final byte BARCODE_TYPE_EAN13 	= 	0x43;//'C'
	private final byte BARCODE_TYPE_EAN8 	= 	0X44;//'D'
	private final byte BARCODE_TYPE_CODE39 	= 	0X45;//'E'
	
	public enum BARCODE_TYPE {UPCA,EAN13,EAN8,CODE39};
	int effectivePrintWidth = 48;
	
	public enum BARCODE_HEIGHT {DOUBLEDENSITY_FULLHEIGHT,TRIPLEDENSITY_FULLHEIGHT,DOUBLEDENSITY_HALFHEIGHT, TRIPLEDENSITY_HALFHEIGHT};
	
	// printing barcode
	public byte[] printBarcode(String barcodeData, BARCODE_TYPE barcodetype, BARCODE_HEIGHT barcodeheight) throws IOException
	{
		byte[] barcodepacket = createBarcodePacket(barcodeData.getBytes(),barcodetype,barcodeheight);
		if(barcodepacket == null)
        	return null;
        return barcodepacket;
	}

			
	private byte[] createBarcodePacket(byte[] barcodeBytes, BARCODE_TYPE barcodetype, BARCODE_HEIGHT height)
	{
		if(barcodetype == BARCODE_TYPE.CODE39)
		{
			byte[] barcodePacket = new byte[barcodeBytes.length + 6];
			barcodePacket[0] = 0x1D;
			barcodePacket[1] = 0x6B;
			barcodePacket[2] = BARCODE_TYPE_CODE39; // barcode type
			barcodePacket[3] = (byte) (barcodeBytes.length + 2); //length of barcode data
			//barcodePacket[4] = getBarcodeHeight(height);
			barcodePacket[4] = 0x2A; //'*' character is reqd for code39
			int i = 0;
		    for(i = 0; i < barcodeBytes.length; i++)
		    	barcodePacket[i + 5] = barcodeBytes[i];
		     barcodePacket[i + 5] = 0x2A;
			return barcodePacket;
		}
		else if(barcodetype == BARCODE_TYPE.UPCA)
		{
			byte[] barcodePacket = new byte[barcodeBytes.length + 5];
			barcodePacket[0] = 0x1D;
			barcodePacket[1] = 0x6B;
			barcodePacket[2] = BARCODE_TYPE_UPCA; // barcode type
			barcodePacket[3] = (byte) (barcodeBytes.length); //length of barcode data
			//barcodePacket[4] = getBarcodeHeight(height);
			for(int i = 0; i < barcodeBytes.length; i++)
				barcodePacket[i + 4] = barcodeBytes[i];
		    return barcodePacket;
		}
		else if(barcodetype == BARCODE_TYPE.EAN13)
		{
			byte[] barcodePacket = new byte[barcodeBytes.length + 5];
			barcodePacket[0] = 0x1D;
			barcodePacket[1] = 0x6B;
			barcodePacket[2] = BARCODE_TYPE_EAN13; // barcode type
			barcodePacket[3] = (byte) (barcodeBytes.length); //length of barcode data
			//barcodePacket[4] = getBarcodeHeight(height);
			for(int i = 0; i < barcodeBytes.length; i++)
				barcodePacket[i + 4] = barcodeBytes[i];
		    return barcodePacket;	     
		}
		else if(barcodetype == BARCODE_TYPE.EAN8)
		{
			byte[] barcodePacket = new byte[barcodeBytes.length + 5];
			barcodePacket[0] = 0x1D;
			barcodePacket[1] = 0x6B;
			barcodePacket[2] = BARCODE_TYPE_EAN8; // barcode type
			barcodePacket[3] = (byte) (barcodeBytes.length); //length of barcode data
			//barcodePacket[4] = getBarcodeHeight(height);
			for(int i = 0; i < barcodeBytes.length; i++)
				barcodePacket[i + 4] = barcodeBytes[i];
		    return barcodePacket;
		}
		return null;
	}

	
	/*private byte getBarcodeHeight(BARCODE_HEIGHT height)
	{
		byte mode = 0x61;
		
		if(height == BARCODE_HEIGHT.DOUBLEDENSITY_FULLHEIGHT)
		{
			mode = 0x61;
		}else if(height == BARCODE_HEIGHT.TRIPLEDENSITY_FULLHEIGHT)
		{
			mode = 0x62;
		}else if(height == BARCODE_HEIGHT.DOUBLEDENSITY_HALFHEIGHT)
		{
			mode = 0x63;
		}else if(height == BARCODE_HEIGHT.TRIPLEDENSITY_HALFHEIGHT)
		{
			mode = 0x64;
		}
		
		return mode;
	}*/
	
	Context m_Context;
	public static final byte IMAGE_LEFT_ALIGNMENT = 0x6C;
	public static final byte IMAGE_CENTER_ALIGNMENT = 0x63;
	public static final byte IMAGE_RIGHT_ALIGNMENT = 0x72;
	
	public byte[] printBitImage(Bitmap originalBitmap, Context context, byte image_alignment) throws IOException
	{
		m_Context = context;
		ImageHandler imageHandler = new ImageHandler(context);
		byte[] imagePacket = imageHandler.getMonoChromeImagePacket(originalBitmap, image_alignment);
		if(imagePacket == null)
			return null;
		//bluetoothSocket.getOutputStream().write(imagePacket, 0, imagePacket.length);
		
        if(deleteFile())
		{
			System.out.print("b");
		}
		else
		{
			System.out.print("b");
		}
        return imagePacket;
	}
	
	private boolean deleteFile()
	{
		return m_Context.deleteFile("my_monochrome_image.bmp");
		
	}
	
	public Bitmap getResizedBitmap(Bitmap bm)
	{
		int newWidth = 248;
		int newHeight = 297;
		int reqWidth = (int) Math.round(effectivePrintWidth*8);
		int width = bm.getWidth();
		int height = bm.getHeight();
		if(width==reqWidth){
			return bm;
		}
		else if(width<reqWidth&&width>16){
			int diff = width%8;
			if(diff!=0){
				newWidth = width - diff;
				newHeight = (int) (width - diff)*height/width;
				float scaleWidth = ((float) newWidth) / width;
				float scaleHeight = ((float) newHeight) / height;
				// CREATE A MATRIX FOR THE MANIPULATION
				Matrix matrix = new Matrix();
				// RESIZE THE BIT MAP
				matrix.postScale(scaleWidth, scaleHeight);

				// "RECREATE" THE NEW BITMAP
				Bitmap resizedBitmap = Bitmap.createBitmap(
						bm, 0, 0, width, height, matrix, false);
				bm.recycle();
				return resizedBitmap;
			}
		}
		else if(width>16){
			newWidth = reqWidth;
			newHeight = (int) reqWidth*height/width;
			float scaleWidth = ((float) newWidth) / width;
			float scaleHeight = ((float) newHeight) / height;
			// CREATE A MATRIX FOR THE MANIPULATION
			Matrix matrix = new Matrix();
			// RESIZE THE BIT MAP
			matrix.postScale(scaleWidth, scaleHeight);

			// "RECREATE" THE NEW BITMAP
			Bitmap resizedBitmap = Bitmap.createBitmap(
					bm, 0, 0, width, height, matrix, false);
			bm.recycle();
			return resizedBitmap;
		}
		return bm;
	}

	public byte[] printImage(Bitmap originalBitmap)
	{
		PrintRasterImage PrintRasterImage = new PrintRasterImage(getResizedBitmap(originalBitmap));
		PrintRasterImage.PrepareImage(dither.floyd_steinberg, 128);
		byte[] imgStr =PrintRasterImage.getPrintImageData();
		return imgStr;
	}
	
	public byte[] printTextAsImage(String TextToConvert)
	{
		
		Converter convert = new Converter();
		Bitmap bmp = convert.textAsBitmap(TextToConvert, 30, 5, Color.BLACK, Typeface.MONOSPACE);
		PrintRasterImage PrintRasterImage = new PrintRasterImage(getResizedBitmap(bmp));
		PrintRasterImage.PrepareImage(dither.floyd_steinberg, 128);
		byte[] imgStr =PrintRasterImage.getPrintImageData();
		return imgStr;
		
	}
	
}
