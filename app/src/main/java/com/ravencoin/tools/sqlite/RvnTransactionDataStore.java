package com.ravencoin.tools.sqlite;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/25/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.ravencoin.presenter.entities.BRTransactionEntity;
import com.ravencoin.tools.manager.BRReportsManager;
import com.ravencoin.tools.util.BRConstants;

import java.util.ArrayList;
import java.util.List;

public class RvnTransactionDataStore implements DataSourceInterface {
    private static final String TAG = RvnTransactionDataStore.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private final RSQLiteHelper dbHelper;
    public static final String[] allColumns = {
            RSQLiteHelper.TX_COLUMN_ID,
            RSQLiteHelper.TX_BUFF,
            RSQLiteHelper.TX_BLOCK_HEIGHT,
            RSQLiteHelper.TX_TIME_STAMP,
            RSQLiteHelper.TX_ISO
    };

    private static RvnTransactionDataStore instance;

    public static RvnTransactionDataStore getInstance(Context context) {
        if (instance == null) {
            instance = new RvnTransactionDataStore(context);
        }
        return instance;
    }

    private RvnTransactionDataStore(Context context) {
        dbHelper = RSQLiteHelper.getInstance(context);

    }

    public BRTransactionEntity putTransaction(Context app, String iso, BRTransactionEntity transactionEntity) {

        Log.e(TAG, "putTransaction: " + transactionEntity.getTxISO() + ":" + transactionEntity.getTxHash() + ", b:" + transactionEntity.getBlockheight() + ", t:" + transactionEntity.getTimestamp());
        Cursor cursor = null;
        try {
            database = openDatabase();
            ContentValues values = new ContentValues();
            values.put(RSQLiteHelper.TX_COLUMN_ID, transactionEntity.getTxHash());
            values.put(RSQLiteHelper.TX_BUFF, transactionEntity.getBuff());
            values.put(RSQLiteHelper.TX_BLOCK_HEIGHT, transactionEntity.getBlockheight());
            values.put(RSQLiteHelper.TX_ISO, iso.toUpperCase());
            values.put(RSQLiteHelper.TX_TIME_STAMP, transactionEntity.getTimestamp());

            database.beginTransaction();
            database.insert(RSQLiteHelper.TX_TABLE_NAME, null, values);
            cursor = database.query(RSQLiteHelper.TX_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            cursor.moveToFirst();
            BRTransactionEntity transactionEntity1 = cursorToTransaction(app, iso.toUpperCase(), cursor);

            database.setTransactionSuccessful();
//            for (OnTxAdded listener : listeners) {
//                if (listener != null) listener.onTxAdded("RVN");
//            }
            return transactionEntity1;
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
            closeDatabase();
            if (cursor != null) cursor.close();
        }
        return null;


    }

    public void deleteAllTransactions(Context app, String iso) {
        try {
            database = openDatabase();

            database.delete(RSQLiteHelper.TX_TABLE_NAME, RSQLiteHelper.TX_ISO + "=?", new String[]{iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    public List<BRTransactionEntity> getAllTransactions(Context app, String iso) {
        List<BRTransactionEntity> transactions = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(RSQLiteHelper.TX_TABLE_NAME,
                    allColumns, RSQLiteHelper.TX_ISO + "=?", new String[]{iso.toUpperCase()}, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BRTransactionEntity transactionEntity = cursorToTransaction(app, iso.toUpperCase(), cursor);
                transactions.add(transactionEntity);
                cursor.moveToNext();
            }

        } finally {
            closeDatabase();
            if (cursor != null)
                cursor.close();
            printTest(app, iso);
        }
        return transactions;
    }


    public static BRTransactionEntity cursorToTransaction(Context app, String iso, Cursor cursor) {
        return new BRTransactionEntity(cursor.getBlob(1), cursor.getInt(2), cursor.getLong(3), cursor.getString(0), iso.toUpperCase());
    }

    public boolean updateTransaction(Context app, String iso, BRTransactionEntity tx) {
        Log.e(TAG, "updateTransaction: " + tx.getTxISO() + ":" + tx.getTxHash() + ", b:" + tx.getBlockheight() + ", t:" + tx.getTimestamp());

        try {
            database = openDatabase();
            ContentValues args = new ContentValues();
            args.put(RSQLiteHelper.TX_BLOCK_HEIGHT, tx.getBlockheight());
            args.put(RSQLiteHelper.TX_TIME_STAMP, tx.getTimestamp());

//            Log.e(TAG, "updateTransaction: size before updating: " + getAllTransactions().size());
            int r = database.update(RSQLiteHelper.TX_TABLE_NAME, args, "_id=? AND " + RSQLiteHelper.TX_ISO + "=?", new String[]{tx.getTxHash(), iso.toUpperCase()});
//            Log.e(TAG, "updateTransaction: size after updating: " + getAllTransactions().size());
            if (r > 0)
                Log.e(TAG, "transaction updated with id: " + tx.getTxHash());
            else Log.e(TAG, "updateTransaction: Warning: r:" + r);

            return true;
        } finally {
            closeDatabase();
        }

    }

    public void deleteTxByHash(Context app, String iso, String hash) {
        try {
            database = openDatabase();
            Log.e(TAG, "transaction deleted with id: " + hash);
            database.delete(RSQLiteHelper.TX_TABLE_NAME,
                    "_id=? AND " + RSQLiteHelper.TX_ISO + "=?", new String[]{hash, iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    @Override
    public SQLiteDatabase openDatabase() {
//        if (mOpenCounter.incrementAndGet() == 1) {
        // Opening new database
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WAL);
//        }
//        Log.d("Database open counter: ",  String.valueOf(mOpenCounter.get()));
        return database;
    }

    @Override
    public void closeDatabase() {

//        if (mOpenCounter.decrementAndGet() == 0) {
//            // Closing database
//            database.close();

//        }
//        Log.d("Database open counter: " , String.valueOf(mOpenCounter.get()));
    }

    private void printTest(Context app, String iso) {
        Cursor cursor = null;
        try {
            database = openDatabase();
            StringBuilder builder = new StringBuilder();

            cursor = database.query(RSQLiteHelper.TX_TABLE_NAME,
                    allColumns, null, null, null, null, null);
            builder.append("Total: " + cursor.getCount() + "\n");
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                BRTransactionEntity ent = cursorToTransaction(app, iso.toUpperCase(), cursor);
                builder.append("ISO:" + ent.getTxISO() + ", Hash:" + ent.getTxHash() + ", blockHeight:" + ent.getBlockheight() + ", timeStamp:" + ent.getTimestamp() + "\n");
            }
            Log.e(TAG, "printTest: " + builder.toString());
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }
    }
}