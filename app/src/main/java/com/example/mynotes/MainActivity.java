package com.example.mynotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mynotes.Adapters.RecyclerviewAdapter;
import com.example.mynotes.Database.AppDatabase;
import com.example.mynotes.Database.AppExecutors;
import com.example.mynotes.Models.Note;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private AppDatabase appDb;
    private RecyclerviewAdapter adapter;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private File[] files;

    private FloatingActionButton add, addNote, addRecording;
    private boolean buttonsVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emptyView = findViewById(R.id.empty_view);

        add = findViewById(R.id.add_btn);
        addNote = findViewById(R.id.add_note_button);
        addRecording = findViewById(R.id.add_recording_button);
        add.setOnClickListener(this);
        addNote.setOnClickListener(this);
        addRecording.setOnClickListener(this);

        adapter = new RecyclerviewAdapter(this);
        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && add.getVisibility() == View.VISIBLE) {
                    add.hide();
                    animations(false);
                    buttonsVisible = true;
                    Log.e(LOG_TAG, "On Scrolling : " + true);
                }
                else if (dy < 0 && add.getVisibility() != View.VISIBLE) {
                    add.show();
                }
            }
        });

        appDb = AppDatabase.getInstance(getApplicationContext());
    }//END OF onCreate

    private void deleteAllNotes(){
        if (adapter.getItemCount() == 0){
            Toast.makeText(MainActivity.this, "No notes to delete", Toast.LENGTH_SHORT).show();
        }
        else {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(getResources().getString(R.string.delete_dialog_title))
                    .setMessage(getResources().getString(R.string.delete_dialog_message))
                    .setCancelable(false)
                    .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton(getResources().getString(R.string.delete_all_btn), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                @Override
                                public void run() {
                                    final List<Note> notes = appDb.notesDao().loadAllNotes();
                                    appDb.notesDao().deleteAllNotes(notes);
                                    final List<Note> newNotes = appDb.notesDao().loadAllNotes();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (files.length > 0) {
                                                for (File file : files) {
                                                    file.delete();
                                                }
                                            }
                                            adapter.setTasks(newNotes);
                                            Toast.makeText(MainActivity.this, "Deleted all notes", Toast.LENGTH_SHORT).show();
                                            emptyView.setVisibility(View.VISIBLE);
                                            recyclerView.setVisibility(View.GONE);
                                            if(add.getVisibility() != View.VISIBLE) {
                                                add.show();
                                            }
                                            dialog.dismiss();
                                        }
                                    });
                                }
                            });
                        }
                    });
            alert.show();
        }
    }//END OF deleteAllNotes

    private void retrieveNotes() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final List<Note> notes = appDb.notesDao().loadAllNotes();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.setTasks(notes);
                        if (adapter.getItemCount() == 0) {
                            emptyView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                        else {
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });
    }//END OF retrieveNotes

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(LOG_TAG, "onResume");
        retrieveNotes();
        String path = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath();
        files = new File(path).listFiles();
        Log.e(LOG_TAG, "Number of files : " + files.length);
        if (!buttonsVisible) {
            animations(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_all:
                deleteAllNotes();
                break;
            case R.id.settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void animations(boolean visible){
        if (visible) {
            addNote.show();
            addRecording.show();
            add.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                add.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorIcons)));
            }
        }
        else {
            add.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_input_add));
            addNote.hide();
            addRecording.hide();
        }

    }//END OF animations

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.add_btn:
                Log.e(LOG_TAG, "On button click : " + buttonsVisible);
                animations(buttonsVisible);
                buttonsVisible = !buttonsVisible;
                break;
            case R.id.add_note_button:
                startActivity(new Intent(MainActivity.this, NoteActivity.class));
                break;
            case R.id.add_recording_button:
                startActivity(new Intent(MainActivity.this, RecordingActivity.class));
                break;
            default:
                break;
        }

    }
}//end of public  class
