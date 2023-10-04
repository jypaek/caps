/**
 * VirtualCellid
 * - Convert cellid to easily readable sequential numbers
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

import java.util.Vector;

//# use easy-to-read cellid's
class VirtualCellid {

    Vector<Integer> vec;

    VirtualCellid() {
        vec = new Vector<Integer>();
    }

    public int get(int cellid) {
        Integer cid = new Integer(cellid);
        if (this.vec.contains(cid) == false) {
            this.vec.add(cid);
        }
        return (this.vec.indexOf(cid) + 1);
    }

    public int getOriginal(int vcid) {
        if (vcid <= vec.size())
            return vec.get(vcid - 1);
        return -1;
    }
}

