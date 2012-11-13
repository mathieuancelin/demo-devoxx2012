/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package devoxx.core.db;

import org.h2.Driver;

import static devoxx.core.db.DB.*;
import devoxx.core.db.NotesModel.Note;
import devoxx.core.fwk.F.Function;
import devoxx.core.fwk.F.Option;
import devoxx.core.fwk.F.Unit;
import java.sql.Connection;
import java.util.List;
import javax.inject.Inject;
import javax.xml.bind.annotation.XmlRootElement;

public class NotesModel {

    public static final DB DB = DB(provider(new Driver(), "jdbc:h2:/tmp/nevernote", "sa", ""));

    public static class Notes extends Table<Note> {
        
        public final Extractor<Long> id = get(Long.class, "id");
        public final Extractor<String> date = get(String.class, "date");
        public final Extractor<String> title = get(String.class, "title");
        public final Extractor<String> content = get(String.class, "content");
        public final Extractor<Boolean> done = get(Boolean.class, "done");

        public static final Notes _ = new Notes().init(Note.class, "notes");

        @Override
        public ExtractorSeq<Note> all() { return seq(id, date, title, content, done); }
    }
    
    @XmlRootElement
    public static class Note extends Model {
        public Long id;
        public String date;
        public String title;
        public String content;
        public Boolean done;

        public Note() {}

        public Note(Long id, String date, String title, String content, Boolean done) {
            this.id = id;
            this.date = date;
            this.title = title;
            this.content = content;
            this.done = done;
        }
        public Long getId() {
            return id;
        }
    }
    
    public static void initDB() {
        DB.withConnection(new Function<Connection, Unit>() {
            @Override
            public Unit apply(Connection _) {
                
                Notes._.ddlDelete();
                
                SQL( 
                    "create table notes (\n" + 
                    "id                    bigint not null,\n" + 
                    "date                  varchar(1000) not null,\n" + 
                    "title                 varchar(1000) not null,\n" + 
                    "content               varchar(1000) not null,\n" +
                    "done                  boolean not null, \n" +
                    "constraint pk_note    primary key (id))\n;"
                ).executeUpdate();
                
                Notes._.insertAll(
                    new Note(1L, "11-11-2012", "Train", "Take the train to Antverp", false),
                    new Note(2L, "11-11-2012", "Wristband", "Take my wristand at Devoxx registration desk on sunday evening", false),
                    new Note(3L, "12-11-2012", "Weld-OSGi", "Do not forget to show up at the Weld-OSGi in Action session, Devoxx, Monday at 5:25PM", false)
                );  
                return Unit.unit();
            }
        });
    }
}
