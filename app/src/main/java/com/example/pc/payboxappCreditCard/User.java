package com.example.pc.payboxappCreditCard;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/**
 * User information-related functionality.
 * @author james
 */
public class User
{
	private static final String lrLivePublicKey = "pk_live_pOyIIOz5rG8rrlAGorSrFn3q00WEpXWHGu";

	/**
	 * Retrieve direct payment information for a user.
	 * @param landlordEmail The email of the landlord to check the information of.
	 * @return Information about the direct payment details for the user
	 */
	public User(){}
	public  String getActiveStripePublicKey(String landlordEmail)
	{
		String linkUrl = "https://payboxtimer.com/api/getDirectPaymentDetails";
		URL url;
		try
		{
			// build json
			JSONObject obj = new JSONObject();
			obj.put("user_email", landlordEmail);

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

				Log.d("mzDebug", sb.toString());

				JSONObject res = new JSONObject(sb.toString());
				if (res.getBoolean("success"))
				{
					boolean enabled = res.getBoolean("stripe_payment_direct");
					boolean testing = res.getBoolean("testing");
					testing = false ;
					return testing ? res.getString("stripe_payment_direct_public_test_key") : res.getString("stripe_payment_direct_public_live_key");


				}
				else { return null; }
			}
		}
		catch (Exception e) { e.printStackTrace(); }
		return null;
	}
}