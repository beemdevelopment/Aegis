package me.impy.aegis;

import java.io.Serializable;

import me.impy.aegis.crypto.KeyInfo;

public class KeyProfile implements Serializable {
    public String Name;
    public String Icon;
    public String Code;
    public KeyInfo Info;
    public int ID;
}
