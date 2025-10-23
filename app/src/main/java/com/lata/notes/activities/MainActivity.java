package com.lata.notes.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.lata.notes.R;
import com.lata.notes.adapters.NotesAdapter;
import com.lata.notes.database.NotesDatabase;
import com.lata.notes.entities.Note;
import com.lata.notes.listeners.NotesListener;
import com.lata.notes.utils.KeyboardUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NotesListener {
    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    private Typeface typeface2;
    private CoordinatorLayout coordinatorLayout;
    private RecyclerView recyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;
    private TextView tvSelectedCount, tvEmptyStateText;
    private EditText edtSearch;
    private ConstraintLayout conSearch;
    private ImageView imgMore, imgMic, imgEmptystate, imgPin, imgExport, imgDelete;
    private LinearLayout selectionToolbar, linHeader, linEmptyState;
    private View horLine;
    private FloatingActionButton fab;
    private Snackbar snackbar;
    private boolean isSelectionMode = false;
    private final List<Note> selectedNotes = new ArrayList<>();
    final int MAX_CHAR_LIMIT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Typeface typeface1 = Typeface.createFromAsset(getAssets(), "fonts/wavehaus_128bold.ttf");
        typeface2 = Typeface.createFromAsset(getAssets(), "fonts/nexatext-regular.ttf");

        TextView tvGreet = findViewById(R.id.tv_greet);
        tvGreet.setTypeface(typeface1);

        coordinatorLayout = findViewById(R.id.main);
        conSearch = findViewById(R.id.con_searchbar);
        linHeader = findViewById(R.id.lin_header);
        linEmptyState = findViewById(R.id.lin_empty_state);
        imgEmptystate = findViewById(R.id.img_empty_state);
        tvEmptyStateText = findViewById(R.id.tv_empty_state_text);
        tvEmptyStateText.setTypeface(typeface2);

        edtSearch = findViewById(R.id.edt_search);
        edtSearch.setTypeface(typeface2);
        edtSearch.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_CHAR_LIMIT)});

        imgMore = findViewById(R.id.img_more);
        imgMic = findViewById(R.id.img_mic);

        imgMic.setOnClickListener(v -> voiceSearchOrClearText());

        fab = findViewById(R.id.floating_action_button);
        fab.setOnClickListener(view -> {
            if (isSelectionMode) clearSelectionMode();
            else startActivityForResult(new Intent(getApplicationContext(), NoteActivity.class), REQUEST_CODE_ADD_NOTE);
        });

        selectionToolbar = findViewById(R.id.lin_multi_select_toolbar);
        horLine = findViewById(R.id.view_hor_line);

        tvSelectedCount = findViewById(R.id.tv_selected_count);
        tvSelectedCount.setTypeface(typeface2);

        ImageView imgSelectAll = findViewById(R.id.img_select_all);
        imgPin = findViewById(R.id.img_pin);
        imgExport = findViewById(R.id.img_share);
        imgDelete = findViewById(R.id.img_delete);

        imgSelectAll.setOnClickListener(v -> toggleSelectAll());

        imgPin.setOnClickListener(v -> pinSelectedNotes()); //pin

        imgExport.setOnClickListener(v->{
            Toast.makeText(getApplicationContext(), "Share is clicked", Toast.LENGTH_SHORT).show();
        });

        imgDelete.setOnClickListener(v -> {
            edtSearch.setText("");
            String currentQuery = edtSearch.getText().toString().trim();
            notesAdapter.setSearchQuery(currentQuery);
            notesAdapter.filter(currentQuery);
            deleteSelectedNotes();
        });

        recyclerView = findViewById(R.id.notes_recycler_view);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        recyclerView.setAdapter(notesAdapter);

        getNotes(REQUEST_CODE_SHOW_NOTES);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setEdtSearch();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isSelectionMode) {
                    clearSelectionMode();
                } else if (!edtSearch.getText().toString().trim().isEmpty()) {
                    edtSearch.setText("");
                    View view = getCurrentFocus();
                    if (view != null) {
                        KeyboardUtils.hideKeyboard(MainActivity.this, view);
                    }
                } else {
                    finish();
                }
            }
        });
    }


    @Override
    public void onNoteClicked(Note note, int position) {
        if (isSelectionMode) toggleNoteSelection(note);
        else {
            Intent intent = new Intent(getApplicationContext(), NoteActivity.class);
            intent.putExtra("isViewOrUpdate", true);
            intent.putExtra("note", note);
            startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
        }
    }

    @Override
    public void onNoteLongClicked(Note note) {
        if (!isSelectionMode) {
            isSelectionMode = true;
            fab.setVisibility(View.VISIBLE);
            View view = this.getCurrentFocus();
            if (view != null) {
                KeyboardUtils.hideKeyboard(this, view);
            }
            fab.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.hintAndIconLowContrastColor)));
            fab.animate().rotation(45f).setDuration(200).start();
            selectedNotes.add(note);
            updateSelectionUI();
            notesAdapter.setSelectedNotes(selectedNotes);
        }
    }

    private void setEdtSearch() {
        String query = edtSearch.getText().toString().trim();
        if (!isSelectionMode) {
            notesAdapter.setSearchQuery(query);
            notesAdapter.filter(query);
            if (query.isEmpty()) {
                if (noteList.isEmpty()) {
                    linEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyStateText.setText(R.string.empty_notes_indication);
                    imgEmptystate.setImageResource(R.drawable.notes_empty_state);
                } else {
                    linEmptyState.setVisibility(View.GONE);
                }
            } else {
                if (notesAdapter.getItemCount() == 0) {
                    linEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyStateText.setText(R.string.no_matches_found);
                    imgEmptystate.setImageResource(R.drawable.ic_search);
                } else {
                    linEmptyState.setVisibility(View.GONE);
                }
            }
        }
        if (!query.isEmpty()) {
            imgMic.setImageResource(R.drawable.ic_close_or_clear);
            fab.setVisibility(View.GONE);
        } else {
            imgMic.setImageResource(R.drawable.ic_mic);
            if (!isSelectionMode) {
                fab.setVisibility(View.VISIBLE);
            }
        }
    }

    private void voiceSearchOrClearText(){
        String searchText = edtSearch.getText().toString().trim();
        if (!searchText.isEmpty()) {
            edtSearch.setText("");
        } else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search");

            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);

            if (!activities.isEmpty()) {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
            } else {
                Toast.makeText(MainActivity.this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleNoteSelection(Note note) {
        if (selectedNotes.contains(note)) selectedNotes.remove(note);
        else selectedNotes.add(note);

        if (selectedNotes.isEmpty()) clearSelectionMode();
        else updateSelectionUI();

        notesAdapter.setSelectedNotes(selectedNotes);
    }

    private void clearSelectionMode() {
        imgDelete.setVisibility(View.VISIBLE);
        imgPin.setVisibility(View.VISIBLE);
        imgExport.setVisibility(View.VISIBLE);
        isSelectionMode = false;
        selectedNotes.clear();
        updateSelectionUI();
        notesAdapter.setSelectedNotes(selectedNotes);
        fab.animate().rotation(0f).setDuration(200).start();
        fab.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent)));
        if(!edtSearch.getText().toString().trim().isEmpty()) {
            fab.setVisibility(View.GONE);
            edtSearch.requestFocus();
            KeyboardUtils.showKeyboard(this, edtSearch);
        }
    }

    private void updateSelectionUI() {
        if (isSelectionMode) {
            selectionToolbar.setVisibility(View.VISIBLE);
            horLine.setVisibility(View.VISIBLE);
            linHeader.setVisibility(View.GONE);
            conSearch.setVisibility(View.GONE);
            tvSelectedCount.setText(selectedNotes.size() + " selected");
            imgMore.setVisibility(View.GONE);
        } else {
            selectionToolbar.setVisibility(View.GONE);
            horLine.setVisibility(View.GONE);
            linHeader.setVisibility(View.VISIBLE);
            conSearch.setVisibility(View.VISIBLE);
            imgMore.setVisibility(View.VISIBLE);
        }
    }

    private void toggleSelectAll() {
        if (selectedNotes.size() == noteList.size()) {
            selectedNotes.clear();
            imgPin.setVisibility(View.GONE);
            imgExport.setVisibility(View.GONE);
            imgDelete.setVisibility(View.GONE);
        }
        else {
            selectedNotes.clear();
            selectedNotes.addAll(noteList);
            imgPin.setVisibility(View.VISIBLE);
            imgExport.setVisibility(View.VISIBLE);
            imgDelete.setVisibility(View.VISIBLE);
        }
        updateSelectionUI();
        notesAdapter.setSelectedNotes(selectedNotes);
    }

    private void pinSelectedNotes(){
        if (selectedNotes.isEmpty()) return;

        List<Integer> noteIds = new ArrayList<>();
        boolean shouldPin = false;

        for (Note note : selectedNotes) {
            noteIds.add(note.getId());
            if (!note.isPinned()) shouldPin = true; // if any note isn’t pinned, we’ll pin all
        }

        boolean finalShouldPin = shouldPin;

        @SuppressLint("StaticFieldLeak")
        class PinNotesTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase db = NotesDatabase.getDatabase(getApplicationContext());
                db.noteDao().updatePinnedStatus(noteIds, finalShouldPin);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                String message = finalShouldPin ? "Note(s) pinned" : "Note(s) unpinned";
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                clearSelectionMode();
                getNotes(REQUEST_CODE_SHOW_NOTES);
            }
        }

        new PinNotesTask().execute();
    }


    private void deleteSelectedNotes() {
        if (selectedNotes.isEmpty()) return;

        final List<Note> notesToDelete = new ArrayList<>(selectedNotes);

        @SuppressLint("StaticFieldLeak")
        class DeleteNotesTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase db = NotesDatabase.getDatabase(getApplicationContext());
                for (Note note : notesToDelete) {
                    db.noteDao().deleteNote(note);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                int count = notesToDelete.size();
                String message = count + " " + (count == 1 ? "note" : "notes") + " deleted";

                if (snackbar != null && snackbar.isShown()) {
                    snackbar.dismiss();
                }

                snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG);
                snackbar.setDuration(7000);

                View snackbarView = snackbar.getView();
                snackbarView.setBackgroundResource(R.drawable.snackbar_background);

                TextView snackbarText = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                snackbarText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.whiteColor));
                snackbarText.setTypeface(typeface2);

                TextView snackbarAction = snackbarView.findViewById(com.google.android.material.R.id.snackbar_action);
                snackbarAction.setTypeface(typeface2);
                snackbarAction.setAllCaps(false);

                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
                params.setMargins(20, 0, 20, 20);
                snackbarView.setLayoutParams(params);

                snackbar.setAction("Undo", v -> {
                    @SuppressLint("StaticFieldLeak")
                    class RestoreNotesTask extends AsyncTask<Void, Void, Void> {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase db = NotesDatabase.getDatabase(getApplicationContext());
                            for (Note note : notesToDelete) {
                                db.noteDao().insertNote(note);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void unused) {
                            getNotes(REQUEST_CODE_SHOW_NOTES);
                            Toast.makeText(MainActivity.this, "Notes restored", Toast.LENGTH_SHORT).show();
                        }
                    }
                    new RestoreNotesTask().execute();
                });

                snackbar.show();

                clearSelectionMode();
                getNotes(REQUEST_CODE_SHOW_NOTES);
            }
        }
        new DeleteNotesTask().execute();
    }



    private void getNotes(final int requestCode) {
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {
            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase.getDatabase(getApplicationContext()).noteDao().getAllNotesOrdered(); //pin

            }
            @Override
            protected void onPostExecute(List<Note> notes) {
                noteList.clear();
                noteList.addAll(notes);
                notesAdapter.updateNotes(notes);

                if (noteList.isEmpty()) {
                    linEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyStateText.setText(R.string.empty_notes_indication);
                } else {
                    linEmptyState.setVisibility(View.GONE);
                }

                if (requestCode == REQUEST_CODE_SHOW_NOTES || requestCode == REQUEST_CODE_ADD_NOTE || requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    notesAdapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(0);
                }
                clearSelectionMode();
            }
        }
        new GetNotesTask().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CODE_ADD_NOTE || requestCode == REQUEST_CODE_UPDATE_NOTE) && resultCode == RESULT_OK) {
            edtSearch.setText("");
            getNotes(requestCode);
        }
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                edtSearch.setText(spokenText);
                edtSearch.setSelection(spokenText.length());
                notesAdapter.setSearchQuery(spokenText);
                notesAdapter.filter(spokenText);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
            snackbar = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (snackbar != null && snackbar.isShownOrQueued()) {
            snackbar.dismiss();
            snackbar = null;
        }
    }


}

