package year2013.ifmo.photogallery;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private ImageAdapter adapter;
    private ProgressBar progressBar;
    private GridView gridView;
    Handler handler;

    private Intent intent;

    private int curPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        IntentFilter mStatusIntentFilter = new IntentFilter(
                ImageIntentService.BROADCAST_ACTION);
        ResponseReceiver responseReceiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(responseReceiver, mStatusIntentFilter);

        gridView = (GridView) findViewById(R.id.gridView);
        adapter = new ImageAdapter(this, new ArrayList<Image>());
        gridView.setAdapter(adapter);
        intent = new Intent(this, FullScreenImageActivity.class);
        gridView.setOnItemClickListener(listener);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                progressBar.setProgress(msg.what);
            }
        };

        curPage = 0;

        getLoaderManager().initLoader(0, null, this);
        startLoading();
    }

    private final AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v,
                                int position, long id) {
            Image image = adapter.getItem(position);
            intent.putExtra(FullScreenImageActivity.EXTRA_LARGE, image.largeUrl);
            intent.putExtra(FullScreenImageActivity.EXTRA_ORIG, image.origUrl);
            intent.putExtra(FullScreenImageActivity.EXTRA_TITLE, image.title);
            intent.putExtra(FullScreenImageActivity.EXTRA_ID, image.id);
            startActivity(intent);
        }
    };

    private void startLoading() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        Intent intent = new Intent(this, ImageIntentService.class);
        intent.setAction(ImageIntentService.ACTION_ALL);
        startService(intent);
    }

    static final String[] SUMMARY_PROJECTION = new String[] {
            Gallery.Images._ID,
            Gallery.Images.SMALL_IMAGE,
            Gallery.Images.LARGE_IMAGE_URL,
            Gallery.Images.ORIG_IMAGE_URL,
            Gallery.Images.LARGE_PATH_NAME,
            Gallery.Images.TITLE
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri = Gallery.Images.CONTENT_URI;

        return new CursorLoader(getBaseContext(), baseUri,
                SUMMARY_PROJECTION, null, null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        try {
            adapter.clear();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                byte[] bytes = cursor.getBlob(cursor.getColumnIndex(Gallery.Images.SMALL_IMAGE));
                ByteArrayInputStream imageStream = new ByteArrayInputStream(bytes);
                Bitmap theImage = BitmapFactory.decodeStream(imageStream);
                theImage = Bitmap.createScaledBitmap(theImage, 200, 200, true);
                Image image = new Image();
                image.bitmap = theImage;
                image.largeUrl = cursor.getString(cursor.getColumnIndex(Gallery.Images.LARGE_IMAGE_URL));
                image.origUrl = cursor.getString(cursor.getColumnIndex(Gallery.Images.ORIG_IMAGE_URL));
                image.id = cursor.getLong(cursor.getColumnIndex(Gallery.Images._ID));
                image.title = cursor.getString(cursor.getColumnIndex(Gallery.Images.TITLE));
                adapter.addImage(image);
                cursor.moveToNext();
            }
            cursor.close();
            curPage = 0;
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter = new ImageAdapter(this, new ArrayList<Image>());
        gridView.setAdapter(adapter);
    }

    private class ResponseReceiver extends BroadcastReceiver {
        private ResponseReceiver() {}

        public void onReceive(Context context, Intent intent) {
            final int progress = intent.getIntExtra(ImageIntentService.EXTRA_PROGRESS, -1);
            handler.sendMessage(handler.obtainMessage(progress));
            if (progress == 100) {
                progressBar.setVisibility(View.GONE);
                getLoaderManager().restartLoader(0, null, MainActivity.this);
            }
        }
    }

    public void updateAll(View view) {
        startLoading();
    }

    public void prevPage(View view) {
        if (curPage > 0) {
            adapter.setPage(--curPage);
        }
    }

    public void nextPage(View view) {
        if (adapter.canSet(curPage + 1)) {
            adapter.setPage(++curPage);
        }
    }
}