package com.ensambladores.error;

import java.io.OutputStream;
import java.io.PrintStream;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class Asm8086ErrorListener extends BaseErrorListener {

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

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e) {
        this.poisoned = true;
        this.longTermPoisoned = true;
        this.out.printf("err [%d:%d]: %s\n", line, charPositionInLine, msg);
    }

}
