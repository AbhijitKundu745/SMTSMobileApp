package com.psllab.smtsmobileapp.databases;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.psllab.smtsmobileapp.helper.AppConstants;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("Range")
public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 8;
    private static final String DATABASE_NAME = "PSL_SMTS-DB";

    private static final String TABLE_LOCATION_MASTER = "Location_Master_Table";
    private static final String TABLE_ASSET_MASTER = "Asset_Master_Table";
    private static final String TABLE_AUTOCLAVE_LOCATION_MASTER = "Autoclave_Location_Master_Table";

    private static final String K_LOCATION_ID = "K_LOCATION_ID";
    private static final String K_LOCATION_NAME = "K_LOCATION_NAME";
    private static final String K_ASSET_ID = "K_ASSET_ID";
    private static final String K_ASSET_EPC = "K_ASSET_EPC";
    private static final String K_ASSET_NAME = "K_ASSET_NAME";
    private static final String K_ASSET_STATUS = "K_ASSET_STATUS";
    private static final String K_ASSET_SRNO = "K_ASSET_SRNO";
    private static final String K_ASSET_TAGID = "K_ASSET_TAGID";
    private static final String K_ASSET_DESC = "K_ASSET_DESC";
    private static final String K_ASSET_GROUP_CODE = "K_ASSET_GROUP_CODE";
    private static final String K_ASSET_CATEGORY_ID = "K_ASSET_CATEGORY_ID";
    private static final String K_ASSET_LEVEL = "K_ASSET_LEVEL";
    private static final String K_ASSET_CREATED_ON = "K_ASSET_CREATED_ON";
    private static final String K_ASSET_OUT_LIFE = "K_ASSET_OUT_LIFE";


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //3rd argument to be passed is CursorFactory instance
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_LOCATION_MASTER_TABLE = "CREATE TABLE "
                + TABLE_LOCATION_MASTER
                + "("
                + K_LOCATION_ID + " TEXT UNIQUE,"//0
                + K_LOCATION_NAME + " TEXT"//1

                + ")";


        String CREATE_AUTOCLAVE_LOCATION_MASTER_TABLE = "CREATE TABLE "
                + TABLE_AUTOCLAVE_LOCATION_MASTER
                + "("
                + K_LOCATION_ID + " TEXT UNIQUE,"//0
                + K_LOCATION_NAME + " TEXT"//1

                + ")";

        String CREATE_ASSET_MASTER_TABLE = "CREATE TABLE "
                + TABLE_ASSET_MASTER
                + "("
                + K_ASSET_ID + " TEXT UNIQUE,"//0
                + K_ASSET_NAME + " TEXT,"//1
                + K_ASSET_SRNO + " TEXT,"//1
                + K_ASSET_TAGID + " TEXT,"//1
                + K_ASSET_DESC + " TEXT,"//1
                + K_ASSET_GROUP_CODE + " TEXT,"//1
                + K_ASSET_CATEGORY_ID + " TEXT,"//1
                + K_ASSET_LEVEL + " TEXT,"//1
                + K_ASSET_CREATED_ON + " TEXT,"//1
                + K_ASSET_OUT_LIFE + " TEXT"//1

                + ")";

        db.execSQL(CREATE_LOCATION_MASTER_TABLE);
        db.execSQL(CREATE_ASSET_MASTER_TABLE);
        db.execSQL(CREATE_AUTOCLAVE_LOCATION_MASTER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION_MASTER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ASSET_MASTER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUTOCLAVE_LOCATION_MASTER);
        // Create tables again
        onCreate(db);
    }

    public void deleteAssetMaster() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ASSET_MASTER, null, null);
        db.close();
    }

    public void deleteLocationMaster() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOCATION_MASTER, null, null);
        db.close();
    }
    public void deleteAutoclaveLocationMaster() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_AUTOCLAVE_LOCATION_MASTER, null, null);
        db.close();
    }


    public void storeAssetMaster(List<AssetMaster> lst) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "INSERT OR REPLACE INTO Asset_Master_Table (K_ASSET_ID,K_ASSET_NAME,K_ASSET_SRNO,K_ASSET_TAGID,K_ASSET_DESC,K_ASSET_GROUP_CODE,K_ASSET_CATEGORY_ID,K_ASSET_LEVEL,K_ASSET_CREATED_ON,K_ASSET_OUT_LIFE) VALUES (? ,? ,? ,? ,? ,? ,? ,? ,? ,?)";
        //db.beginTransaction();
        db.beginTransactionNonExclusive();
        SQLiteStatement stmt = db.compileStatement(sql);
        try {
            for (int i = 0; i < lst.size(); i++) {
                stmt.bindString(1, lst.get(i).getAssetId());
                stmt.bindString(2, lst.get(i).getAssetName());
                stmt.bindString(3, lst.get(i).getAssetSrNo());
                stmt.bindString(4, lst.get(i).getAssetTagId());
                stmt.bindString(5, lst.get(i).getAssetDesc());
                stmt.bindString(6, lst.get(i).getAssetGroupCode());
                stmt.bindString(7, lst.get(i).getAssetCategoryId());
                stmt.bindString(8, lst.get(i).getAssetLevel());
                stmt.bindString(9, lst.get(i).getAssetCreatedOn());
                stmt.bindString(10, lst.get(i).getAssetOutLife());
                stmt.execute();
                stmt.clearBindings();
            }
            db.setTransactionSuccessful();
            // db.endTransaction();
        } catch (Exception e) {
            Log.e("ASSETMASTERSTOREEXC", e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }


    public void storeLocationMaster(List<LocationMaster> lst) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "INSERT OR REPLACE INTO Location_Master_Table (K_LOCATION_ID,K_LOCATION_NAME) VALUES (? ,?)";
        //db.beginTransaction();
        db.beginTransactionNonExclusive();
        SQLiteStatement stmt = db.compileStatement(sql);
        try {
            for (int i = 0; i < lst.size(); i++) {
                stmt.bindString(1, lst.get(i).getLocationId());
                stmt.bindString(2, lst.get(i).getLocationName());
                // stmt.bindString(3, lst.get(i).getLocationRfid());
                //stmt.bindString(5, lst.get(i).getIsAssetActive());
                stmt.execute();
                stmt.clearBindings();
            }
            db.setTransactionSuccessful();
            // db.endTransaction();
        } catch (Exception e) {
            Log.e("MASTERSTOREEXC", e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }
    public void storeAutoclaveLocationMaster(List<LocationMaster> lst) {
        SQLiteDatabase db = this.getWritableDatabase();
        String sql = "INSERT OR REPLACE INTO Autoclave_Location_Master_Table (K_LOCATION_ID,K_LOCATION_NAME) VALUES (? ,?)";
        //db.beginTransaction();
        db.beginTransactionNonExclusive();
        SQLiteStatement stmt = db.compileStatement(sql);
        try {
            for (int i = 0; i < lst.size(); i++) {
                stmt.bindString(1, lst.get(i).getLocationId());
                stmt.bindString(2, lst.get(i).getLocationName());
                // stmt.bindString(3, lst.get(i).getLocationRfid());
                //stmt.bindString(5, lst.get(i).getIsAssetActive());
                stmt.execute();
                stmt.clearBindings();
            }
            db.setTransactionSuccessful();
            // db.endTransaction();
        } catch (Exception e) {
            Log.e("MASTERSTOREEXC", e.getMessage());
        } finally {
            if (db != null) {
                db.endTransaction();
            }
        }
    }
    public int getAssetMasterCount() {
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT  " + K_ASSET_ID + "," + " count(*) " + " FROM " + TABLE_ASSET_MASTER;
        Cursor cursor = db.rawQuery(selectQuery, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            do {
                count = count + cursor.getInt(1);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getLocationMasterCount() {
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT  " + K_LOCATION_ID + "," + " count(*) " + " FROM " + TABLE_LOCATION_MASTER;
        Cursor cursor = db.rawQuery(selectQuery, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            do {
                count = count + cursor.getInt(1);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return count;
    }
    public int getAutoclaveLocationMasterCount() {
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT  " + K_LOCATION_ID + "," + " count(*) " + " FROM " + TABLE_AUTOCLAVE_LOCATION_MASTER;
        Cursor cursor = db.rawQuery(selectQuery, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            do {
                count = count + cursor.getInt(1);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return count;
    }

    public String getLocationNameByLocationId(String assetid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOCATION_MASTER, new String[]{K_LOCATION_NAME}, K_LOCATION_ID + "='" + assetid + "'", null, null, null, null);
        System.out.println("Cursor" + cursor.getCount());
        try {
            if (cursor == null || cursor.getCount() == 0) {
                return AppConstants.UNKNOWN_ASSET;
            } else {
                if (cursor.getCount() > 0) {
                    //PID Note
                    cursor.moveToFirst();
                    return cursor.getString(cursor.getColumnIndex(K_LOCATION_NAME));
                } else {
                    //PID Not Found
                    return AppConstants.UNKNOWN_ASSET;
                }
            }
        } catch (Exception e) {
            return AppConstants.UNKNOWN_ASSET;
        } finally {
            cursor.close();
        }
    }

    public String getAssetNameByTagId(String assettagid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ASSET_MASTER, new String[]{K_ASSET_NAME}, K_ASSET_TAGID + "='" + assettagid + "'", null, null, null, null);
        System.out.println("Cursor" + cursor.getCount());
        try {
            if (cursor == null || cursor.getCount() == 0) {
                return AppConstants.UNKNOWN_ASSET;
            } else {
                if (cursor.getCount() > 0) {
                    //PID Note
                    cursor.moveToFirst();
                    return cursor.getString(cursor.getColumnIndex(K_ASSET_NAME));
                } else {
                    //PID Not Found
                    return AppConstants.UNKNOWN_ASSET;
                }
            }
        } catch (Exception e) {
            return AppConstants.UNKNOWN_ASSET;
        } finally {
            cursor.close();
        }
    }

    public String getLocationIdByLocationName(String assetName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOCATION_MASTER, new String[]{K_LOCATION_ID}, K_LOCATION_NAME + "='" + assetName + "'", null, null, null, null);
        System.out.println("Cursor" + cursor.getCount());
        Log.e("Cursor",""+cursor.getCount());
        try {
            if (cursor == null || cursor.getCount() == 0) {
                return AppConstants.UNKNOWN_ASSET;
            } else {
                if (cursor.getCount() > 0) {
                    //PID Note
                    cursor.moveToFirst();
                    return cursor.getString(cursor.getColumnIndex(K_LOCATION_ID));
                } else {
                    //PID Not Found
                    return AppConstants.UNKNOWN_ASSET;
                }
            }
        } catch (Exception e) {
            Log.e("LOCIDDBEXC",""+e.getMessage());
            return AppConstants.UNKNOWN_ASSET;
        } finally {
            cursor.close();
        }
    }

    public String getAutoclaveLocationIdByLocationName(String assetName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_AUTOCLAVE_LOCATION_MASTER, new String[]{K_LOCATION_ID}, K_LOCATION_NAME + "='" + assetName + "'", null, null, null, null);
        System.out.println("Cursor" + cursor.getCount());
        Log.e("Cursor",""+cursor.getCount());
        try {
            if (cursor == null || cursor.getCount() == 0) {
                return AppConstants.UNKNOWN_ASSET;
            } else {
                if (cursor.getCount() > 0) {
                    //PID Note
                    cursor.moveToFirst();
                    return cursor.getString(cursor.getColumnIndex(K_LOCATION_ID));
                } else {
                    //PID Not Found
                    return AppConstants.UNKNOWN_ASSET;
                }
            }
        } catch (Exception e) {
            Log.e("LOCIDDBEXC",""+e.getMessage());
            return AppConstants.UNKNOWN_ASSET;
        } finally {
            cursor.close();
        }
    }

    public ArrayList<String> getAllLocationsForSearchSpinner() {
        ArrayList<String> searchogs = new ArrayList<String>();
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT  * FROM " + TABLE_LOCATION_MASTER;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                //AppConstants.ASSET_TYPE_SPLIT_DATA
                 String assetname = cursor.getString(cursor.getColumnIndex(K_LOCATION_NAME));
                String assetid = cursor.getString(cursor.getColumnIndex(K_LOCATION_ID));
                //searchogs.add(assetname+ AppConstants.ASSET_TYPE_SPLIT_DATA+assetid);
                searchogs.add(assetname);
            } while (cursor.moveToNext());
        }
        return searchogs;
    }

    public ArrayList<String> getAllAutoclaveLocationsForSearchSpinner() {
        ArrayList<String> searchogs = new ArrayList<String>();
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT  * FROM " + TABLE_AUTOCLAVE_LOCATION_MASTER;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                //AppConstants.ASSET_TYPE_SPLIT_DATA
                String assetname = cursor.getString(cursor.getColumnIndex(K_LOCATION_NAME));
                String assetid = cursor.getString(cursor.getColumnIndex(K_LOCATION_ID));
                //searchogs.add(assetname+ AppConstants.ASSET_TYPE_SPLIT_DATA+assetid);
                searchogs.add(assetname);
            } while (cursor.moveToNext());
        }
        return searchogs;
    }

}

