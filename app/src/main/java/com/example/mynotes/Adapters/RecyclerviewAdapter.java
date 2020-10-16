package com.example.mynotes.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mynotes.Models.Note;
import com.example.mynotes.NoteActivity;
import com.example.mynotes.R;
import com.example.mynotes.RecordingActivity;

import java.util.List;

public class RecyclerviewAdapter extends RecyclerView.Adapter<RecyclerviewAdapter.MyViewHolder> {
    private static final String LOG_TAG = RecyclerviewAdapter.class.getSimpleName();
    private List<Note> notesList;
    private Context context;

    public RecyclerviewAdapter(Context context) {
        this.context = context;
    }

    public interface ClickListener {
        void onClick(View view, int position, boolean isLongClick);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        ClickListener clickListener;
        TextView title, body, date;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            body = itemView.findViewById(R.id.body);
            date = itemView.findViewById(R.id.date);
            itemView.setOnClickListener(this);
        }

        public void setClickListener(ClickListener itemClickListener) {
            this.clickListener = itemClickListener;
        }

        @Override
        public void onClick(View v) {
            clickListener.onClick(v, getAdapterPosition(), false);
        }

        @Override
        public boolean onLongClick(View v) {
            clickListener.onClick(v, getAdapterPosition(), true);
            return true;
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Note currentList = notesList.get(position);

        holder.title.setText(currentList.getTitle());
        holder.body.setText(currentList.getBody());
        holder.date.setText(currentList.getDate());

        String recording = currentList.getRecording();
        Log.e(LOG_TAG, currentList.getTitle());
        Log.e(LOG_TAG, "For mic : " + recording);
        if (recording != null && !recording.isEmpty()){
            holder.title.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_mic_24, 0);
            Log.e(LOG_TAG, "Mic visible");
        }
        else {
            holder.title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        holder.setClickListener(new ClickListener() {
            @Override
            public void onClick(View view, int position, boolean isLongClick) {
                String rec = notesList.get(position).getRecording();
                Log.e(LOG_TAG, "" + rec);
                int id = notesList.get(position).getId();
                Intent i;
                if (rec != null && !rec.isEmpty()) {
                    Log.e(LOG_TAG, "RecordingActivity started");
                    i = new Intent(context, RecordingActivity.class);
                }
                else {
                    Log.e(LOG_TAG, "NoteActivity started");
                    i = new Intent(context, NoteActivity.class);
                }
                i.putExtra("note_id", id);
                context.startActivity(i);
            }
        });
    }

    public void setTasks(List<Note> notesList) {
        this.notesList = notesList;
        notifyDataSetChanged();
    }

// --Commented out by Inspection START (16/10/2020 19:03):
//    public List<Note> getTasks() {
//        return notesList;
//    }
// --Commented out by Inspection STOP (16/10/2020 19:03)

    @Override
    public int getItemCount() {
        if (notesList == null) {
            return 0;
        }
        return notesList.size();
    }
}//END OF public class
