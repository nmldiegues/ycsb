package com.yahoo.ycsb.db;

import java.io.Serializable;

public class MagicKey implements Serializable {

    private static final long serialVersionUID = -1072474466685642719L;
    public static int CLIENTS;
    public static int NUMBER;
    
    public final String key;
    public final int num;
    public final int node;
    
    public MagicKey(String key, int num) {
	this.key = key;
	this.num = num;
	this.node = (int) Math.floor((double)((num * CLIENTS) / NUMBER));
    }
    
    public MagicKey(int node, String key, int num) {
	this.node = node;
	this.num = num;
	this.key = key;
    }

    @Override
    public boolean equals (Object o) {
       if (this == o) return true;
       if (o == null || getClass() != o.getClass()) return false;

       MagicKey other = (MagicKey) o;

       if (this.hashCode() != other.hashCode()) return false;
       return this.key == other.key && this.node == other.node && this.num == other.num;
    }
    
    public int hashCode() {
	return num;
    }
    
    @Override
    public String toString() {
        return this.node + " owns " + this.key; 
    }
}
