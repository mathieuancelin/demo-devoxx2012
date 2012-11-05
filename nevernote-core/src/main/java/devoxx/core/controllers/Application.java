package devoxx.core.controllers;

import devoxx.core.db.DB;
import devoxx.core.db.NotesModel;
import devoxx.core.db.NotesModel.Notes;
import devoxx.core.db.NotesModel.Note;
import devoxx.core.fwk.api.Controller;
import devoxx.core.util.F;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 *
 * @author mathieuancelin
 */
@Path("notes")
public class Application implements Controller {
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Note> notes() {
        final List<Note> notes = new ArrayList<Note>();
        NotesModel.DB.withConnection(new F.Function<Connection, F.Unit>() {
            @Override
            public F.Unit apply(Connection _) {
                notes.addAll(Notes._.findAll());
                return F.Unit.unit();
            }
        });
        return notes;
    }
}
