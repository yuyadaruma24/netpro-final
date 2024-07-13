package botu;

import java.io.Serializable;

public class TerminalInput implements Serializable {

    private static final long serialVersionUID = 1L;

    String message;
    String content;
    String date;
    String task;
    String rgba[];
    String name;
    String detail;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate(){
        return date;
    }

    public void setDate(String date){
        this.date = date;
    }

    public String getTask(){
        return task;
    }

    public void setTask(String task){
        this.task = task;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setRgba(String[] rgba){
        this.rgba = rgba;
    }

    public String[] getRgba(){
        return rgba;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public void setDetail(String detail){
        this.detail = detail;
    }

    public String getDetail(){
        return detail;
    }
}
