package com.example.pc.payboxappCreditCard;

/**
 * Holds information about the result of a credit card charge attempt.
 * @author james
 */
public class CreditCardChargeResult
{
	private boolean success;
	private String msg;
	
	public CreditCardChargeResult(boolean s, String m)
	{
		success = s;
		msg = m;
	}
	
	/**
	 * @return Whether or not the charge succeeded.
	 */
	public boolean succeeded() { return success; }
	
	/**
	 * @return The message corresponding to the result.
	 */
	public String getMessage() { return msg; }
}
