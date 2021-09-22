package hu.blackbelt.structured.map.proxy.entity;

import lombok.EqualsAndHashCode;

import java.util.Date;

public interface Event {

    String getTitle();
    void setTitle(String title);

    Date getDate();
    void setDate(Date date);

    boolean isPrivate();
    void setPrivate(boolean _private);

    UpperCaseString getRoom();
    void setRoom(UpperCaseString room);

    String getNotes();
    void setNotes(String notes);

    @EqualsAndHashCode
    class UpperCaseString {

        private String _internal;

        private UpperCaseString() {
        }

        public static UpperCaseString parse(String str) {
            UpperCaseString instance = new UpperCaseString();
            instance._internal = str != null ? str.toUpperCase() : null;
            return instance;
        }

        @Override
        public String toString() {
            return String.valueOf(_internal);
        }
    }
}
