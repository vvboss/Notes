package com.lata.notes.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Objects;

@Entity(tableName = "notes")
public class Note implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "title")
    private String title;
    @ColumnInfo(name = "date")
    private String date;
    @ColumnInfo(name = "note")
    private String note;
    @ColumnInfo(name = "color")
    private String color;
    @ColumnInfo(name = "last_updated")
    private long lastUpdated;
    @ColumnInfo(name = "image") // not used
    private String image;
    @ColumnInfo(name = "link") // not used
    private String link;
    @ColumnInfo(name = "todo_list_json")
    private String todoListJson;
    @ColumnInfo(name = "is_todo")
    private boolean isTodo;


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getImage() { return image; } // not used
    public void setImage(String image) { this.image = image; } // not used

    public String getLink() { return link; } // not used
    public void setLink(String link) { this.link = link; } // not used

    public String getTodoListJson() {
        return isTodo ? getNote() : null;
    }

    public void setTodoListJson(String todoListJson) {
        this.todoListJson = todoListJson;
    }
    public boolean isTodo() {
        return isTodo;
    }
    public void setTodo(boolean todo) {
        isTodo = todo;
    }

    @NonNull
    @Override
    public String toString() {
        return title + " | " + note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note)) return false;
        Note note1 = (Note) o;
        return id == note1.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
