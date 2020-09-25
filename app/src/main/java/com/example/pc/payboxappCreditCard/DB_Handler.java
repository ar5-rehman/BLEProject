package com.example.pc.payboxappCreditCard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by PC on 11/9/2018.
 */

public class DB_Handler extends SQLiteOpenHelper {

    String debTag = "myDebug" ;
    //information of database
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "landlordDB.db";
    /// land table
    private static final String TABLE_NAME = "Landevices";
    private static final String COL_1 = "ID";
    private static final String COL_2 = "deviceID";
    private static final String COL_3 = "deviceLabel";
    private static final String COL_4 = "moneyType";
    private static final String COL_5 = "durationMinute";

    // tenant table
    private static final String TABLE_Tennat = "Tenantdevices";
    private static final String COL_1T = "ID";
    private static final String COL_2T = "deviceID";
    private static final String COL_3T = "deviceLabel";
    private static final String COL_4T = "moneyType";
    private static final String COL_5T = "durationMinute";


    private static final String Land_Static = "landStat";
    private static final String COL = "ID";
    private static final String COL2 = "currency";
    private static final String COL3= "landMac";

    private static final String Tenant_Static = "tenantStat";
    private static final String COL1T = "ID";
    private static final String COL2T = "currency";
    private static final String COL3T= "tenantMac";
    private static final String COL4T= "walletBalance";



    //initialize the database
    public DB_Handler(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATION_TABLE = "CREATE TABLE " +TABLE_NAME+" ( "
                + COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_2 +" TEXT, " + COL_3 + " TEXT," + COL_4 +" TEXT, "+ COL_5 +" TEXT)" ;

        db.execSQL(CREATION_TABLE);

         CREATION_TABLE = "CREATE TABLE " +TABLE_Tennat+" ( "
                + COL_1T + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_2T +" TEXT," + COL_3T + " TEXT," + COL_4T +" TEXT, "+ COL_5T +" TEXT)" ;

        db.execSQL(CREATION_TABLE);
        CREATION_TABLE = "CREATE TABLE " + Land_Static + " (" + COL + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL2 + " INTEGER, " + COL3 + " TEXT)";

        db.execSQL(CREATION_TABLE);

        CREATION_TABLE = "CREATE TABLE " + Tenant_Static + " (" + COL1T + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL2T + " INTEGER, " +  COL3T + " TEXT, " + COL4T + " INTEGER)";

        db.execSQL(CREATION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    public void deleteDevice(String  Id) {
        // Get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COL_2+" = ?", new String[] { Id });
        db.close();
    }
    public String getStringValue ( int index,  String col , String database) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT  * FROM " + database;
        Cursor C = db.rawQuery(query, null);
        String dev = null;
        if (C.moveToFirst()) {
            do {
                dev =  C.getColumnName(index) ;
                if (dev.equals(col)) {
                    Log.d("mlog myDatabase " + col + " " , C.getString(index) + " at index  " + String.valueOf(index));
                    String v =  C.getString(index);
                    C.close();
                    db.close();
                    return v ;
                }
            } while (C.moveToNext());
        }
        Log.d("mlog myDatabase " + col , " not found");
        ContentValues values = new ContentValues();
        values.put(col, "");
        // insert
        db.insert(database,null, values);
        db.close();
      return null ;
    }

    public int getIntValue ( int index,  String col , String database) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT  * FROM " + database;
        Cursor C = db.rawQuery(query, null);
        String dev = null;
        if (C.moveToFirst()) {
            do {
                dev =  C.getColumnName(index) ;
                if (dev.equals(col)) {
                    Log.d("mlog myDatabase " + col + " " , String.valueOf(C.getInt(index)) + " at index  " + String.valueOf(index));
                    int v =  C.getInt(index);
                    C.close();
                    db.close();
                    return v ;
                }
            } while (C.moveToNext());
        }
        Log.d("mlog myDatabase " + col , " not found");
        ContentValues values = new ContentValues();
        values.put(col, 0);
        // insert
        db.insert(database,null, values);
        db.close();
        return 0 ;
    }


    //
    public void SaveInt(String database , int  val, String col ) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(col, val);
        String[] args = new String[]{col};
        try{
            //db.update(database, values );
            db.execSQL("UPDATE " +  database + " SET " + col + " =  " + val ) ;
            Log.d(debTag+ database , "value " + String.valueOf(val)+ " saved");
        }catch(SQLException ex){
            Log.d(debTag + " save int "+ database ,  " col "  + col + " error " + ex.toString() );
        }
        db.close();
    }

    public void SaveString(String database , String  val, String col ) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(col, val);
        String[] args = new String[]{col};
        try{
            //db.update(database, values );
            db.execSQL("UPDATE " +  database + " SET " + col + " =  '" + val+"' " ) ;
            Log.d(debTag+ database , "value " + val+ " saved");
        }catch(SQLException ex){
            Log.d(debTag + " save string"+ database , "save failed col " + col + " " + ex.toString());
        }
    }



//    public void addDevice(String Id, String label) {
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();
//        values.put(COL_2, Id);
//        values.put(COL_3, label);
//        Log.d("mlog added to database ", Id) ;
//        // insert
//        db.insert(TABLE_NAME,null, values);
//        db.close();
//    }
//
    // add device to landlord databse
    public void addDevice(String Id, String label , String moneyType, int mins) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_2, Id);
        values.put(COL_3, label);
        values.put(COL_4, moneyType);
        values.put(COL_5, mins);
        Log.d("mlog added to database ", Id) ;
        // insert
        db.insert(TABLE_NAME,null, values);
        db.close();
    }
    // add device to tennant database
    public void TaddDevice(String Id ,String label , String moneyType, int mins) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_2T, Id);
        values.put(COL_3T, label);
        values.put(COL_4T, moneyType);
        values.put(COL_5T, mins);
        Log.d("mlog added to database ", Id) ;
        // insert
        db.insert(TABLE_Tennat,null, values);
        db.close();
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from "+TABLE_NAME,null);
        return res;
    }

    // function to retrieve all devices from landlord database

    public ArrayList<Device> AllDevices() {

        ArrayList<Device> landDevices = new ArrayList<>() ;
        String query = "SELECT  * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Device dev = null;

        if (cursor.moveToFirst()) {
            do {
                dev = new Device(cursor.getString(1),cursor.getString(2),cursor.getString(3),cursor.getInt(4));
                landDevices.add(dev);
                Log.d("mlog database found ", dev.getId()) ;
            } while (cursor.moveToNext());

        }

        return landDevices;
    }

    // function to retrieve all devices from landlord database

    public ArrayList<Device> TenantDevices() {

        ArrayList<Device> tenantDevices = new ArrayList<>() ;
        String query = "SELECT  * FROM " + TABLE_Tennat ;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Device dev = null;

        if (cursor.moveToFirst()) {
            do {
                dev = new Device(cursor.getString(1),cursor.getString(2),cursor.getString(3),cursor.getInt(4));
                tenantDevices.add(dev);
                Log.d("mlog database found ", dev.getId()) ;
            } while (cursor.moveToNext());
        }

        return tenantDevices;
    }
  /*  public boolean deleteLandDevice(String name)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, COL_2 + "=?", new String[]{name}) > 0;
    }
*/

    public void deleteData () {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ TABLE_Tennat);
        db.close();
    }
}