/**
 * RunCellidSeq
 * - Currently running cellid sequence, used for estimating current position
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

import java.util.Vector;
import java.util.HashMap;

class RunCellidSeq {

    Vector<Integer> vec;
    HashMap<Integer, Integer> map;

    public static int MAX_RUN_LEN = Const.MAX_RUN_LEN;

    public static LogEventInterface logInt = null;

    RunCellidSeq() {
        this.vec = new Vector<Integer>();
        this.map = new HashMap<Integer, Integer>();
    }

    //# add the new values into the current sequence
    public void insert_to_run_sequence (int aCellid, long aTime) {
        int old_id;
    
        if (vec.size() > MAX_RUN_LEN) {
            println("ERROR!!!!!!\n");
        } else if (vec.size() == MAX_RUN_LEN) {
            old_id = vec.remove(0);
            if (map.get(old_id) > 1) {
                map.put(old_id, map.get(old_id) - 1);
            } else {
                map.remove(old_id);
            }
        }
        /*
            if (map.containsKey(aCellid)) {
                while(vec.get(0) != aCellid) {
                    old_id = vec.remove(0);
                    if (map.get(old_id) > 1) {
                        map.put(old_id, map.get(old_id) - 1);
                    } else {
                        map.remove(old_id);
                    }
                }
                if (vec.size() == 1) {
                    old_id = vec.remove(0);
                    if (map.get(old_id) > 1) {
                        map.put(old_id, map.get(old_id) - 1);
                    } else {
                        map.remove(old_id);
                    }
                }
            }
        */
        vec.add(aCellid);     // push first cellid
        if (map.containsKey(aCellid)) {
            map.put(aCellid, map.get(aCellid) + 1);
        } else {
            map.put(aCellid, 1);
        }
    }

    public int[] intArray() {
        int[] carray = new int[vec.size()];
        for (int i = 0; i < vec.size(); i++) {
            carray[i] = vec.get(i).intValue();
        }
        return carray;
    }
    
    public String[] strArray() {
        String[] carray = new String[vec.size()];
        for (int i = 0; i < vec.size(); i++) {
            carray[i] = Integer.toString(vec.get(i));
        }
        return carray;
    }

    public void clear() {
        vec.clear();
        map.clear();
    }
    
    public String toString() {
        String key = "";
        for (int i = 0; i < vec.size(); i++) {
            key = key + vec.get(i) + " ";
        }
        return key;
    }
    
    public int size() { return vec.size(); }
    public int length() { return vec.size(); }
    public static void println(String str) { if (logInt != null) logInt.println(str); }
}

