package me.impy.aegis;

import java.io.Serializable;
import java.security.Key;

import me.impy.aegis.crypto.KeyInfo;

public class KeyProfile implements Serializable {
    public String Name;
    public String Icon;
    public String Code;
    public String Issuer;
    public KeyInfo Info;
    public int Order;
    public int ID;


    public int compareTo(KeyProfile another) {
        if (this.Order>another.Order){
            return -1;
        }else{
            return 1;
        }
    }
}
