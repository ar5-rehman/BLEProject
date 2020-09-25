package com.example.pc.payboxappCreditCard;

/**
 * Created by PC on 11/1/2018.
 */

class Utils {


        private String HEX_CHARS = "0123456789ABCDEF" ;

        public static byte[] hexStringToByteArray(String datsString) {
            int len = datsString.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(datsString.charAt(i), 16) << 4)
                        + Character.digit(datsString.charAt(i+1), 16));
            }
            return data;
        }

        private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
        public static String bytesToHex(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            for ( int j = 0; j < bytes.length; j++ ) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }


    public static String StringToHex(String k) {
        byte[] d = k.getBytes();
        StringBuilder sb = new StringBuilder();
        for (byte b : d) {
            sb.append(String.format("%02X ", b));
        }
        String x = sb.toString();
        return x.replaceAll(" ", "");
    }

}