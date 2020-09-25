package com.example.pc.payboxappCreditCard;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/**
 * An instance of a PayBox device.
 * @author james
 */
public class Device
{
    private String id = "";
    private String label = "";
    private String moneyType = "American Quarter";
    private int durationMinutes = 5;
    private boolean disabled = false;
    private String landlordEmail = "";
    private boolean isPublic = false;
    public String debTag = "myDebug" ;
    public Device() {}
    public Device(String id) { setId(id); }
    public Device(String id, String label, String moneyType, int mins)
    {
        this.id = id;
        this.label = label;
        this.moneyType = moneyType;
        durationMinutes = mins;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getMoneyType() { return moneyType; }
    public void setMoneyType(String type) { moneyType = type; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int mins) { durationMinutes = mins; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean d) { disabled = d; }
    public String getLandlordEmail() { return landlordEmail; }
    public void setLandlordEmail(String email) { landlordEmail = email; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean p) { isPublic = p; }
    /**
     * Calculates the number of pulses needed at minimum to supply the PayBox with the desired time.
     * @param mins The number of minutes desired.
     * @return The number of pulses to send.
     */
    public int getPulsesNeededForTime(int mins)
    {
        if (mins <= 0) { return 0; }
        return (int)Math.ceil((float)mins / (float)this.durationMinutes);
    }

    /**
     * Retrieve the value of a single currency type for this device.
     * @return The currency value.
     */
    public float getMoneyTypeValue()
    {
        String type_string = moneyType.toUpperCase();

        if (type_string.equals("AMERICAN QUARTER") || type_string.equals("CANADIAN QUARTER")) { return 0.25f; }
        else if (type_string.equals("CANADIAN LOONIE")) { return 1.0f; }
        else if (type_string.equals("CANADIAN TWOONIE")) { return 2.0f; }
        return 0.0f;
    }

    /**
     * Calculate the price it would cost for this device to supply the desired time in minutes.
     * @param durationMins The amount of minutes, at minimum, desired.
     * @return The price.
     */
    public float getPriceForTime(int durationMins)
    {
        return getMoneyTypeValue() * (float)getPulsesNeededForTime(durationMins);
    }

    /**
     * Retrieve information for a PayBox device.
     * @param id The PayBox number to search for.
     * @return Device information in the Device object.
     */

    public static Device makeDevice(final String sb) {
        try
        {
        JSONObject obj = new JSONObject(sb.toString());
        if (!obj.getBoolean("success")) {
            Log.d("myDebug", "obj not successful) ") ;
            return null;
        }

        JSONObject dev = obj.getJSONObject("device");
        //  if (!dev.getBoolean("credit_card_enabled")) { return null; }

        Device ret = new Device();
        ret.disabled = dev.getBoolean("disabled");
        ret.durationMinutes = dev.getInt("duration_minutes");
        Log.d("myDebug","got time " + String.valueOf(ret.durationMinutes) ) ;
        ret.label = dev.getString("label");
        ret.moneyType = dev.getString("money_type");
        ret.landlordEmail = dev.getString("landloard_email");
        ret.id = dev.getString("uid");
        ret.isPublic = dev.getBoolean("isPublic");
        return ret;
        }
        catch (Exception e) { e.printStackTrace();
            Log.d("myDebug", "exception " + e.toString());}
        return null;
    }
    public static Device load(final String id)
    {
         String debTag = "myDebug" ;
        String linkUrl = "https://payboxtimer.com/api/device_number?number=" + id;
        URL url;



        try
        {
            url = new URL(linkUrl);
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            StringBuilder sb = new StringBuilder();
            int chr = reader.read();
            while (chr != -1)
            {
                sb.append((char)chr);
                chr = reader.read();
            }

            //System.out.println(sb.toString());

            JSONObject obj = new JSONObject(sb.toString());
            if (!obj.getBoolean("success")) {
                Log.d("myDebug", "obj not successful) ") ;
                return null;
                }

            JSONObject dev = obj.getJSONObject("device");
          //  if (!dev.getBoolean("credit_card_enabled")) { return null; }

            Device ret = new Device();
            ret.disabled = dev.getBoolean("disabled");
            ret.durationMinutes = dev.getInt("duration_minutes");
            Log.d(debTag,"got time " + String.valueOf(ret.durationMinutes) ) ;
            ret.label = dev.getString("label");
            ret.moneyType = dev.getString("money_type");
            ret.landlordEmail = dev.getString("landloard_email");
            ret.id = dev.getString("uid");
            ret.isPublic = dev.getBoolean("isPublic");
            return ret;
        }
        catch (Exception e) { e.printStackTrace();
        Log.d(debTag, "exception " + e.toString());}
        return null;
    }

    /**
     Charges a credit card using Stripe and returns information about the operation.
     @param stripeToken The token retrieved from Stripe for the charge.
     @param durationMins The duration in minutes being requested to pay for.
      * @return Whether or not the charge succeeded and pulses should be sent and a message containing additional information.
     */
        public CreditCardChargeResult chargeCreditCard(String stripeToken, int durationMins)
    {
        if (isDisabled()) { return new CreditCardChargeResult(false, "This device is disabled."); }

        String linkUrl = "https://payboxtimer.com/api/stripe/mobilePayment";
        URL url;
        try
        {
            // build json
            if (this.getPriceForTime(durationMins)<1) {
                do {
                    durationMins++ ;
                }while (this.getPriceForTime(durationMins)<1) ;
            }
            JSONObject obj = new JSONObject();
            obj.put("stripe_token", stripeToken);
            obj.put("paybox_number", id);
            obj.put("selected_duration", durationMins);
            obj.put("payment_method", "Credit Card");

            // make request
            url = new URL(linkUrl);
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(obj.toString());
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();

            if (responseCode == 200)
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                StringBuilder sb = new StringBuilder();
                int chr = reader.read();
                while (chr != -1)
                {
                    sb.append((char)chr);
                    chr = reader.read();
                }

                //System.out.println(sb.toString());
                Log.d("myDebug" ,"charge result" + sb.toString()) ;
                JSONObject res = new JSONObject(sb.toString());
                return new CreditCardChargeResult(res.getBoolean("success"), res.getString("msg"));
            }
        }
        catch (Exception e) { e.printStackTrace(); }
        return new CreditCardChargeResult(false, "API error.");
    }
}