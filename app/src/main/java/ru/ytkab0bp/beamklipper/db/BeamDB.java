package ru.ytkab0bp.beamklipper.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.ytkab0bp.beamklipper.InstanceIcon;
import ru.ytkab0bp.beamklipper.KlipperApp;
import ru.ytkab0bp.beamklipper.KlipperInstance;
import ru.ytkab0bp.beamklipper.cloud.CloudAPI;
import ru.ytkab0bp.beamklipper.events.InstanceCreatedEvent;
import ru.ytkab0bp.beamklipper.events.InstanceDestroyedEvent;
import ru.ytkab0bp.beamklipper.events.InstanceUpdatedEvent;

public class BeamDB extends SQLiteOpenHelper {
    private final static String DB_NAME = "beam.db";
    private final static int VERSION = 2;

    private final static String TABLE_INSTANCES     = "instances";
    private final static String COLUMN_ID           = "id";
    private final static String COLUMN_NAME         = "name";
    private final static String COLUMN_ICON         = "icon";
    private final static String COLUMN_AUTOSTART    = "autostart";
    private final static String COLUMN_REMOTE_ID    = "remote_id";
    private final static String COLUMN_REMOTE_TOKEN = "remote_token";

    public BeamDB(@Nullable Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s (%s TEXT, %s TEXT, %s TEXT, %s INTEGER, %s TEXT, %s TEXT)", TABLE_INSTANCES,
                COLUMN_ID, COLUMN_NAME, COLUMN_ICON, COLUMN_AUTOSTART, COLUMN_REMOTE_ID, COLUMN_REMOTE_TOKEN));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT", TABLE_INSTANCES, COLUMN_REMOTE_ID));
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s TEXT", TABLE_INSTANCES, COLUMN_REMOTE_TOKEN));
        }
    }

    public List<KlipperInstance> getInstances() {
        List<KlipperInstance> instances = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(String.format("SELECT * FROM %s", TABLE_INSTANCES), null);
        ContentValues cv = new ContentValues();
        while (c.moveToNext()) {
            DatabaseUtils.cursorRowToContentValues(c, cv);
            KlipperInstance inst = new KlipperInstance();
            inst.id = cv.getAsString(COLUMN_ID);
            inst.name = cv.getAsString(COLUMN_NAME);
            inst.icon = InstanceIcon.byKey(cv.getAsString(COLUMN_ICON));
            inst.autostart = "1".equals(cv.get(COLUMN_AUTOSTART));
            inst.remoteId = cv.containsKey(COLUMN_REMOTE_ID) ? cv.getAsString(COLUMN_REMOTE_ID) : null;
            inst.remoteToken = cv.containsKey(COLUMN_REMOTE_TOKEN) ? cv.getAsString(COLUMN_REMOTE_TOKEN) : null;
            instances.add(inst);
        }
        c.close();
        return instances;
    }

    public void insert(KlipperInstance inst) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, inst.id);
        cv.put(COLUMN_NAME, inst.name);
        cv.put(COLUMN_ICON, inst.icon.name());
        cv.put(COLUMN_AUTOSTART, inst.autostart);
        cv.put(COLUMN_REMOTE_ID, inst.remoteId);
        cv.put(COLUMN_REMOTE_TOKEN, inst.remoteToken);
        getWritableDatabase().insert(TABLE_INSTANCES, null, cv);
        KlipperInstance.onInstancesLoadedFromDB(getInstances());
        KlipperApp.EVENT_BUS.fireEvent(new InstanceCreatedEvent(inst.id));
    }

    public void update(KlipperInstance inst) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, inst.id);
        cv.put(COLUMN_NAME, inst.name);
        cv.put(COLUMN_ICON, inst.icon.name());
        cv.put(COLUMN_AUTOSTART, inst.autostart);
        cv.put(COLUMN_REMOTE_ID, inst.remoteId);
        cv.put(COLUMN_REMOTE_TOKEN, inst.remoteToken);
        getWritableDatabase().update(TABLE_INSTANCES, cv, "id = ?", new String[]{inst.id});
        KlipperInstance.onInstancesLoadedFromDB(getInstances());
        KlipperApp.EVENT_BUS.fireEvent(new InstanceUpdatedEvent(inst.id));
    }

    private static void deleteRecur(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles()) {
                deleteRecur(c);
            }
        }
        f.delete();
    }

    public void delete(KlipperInstance inst) {
        if (inst.remoteId != null) {
            CloudAPI.INSTANCE.remoteDeletePrinter(inst.remoteId, response -> {});
        }
        getWritableDatabase().delete(TABLE_INSTANCES, "id = ?", new String[]{inst.id});
        KlipperInstance.onInstancesLoadedFromDB(getInstances());
        deleteRecur(inst.getDirectory());
        KlipperApp.EVENT_BUS.fireEvent(new InstanceDestroyedEvent(inst.id));
    }
}
