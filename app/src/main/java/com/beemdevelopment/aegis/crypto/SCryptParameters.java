package com.beemdevelopment.aegis.crypto;

import java.io.Serializable;

public class SCryptParameters implements Serializable {
    private int _n;
    private int _r;
    private int _p;
    private byte[] _salt;

    public SCryptParameters(int n, int r, int p, byte[] salt) {
        _n = n;
        _r = r;
        _p = p;
        _salt = salt;
    }

    public byte[] getSalt() {
        return _salt;
    }

    public int getN() {
        return _n;
    }

    public int getR() {
        return _r;
    }

    public int getP() {
        return _p;
    }
}
