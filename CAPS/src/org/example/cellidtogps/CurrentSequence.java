/**
 * CurrentSequence
 * - Current(recent) sequence of cellid's
 * - Used for constructing the database at runtime
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

class CurrentSequence {

    public Sequence seq;
    public CellidPoint curr_cp;

    public static LogEventInterface logInt = null;

    CurrentSequence() {
        this.seq = new Sequence();
        this.curr_cp = null;
    }

    public Sequence get() {
        return this.seq;
    }

    public Sequence replace(Sequence s) {
        Sequence old = this.seq;
        this.seq = s;
        return old;
    }

    //# add the new values into the current sequence
    public void addCellidPoint(int cellid, double lat, double lon, long time) {
        int dist;
        if (this.seq.size() > 0) {
            dist = (int)Point.distance(lat, lon, this.seq.getLast().x(), this.seq.getLast().y());
        } else {
            dist = 0;
        }
        
        CellidPoint cp = this.seq.add(cellid, lat, lon, time);
        cp.dist = dist;
        this.curr_cp = cp;
    }

    //# add the new values into the current sequence
    public void addPointToCurrentCellidPoint(double lat, double lon, long time) {
        CellidPoint lcp = this.seq.getLast();
        if (lcp != null) {
            if (lcp != this.curr_cp) {
                println("ERRRRRRRRRROR\n");
                System.exit(1);
            }
            lcp.add(lat, lon, time);
        }
    }

    public Sequence startNew() {
        // hope this works... forgot how java handles memeory....
        this.seq = null;
        this.seq = new Sequence();  
        return this.seq;
    }

/////////////////////////////////////////////////////////

    public void add(CellidPoint cp) {
        this.seq.add(cp);
    }

    public CellidPoint add(int cellid, Point p) {
        return this.seq.add(cellid, p);
    }

    public CellidPoint add(int cellid, double x, double y, long time) {
        return this.seq.add(cellid, x, y, time);
    }

    public boolean contains(int cellid) {
        return this.seq.contains(cellid);
    }

    public boolean has(int cellid) {
        return this.seq.has(cellid);
    }

    public int length() {
        return this.seq.size();
    }
    
    public int size() {
        return this.seq.size();
    }

    public double cellid(int idx) {
        return this.seq.cellid(idx);
    }

    public double cid(int idx) {
        return cellid(idx);
    }

    public double x(int idx) {
        return this.seq.x(idx);
    }

    public double y(int idx) {
        return this.seq.y(idx);
    }

    public double time(int idx) {
        return this.seq.time(idx);
    }

    public String toString() {
        return this.seq.toString();
    }
    
    public String key() {
        return this.seq.key();
    }
    
    public int[] intArray() {
        return this.seq.intArray();
    }
    
    public String[] strArray() {
        return this.seq.strArray();
    }

    public Point followTrace(int cidIndex, long timediff) {
        return this.seq.followTrace(cidIndex, timediff);
    }
    
    public Point estimatePoint(int cidIndex, long timediff) {
        return this.seq.followTrace(cidIndex, timediff);
    }
    
    public static void println(String str) { if (logInt != null) logInt.println(str); }
}

