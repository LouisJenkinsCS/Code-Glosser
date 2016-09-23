/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

/**
 *
 * @author Louis
 */
public class Note {
    private String msg;
    private String id;
    private int start;
    private int end;

    public Note(String msg, String id, int start, int end) {
        this.msg = msg;
        this.id = id;
        this.start = start;
        this.end = end;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "Note{" + "msg=" + msg + ", start=" + start + ", end=" + end + '}';
    }
}
