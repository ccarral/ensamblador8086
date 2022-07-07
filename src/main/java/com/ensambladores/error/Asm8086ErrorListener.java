package com.ensambladores.error;

import java.io.OutputStream;
import java.io.PrintStream;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class Asm8086ErrorListener extends BaseErrorListener {


    private String errMsgBuf = new String();

    private PrintStream out;
    private boolean poisoned = false;
    private boolean longTermPoisoned = false;

    public boolean isPoisoned() {
        return poisoned;
    }

    public boolean isLongTermPoisoned(){
        return this.longTermPoisoned;
    }

    public void setPoisoned(boolean val){
        this.poisoned = val;
    }

    public Asm8086ErrorListener(PrintStream out) {
        this.out = out;
    }

    public String getErrMsgBuf() {
        return errMsgBuf;
    }
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e) {
        this.poisoned = true;
        this.longTermPoisoned = true;
        this.writeToErrMsgBuf(String.format("err: %s", msg));
    }

    public void writeToErrMsgBuf(String msg){
        this.errMsgBuf = this.errMsgBuf.concat(msg);
    }

    public void flushErrMsgBuf(){
        this.errMsgBuf = new String();
    }

}
