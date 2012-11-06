package devoxx.api;

public class NoteDoneEvent {
    public final Long noteId;
    public final String date;
    public final String title;
    public final String content;

    public NoteDoneEvent(Long noteId, String date, String title, String content) {
        this.noteId = noteId;
        this.date = date;
        this.title = title;
        this.content = content;
    }
    
}
