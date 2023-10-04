/**
 * Sequence
 * - Data structure a sequence of CellID points
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Nov. 28, 2010
 **/
package org.example.cellidtogps;

import java.util.HashMap;
import java.util.Vector;


public class Sequence implements Comparable<Sequence> {

    // list of cellids
    Vector<CellidPoint> vec;
    
    // for counting # cellid's within this sequence
    HashMap<Integer, Integer> map;   

    int cnt;
    int rate;
    int seqno;

    public int ptype;
    public static LogEventInterface logInt = null;
    
    Sequence() {
        vec = new Vector<CellidPoint>();
        map = new HashMap<Integer,Integer>();
        cnt = 0;
        rate = 0;
        seqno = 0;
    }

    public void add(CellidPoint cp) {
        vec.add(cp);

        int cid = cp.cellid();
        if (map.containsKey(cid)) {
            //println("add cellid to sequence - already contains " + cp.cellid());
            map.put(cid, map.get(cid) + 1);
        } else {
            //println("add cellid to sequence - " + cp.cellid());
            map.put(cid, 1);
        }
    }

    public CellidPoint add(int cellid, Point p) {
        CellidPoint cp = new CellidPoint(cellid, p);
        this.add(cp);
        return cp;
    }

    public CellidPoint add(int cellid, double x, double y, long time) {
        CellidPoint cp = new CellidPoint(cellid, x, y, time);
        this.add(cp);
        return cp;
    }

    public void unshift(CellidPoint cp) {
        vec.add(0, cp);
        int cid = cp.cellid();
        if (map.containsKey(cid)) {
            map.put(cid, map.get(cid) + 1);
        } else {
            map.put(cid, 1);
        }
    }

    public CellidPoint unshift(int cellid, Point p) {
        CellidPoint cp = new CellidPoint(cellid, p);
        this.unshift(cp);
        return cp;
    }

    public CellidPoint unshift(int cellid, double x, double y, long time) {
        CellidPoint cp = new CellidPoint(cellid, x, y, time);
        this.unshift(cp);
        return cp;
    }

    public void push(CellidPoint cp) { add(cp); }
    public CellidPoint push(int cellid, Point p) { return add(cellid, p); }
    public CellidPoint push(int cellid, double x, double y, long time) { return add(cellid, x, y, time); }
    
    public CellidPoint shift() { return remove(0); }
    public CellidPoint pop() { return remove(vec.size()-1); }


    public boolean remove(CellidPoint cp) {
        int idx = vec.indexOf(cp);
        CellidPoint rcp = remove(idx);
        return (cp == rcp);
    }

    public CellidPoint remove(int index) {
        CellidPoint cp = vec.remove(index);

        if (cp != null) {
            int cid = cp.cellid();
            if (map.containsKey(cid)) {
                if (map.get(cid) == 1) {
                    map.remove(cid);
                } else {
                    map.put(cid, map.get(cid) - 1);
                }
            }
        }
        return cp;
    }

    public void clear() {
        vec.clear();
        map.clear();
    }

    public boolean contains(int cellid) {
        if (this.has(cellid)) {
            println("check - already contains " + cellid);
            return true;
        } else {
            println("check - does not contain " + cellid);
            return false;
        }
    }

    public boolean has(int cellid) {
        int cid = cellid;
        if (map.containsKey(cid)) {
            return true;
        } else {
            return false;
        }
    }

    public CellidPoint get(int index) { if (vec.size() > index) return vec.get(index); return null; }
    public CellidPoint getLast() { if (vec.size() > 0) return vec.lastElement(); return null; }
    public CellidPoint getFirst() { if (vec.size() > 0) return vec.get(0); return null; }

    public int length() { return this.size(); }
    public int size() { return vec.size(); }

    public double cellid(int idx) { return vec.get(idx).cellid(); }
    public double cid(int idx) { return cellid(idx); }
    public double x(int idx) { return vec.get(idx).x(); }
    public double y(int idx) { return vec.get(idx).y(); }
    public long time(int idx) { return vec.get(idx).time(); }

    public String toString() {
        String res = "Sequence: " + key() + "\n";
        res = res + vec.toString();
        return res;
    }
    
    public String key() {
        CellidPoint cp;
        String key = "";
        for (int i = 0; i < vec.size(); i++) {
            cp = vec.get(i);
            key = key + cp.cellid() + " ";
        }
        return key;
    }
    
    public int[] intArray() {
        CellidPoint cp;
        int[] carray = new int[vec.size()];
        for (int i = 0; i < vec.size(); i++) {
            cp = vec.get(i);
            carray[i] = cp.cellid();
        }
        return carray;
    }
    
    public String[] strArray() {
        CellidPoint cp;
        String[] carray = new String[vec.size()];
        for (int i = 0; i < vec.size(); i++) {
            cp = vec.get(i);
            carray[i] = Integer.toString(cp.cellid());
        }
        return carray;
    }

    public Point followCellidTrace(int cidIndex, long timediff) {
        Point p = null;

        if (cidIndex < vec.size()) {
            CellidPoint cp = vec.get(cidIndex);
            p = cp.followTrace(timediff);
        }
        return p;
    }
    
    public Point followNextCellidTrace(int cidIndex, long timediff) {
        Point p = null;
        int next_idx = cidIndex + 1;

        if (next_idx < vec.size()) {
            CellidPoint ncp = vec.get(next_idx);
            long time_diff_offset = ncp.time() - vec.get(cidIndex).time();
            long cp_time_diff = timediff - time_diff_offset;  // time from beginning of cp

            println(String.format("  -- FOLLOW-NEXT %d", ncp.cellid()));
            // follow the point withing cellid of cp !!
            p = ncp.followTrace(cp_time_diff);
        }
        return p;
    }

    public Point followSequenceTrace(int cidIndex, long timediff) {
        Point p = null;

        for (int i = cidIndex; i + 1 < vec.size(); i++) {
            CellidPoint cp = vec.get(i);
            CellidPoint ncp = vec.get(i+1);
            long prev_time_diff = cp.time() - vec.get(cidIndex).time();
            long next_time_diff = ncp.time() - vec.get(cidIndex).time();

            if ((prev_time_diff <= timediff) && (next_time_diff >= timediff)) {
                Point pp = cp.firstPoint();    // first point of current cellpoint
                Point np = ncp.firstPoint();  // first point of next cellpoint
                println(String.format("  -- EXT-INTERPOLATE  (%d, %.6f, %.6f) and (%d, %.6f %.6f) between time (%d <= %d <= %d)",
                            cp.cellid(), pp.x, pp.y, cp.cellid(), np.x, np.y, prev_time_diff, timediff, next_time_diff));
                p = Point.interpolate(pp, prev_time_diff, np, next_time_diff, timediff);
                break;
            }
        }
        if (p == null) {
            if (cidIndex == vec.size() - 1) {
                p = vec.get(cidIndex).makePoint();
                p.flag = 1;
                println("  -- EXT-LAST");
            } else {
                println("  -- EXT-EMPTY");
            }
        }
        return p;
    }

    public Point followSequenceCellidTrace(int cidIndex, long timediff) {
        Point p = null;

        for (int i = cidIndex; i + 1 < vec.size(); i++) {
            CellidPoint cp = vec.get(i);
            CellidPoint ncp = vec.get(i+1);
            long prev_time_diff = cp.time() - vec.get(cidIndex).time();
            long next_time_diff = ncp.time() - vec.get(cidIndex).time();
            long cp_time_diff = timediff - prev_time_diff;  // time from beginning of cp

            if ((prev_time_diff <= timediff) && (next_time_diff >= timediff)) {

                println(String.format("  -- FOLLOW-EXT-CELLID %d", cp.cellid()));
                // follow the point withing cellid of cp !!
                p = cp.followTrace(cp_time_diff);

                if (p.flag == 1) {
                    Point lp = cp.lastPoint();     // last point of current cellpoint
                    Point nfp = ncp.firstPoint();  // first point of next cellpoint
                    long lp_time_diff = lp.time - cp.time();
                    long nfp_time_diff = nfp.time - cp.time();
                    if (cp_time_diff < nfp_time_diff) {
                        println(String.format("  -- EXT-INTERPOLATE  (%d, %.6f, %.6f) and (%d, %.6f %.6f) between time (%d <= %d < %d)",
                                            cp.cellid(), lp.x, lp.y, ncp.cellid(), nfp.x, nfp.y, lp_time_diff, cp_time_diff, nfp_time_diff));
                        p = Point.interpolate(lp, lp_time_diff, nfp, nfp_time_diff, cp_time_diff);
                    } else {
                        println("ERROR in followSequenceCellidTrace?\n"); System.exit(1);
                    }
                }
                break;
            }
        }
        if (p == null) {
            if (cidIndex == vec.size() - 1) {
                // follow the point withing cellid of cp !!
                println("  -- FOLLOW-LAST");
                CellidPoint cp = vec.get(cidIndex);
                p = cp.followTrace(timediff);
            } else {
                println("  -- FOLLOW-EMPTY");
            }
        }

        return p;
    }

    public Point followTrace(int cidIndex, long timediff) {

        CellidPoint cp = vec.get(cidIndex);
        Point p = cp.followTrace(timediff);
        ptype = 3;

        if ((p == null) || (p.flag == 1)) { 
            long ptime = p.time - cp.time();

            if (timediff - ptime < Const.OUTTIME) {
                println(String.format("time passed timediff within 180 sec of ptime %d, try next cellid", ptime));
                p = followNextCellidTrace(cidIndex, timediff);
                ptype = 5;

                if ((p == null) || (p.flag == 1)) { 
                    if (timediff - ptime < Const.OUTTIME2) {
                        println(String.format("time passed timediff within 90 sec of ptime %d, follow all trace", ptime));
                        p = followSequenceCellidTrace(cidIndex, timediff);
                        ptype = 6;

                        if ((p != null) && (p.flag == 1)) { 
                            p = null;
                            ptype = 4;
                        }    
                    } else {
                        p = null;
                        ptype = 9;
                    }    
                }    
            } else {
                println("time passed timediff outside 180 sec of ptime ptime\n");
                p = null;
                ptype = 7;
            }    
        }
        if (p != null) { p.flag = 0; }
        return p;
    }

    public Point estimatePoint(int cidIndex, long timediff) {
        return followTrace(cidIndex, timediff);
    }

    public Point estimatePointWithinCellid(int cidIndex, long timediff) {
        return followCellidTrace(cidIndex, timediff);
    }

    public Point estimatePointInSequenceTrace(int cidIndex, long timediff) {
        return followSequenceTrace(cidIndex, timediff);
    }

    public Point estimatePointInSequenceCellidTrace(int cidIndex, long timediff) {
        return followSequenceCellidTrace(cidIndex, timediff);
    }

    public void addLastPoint(double lat, double lon, long time) {
        CellidPoint lcp = this.getLast();
        if (lcp != null) {
            lcp.add(lat, lon, time);
        }
    }

    public int compareTo(Sequence s) {
        return this.key().compareTo(s.key());
    }

    public int increment_rate() { return ++this.rate; }
    public int decrement_rate() { return --this.rate; }
    public void println(String str) { if (logInt != null) logInt.println(str); }
}

