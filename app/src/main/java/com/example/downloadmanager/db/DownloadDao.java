package com.example.downloadmanager.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;

@Dao
public interface DownloadDao {
    @Insert
    Completable insert(DownloadItem item);

    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    Flowable<List<DownloadItem>> getAllItems();

    @Query("SELECT * FROM downloads WHERE fileName LIKE '%' || :query || '%'")
    Flowable<List<DownloadItem>> searchItems(String query);
}

