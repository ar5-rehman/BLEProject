package com.example.pc.payboxappCreditCard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardNumberEditText;
import com.stripe.android.view.ExpiryDateEditText;
import com.stripe.android.view.StripeEditText ;

import static android.content.ContentValues.TAG;
import static com.example.pc.payboxappCreditCard.BluetoothLeService.EXTRAS_DEVICE_NAME;
import static com.example.pc.payboxappCreditCard.Device.makeDevice;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private LeDeviceListAdapter deviceListAdapter;
    private ArrayList<BluetoothDevice> devices_ble = new ArrayList<>();
    BluetoothAdapter btAdapter;
    private BluetoothLeScanner btScanner =
            BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

    private BluetoothLeService BleServe;
    private String deviceAddress;
    private  boolean mScanning  = false ;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final long SCAN_PERIOD = 20000;
    private static final String URL_getDevice = "https://payboxtimer.com/api/device_number?number=" ;
    private int DevicePosition = 0 ;
    Toast toast ;
    boolean active  =false  ;
    enum AppState {get_price, chargeCredit , paying}

    enum AsyncCallType { getPrice,getGetPrice_position, chargeCredit , getKey}
    AsyncCallType callType ;
    private  AppState  appState  = AppState.get_price ;

    /// NFC Adapter components
    @SuppressLint("StaticFieldLeak")
    static MainActivity instance ;
    //Arrays check routine
     private Handler handler = new Handler();

    private TextView deviceID;
    private TextView Price ;
    private LinearLayout labels ;
    private TextView label_pulses ;
    private TextView label_money_type ;
    private TextView label_duration ;

    private Spinner TimeNeeded;

    // Main buttons
    protected Button Pay;
    protected Button HowTo ;
    protected Button Contact ;
    private Button GetPrice;
    private Button chargeCreditCard;
    protected Button refresh_button ;
    protected ListView devicesList ;

    ProgressDialog progressDialog;
    private String devid ="";

    // times Set
    private List<String> timeList = Arrays.asList("20 min", "40 min", "60 min","80 min", "120 min", "140 min",  "160 min");
    // target device
    private Device device = null ;

    private boolean connected = false ;
    private String connected_name = ""  ;
    private String key = "pk_live_pOyIIOz5rG8rrlAGorSrFn3q00WEpXWHGu" ;

   // key = "pk_test_TZeQn0EcB7pf8dPM8A1RKvz800DT9SVL1s" ;


    NfcAdapter mAdapter ;
    Context context;
    public String debTag = "myDebug" ;

    // default needed time
    private  int timeNeeded = 20 ;

    private float balance  ;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BleServe = ((BluetoothLeService.LocalBinder) service).getService();
            if (!BleServe.initialize()) {
                Log.e(debTag, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            for(int i=0;i<devices_ble.size();i++)
            {
                deviceAddress = devices_ble.get(i).getAddress();
                BleServe.connect(deviceAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            BleServe = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                if (intent.getStringExtra(EXTRAS_DEVICE_NAME).equals(device.getId() )) {
                    connected = true ;
                    connected_name = device.getId() ;
                    setToast("Paybox " + device.getId() + " connected") ;
                    deviceID.setText(device.getId() );
                    appState = AppState.chargeCredit ;
                    labels.setVisibility(View.VISIBLE);
                    label_duration.setText("Min: " + timeNeeded );
                    label_money_type.setText("C: " + device.getMoneyType());
                    label_pulses.setText("p: " +  device.getPulsesNeededForTime(timeNeeded));
                    try {
                        View view = devicesList.getChildAt(DevicePosition);
                        TextView devName = view.findViewById(R.id.device_name) ;
                        devName.setTextColor(Color.parseColor("#009900"));
                        setButtons(appState);
                    }
                    catch (Exception e) {
                        Log.d(debTag, "Unable to find connected device in devices list") ;
                    }

                }
                else {
                    setToast("incorrect device connected " + intent.getStringExtra(EXTRAS_DEVICE_NAME) + " mydev " + device.getId());
                    BleServe.disconnect();
                }
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                for (int i = 0; i < devices_ble.size(); i++){
                    appState = AppState.get_price;
                    View view = devicesList.getChildAt(i);
                    TextView devName = view.findViewById(R.id.device_name);
                    devName.setTextColor(Color.parseColor("#000000"));
                }
                if (appState == AppState.chargeCredit) appState = AppState.get_price ;
                if (connected)
                setToast("Device Disconnected");
                labels.setVisibility(View.INVISIBLE);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                BleServe.getSupportedGattServices();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            }
        }
    };


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.one_layout);
//         getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_text);

        context = this;
        deviceID = findViewById(R.id.inputID);
        GetPrice = findViewById(R.id.getPrice);
        chargeCreditCard = findViewById(R.id.charge);
        Price = findViewById(R.id.amount);
        Pay = findViewById(R.id.Pay);
        HowTo = findViewById(R.id.HowTo);
        Contact = findViewById(R.id.Contact);
        refresh_button = findViewById(R.id.refresh) ;
        TimeNeeded = findViewById(R.id.TimeNeeded);
        devicesList = findViewById(R.id.devicesList)  ;
        labels = findViewById(R.id.labels) ;
        label_duration = findViewById(R.id.label_duration) ;
        label_money_type = findViewById(R.id.label_money_Type) ;
        label_pulses = findViewById(R.id.label_pulses) ;
        labels.setVisibility(View.INVISIBLE);
        final CardNumberEditText mCardInputWidget =  findViewById(R.id.InputCardNum);
        final ExpiryDateEditText expDate = findViewById(R.id.expDate);
        final StripeEditText CSV = findViewById(R.id.InputCSV);
        active = true;
        TimeNeeded.setOnItemSelectedListener(this);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_itrm, timeList);
        TimeNeeded.setAdapter(dataAdapter);
        try {
            mAdapter = NfcAdapter.getDefaultAdapter(this);
            if (!mAdapter.isEnabled()){
                setToast( "Please activate NFC");
                showDialog();
            }

        }
        catch (Exception e) {
            setToast("cant get nfc adapter");

        }



        String provider = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (provider == null || !provider.contains("gps") || !provider.contains("network")) {
            new AlertDialog.Builder(this).setTitle("Location Settings").setMessage(
                    "You should enable both network-based and GPS-based location services to ensure you can find nearby groups.")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    } catch (Throwable t) {
                        Log.e(TAG, "failed to launch location settings", t);
                    }
                }
            }).show();
        }

        hasPermissions();


        instance = this;
//        try {
            chargeCreditCard.setBackgroundColor(ContextCompat.getColor(context, R.color.buttonColor));

            Contact.setOnClickListener(new View.OnClickListener() {
                                           @Override
                                           public void onClick(View v) {
                                               Uri uri = Uri.parse("https://www.payboxtimer.com/dashboard/contact"); // missing 'http://' will cause crashed
                                               Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                               startActivity(intent);
                                           }
                                       }

            );
            refresh_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (btAdapter != null && !btAdapter.isEnabled()) {
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
                    }
                    else {
                        deviceListAdapter.clear();
                        deviceListAdapter.notifyDataSetChanged();
                        startScanning();
                    }
                }
            }
            );
            HowTo.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             Uri uri = Uri.parse("https://www.limitedresources.us/how-to.html#/"); // missing 'http://' will cause crashed
                                             Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                             startActivity(intent);
                                         }
                                     }

            );
            GetPrice.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                if (appState == AppState.paying ) {
                                                    setToast(getString(R.string.Error_getPrice_PayState));
                                                    Log.d(debTag, getString(R.string.Error_getPrice_PayState));
                                                } else {
                                                    if (deviceID.getText().toString().length() == 0 && connected)
                                                        devid = connected_name ;
                                                    else
                                                        devid = deviceID.getText().toString();
                                                    if (devid.length() == 0) {
                                                        setToast(getString(R.string.Error_getPrice_noID));

                                                        return;
                                                    } else if (devid.length() < 6 || devid.length() > 20) {
                                                        setToast(getString(R.string.Error_getPrice_invalidID));
                                                        Log.d(debTag, getString(R.string.Error_getPrice_invalidID));
                                                        return;
                                                    }
                                                    else if (connected && connected_name.equals(devid) != true)
                                                        BleServe.disconnect();
                                                    progressDialog = new ProgressDialog(MainActivity.this);
                                                    progressDialog.setMessage("Processing"); // Setting Message
                                                    progressDialog.setTitle("Getting price"); // Setting Title
                                                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
                                                    progressDialog.show(); // Display Progress Dialog
                                                    progressDialog.setCancelable(false);
                                                    new URLTASK().execute("https://payboxtimer.com/api/device_number?number=" + devid, "device-ble");
                                                    //   if (devid!=null)
                                                    //      Log.d(debTag,"Price for time is "+ device.getMoneyType());
                                                }

                                            }
                                        }

            );
            chargeCreditCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (appState == AppState.get_price ) {
                        setToast(getString(R.string.charge_Error_tapToPayState));
                        Log.d(debTag, getString(R.string.charge_Error_tapToPayState));
                        return;
                    } else if (appState == AppState.paying) {
                        setToast(getString(R.string.charge_Error_getPriceState));
                        Log.d(debTag, getString(R.string.charge_Error_getPriceState));
                        return;
                    }
                    else if (device.isDisabled()) {
                        setToast("The current device is disabled, please enter a new device ID");
                    }else {

                        if (mCardInputWidget.isCardNumberValid() && expDate.isDateValid() && CSV.length() == 3) {
                            final int[] cardDate = expDate.getValidDateFields();
                            assert cardDate != null;
                            final Card card = Card.create(mCardInputWidget.getCardNumber(), cardDate[0], cardDate[1], Objects.requireNonNull(CSV.getText()).toString());
                            card.toBuilder().build();
                            if (card.validateCard()) {
                                // Show errors
                                Log.d(debTag, "Card Valid");
                                setToast(getString(R.string.charge_validCard));
                                progressDialog = new ProgressDialog(MainActivity.this);
                                progressDialog.setMessage("Processing"); // Setting Message
                                progressDialog.setTitle("Paying"); // Setting Title
                                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
                                progressDialog.show(); // Display Progress Dialog
                                progressDialog.setCancelable(false);

//                            try {
//                                String str_result = new URLTASK().execute("getKey").get();
//                            }
//                            catch (Exception e) {
//                                Log.d(debTag, "Exception for create tokrn " + e.toString());
//                                setToast(getString(R.string.charge_negative));
//                                return ;
//                            }

                                final Stripe stripe = new Stripe(
                                        getApplicationContext(),
                                        key
                                );
                                try {
                                    stripe.createToken(
                                            card,
                                            new TokenCallback() {
                                                public void onSuccess(@NonNull Token token) {
                                                    // Send token to your server
                                                    Log.d(debTag, "token " + token.getId());
                                                    Log.d(debTag, "final key value " + key);
                                                    new URLTASK().execute(token.getId(), "pay");

                                                }

                                                public void onError(@NonNull Exception error) {
                                                    // Show localized error message
                                                    Log.d(debTag, "Token generation " + error.toString());
                                                }
                                            }
                                    );

                                } catch (Exception e) {
                                    Log.d(debTag, "Exception for create token " + e.toString());
                                    setToast(getString(R.string.charge_negative));
                                    if (progressDialog != null) progressDialog.cancel();
                                }
                            } else {
                                Log.d(debTag, "Card inValid");
                                setToast(getString(R.string.charge_Error_invalidCard));
                            }

                        } else {
                            Log.d(debTag, "data inValid");
                            setToast("Invalid card input fields ");
                        }
                    }


                }
            });

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        } else {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
// Create the adapter to convert the array to views
        deviceListAdapter = new LeDeviceListAdapter(this, devices_ble);
        btScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        devicesList.setAdapter(deviceListAdapter);
        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position,
                                    long arg3)
            {
                BluetoothDevice dev = (BluetoothDevice) adapter.getItemAtPosition(position);

                Log.d(debTag, "selected" + dev.getName()) ;
                DevicePosition = position ;
                new URLTASK().execute(URL_getDevice + dev.getName(), "device-ble");

            }
        });

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        if(btAdapter!=null) {
            startScanning();
        }
    }
    public String encrypt( String content) {
        String result = null;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec("abcdefghijklmnop".getBytes("utf-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
         /*   byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);*/
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte encoded[]  = cipher.doFinal(content.getBytes("utf-8"));
            Log.d(debTag, new String(encoded) + " size of result " + encoded.length);
            return  new String(encoded);
        } catch (Exception e) {
            Log.d(debTag, "encrypt " + e.toString());
            e.printStackTrace();
        }
        return result;
    }

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if(!devices_ble.contains(result.getDevice())) {
                        if (deviceListAdapter != null ) {
                            deviceListAdapter.add(result.getDevice());
                            deviceListAdapter.notifyDataSetChanged();
                            setListViewHeightBasedOnItems(devicesList);
                        }
                    }

                }
            };

    private void startScanning() {

            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    Log.d(debTag, "stop scanning") ;
                    btScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);
            ParcelUuid  target = new ParcelUuid(UUID.fromString("9a8ca9ef-e43f-4157-9fee-c37a3d7dc12d")) ;
            List<ScanFilter> filters = Collections.singletonList(new ScanFilter.Builder().setServiceUuid(target).build());
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                    .build();
            Log.d(debTag, "start scanning") ;
            mScanning = true;
            if(btScanner!=null) {
                btScanner.startScan(leScanCallback);
            }
    }


    public void stopScanning() {
        System.out.println("stopping scanning");
        mScanning = false;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (btScanner != null)
                    btScanner.stopScan(leScanCallback);
            }
        });
    }

    public void showDialog() throws Exception {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("NFC is disabled");
        builder.setMessage("Do you want to activate NFC?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog
                startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Do nothing
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
        builder.show();
    }




    public void setToast(String txt) {
        if (toast!= null) toast.cancel();
        toast = new Toast(getApplicationContext());
        Log.d(debTag, "set toast "+ txt) ;
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast,
                (ViewGroup) findViewById(R.id.toast_view));
        TextView text = layout.findViewById(R.id.text);
        text.setText(txt);
        text.setGravity(Gravity.CENTER_HORIZONTAL);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG) ;
        toast.setView(layout);
        toast.show();
    }


    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    protected void onPause()  {
        super.onPause();
        active  =false ;
        if (toast != null)  toast.cancel();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onStop()  {
        super.onStop();
        if (toast != null)  toast.cancel();
    }
    @Override
    protected void onResume()  {
        super.onResume();
        if (toast != null)  toast.cancel();
        active  =true ;
        if (!btAdapter.isEnabled()) {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (BleServe != null) {
            final boolean result = BleServe.connect(deviceAddress);
            Log.d(debTag, "Connect request result=" + result);
        }

        // Initializes list view adapter.
     //   setListAdapter(devicesListAdapter);
//        startScanning();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            String item = parent.getItemAtPosition(position).toString();
            setToast("Selecetd : " + item);
            //  toast.show();
            Log.d(debTag, "selected Item " + item);

            if (parent.getId() == R.id.TimeNeeded) {
                timeNeeded = Integer.parseInt(item.substring(0,item.length()-4)) ;
                Log.d(debTag, "time Needed " + timeNeeded);


                }
    }



    public static MainActivity getInstance() {
        return instance;
    }
//
    @SuppressLint("StaticFieldLeak")
    private class URLTASK extends AsyncTask<String, Void, String> {
        String ret = null ;
        CreditCardChargeResult res;
        // onPreExecute called before the doInBackgroud start for display
        // progress dialog.
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected String doInBackground(String... urls) {
            if (urls[0].equals("getKey")){


                callType =AsyncCallType.getKey;
                User user = new User() ;
                ret = user.getActiveStripePublicKey(device.getLandlordEmail()) ;
                return ret ;
            }
            else if(urls[1].equals("device-ble")) {
                try {
                    callType = AsyncCallType.getPrice ;
                    ret =  GetData.downloadDataFromUrl(urls[0]);
                    return ret;

                } catch (IOException e) {
                    device = null ;
                    ret = null ;
                    return null;
                }
            }

            else if (urls[1].equals("pay")) {
                callType = AsyncCallType.chargeCredit;
                Log.d(debTag, "paying with token " + urls[0]);
                res = device.chargeCreditCard(urls[0], timeNeeded);
//                res = new CreditCardChargeResult(true , "{\"success\":true,\"msg\":\"Charge successful.\"}");
                ret  = res.getMessage();

                Log.d(debTag, " Payement result " + ret);

                return ret;
            }
            else return null ;


        }

        // onPostExecute displays the results of the doInBackgroud and also we
        // can hide progress dialog.
        @Override
        protected void onPostExecute(String result) {

            if (progressDialog != null) progressDialog.cancel();
            if (ret!= null){
                // getPrice
                if (callType ==AsyncCallType.getPrice) {
                    device = makeDevice(ret);
                    if (device == null) {
                        if (ret.equals("{\"success\":false}") && callType == AsyncCallType.getPrice) setToast("Device not found");
                        Log.d(debTag, "device null ");
                        ret = null;
                    }
                    else {
                        setToast("Price for " + timeNeeded + "min is " + "$" + device.getPriceForTime(timeNeeded) );
                        float prix = device.getPriceForTime(timeNeeded) ;
                        if (prix <1 ) prix = 1 ;
                        Price.setText("Total amount : $"+prix);
                        Log.d(debTag, "finish");
                       connectDevice(device.getId()) ;

                       /* if (!connected ||  !connected_name.equals(device.getId())){
                            stopScanning();
                            BleServe.connect(deviceAddress) ;
                            //BleServe.connect(devices_ble.get(i).getAddress());
                        }*/


                    }
                }
                // getkey
                else if (callType ==AsyncCallType.getKey) {
                    key = ret ;
                    Log.d(debTag, "get Token result " + ret) ;
                }
                // payement result
                else if (callType == AsyncCallType.chargeCredit  ) {


                    if (res.succeeded() &&  ret.contains("success")) {
                        setToast(getString(R.string.charge_Postive));
                        GetPrice.setBackgroundColor(ContextCompat.getColor(context, R.color.buttonColor)) ;
                        chargeCreditCard.setBackgroundColor(ContextCompat.getColor(context, R.color.buttonColor)) ;

                        if (device.getPriceForTime(timeNeeded) < 1) balance += 1 ;
                        else balance += device.getPriceForTime(timeNeeded) ;
                        appState = AppState.paying;
                        setButtons(AppState.paying);
                        Price.setText("Total amount : $"+String.valueOf(balance));
                        TimeNeeded.setEnabled(true);
                        deviceID.setFocusable(true);
                        /// Send Pulses
                        if (BleServe.writeCharacteristic(device.getPulsesNeededForTime(timeNeeded), device.getId()) ) {
                            appState = AppState.chargeCredit;
                            setButtons(appState);
                            setToast("Pulses sent");
                        }

                    } else {
                        setToast("Payment result : "+ret);
                        TimeNeeded.setEnabled(true);
                        deviceID.setFocusable(true);
                    }
                    if (progressDialog != null) progressDialog.cancel();

                }
            }
            else {
                if (callType==AsyncCallType.chargeCredit) setToast(getString(R.string.charge_negative));
                if (callType == AsyncCallType.getPrice) {
                    setToast("Check your internet connection");
                }
            }

        }
    }
//
//
//
    void connectDevice(String devName) {
        for (int i = 0; i < devices_ble.size(); i++) {
            if (devices_ble.get(i).getName().trim().equals(devName.trim())) {
                startScanning();
                Log.d(debTag, "connecting " + devName + " address " + devices_ble.get(i).getAddress() );
                deviceAddress  =devices_ble.get(i).getAddress();
                if (BleServe == null) {
                    Log.d(debTag, "BleServ is null") ;
                 //   BleServe.initialize() ;
                   // BleServe.connect(devices_ble.get(i).getAddress());
                }
                else if (!connected ||  !connected_name.equals(devName)){
                    stopScanning();
                    BleServe.connect(devices_ble.get(i).getAddress()) ;
                }
                return;
            }
        }

        Log.d(debTag, "connectDevice ended") ;
    }


    byte[] scanId(String id, byte sequence) {

        byte[] result = {0x55, 0x55 ,0x55 , 0x55 , sequence, 0 }  ;
        setToast("Device scanned " + id);
        return  result ;

    }

    public void setButtons (AppState state) {
        switch (state){
            case get_price:
                Log.d(debTag, "setButtons initState");
                GetPrice.setBackground(ContextCompat.getDrawable(context,R.drawable.btn_box));
                chargeCreditCard.setBackground(ContextCompat.getDrawable(context,R.drawable.btn_box));
                break;
            case chargeCredit:
                Log.d(debTag, "setButtons chargeCredit");
                GetPrice.setBackground(ContextCompat.getDrawable(context,R.drawable.btn_box));
                chargeCreditCard.setBackground(ContextCompat.getDrawable(context,R.drawable.btn_box));
                break ;

            case paying:
                Log.d(debTag, "setButtons tapTopay");
                GetPrice.setBackground(ContextCompat.getDrawable(context,R.drawable.invalid_btn));
                chargeCreditCard.setBackground(ContextCompat.getDrawable(context,R.drawable.invalid_btn));
                break;
        }

        Log.d(debTag, "END");

    }



    private class LeDeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
        public LeDeviceListAdapter(Context context, ArrayList<BluetoothDevice> devices) {
            super(context, 0, devices);
        }

        @NonNull
        @Override
        public View getView(int position, View view, ViewGroup parent) {
            // General ListView optimization code.
            BluetoothDevice device = getItem(position);
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.ble_item, parent, false);
            }
            TextView devName = view.findViewById(R.id.device_name) ;
            devName.setText(device.getName()  );
            return view;
        }
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public static boolean setListViewHeightBasedOnItems(ListView listView) {

        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter != null) {

            int numberOfItems = (listAdapter.getCount() < 5 ) ? listAdapter.getCount() : 4 ;

            // Get total height of all items.
            int totalItemsHeight = 0;
            for (int itemPos = 0; itemPos < numberOfItems; itemPos++) {
                View item = listAdapter.getView(itemPos, null, listView);
                float px = 500 * (listView.getResources().getDisplayMetrics().density);
                item.measure(View.MeasureSpec.makeMeasureSpec((int) px, View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                totalItemsHeight += item.getMeasuredHeight();
            }

            // Get total height of all item dividers.
            int totalDividersHeight = listView.getDividerHeight() *
                    (numberOfItems - 1);
            // Get padding
            int totalPadding = listView.getPaddingTop() + listView.getPaddingBottom();


            // Set list height.
            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = 10 + totalItemsHeight + totalDividersHeight + totalPadding;
            listView.setLayoutParams(params);
            listView.requestLayout();
            //setDynamicHeight(listView);
            return true;

        } else {
            return false;
        }

    }

    private void hasPermissions() {
        int targetSdkVersion = getApplicationInfo().targetSdkVersion;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);

            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, 1);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED ) {


                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        BleServe.disconnect();
        BleServe.close();
    }
}







