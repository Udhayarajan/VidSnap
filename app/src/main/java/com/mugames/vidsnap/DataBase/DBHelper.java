package com.mugames.vidsnap.DataBase;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import androidx.annotation.Nullable;

import com.mugames.vidsnap.Utility.Bundles.HistoryDetails;

import static com.mugames.vidsnap.Utility.UtilityClass.bitmapToBytes;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        //Name,src,date,size,uri,image,image_width,image_height

        queryData("CREATE TABLE IF NOT exists HISTORY (Count INTEGER primary key autoincrement," +
                "name VARCHAR, fileType VARCHAR, src VARCHAR, date VARCHAR, size VARCHAR, uri VARCHAR, thumbnail BLOB, width LONG, height LONG )");
    }

    public void queryData(String sql){
        SQLiteDatabase database = getWritableDatabase();
        database.execSQL(sql);
    }

    public void insertItem(HistoryDetails details){
        SQLiteDatabase database =getWritableDatabase();
        //Name,type,src,date,size,uri,image,image_width,image_height
        //size will be in long but string (1000 for 1Bytes)
        String sql = "INSERT INTO HISTORY values(NULL,?,?,?,?,?,?,?,?,?) ";
        SQLiteStatement statement = database.compileStatement(sql);

        statement.bindString(1,details.getDownloadDetails().fileName);
        statement.bindString(2,details.getDownloadDetails().fileType);
        statement.bindString(3,details.getDownloadDetails().src);
        statement.bindString(4,details.getDate());
        statement.bindLong(5,details.getDownloadDetails().videoSize);
        statement.bindString(6,details.getUri()+"");
        statement.bindBlob(7,bitmapToBytes(details.getDownloadDetails().thumbNail));
        statement.bindLong(8,details.getDownloadDetails().thumbNail.getWidth());
        statement.bindLong(9,details.getDownloadDetails().thumbNail.getHeight());

        statement.executeInsert();

    }

    public Cursor getData(String sql){
        return getReadableDatabase().rawQuery(sql,null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
