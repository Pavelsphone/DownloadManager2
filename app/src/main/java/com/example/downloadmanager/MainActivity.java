package com.example.downloadmanager;
import static android.content.Context.RECEIVER_EXPORTED;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import com.example.downloadmanager.adapter.DownloadAdapter;
import com.example.downloadmanager.db.AppDatabase;
import com.example.downloadmanager.db.DownloadItem;
import com.jakewharton.rxbinding4.widget.RxTextView;
import java.util.concurrent.TimeUnit;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private DownloadManager downloadManager;
    private long downloadId;
    private AppDatabase db;
    private DownloadAdapter adapter;
    private CompositeDisposable disposables = new CompositeDisposable();

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        db = Room.databaseBuilder(this, AppDatabase.class, "downloads-db").build();

        EditText editTextUrl = findViewById(R.id.editTextUrl);
        EditText editTextSearch = findViewById(R.id.editTextSearch);
        Button buttonDownload = findViewById(R.id.buttonDownload);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        adapter = new DownloadAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        buttonDownload.setOnClickListener(v -> startDownload(editTextUrl.getText().toString()));

        disposables.add(
                RxTextView.textChanges(editTextSearch)
                        .debounce(300, TimeUnit.MILLISECONDS)
                        .switchMap(query -> db.downloadDao().searchItems(query.toString()).toObservable()) // Преобразование Flowable -> Observable
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(adapter::setItems, Throwable::printStackTrace)
        );

        db.downloadDao().getAllItems()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(adapter::setItems, Throwable::printStackTrace);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    RECEIVER_EXPORTED);
        } else {
            registerReceiver(onDownloadComplete,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }


    }

    private void startDownload(String url) {
        if (url.isEmpty()) {
            Toast.makeText(this, "Введите URL", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setTitle("Загрузка MP3");
        request.setDescription("Файл загружается...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "downloaded_file.mp3");
        request.setMimeType("audio/mpeg");

        downloadId = downloadManager.enqueue(request);

        db.downloadDao().insert(new DownloadItem(url, "downloaded_file.mp3"))
                .subscribeOn(Schedulers.io())
                .subscribe();

        Toast.makeText(this, "Загрузка началась", Toast.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == id) {
                Toast.makeText(context, "Загрузка завершена", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
        disposables.clear();
    }
}

