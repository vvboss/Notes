package com.lata.notes.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.lata.notes.R;
import com.lata.notes.database.NotesDatabase;
import com.lata.notes.entities.Note;
import com.lata.notes.utils.KeyboardUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteActivity extends AppCompatActivity {
    private EditText edtTitle, edtNote;
    private EditText lastFocusedEditText = null;
    private TextView textDate;
    private LinearLayout todoContainer, linAddItem;
    private Typeface tf_bold, tf_regular;
    private ImageView btnToggleTodo;
    private boolean isTodoMode = false;
    private CoordinatorLayout coordinatorLayout;
    private ScrollView scrollView;
    private BottomSheetBehavior<FrameLayout> bottomSheetBehavior;
    private String selectedNoteColor;
    private Note alreadyAvailableNote;
    private String originalTitle = "";
    private String originalNote = "";
    private String originalColor = "";
    final int MAX_CHAR_LIMIT = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note);

        edtTitle = findViewById(R.id.edt_title);
        edtNote = findViewById(R.id.edt_note);
        textDate = findViewById(R.id.tv_date);
        todoContainer = findViewById(R.id.todo_container);
        btnToggleTodo = findViewById(R.id.btn_toggle_todo);
        ImageView imgShare = findViewById(R.id.img_share);
        coordinatorLayout = findViewById(R.id.coordinator_root);
        scrollView = findViewById(R.id.scroll_view);
        linAddItem = findViewById(R.id.lin_add_item);
        TextView textAddItemHint = findViewById(R.id.tv_add_item_hint);

        tf_bold = Typeface.createFromAsset(getAssets(), "fonts/nexatext-extrabold.ttf");
        tf_regular = Typeface.createFromAsset(getAssets(), "fonts/nexatext-regular.ttf");

        edtTitle.setTypeface(tf_bold);
        edtNote.setTypeface(tf_regular);
        textDate.setTypeface(tf_regular);
        textAddItemHint.setTypeface(tf_regular);

        textDate.setText(new SimpleDateFormat("dd/MM/yy | EEE | hh:mm a", Locale.getDefault()).format(new Date()));
        selectedNoteColor = "#FFFFFF";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        } else {
            btnToggleTodo.setVisibility(View.VISIBLE);
        }

        btnToggleTodo.setOnClickListener(v -> toggleMode());
        imgShare.setOnClickListener(v -> shareNote());

        edtTitle.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_CHAR_LIMIT)});
        edtTitle.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (isTodoMode) {
                    int childCount = todoContainer.getChildCount();
                    if (childCount > 0) {

                        for (int i = childCount - 1; i >= 0; i--) {
                            View item = todoContainer.getChildAt(i);
                            EditText editText = item.findViewById(R.id.edit_text);
                            if (editText != null && editText.getText().toString().trim().isEmpty()) {
                                editText.requestFocus();
                                return true;
                            }
                        }

                        View lastItem = todoContainer.getChildAt(childCount - 1);
                        EditText lastEditText = lastItem.findViewById(R.id.edit_text);
                        if (lastEditText != null) {
                            lastEditText.requestFocus();
                            return true;
                        }
                    } else {
                        addTodoItem(null, false, true);
                        View newItem = todoContainer.getChildAt(todoContainer.getChildCount() - 1);
                        if (newItem != null) {
                            EditText newEditText = newItem.findViewById(R.id.edit_text);
                            if (newEditText != null) {
                                newEditText.requestFocus();
                            }
                        }
                        return true;
                    }
                } else {
                    edtNote.requestFocus();
                    return true;
                }
            }
            return false;
        });

        linAddItem.setOnClickListener(v -> {
            if (lastFocusedEditText == null) {
                addTodoItem("", false, true);
                return;
            }

            String currentText = lastFocusedEditText.getText().toString().trim();
            if (!currentText.isEmpty()) {
                EditText empty = getFirstEmptyTodoEditText();
                if (empty == null) {
                    addTodoItem("", false, true);
                } else {
                    empty.requestFocus();
                    linAddItem.setVisibility(View.INVISIBLE);
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else {
                    handleSaveOrExit();
                }
            }
        });

        findViewById(R.id.img_close).setOnClickListener(view -> {
            KeyboardUtils.hideKeyboard(this, edtTitle);
            finish();
        });

        initMiscellaneous();
        setNoteBackgroundColor();
    }

    private void toggleMode() {
        if (isTodoMode) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < todoContainer.getChildCount(); i++) {
                View view = todoContainer.getChildAt(i);
                EditText editText = view.findViewById(R.id.edit_text);
                String text = editText.getText().toString().trim();
                if (!text.isEmpty()) {
                    sb.append(text).append("\n");
                }
            }

            edtTitle.clearFocus();
            edtNote.setText(sb.toString().trim());
            edtNote.setVisibility(View.VISIBLE);
            todoContainer.setVisibility(View.GONE);
            linAddItem.setVisibility(View.GONE);
            isTodoMode = false;
            btnToggleTodo.setImageResource(R.drawable.ic_todo_checked);
            edtNote.requestFocus();
            KeyboardUtils.showKeyboard(this, edtNote);

        } else {
            todoContainer.removeAllViews();
            String noteText = edtNote.getText().toString().trim();
            if (!noteText.isEmpty()) {
                for (String line : noteText.split("\n")) {
                    line = line.replaceFirst("•\\s?", "").trim();
                    if (!line.isEmpty()) {
                        addTodoItem(line, false, false);
                    }
                }
            }
            if (todoContainer.getChildCount() == 0) {
                addTodoItem(null, false, false);
            }
            edtNote.setVisibility(View.GONE);
            todoContainer.setVisibility(View.VISIBLE);
            linAddItem.setVisibility(View.VISIBLE);
            isTodoMode = true;
            btnToggleTodo.setImageResource(R.drawable.ic_note);

            if (todoContainer.getChildCount() > 0) {
                View firstChild = todoContainer.getChildAt(0);
                if (firstChild instanceof LinearLayout) {
                    EditText editText = firstChild.findViewById(R.id.edit_text);
                    if (editText != null) {
                        editText.requestFocus();
                        KeyboardUtils.showKeyboard(this, editText);
                    }
                }
            }
        }
    }

//    private void shareNote() {
//
//        String title = edtTitle.getText().toString().trim();
//        String content = edtNote.getText().toString().trim();
//
//        if (title.isEmpty() && content.isEmpty()) {
//            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        StringBuilder shareText = new StringBuilder();
//        if (!title.isEmpty()) shareText.append(title).append("\n\n");
//        if (!content.isEmpty()) shareText.append(content);
//
//        Intent shareIntent = new Intent(Intent.ACTION_SEND);
//        shareIntent.setType("text/plain");
//        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Note");
//        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
//
//        startActivity(Intent.createChooser(shareIntent, "Share note via"));
//    }

    private void shareNote() {
        String title = edtTitle.getText().toString().trim();
        StringBuilder shareText = new StringBuilder();

        if (!title.isEmpty()) {
            shareText.append(title).append("\n\n");
        }

        if (isTodoMode) {
            StringBuilder todoBuilder = new StringBuilder();
            for (int i = 0; i < todoContainer.getChildCount(); i++) {
                View view = todoContainer.getChildAt(i);
                CheckBox checkBox = view.findViewById(R.id.checkbox);
                EditText editText = view.findViewById(R.id.edit_text);

                if (editText != null) {
                    String text = editText.getText().toString().trim();
                    if (!text.isEmpty()) {
                        String checkboxSymbol = (checkBox != null && checkBox.isChecked()) ? "- [x] " : "- [ ] ";
                        todoBuilder.append(checkboxSymbol).append(text).append("\n");
                    }
                }
            }

            if (todoBuilder.length() == 0) {
                Toast.makeText(this, "No To-Do items to share", Toast.LENGTH_SHORT).show();
                return;
            }

            shareText.append(todoBuilder.toString().trim());

        } else {
            String content = edtNote.getText().toString().trim();

            if (content.isEmpty() && title.isEmpty()) {
                Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!content.isEmpty()) {
                shareText.append(content);
            }
        }

        // Create and launch the share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Note");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());

        startActivity(Intent.createChooser(shareIntent, "Share note via"));
    }


    private void addTodoItem(String text, Boolean isChecked, boolean focus) {
        View view = getLayoutInflater().inflate(R.layout.todo_item, todoContainer, false);
        CheckBox checkBox = view.findViewById(R.id.checkbox);
        EditText editText = view.findViewById(R.id.edit_text);
        ImageView deleteIcon = view.findViewById(R.id.delete_icon);

        editText.setTypeface(tf_regular);

        if (text != null) editText.setText(text);
        if (isChecked != null) checkBox.setChecked(isChecked);

        if (isChecked != null && isChecked && !editText.getText().toString().trim().isEmpty()) {
            editText.setPaintFlags(editText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }

        checkBox.setOnCheckedChangeListener((buttonView, isChecked1) -> {
            String currentText = editText.getText().toString().trim();
            if (!currentText.isEmpty()) {
                if (isChecked1) {
                    editText.setPaintFlags(editText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    editText.setPaintFlags(editText.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                }
            }
        });

        deleteIcon.setVisibility(View.INVISIBLE);
        deleteIcon.setOnClickListener(v -> {
            int index = todoContainer.indexOfChild(view);

            if (lastFocusedEditText == editText) {
                lastFocusedEditText = null;
            }
            todoContainer.removeView(view);

            if (todoContainer.getChildCount() == 0) {
                edtTitle.requestFocus();
                linAddItem.setVisibility(View.VISIBLE);
            } else {
                int nextFocusIndex = index > 0 ? index - 1 : 0;
                View nextFocusItem = todoContainer.getChildAt(nextFocusIndex);
                if (nextFocusItem != null) {
                    EditText nextFocusEdit = nextFocusItem.findViewById(R.id.edit_text);
                    if (nextFocusEdit != null) {
                        nextFocusEdit.requestFocus();
                    }
                }
            }
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            String currentText = editText.getText().toString().trim();
            boolean notEmpty = !currentText.isEmpty();
            deleteIcon.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);

            if (hasFocus) {
                lastFocusedEditText = editText;
                linAddItem.setVisibility(notEmpty && getFirstEmptyTodoEditText() == null ? View.VISIBLE : View.INVISIBLE);
            } else {
                linAddItem.setVisibility(View.INVISIBLE);
            }

        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String currentText = s.toString().trim();
                boolean notEmpty = !currentText.isEmpty();
                boolean hasFocus = editText.hasFocus();

                if (hasFocus && notEmpty && getFirstEmptyTodoEditText() == null) {
                    linAddItem.setVisibility(View.VISIBLE);
                } else {
                    linAddItem.setVisibility(View.INVISIBLE);
                }

                if (currentText.isEmpty()) {
                    editText.setPaintFlags(editText.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                } else if (checkBox.isChecked()) {
                    editText.setPaintFlags(editText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                }
            }
        });

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String currentText = editText.getText().toString().trim();
                if (!currentText.isEmpty()) {
                    EditText empty = getFirstEmptyTodoEditText();
                    if (empty == null) {
                        addTodoItem(null, false, true);
                    } else {
                        empty.requestFocus();
                        linAddItem.setVisibility(View.INVISIBLE);
                    }
                } else {
                    linAddItem.setVisibility(View.INVISIBLE);
                }
                return true;
            }
            return false;
        });

        todoContainer.addView(view);

        if (focus) {
            editText.post(() -> {
                edtTitle.clearFocus();
                scrollView.fullScroll(View.FOCUS_DOWN);
                editText.requestFocus();
                KeyboardUtils.showKeyboard(this, editText);
            });
        }
    }

    private void setViewOrUpdateNote() {
        edtTitle.setText(alreadyAvailableNote.getTitle());
        edtNote.setText(alreadyAvailableNote.getNote());
        textDate.setText(alreadyAvailableNote.getDate());
        selectedNoteColor = alreadyAvailableNote.getColor();

        originalTitle = alreadyAvailableNote.getTitle();
        originalNote = alreadyAvailableNote.getNote();
        originalColor = alreadyAvailableNote.getColor();

        btnToggleTodo.setVisibility(View.GONE);

        if (alreadyAvailableNote.getNote() != null && alreadyAvailableNote.getNote().trim().startsWith("[")) {
            try {
                isTodoMode = true;
                edtNote.setVisibility(View.GONE);
                todoContainer.setVisibility(View.VISIBLE);
                linAddItem.setVisibility(View.INVISIBLE);
                JSONArray array = new JSONArray(alreadyAvailableNote.getNote());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String text = obj.getString("text");
                    boolean checked = obj.getBoolean("checked");
                    addTodoItem(text, checked, false);
                }
                if (todoContainer.getChildCount() > 0) {
                    View firstChild = todoContainer.getChildAt(0);
                    if (firstChild instanceof LinearLayout) {
                        EditText editText = firstChild.findViewById(R.id.edit_text);
                        if (editText != null) {
                            editText.requestFocus();
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e("NoteActivity", "Error parsing to-do JSON", e);
            }
        } else {
            edtNote.setText(alreadyAvailableNote.getNote());
            edtNote.requestFocus();
        }
    }

    private void handleSaveOrExit() {
        String currentTitle = edtTitle.getText().toString().trim();
        String currentNote = isTodoMode ? getTodoAsJson() : edtNote.getText().toString().trim();

        if (currentTitle.isEmpty()) {
            if (!isTodoMode && currentNote.isEmpty()) {
                finish();
                return;
            }

            if (isTodoMode) {
                boolean hasTodoText = false;
                for (int i = 0; i < todoContainer.getChildCount(); i++) {
                    View view = todoContainer.getChildAt(i);
                    EditText editText = view.findViewById(R.id.edit_text);
                    if (editText != null && !editText.getText().toString().trim().isEmpty()) {
                        hasTodoText = true;
                        break;
                    }
                }

                if (!hasTodoText) {
                    finish();
                    return;
                }
            }
        }
        String currentColor = selectedNoteColor;

        if (alreadyAvailableNote != null) {
            boolean isModified = !currentTitle.equals(originalTitle) || !currentNote.equals(originalNote) || !currentColor.equals(originalColor);

            if (!isModified) {
                finish();
                return;
            }
        }

        final Note note = new Note();
        note.setTitle(currentTitle);
        note.setNote(currentNote);
        note.setDate(new SimpleDateFormat("dd/MM/yy | EEE | hh:mm a", Locale.getDefault()).format(new Date()));
        note.setColor(selectedNoteColor);
        note.setLastUpdated(System.currentTimeMillis());
        note.setTodo(isTodoMode);

        if (alreadyAvailableNote != null) {
            note.setId(alreadyAvailableNote.getId());
        }

        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }
            @Override
            protected void onPostExecute(Void unused) {
                setResult(RESULT_OK);
                finish();
            }
        }
        new SaveNoteTask().execute();
    }

    private String getTodoAsJson() {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < todoContainer.getChildCount(); i++) {
            View view = todoContainer.getChildAt(i);
            EditText editText = view.findViewById(R.id.edit_text);
            CheckBox checkBox = view.findViewById(R.id.checkbox);
            String text = editText.getText().toString().trim();
            if (!text.isEmpty()) {
                JSONObject item = new JSONObject();
                try {
                    item.put("text", text);
                    item.put("checked", checkBox.isChecked());
                    jsonArray.put(item);
                } catch (JSONException e) {
                    Log.e("NoteActivity", "Error parsing to-do JSON", e);
                }
            }
        }
        return jsonArray.toString();
    }

    private void initMiscellaneous() {
        final FrameLayout bottomSheetContainer = findViewById(R.id.bottomSheetContainer);
        final TextView textView = findViewById(R.id.textMiscellaneous);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetContainer);

        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        textView.setTypeface(tf_bold);

        ImageView imgMore = findViewById(R.id.img_more);
        imgMore.setOnClickListener(view -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));

        View outsideTouchView = findViewById(R.id.outside_touch_view);
        outsideTouchView.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    view.performClick();
                }
                return true;
            }
            return false;
        });

        int[] viewColorIds = {
                R.id.viewColor0, R.id.viewColor1, R.id.viewColor2, R.id.viewColor3, R.id.viewColor4,
                R.id.viewColor5, R.id.viewColor6, R.id.viewColor7, R.id.viewColor8, R.id.viewColor9
        };

        int[] imageColorIds = {
                R.id.imageColor0, R.id.imageColor1, R.id.imageColor2, R.id.imageColor3, R.id.imageColor4,
                R.id.imageColor5, R.id.imageColor6, R.id.imageColor7, R.id.imageColor8, R.id.imageColor9
        };

        String[] colors = {
                "#FFFFFF", "#B4C4F0", "#B4F0B4", "#F0B8B8", "#F0CBA4",
                "#A4E4F0", "#D4B4F0", "#F5E79C", "#94E4ED", "#90E9B9"
        };


        ImageView[] colorImages = new ImageView[imageColorIds.length];
        for (int i = 0; i < imageColorIds.length; i++) {
            colorImages[i] = findViewById(imageColorIds[i]);
        }

        for (int i = 0; i < viewColorIds.length; i++) {
            final int index = i;
            findViewById(viewColorIds[i]).setOnClickListener(view -> {
                selectedNoteColor = colors[index];
                for (int j = 0; j < colorImages.length; j++) {
                    colorImages[j].setImageResource(j == index ? R.drawable.ic_done : 0);
                }
                setNoteBackgroundColor();
            });
        }

        if (alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null) {
            for (int i = 0; i < colors.length; i++) {
                if (colors[i].equals(alreadyAvailableNote.getColor())) {
                    findViewById(viewColorIds[i]).performClick();
                    break;
                }
            }
        }
    }

    private void setNoteBackgroundColor() {
        coordinatorLayout.setBackgroundColor(Color.parseColor(selectedNoteColor));
    }

    private EditText getFirstEmptyTodoEditText() {
        for (int i = 0; i < todoContainer.getChildCount(); i++) {
            View child = todoContainer.getChildAt(i);
            EditText edit = child.findViewById(R.id.edit_text);
            if (edit.getText().toString().trim().isEmpty()) {
                return edit;
            }
        }
        return null;
    }

}
