package com.example.downloadmanager.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloads")
public class DownloadItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String url;
    public String fileName;
    public long timestamp;

    public DownloadItem(String url, String fileName) {
        this.url = url;
        this.fileName = fileName;
        this.timestamp = System.currentTimeMillis();
    }
}

