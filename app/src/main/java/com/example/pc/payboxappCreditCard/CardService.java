package com.example.pc.payboxappCreditCard;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;


public class CardService  extends HostApduService{

    private String TAG ="mlog: cardSercice" ;
    private String  STATUS_FAILED = "1111" ;
    private String CLA_NOT_SUPPORTED = "7E00";
    private String INS_NOT_SUPPORTED = "6D00";
    private String AID = "A0000002471001";
    private String SELECT_INS1 = "A5";
    private String SELECT_INS2 = "A4";
    private String DEFAULT_CLA = "00";
    private int MIN_APDU_LENGTH = 12;


    String StartSequence = "55" ;
    String getPulses = "56" ;
    String EndSequence = "57" ;
    private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnmABCDEFGHIGKLMNOPQRSTVWXYZ<>,?)+-(*&^%$#@!~`[]/";
    MainActivity a = MainActivity.getInstance();

    @Override
    public void onDeactivated(int b ){
            Log.d(TAG, "Deactivated: " + b) ;
            }

    public byte [] processCommandApdu (byte[] command , Bundle extra){
        if (command == null) {
            return Utils.hexStringToByteArray(STATUS_FAILED) ;
        }

        String hexCommandApdu = Utils.bytesToHex(command) ;


        if (hexCommandApdu.length() < MIN_APDU_LENGTH) {

            return Utils.hexStringToByteArray(STATUS_FAILED);
        }

        if (!hexCommandApdu.substring(0, 2).equals(DEFAULT_CLA)) {
            Log.d(TAG,hexCommandApdu.substring(0, 2) ) ;
            return Utils.hexStringToByteArray(CLA_NOT_SUPPORTED);
        }

        if (!hexCommandApdu.substring(2, 4).equals(SELECT_INS1)) {
            if (!hexCommandApdu.substring(2, 4).equals(SELECT_INS2)) {
                return Utils.hexStringToByteArray(INS_NOT_SUPPORTED);
            }
        }
        else Log.d(TAG, "my INS: "  + hexCommandApdu.substring(2, 4));
        if (hexCommandApdu.substring(10,24).equals(AID)) {


            if (command[12] == 0x55 || command[12] == 0x56 || command[12] == 0x57) {
                if (a==null) return Utils.hexStringToByteArray(INS_NOT_SUPPORTED);
                Log.d(a.debTag, "command " + hexCommandApdu) ;
                int idLength = command[13] ;
                Log.d(a.debTag, "ID length " + String.valueOf(idLength)) ;
               // byte [] devID = Utils.hexStringToByteArray(hexCommandApdu.substring(27)) ;
                String dev = "" ;
                for(int  i = 0 ; i< idLength; i++) { dev+= (char)command[14+i];}
                Log.d(a.debTag,"Length of id " + String.valueOf(idLength) + " ID value " + dev );
                return (a.scanId(dev,command[12] ));
//                return Utils.hexStringToByteArray(INS_NOT_SUPPORTED);

            }

            }

        return Utils.hexStringToByteArray(STATUS_FAILED);
    }


}
