package com.example.pc.payboxappCreditCard;

import android.util.Log;

import org.json.JSONObject;

/**
 * Information about the direct payment settings for a user.
 * @author james
 */
public class UserDirectPaymentInfo
{
	private boolean enabled = false;
	private String publicTestKey = "";
	private String privateTestKey = "";
	private String publicLiveKey = "";
	private String privateLiveKey = "";
	
	public UserDirectPaymentInfo(JSONObject res)
	{
		try {
			enabled = res.getBoolean("stripe_payment_direct");
			if (enabled) {
				publicTestKey = res.getString("stripe_payment_direct_public_test_key");
				privateTestKey = res.getString("stripe_payment_direct_private_test_key");
				publicLiveKey = res.getString("stripe_payment_direct_public_live_key");
				privateLiveKey = res.getString("stripe_payment_direct_private_live_key");
			}
		}
		catch (Exception ex) {
			Log.d("myDebug", "UserDirectPaymentInfo error");
		}
	}
	
	public boolean isEnabled() { return enabled; }
	public String getPublicTestKey() { return publicTestKey; }
	public String getPrivateTestKey() { return privateTestKey; }
	public String getPublicLiveKey() { return publicLiveKey; }
	public String getPrivateLiveKey() { return privateLiveKey; }
}
