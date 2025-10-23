package com.lata.notes.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lata.notes.R;
import com.lata.notes.entities.Note;
import com.lata.notes.listeners.NotesListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private final List<Note> notes;
    private List<Note> notesFull;
    private final NotesListener notesListener;
    private List<Note> selectedNotes = new ArrayList<>();
    private String searchQuery = "";

    public NotesAdapter(List<Note> notes, NotesListener notesListener) {
        this.notes = notes;
        this.notesFull = new ArrayList<>(notes);
        this.notesListener = notesListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSearchQuery(String query) {
        this.searchQuery = query;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filter(String query) {
        List<Note> filteredList = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(notesFull);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();

            for (Note note : notesFull) {
                String title = note.getTitle() != null ? note.getTitle().toLowerCase() : "";
                String desc = note.getNote() != null ? note.getNote().toLowerCase() : "";
                String dateTime = note.getDate() != null ? note.getDate().toLowerCase() : "";

                if (title.contains(lowerCaseQuery) ||
                        desc.contains(lowerCaseQuery) ||
                        dateTime.contains(lowerCaseQuery)) {
                    filteredList.add(note);
                }
            }
        }
        notes.clear();
        notes.addAll(filteredList);
        notifyDataSetChanged();
    }


    @SuppressLint("NotifyDataSetChanged")
    public void updateNotes(List<Note> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        notesFull = new ArrayList<>(newNotes);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedNotes(List<Note> selectedNotes) {
        this.selectedNotes = new ArrayList<>(selectedNotes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note currentNote = notes.get(position);
        holder.setNote(currentNote, searchQuery);

        holder.layoutNote.setOnClickListener(view -> notesListener.onNoteClicked(currentNote, position));
        holder.layoutNote.setOnLongClickListener(view -> {
            notesListener.onNoteLongClicked(currentNote);
            return true;
        });

        if (selectedNotes.contains(currentNote)) {
            holder.setSelectedBackground();
        } else {
            holder.setDefaultBackground(currentNote);
        }

        //pin
        if (currentNote.isPinned()) {
            holder.imgPin.setVisibility(View.VISIBLE);
        } else {
            holder.imgPin.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textNote, textDate;
        LinearLayout layoutNote, todoContainer;
        ImageView imgPin; //pin

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textNote = itemView.findViewById(R.id.textNote);
            textDate = itemView.findViewById(R.id.textDate);
            layoutNote = itemView.findViewById(R.id.layoutNote);
            todoContainer = itemView.findViewById(R.id.todoContainer);
            imgPin = itemView.findViewById(R.id.img_pin); //pin
        }

        public void setNote(Note note, String query) {
            Typeface typeface1 = Typeface.createFromAsset(itemView.getContext().getAssets(), "fonts/nexatext-extrabold.ttf");
            Typeface typeface2 = Typeface.createFromAsset(itemView.getContext().getAssets(), "fonts/nexatext-regular.ttf");

            String title = note.getTitle() != null ? note.getTitle().trim() : "";
            String content = note.getNote() != null ? note.getNote().trim() : "";
            String date = note.getDate() != null ? note.getDate().trim() : "";

            if (!title.isEmpty()) {
                textTitle.setVisibility(View.VISIBLE);
                textTitle.setTypeface(typeface1);
                textTitle.setText(applyHighlight(itemView.getContext(),title, query));
            } else {
                textTitle.setVisibility(View.GONE);
            }

            if (note.isTodo()) {
                textNote.setVisibility(View.GONE);
                todoContainer.setVisibility(View.VISIBLE);
                renderCheckboxesFromJson(note.getTodoListJson(), query);
            } else {
                todoContainer.setVisibility(View.GONE);
                if (!content.isEmpty()) {
                    textNote.setVisibility(View.VISIBLE);
                    textNote.setTypeface(typeface2);
                    textNote.setText(applyHighlight(itemView.getContext(), content, query));
                } else {
                    textNote.setVisibility(View.GONE);
                }
            }
            textDate.setTypeface(typeface2);
            textDate.setText(applyHighlight(itemView.getContext(), date, query));
        }

        private void renderCheckboxesFromJson(String content, String query) {
            todoContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());

            try {
                Log.d("NotesAdapter", "To-do content: " + content);
                JSONArray todos = new JSONArray(content);

                int limit = Math.min(todos.length(), 5);

                for (int i = 0; i < limit; i++) {
                    JSONObject item = todos.getJSONObject(i);
                    String text = item.getString("text").trim();
                    boolean checked = item.getBoolean("checked");

                    View todoView = inflater.inflate(R.layout.todo_item_display, todoContainer, false);
                    CheckBox checkBox = todoView.findViewById(R.id.checkbox);
                    TextView textView = todoView.findViewById(R.id.edit_text);

                    checkBox.setChecked(checked);
                    textView.setText(applyHighlight(itemView.getContext(), text, query));
                    textView.setTypeface(Typeface.createFromAsset(itemView.getContext().getAssets(), "fonts/nexatext-regular.ttf"));

                    if (checked) {
                        textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    }
                    todoContainer.addView(todoView);
                }

                if (todos.length() > 5) {
                    TextView moreText = new TextView(itemView.getContext());
                    int remainingCount = todos.length() - 5;
                    String moreTextStr = itemView.getContext().getString(R.string.more_todos, remainingCount);
                    moreText.setText(moreTextStr);
                    moreText.setTypeface(Typeface.createFromAsset(itemView.getContext().getAssets(), "fonts/nexatext-regular.ttf"));
                    moreText.setPadding(25, 8, 0, 8);
                    moreText.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.textAndIconColor));
                    todoContainer.addView(moreText);
                }

            } catch (JSONException e) {
                TextView fallback = new TextView(itemView.getContext());
                fallback.setText(content);
                todoContainer.addView(fallback);
            }
        }

        private Spannable applyHighlight(Context context, String text, String query) {
            SpannableString spannable = new SpannableString(text);
            if (query != null && !query.trim().isEmpty()) {
                String lowerText = text.toLowerCase();
                String lowerQuery = query.toLowerCase();
                int startIndex = lowerText.indexOf(lowerQuery);
                while (startIndex >= 0) {
                    int endIndex = startIndex + lowerQuery.length();
                    spannable.setSpan(
                            new BackgroundColorSpan(ContextCompat.getColor(context, R.color.searchHighlightColor)),
                            startIndex,
                            endIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    startIndex = lowerText.indexOf(lowerQuery, endIndex);
                }
            }
            return spannable;
        }

        public void setDefaultBackground(Note note) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(dpToPx(10, itemView));
            String color;
            if (note.getColor() != null && !note.getColor().trim().isEmpty())
                color = note.getColor();
            else color = "#FFFFFF";
            drawable.setColor(Color.parseColor(color));
            if (color.equalsIgnoreCase("#FFFFFF"))
                drawable.setStroke(dpToPx(1.5f, itemView), Color.parseColor("#BDBDBD"));
            else {
                drawable.setStroke(dpToPx(1.5f, itemView), Color.parseColor(color));
            }
            layoutNote.setBackground(drawable);
        }

        public void setSelectedBackground() {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(dpToPx(10, itemView));
            drawable.setColor(ContextCompat.getColor(itemView.getContext(), R.color.selectedNoteBackground));
            drawable.setStroke(dpToPx(2f, itemView), ContextCompat.getColor(itemView.getContext(), R.color.hintAndIconLowContrastColor));
            layoutNote.setBackground(drawable);
        }

        private int dpToPx(float dp, View view) {
            float density = view.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }
}
