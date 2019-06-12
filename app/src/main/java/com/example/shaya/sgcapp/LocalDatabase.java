package com.example.shaya.sgcapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

public class LocalDatabase extends SQLiteOpenHelper {

    public static final String databaseName = "sgcAppV2_keys.db";
    public static final String tableName = "keyGen";

    public static final String groupId = "GroupId";
    public static final String i = "version";
    public static final String s = "s";
    public static final String t = "t";

    public LocalDatabase(@Nullable Context context) {
        super(context, databaseName, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("create table "+tableName+"("+groupId+" TEXT PRIMARY KEY, "+i+" TEXT, "+s+" Text, "+t+" Text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS "+tableName);
        onCreate(db);
    }

    public boolean insertData(String group_Id, String version, String s0, String t0)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(groupId, group_Id);
        contentValues.put(i, version);
        contentValues.put(s, s0);
        contentValues.put(t, t0);


        long result = db.replace(tableName, null, contentValues);
        if(result != -1)
        {
            return true;
        }
        else
            return false;
    }

    public Cursor getData(String group_Id)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        // result = db.rawQuery("select * from "+tableName+" where "+groupId+" = "+group_Id+"", null);
        Cursor result = db.rawQuery("select * from "+tableName+"",null);
        return  result;
    }

    public boolean deleteData(String grpId)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete(tableName, groupId + "=" + grpId, null) > 0;
    }
}
