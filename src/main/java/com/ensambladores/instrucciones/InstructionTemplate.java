package com.ensambladores.instrucciones;

import com.ensambladores.sym.TamañoSimbolo;
import com.google.common.primitives.UnsignedBytes;

import java.util.Arrays;

public class InstructionTemplate {
    private String stringTemplate;
    private MMMField mmm = MMMField.MMM000;
    private OOModifierBits ooo = OOModifierBits.OO00;
    private RRRField rrr = RRRField.RRR000;

    private TamañoSimbolo w = TamañoSimbolo.BYTE;

    private String disp = "1111111111111111";

    public InstructionTemplate(String template){
        this.stringTemplate = template;
    }

    public void setStringTemplate(String stringTemplate) {
        this.stringTemplate = stringTemplate;
    }

    public void setMmm(MMMField mmm) {
        this.mmm = mmm;
    }

    public void setOoo(OOModifierBits ooo) {
        this.ooo = ooo;
    }

    public void setRrr(RRRField rrr) {
        this.rrr = rrr;
    }

    public String buildString(){
        String s = this.stringTemplate;
        //System.out.println("mmm:"+ mmm.toString());
        //System.out.println("oo:"+ooo.toString());
        s = s.replaceAll("mmm", this.mmm.toString());
        s = s.replaceAll("oo", this.ooo.toString());
        s = s.replaceAll("rrr", this.rrr.toString());
        s = s.replaceAll("w", this.w == TamañoSimbolo.BYTE ? "0" : "1");
        // Esto se modifica directamente en @exitInst
        s = s.replaceAll("disp", "");
        return s;
    }

    public byte[] buildBytes(){
        byte[] buf = new byte[32];
        String bits = this.buildString();
        //System.out.println("bits:"+bits);
        //System.out.println("bits: " + bits);
        int bitStrSize = bits.length();
        assert bitStrSize % 8 == 0;
        int byteArrLength = Math.floorDiv(bitStrSize,8);
        for(int i=0; i<byteArrLength;i++){
            String slice = bits.substring(i*8, (i*8)+8);
            byte b = UnsignedBytes.parseUnsignedByte(slice,2);
            //System.out.println("b:"+UnsignedBytes.toString(b,2));
            buf[i] = b;
        }
        return Arrays.copyOfRange(buf,0, byteArrLength);
    }

    public void setW(TamañoSimbolo w) {
        this.w = w;
    }

    public void setDisp(String disp) {
        // Desplazamiento de 16 bits
        assert disp.length() == 16 ;
        this.disp = disp;
    }
}
