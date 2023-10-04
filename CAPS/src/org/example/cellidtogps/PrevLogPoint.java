/**
 * PrevLogPoint
 * - Data structure for previous log
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Nov. 15, 2010
 **/
package org.example.cellidtogps;

class PrevLogPoint {

    // last log point
    public CellidPoint prev;

    // first point when cellid changed
    public Point first_in_cellid;

    public static final long MIN = Const.MIN;
    public static final long TIME_JUMP_THRESH = Const.TIME_JUMP_THRESH;
    public static final long TIME_STAY_THRESH = Const.TIME_STAY_THRESH;
    public static final int DIST_JUMP_THRESH = Const.DIST_JUMP_THRESH;

    public long jump_time;
    public long stay_time;
    public int jump_dist;
    public int stay_dist;

    boolean cellid_change_detected;
    boolean time_jump_detected;
    boolean long_stay_detected;
    boolean dist_jump_detected;

    boolean new_seq_detected;
    boolean new_diff_cell_detected;
    boolean new_same_cell_detected;
    boolean old_cell_detected;

    public static LogEventInterface logInt = null;

    PrevLogPoint() {
        this.prev = new CellidPoint();
        this.first_in_cellid = new Point();
        this.jump_time = 0;
        this.stay_time = 0;
        this.jump_dist = 0;
    }

    public void resetAll() {
        this.reset();
        this.resetFirstInCellid();
    }

    public CellidPoint get() { return prev; }
    public void set(int cellid, double x, double y, long time) { this.prev.set(cellid, x, y, time); }
    public void reset() { this.prev.set(0,0,0,0); }
    public double x() { return this.prev.x(); }
    public double y() { return this.prev.y(); }
    public long time() { return this.prev.time(); }
    public int cellid() { return this.prev.cellid(); }
    public Point makePoint() { return this.prev.makePoint(); }
    public String toString() { 
        String res = "prev: " + this.prev.toString() + "\n  (first in cellid: " + this.first_in_cellid.toString() + ")"; 
        return res; 
    }
    
    public Point getFirstInCellid() { return first_in_cellid; }
    public void setFirstInCellid(double x, double y, long time) { this.first_in_cellid.set(x, y, time); }
    public void resetFirstInCellid() { this.first_in_cellid.set(0,0,0); }
    public double xOfFirstInCellid() { return this.first_in_cellid.x(); }
    public double yOfFirstInCellid() { return this.first_in_cellid.y(); }
    public long timeOfFirstInCellid() { return this.first_in_cellid.time(); }
        
    public long time_passed_in_cellid(long time) {
        return time - first_in_cellid.time();
    }

    public long time_passed_since_prev(long time) {
        return time - prev.time();
    }

    public boolean cellid_change_detected(int cellid) {
        // did cell-id change??
        if (cellid != prev.cellid()) {
            println("" + cellid);
            return true;
        } else {
            return false;
        } 
    }

    public boolean time_jump_detected(long time) {
        // time jump of more than 60 minutes !!
        jump_time = time - prev.time();
        if ((this.prev.time() > 0) && ((jump_time > TIME_JUMP_THRESH) || (jump_time < 0))) {
            println("time jump detected : " + jump_time);
            return true;
        } else {
            return false;
        }
    }

    public boolean long_stay_detected(int cellid, long time) {
        // did I stay in one cell-id for long time?
        stay_time = time_passed_in_cellid(time);
        if ((this.first_in_cellid.time() > 0) && (stay_time > TIME_STAY_THRESH)
            && (time_jump_detected(time) == false)
            && (cellid == prev.cellid())) {
            println("long stay detected (" + cellid + ") stay_time: " + stay_time);
            return true;
        } else {
            return false;
        }
    }

    public boolean dist_jump_detected(int cellid, double lat, double lon, long time) {
        jump_dist = (int)Point.distance(lat, lon, this.prev.x(), this.prev.y());
        if ((this.prev.x() != 0) && (jump_dist > DIST_JUMP_THRESH)
            && (time_jump_detected(time) == false)
            ) {
            println("dist jump detected : " + jump_dist);
            println(String.format(" >> %.6f %.6f  <=>  %.6f %.6f", lat, lon, this.prev.x(), this.prev.y()));
            return true;
        } else {
            return false;
        }
    }

    public void detect_change_from_prev_log(int cellid, double lat, double lon, long time) {
        cellid_change_detected = cellid_change_detected(cellid);
        time_jump_detected = time_jump_detected(time);
        long_stay_detected = long_stay_detected(cellid, time);
        dist_jump_detected = dist_jump_detected(cellid, lat, lon, time);
        
        new_seq_detected = false;
        new_diff_cell_detected = false;
        new_same_cell_detected = false;
        old_cell_detected = false;

        if (time_jump_detected == true) { 
            logInt.println("new sequence detected ($cellid)\n");
            new_seq_detected = true; 
        } else if (cellid_change_detected == true) { 
            new_diff_cell_detected = true; 
        } else if (long_stay_detected == true) { 
            logInt.println("new same cellid within current sequence detected ($cellid)\n");
            new_same_cell_detected = true; 
        } else {
            old_cell_detected = true; 
        } 
    }

    public long jump_time() { return this.jump_time; }
    public long stay_time() { return this.stay_time; }
    public static void println(String str) { if (logInt != null) logInt.println(str); }
}

