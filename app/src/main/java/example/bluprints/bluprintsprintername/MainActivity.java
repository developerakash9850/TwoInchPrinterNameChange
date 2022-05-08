package example.bluprints.bluprintsprintername;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

   public class MainActivity extends AppCompatActivity implements IAemCardScanner, IAemScrybe {
    int effectivePrintWidth = 48;
    AEMScrybeDevice m_AemScrybeDevice;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    Bitmap imageBitmap;
    CardReader m_cardReader = null;
    AEMPrinter m_AemPrinter = null;
    ArrayList<String> printerList;
    String creditData;
    ProgressDialog m_WaitDialogue;
    CardReader.CARD_TRACK cardTrackType;
    int glbPrinterWidth;
    EditText editText,rfText;
    private PrintWriter printOut;
    private Socket socketConnection;
    private String txtIP="";
    Spinner spinner;
    String encoding = "US-ASCII";
    EditText edtName,edtPin;
    String data;
    private String strShow = "";
    Bitmap mBitmap;

    char[] paperCommand=new char[]{0x1B,0x7E,0x42,0x50,0x7C,0x47,0x45,0x54,0x7C,0x50,0x41,0x5F,0x53,0x54,0x53,0x5E};
    char[] labelFullCut=new char[] {0x1D,0x56,0x00};
    char[] labelHalfCut=new char[] {0x1D,0x56,0x01};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        printerList = new ArrayList<String>();
        creditData = new String();
        edtName=findViewById(R.id.edtName);
        edtPin=findViewById(R.id.edtPin);
        spinner=(Spinner)findViewById(R.id.spinner);

        m_AemScrybeDevice = new AEMScrybeDevice(this);
        Button discoverButton = (Button) findViewById(R.id.pairing);
        registerForContextMenu(discoverButton);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.printer_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view,int position, long id) {
                if(position==1)
                {
                    glbPrinterWidth=48;
                    onSetPrinterType(view);
                }
                else{
                    glbPrinterWidth=32;
                    onSetPrinterType(view);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }
        });

        m_AemScrybeDevice = new AEMScrybeDevice(this);
        Button pairing = (Button) findViewById(R.id.pairing);
        registerForContextMenu(pairing);

    }

    public void onSetPrinterType(View v)
    {
        if(glbPrinterWidth == 32)
        {
            glbPrinterWidth = 32;
            showAlert("32 Characters / Line or 2 Inch (58mm) Printer Selected!");
        }
        else
        {
            glbPrinterWidth = 48;
            showAlert("48 Characters / Line or 3 Inch (80mm) Printer Selected!");
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.setHeaderTitle("Select Printer to connect");

        for (int i = 0; i < printerList.size(); i++)
        {
            menu.add(0, v.getId(), 0, printerList.get(i));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        super.onContextItemSelected(item);
        String printerName = item.getTitle().toString();
        try
        {
            m_AemScrybeDevice.connectToPrinter(printerName);
            m_cardReader = m_AemScrybeDevice.getCardReader(this);
            m_AemPrinter = m_AemScrybeDevice.getAemPrinter();
            Toast.makeText(MainActivity.this,"Connected with " + printerName,Toast.LENGTH_SHORT ).show();

            //  m_cardReader.readMSR();
        }
        catch (IOException e)
        {
            if (e.getMessage().contains("Service discovery failed"))
            {
                Toast.makeText(MainActivity.this,"Not Connected\n"+ printerName + " is unreachable or off otherwise it is connected with other device",Toast.LENGTH_SHORT ).show();
            }
            else if (e.getMessage().contains("Device or resource busy"))
            {
                Toast.makeText(MainActivity.this,"the device is already connected",Toast.LENGTH_SHORT ).show();
            }
            else
            {
                Toast.makeText(MainActivity.this,"Unable to connect",Toast.LENGTH_SHORT ).show();
            }
        }
        return true;
    }
    @Override
    protected void onDestroy()
    {
        if (m_AemScrybeDevice != null)
        {
            try
            {
                m_AemScrybeDevice.disConnectPrinter();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    public void onShowPairedPrinters(View v)
    {

        String p = m_AemScrybeDevice.pairPrinter("BTprinter0314");
        // showAlert(p);
        printerList = m_AemScrybeDevice.getPairedPrinters();

        if (printerList.size() > 0)
            openContextMenu(v);
        else
            showAlert("No Paired Printers found");
    }
    public void onDisconnectDevice(View v)
    {
        if (m_AemScrybeDevice != null)
        {
            try
            {
                m_AemScrybeDevice.disConnectPrinter();
                Toast.makeText(MainActivity.this, "disconnected", Toast.LENGTH_SHORT).show();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void printerName(View view) throws IOException {
        if (m_AemPrinter == null)
        {
            Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        String nameeditText = null;
        nameeditText=edtName.getText().toString();
        if(nameeditText.isEmpty())
        {
            showAlert("Please Write Printer Name");
        }
        char[] printerName=new char[]{0x1b,'9','N'};  // Z
        String name=new String(printerName);
        String printerNameData=new String(name.concat(nameeditText));
        char[] line=new char[]{0X0A};
        String lineFeed=new String(line);
        String data=new String(printerNameData.concat(lineFeed));
        m_AemPrinter.print(data);
        m_AemScrybeDevice.disConnectPrinter();
    }

    public void pin(View view) throws IOException {
        if (m_AemPrinter == null)
        {
            Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        String pineditText = null;
        pineditText=edtPin.getText().toString();

        if(pineditText.isEmpty())
        {
            showAlert("Please Write Pin");
        }

        char[] printerPin=new char[]{0x1b,'9','P'};
        String pinCommand=new String(printerPin);
        String printerNameData=new String(pinCommand.concat(pineditText));
        m_AemPrinter.print(printerNameData);
        m_AemScrybeDevice.disConnectPrinter();

    }
    public void secureModeOn(View view) throws IOException {
        if (m_AemPrinter == null)
        {
            Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        char[] secureModeOnCommand=new char[]{0x1b,'9','S'};
        String secureModeOn=new String(secureModeOnCommand);
        m_AemPrinter.print(secureModeOn);
        m_AemScrybeDevice.disConnectPrinter();
    }
    public void secureModeOff(View view) throws IOException {

        if (m_AemPrinter == null)
        {
            Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        char[] secureModeOffCommand=new char[]{0x1b,'9','O'};
        String secureModeOff=new String(secureModeOffCommand);
        m_AemPrinter.print(secureModeOff);
        m_AemScrybeDevice.disConnectPrinter();
    }

    public void showAlert(String alertMsg)
    {
        AlertDialog.Builder alertBox = new AlertDialog.Builder(MainActivity.this);

        alertBox.setMessage(alertMsg).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                return;
            }
        });

        AlertDialog alert = alertBox.create();
        alert.show();
    }

    @Override
    public void onScanMSR(String buffer, CardReader.CARD_TRACK cardtrack) {

    }

    @Override
    public void onScanDLCard(String buffer) {

    }

    @Override
    public void onScanRCCard(String buffer) {

    }

    @Override
    public void onScanRFD(String buffer) {

    }

    @Override
    public void onScanPacket(String buffer) {

    }

    @Override
    public void onDiscoveryComplete(ArrayList<String> aemPrinterList) {

    }

    public void onPrintBill(View v)
    {
        int numChars = glbPrinterWidth;//CheckPrinterWidth();
        Toast.makeText(MainActivity.this, "Printing " + numChars + " Character/Line Bill", Toast.LENGTH_SHORT).show();
        onPrintBillBluetooth(numChars);
    }

    public void onPrintBillBluetooth(int numChars)
    {

        if (m_AemPrinter == null)
        {
            Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        String data = "TWO INCH PRINTER: TEST PRINT \n";
        String d =    "_________________________________\n";
        try
        {
            m_AemPrinter.setFontType(AEMPrinter.DOUBLE_HEIGHT);
            m_AemPrinter.setFontType(AEMPrinter.TEXT_ALIGNMENT_CENTER);
            if(numChars == 32) {
                m_AemPrinter.print(data);
                m_AemPrinter.print(d);
                data = "CODE|DESC|RATE(Rs)|QTY |AMT(Rs)\n";
                m_AemPrinter.print(data);
                m_AemPrinter.print(d);
                data = "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                    /*    "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +
                        "13|ColgateGel |35.00|02|70.00\n" +
                        "29|Pears Soap |25.00|01|25.00\n" +
                        "88|Lux Shower |46.00|01|46.00\n" +
                        "15|Dabur Honey|65.00|01|65.00\n" +
                        "52|Dairy Milk |20.00|10|200.00\n" +
                        "128|Maggie TS |36.00|04|144.00\n" +*/
                        "_______________________________\n";

                m_AemPrinter.setFontType(AEMPrinter.FONT_NORMAL);
                m_AemPrinter.print(data);
                m_AemPrinter.setFontType(AEMPrinter.DOUBLE_HEIGHT);
                m_AemPrinter.setFontType(AEMPrinter.TEXT_ALIGNMENT_CENTER);
                data = "   TOTAL AMOUNT (Rs.)   550.00\n";
                m_AemPrinter.print(data);
                m_AemPrinter.setFontType(AEMPrinter.FONT_002);
                m_AemPrinter.print(d);
                data = "   Thank you! \n";
                m_AemPrinter.setFontType(AEMPrinter.DOUBLE_WIDTH);
                m_AemPrinter.print(data);
                //m_AemPrinter.setCarriageReturn();
                //  m_AemPrinter.setCarriageReturn();
                //  m_AemPrinter.setCarriageReturn();
                //   m_AemPrinter.setCarriageReturn();
                onPrintImageBT();
            }
            else
            {
                data = "         THREE INCH PRINTER: TEST PRINT\n";
                m_AemPrinter.printThreeInch(data);
                m_AemPrinter.setLineFeed(1);

                // d =    "________________________________________________\n";

                data = 	"CODE|   DESCRIPTION   |RATE(Rs)|QTY |AMOUNT(Rs)\n";
                m_AemPrinter.printThreeInch(data);
                data =  " 13 |Colgate Total Gel | 35.00  | 02 |  70.00\n"+
                        " 29 |Pears Soap 250g   | 25.00  | 01 |  25.00\n"+
                        " 88 |Lux Shower Gel 500| 46.00  | 01 |  46.00\n"+
                        " 15 |Dabur Honey 250g  | 65.00  | 01 |  65.00\n"+
                        " 52 |Cadbury Dairy Milk| 20.00  | 10 | 200.00\n"+
                        "128 |Maggie Totamto Sou| 36.00  | 04 | 144.00\n";

                m_AemPrinter.printThreeInch(data);
                data = "          TOTAL AMOUNT (Rs.)   550.00\n";
                m_AemPrinter.printThreeInch(data);
                data = "        Thank you! \n";
                m_AemPrinter.printThreeInch(data);
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
                // m_AemPrinter.setLineFeed(8);
            }
        }
        catch (IOException e) {

            if (e.getMessage().contains("socket closed"))
                Toast.makeText(MainActivity.this, "Printer not connected", Toast.LENGTH_SHORT).show();
        }
    }

    public void onPrintImageBT()
    {
        if (m_AemPrinter == null)
        {
            showAlert("Printer not connected");
            return;
        }
        try
        {
//    			m_AemPrinter.printAEMLogo();
            //  m_AemPrinter.setCarriageReturn();
            //    m_AemPrinter.setCarriageReturn();
            //   m_AemPrinter.setCarriageReturn();
            //   m_AemPrinter.setCarriageReturn();
            InputStream is = getAssets().open("bluprintlogo1.jpg");
            Bitmap inputBitmap = BitmapFactory.decodeStream(is);
            Bitmap resizedBitmap = null;
            if(glbPrinterWidth == 32)
                resizedBitmap = Bitmap.createScaledBitmap(inputBitmap, 384, 384, false);
            else
                resizedBitmap = Bitmap.createScaledBitmap(inputBitmap, 384, 384, false);
            //m_AemPrinter.printBitImage(resizedBitmap,BluetoothActivity.this,m_AemPrinter.IMAGE_CENTER_ALIGNMENT);
            RasterBT(resizedBitmap);
            //m_AemPrinter.setCarriageReturn();
            // m_AemPrinter.setCarriageReturn();
            // m_AemPrinter.setCarriageReturn();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    protected void RasterBT(Bitmap image) {
        try {
            if (glbPrinterWidth == 32)
            {
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.printImage(image);
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
            }
            else
            {
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.printImageThreeInch(image);
                m_AemPrinter.setLineFeed(6);
                m_AemPrinter.setCarriageReturn();
                m_AemPrinter.setCarriageReturn();
            }
        }
        catch (IOException e)
        {
            showAlert("IO EX:  " + e.toString());
        }
    }

    public void onCutterBill(View view) {
        Toast.makeText(MainActivity.this, "Printer is not connect", Toast.LENGTH_SHORT).show();

    }


/*
    public void onHalfCutterBill(View view) throws IOException {
        data = "                 Token Number 1 \n";

        //  data = "         THREE INCH PRINTER: TEST PRINT\n";
        m_AemPrinter.printThreeInch(data);
        m_AemPrinter.setLineFeed(1);
        // d =    "________________________________________________\n";

        data = 	"CODE|   DESCRIPTION   |RATE(Rs)|QTY |AMOUNT(Rs)\n";
        m_AemPrinter.printThreeInch(data);
        data =  " 13 |Colgate Total Gel | 35.00  | 02 |  70.00\n"+
                " 29 |Pears Soap 250g   | 25.00  | 01 |  25.00\n"+
                " 88 |Lux Shower Gel 500| 46.00  | 01 |  46.00\n"+
                " 15 |Dabur Honey 250g  | 65.00  | 01 |  65.00\n"+
                " 52 |Cadbury Dairy Milk| 20.00  | 10 | 200.00\n"+
                "128 |Maggie Totamto Sou| 36.00  | 04 | 144.00\n";

        m_AemPrinter.printThreeInch(data);
        data = "          TOTAL AMOUNT (Rs.)   550.00\n";
        m_AemPrinter.printThreeInch(data);
        data = "               Thank you! \n";
        m_AemPrinter.printThreeInch(data);
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        String data=new String(labelFullCut);
        m_AemPrinter.print(data);

    }
*/

/*
    public void onFullCutterBill(View view) throws IOException {

      //  data = "         THREE INCH PRINTER: TEST PRINT\n";
        data = "               Token Number 1 \n";
        m_AemPrinter.printThreeInch(data);
        m_AemPrinter.setLineFeed(1);

        // d =    "________________________________________________\n";

        data = 	"CODE|   DESCRIPTION   |RATE(Rs)|QTY |AMOUNT(Rs)\n";
        m_AemPrinter.printThreeInch(data);
        data =  " 13 |Colgate Total Gel | 35.00  | 02 |  70.00\n"+
                " 29 |Pears Soap 250g   | 25.00  | 01 |  25.00\n"+
                " 88 |Lux Shower Gel 500| 46.00  | 01 |  46.00\n"+
                " 15 |Dabur Honey 250g  | 65.00  | 01 |  65.00\n"+
                " 52 |Cadbury Dairy Milk| 20.00  | 10 | 200.00\n"+
                "128 |Maggie Totamto Sou| 36.00  | 04 | 144.00\n";

        m_AemPrinter.printThreeInch(data);
        data = "          TOTAL AMOUNT (Rs.)   550.00\n";
        m_AemPrinter.printThreeInch(data);
        data = "              Thank you! \n";
        m_AemPrinter.printThreeInch(data);
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        m_AemPrinter.setCarriageReturn();
        String data=new String(labelHalfCut);
        m_AemPrinter.print(data);

    }
*/
}
