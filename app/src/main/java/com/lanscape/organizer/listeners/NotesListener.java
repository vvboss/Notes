package com.lanscape.organizer.listeners;

import com.lanscape.organizer.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
    void onNoteLongClicked(Note note);
}
