package com.spms.view;

import com.spms.model.Note;
import com.spms.service.DatabaseService;
import com.spms.util.DateUtil;
import com.spms.util.UIFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Digital Notes Manager: create, search, edit, and delete notes with tagging.
 */
public class NotesView {

    private static final DateTimeFormatter SAVED_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final DatabaseService      db        = DatabaseService.getInstance();
    private final ObservableList<Note> notesList = FXCollections.observableArrayList();

    private ListView<Note>   listView;
    private TextField        searchField;
    private ComboBox<String> subjectFilter;

    // Editor pane
    private TextField titleEdit;
    private TextField subjectEdit;
    private TextField tagsEdit;
    private TextArea  contentEdit;
    private Label     lastSavedLabel;
    private Note      currentNote;
    private boolean   newNoteMode = false;

    public Node build() {
        VBox page = new VBox(0);
        page.getStyleClass().add("view-root");
        page.getChildren().add(buildHeader());

        SplitPane split = new SplitPane(buildNoteList(), buildEditor());
        split.setDividerPositions(0.33);
        split.getStyleClass().add("notes-split");
        VBox.setVgrow(split, Priority.ALWAYS);
        page.getChildren().add(split);

        refreshList(db.getAllNotes());
        return page;
    }

    private Node buildHeader() {
        HBox h = new HBox(12);
        h.getStyleClass().add("page-header");
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(22, 28, 22, 28));
        Label title = new Label("📝  Notes");
        title.getStyleClass().add("page-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button newBtn = UIFactory.createPrimaryButton("+ New Note");
        newBtn.setOnAction(e -> startNewNote());
        h.getChildren().addAll(title, sp, newBtn);
        return h;
    }

    // ── Left: note list ───────────────────────────────────────────────────────

    private Node buildNoteList() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("notes-list-panel");
        panel.setPadding(new Insets(14));
        panel.setMinWidth(260);

        searchField = UIFactory.createTextField("🔍  Search notes…");
        searchField.textProperty().addListener((obs, old, val) -> performSearch(val));

        subjectFilter = new ComboBox<>();
        subjectFilter.getStyleClass().add("combo-field");
        subjectFilter.setMaxWidth(Double.MAX_VALUE);
        subjectFilter.getItems().add("All Subjects");
        db.getAllNotes().stream()
          .map(Note::getSubject)
          .filter(s -> s != null && !s.isBlank())
          .distinct()
          .sorted()
          .forEach(s -> subjectFilter.getItems().add(s));
        subjectFilter.getSelectionModel().select(0);
        subjectFilter.setOnAction(e -> {
            String sel = subjectFilter.getValue();
            if (sel == null || sel.equals("All Subjects")) refreshList(db.getAllNotes());
            else refreshList(db.getNotesBySubject(sel));
        });

        listView = new ListView<>(notesList);
        listView.getStyleClass().add("notes-listview");
        listView.setCellFactory(lv -> new NoteCell());
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, note) -> { if (note != null) loadNote(note); });

        panel.getChildren().addAll(searchField, subjectFilter, listView);
        return panel;
    }

    // ── Right: editor ─────────────────────────────────────────────────────────

    private Node buildEditor() {
        VBox editor = new VBox(10);
        editor.getStyleClass().add("note-editor");
        editor.setPadding(new Insets(20));
        editor.setMinWidth(380);

        titleEdit   = UIFactory.createTextField("Note title");
        titleEdit.getStyleClass().add("note-title-field");
        subjectEdit = UIFactory.createTextField("Subject (e.g. Mathematics)");
        tagsEdit    = UIFactory.createTextField("Tags (comma-separated)");
        contentEdit = UIFactory.createTextArea("Write your note here…", 20);
        VBox.setVgrow(contentEdit, Priority.ALWAYS);

        HBox metaRow = new HBox(12, subjectEdit, tagsEdit);
        HBox.setHgrow(subjectEdit, Priority.ALWAYS);
        HBox.setHgrow(tagsEdit,    Priority.ALWAYS);

        lastSavedLabel = new Label("");
        lastSavedLabel.getStyleClass().add("text-muted-sm");

        Button saveBtn   = UIFactory.createPrimaryButton("💾  Save");
        Button deleteBtn = UIFactory.createDangerButton("🗑  Delete");
        Button clearBtn  = UIFactory.createSecondaryButton("✕  Clear");
        saveBtn.setOnAction(e   -> saveCurrentNote());
        deleteBtn.setOnAction(e -> deleteCurrentNote());
        clearBtn.setOnAction(e  -> clearEditor());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox actionRow = new HBox(10, saveBtn, clearBtn, sp, lastSavedLabel, deleteBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        editor.getChildren().addAll(
                UIFactory.createSectionTitle("Note Editor"),
                UIFactory.hSeparator(),
                titleEdit, metaRow, contentEdit, actionRow);
        return editor;
    }

    // ── Note cell ─────────────────────────────────────────────────────────────

    private class NoteCell extends ListCell<Note> {
        @Override
        protected void updateItem(Note note, boolean empty) {
            super.updateItem(note, empty);
            if (empty || note == null) { setGraphic(null); return; }

            VBox cell = new VBox(4);
            cell.setPadding(new Insets(8, 6, 8, 6));

            Label titleLbl = new Label(note.getTitle());
            titleLbl.getStyleClass().add("note-cell-title");
            titleLbl.setMaxWidth(Double.MAX_VALUE);

            Label previewLbl = new Label(note.getPreview());
            previewLbl.getStyleClass().add("note-cell-preview");
            previewLbl.setMaxWidth(Double.MAX_VALUE);

            HBox metaRow = new HBox(6);
            metaRow.setAlignment(Pos.CENTER_LEFT);
            if (note.getSubject() != null && !note.getSubject().isBlank()) {
                metaRow.getChildren().add(UIFactory.createBadgeLabel(note.getSubject(), "badge-info"));
            }
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
            Label dateLbl = new Label(DateUtil.formatDateShort(
                    note.getUpdatedAt() != null ? note.getUpdatedAt().toLocalDate() : null));
            dateLbl.getStyleClass().add("text-dim");
            metaRow.getChildren().addAll(sp, dateLbl);

            cell.getChildren().addAll(titleLbl, previewLbl, metaRow);
            setGraphic(cell);
        }
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void loadNote(Note note) {
        currentNote  = note;
        newNoteMode  = false;
        titleEdit.setText(note.getTitle() != null ? note.getTitle() : "");
        subjectEdit.setText(note.getSubject() != null ? note.getSubject() : "");
        tagsEdit.setText(note.getTags() != null ? note.getTags() : "");
        contentEdit.setText(note.getContent() != null ? note.getContent() : "");
        lastSavedLabel.setText("Last saved: " + DateUtil.formatDateTime(note.getUpdatedAt()));
    }

    private void startNewNote() {
        currentNote = null;
        newNoteMode = true;
        listView.getSelectionModel().clearSelection();
        clearEditor();
        titleEdit.requestFocus();
    }

    private void saveCurrentNote() {
        String title = titleEdit.getText().trim();
        if (title.isBlank()) { UIFactory.showError("Note title is required."); return; }

        if (newNoteMode || currentNote == null) {
            Note n = new Note(title, contentEdit.getText(),
                    subjectEdit.getText().trim(), tagsEdit.getText().trim());
            db.saveNote(n);
            currentNote = n;
            newNoteMode = false;
        } else {
            currentNote.setTitle(title);
            currentNote.setContent(contentEdit.getText());
            currentNote.setSubject(subjectEdit.getText().trim());
            currentNote.setTags(tagsEdit.getText().trim());
            currentNote.setUpdatedAt(LocalDateTime.now());
            db.updateNote(currentNote);
        }
        lastSavedLabel.setText("Saved at " + LocalDateTime.now().toLocalTime().format(SAVED_TIME_FMT));
        refreshList(db.getAllNotes());
    }

    private void deleteCurrentNote() {
        if (currentNote == null) return;
        if (UIFactory.confirmDelete(currentNote.getTitle())) {
            db.deleteNote(currentNote.getId());
            currentNote = null;
            clearEditor();
            refreshList(db.getAllNotes());
        }
    }

    private void clearEditor() {
        titleEdit.clear(); subjectEdit.clear(); tagsEdit.clear(); contentEdit.clear();
        lastSavedLabel.setText("");
    }

    private void performSearch(String query) {
        refreshList(query == null || query.isBlank()
                ? db.getAllNotes()
                : db.searchNotes(query));
    }

    private void refreshList(List<Note> notes) {
        notesList.setAll(notes);
    }
}
