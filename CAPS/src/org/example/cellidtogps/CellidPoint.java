/**
 * CellidPoint
 * - Data structure for each CellID and its associated GPS points
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

import java.util.Vector;

class CellidPoint {

    private int cellid;
    private double x;       // x-coordinate : first latitude entering this cellid
    private double y;       // y-coordinate : first longitude entering this cellid
    private long time;      // first time entering this cellid

    public int dist;

    Vector<Point> points;

    public static boolean USE_POINT_INTERPOLATE = true;

    public static LogEventInterface logInt = null;
    
    CellidPoint() {
        set(0, 0, 0, 0);
        this.points = new Vector<Point>();
    }

    CellidPoint(int cellid, double x, double y, long time) {
        set(cellid, x, y, time);
        this.points = new Vector<Point>();
        add(x, y, time);
    }

    CellidPoint(int cellid, Point p) {
        set(cellid, p.x, p.y, p.time);
        this.points = new Vector<Point>();
        add(p);
    }

    public void set(int cellid, double x, double y, long time) {
        this.cellid = cellid;
        this.x = x;
        this.y = y;
        this.time = time;
    }

    public String toString() {
        String res = String.format("cellid = %d, x = %.6f, y = %.6f, time = %d\n", cellid, x, y, time);
        res = res + points.toString();
        return res;
    }

    public void clear() {
        points.clear();
    }

    public void add(double x, double y, long time) {
        Point p = new Point(x, y, time);
        points.add(p);
    }

    public void add(Point p) {
        points.add(p);
    }

    public double x() { return this.x; }
    public double y() { return this.y; }
    public long time() { return this.time; }

    public int cellid() { return this.cellid; }
    public double x(int idx) { return points.get(idx).x; }
    public double y(int idx) { return points.get(idx).y; }
    public long time(int idx) { return points.get(idx).time; }

    public Point get(int idx) { return points.get(idx); }
    public Point getPoint(int idx) { return points.get(idx); }
    public Point getFirst() { return points.get(0); }
    public Point getLast() { return points.lastElement(); }
    public Point firstPoint() { return points.get(0); }
    public Point lastPoint() { return points.lastElement(); }

    public Point makePoint() {
        Point p = new Point(this.x, this.y, this.time);
        return p;
    }

    private Point estimate_based_on_timediff(long timediff, boolean interpolate) {
        Point p = null;
        long prev_time_diff;
        long next_time_diff;

        if (points.size() == 0) {
            println("  -- FOLLOW-EMPTY (error?)\n");
            return null;
        }

        prev_time_diff = points.get(0).time - this.time;
        next_time_diff = points.lastElement().time - this.time;

        if (timediff <= prev_time_diff) {
            p = points.get(0);
            println(String.format("  -- FOLLOW-OUT 0/%d (%d < %d), (%.6f, %.6f)",
                                points.size(), timediff, prev_time_diff, p.x, p.y));
            if (timediff < prev_time_diff)
                p.flag = -1;
            else 
                p.flag = 0;
        }
        else if (timediff >= next_time_diff) {
            p = points.lastElement();
            println(String.format("  -- FOLLOW-OUT %d/%d (%d > %d), (%.6f, %.6f)",
                                points.size(), points.size(), timediff, next_time_diff, p.x, p.y));
            if (timediff > next_time_diff)
                p.flag = 1;
            else 
                p.flag = 0;
        }
        else {
            for (int i = 0; i + 1 < points.size(); i++) {
                prev_time_diff = points.get(i).time - this.time;
                next_time_diff = points.get(i+1).time - this.time;

                if ((prev_time_diff <= timediff) && (next_time_diff >= timediff)) {
                    if (interpolate == true) {
                        p = Point.interpolate(points.get(i), prev_time_diff, points.get(i+1), next_time_diff, timediff);
                        println(String.format("  -- FOLLOW+INTERPOLATE %d/%d (%d < %d < %d), (%.6f, %.6f)--(%.6f, %.6f)",
                                i, points.size(), prev_time_diff, timediff, next_time_diff,
                                points.get(i).x, points.get(i).y, points.get(i+1).x, points.get(i+1).y));
                    } else {
                        if ((timediff - prev_time_diff) < (next_time_diff - timediff)) {
                            p = points.get(i);
                        } else {
                            p = points.get(i+1);
                        }
                        println(String.format("  -- FOLLOW+CLOSER %d/%d (%d < %d < %d), (%.6f, %.6f)", 
                                i, points.size(), prev_time_diff, timediff, next_time_diff, p.x, p.y));
                    }
                    break;
                }
            }
            p.flag = 0;
        }
        return p;
    }

    public Point followTrace(long timediff) {
        return estimate_based_on_timediff(timediff, USE_POINT_INTERPOLATE);
    }
    public Point followTraceWithInterpolate(long timediff) {
        return estimate_based_on_timediff(timediff, true);
    }
    public Point followTraceToCloser(long timediff) {
        return estimate_based_on_timediff(timediff, false);
    }

    /* just aliases */
    public Point estimatePoint(long timediff) {
        return estimate_based_on_timediff(timediff, USE_POINT_INTERPOLATE);
    }
    public Point estimatePointWithInterpolate(long timediff) {
        return estimate_based_on_timediff(timediff, true);
    }
    public Point estimatePointToCloser(long timediff) {
        return estimate_based_on_timediff(timediff, false);
    }

    public int isOutOfBound(long timediff) {
        if (points.size() == 0) {
            return 2;
        } else {
            if (timediff < points.get(0).time - this.time) {
                return -1;
            }
            if (timediff > points.lastElement().time - this.time) {
                return 1;
            }
        }
        return 0;
    }

    public static Point interpolate(CellidPoint p1, CellidPoint p2, long timediff) {
        return interpolate(p1.x, p1.y, 0, p2.x, p2.y, p2.time - p1.time, timediff);
    }
    
    public static Point interpolate(CellidPoint p1, long t1, CellidPoint p2, long t2, long time) {
        return interpolate(p1.x, p1.y, t1, p2.x, p2.y, t2, time);
    }
    
    public static Point interpolate(double x1, double y1, long t1, double x2, double y2, long t2, long tm) {
        return Point.interpolate(x1, y1, t1, x2, y2, t2, tm);
    }

    public int length() { return points.size(); }
    public int size() { return points.size(); }
    public void println(String str) { if (logInt != null) logInt.println(str); }
}

