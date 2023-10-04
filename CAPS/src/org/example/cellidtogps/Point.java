/**
 * Point
 * - Data structure for each GPS points
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

import java.lang.Math;

class Point {

    public double x;       // x-coordinate : latitude * 1000000
    public double y;       // y-coordinate : longitude * 1000000
    public long time;      // time
    public int flag;       //

    Point() {
        set(0.0, 0.0, 0);
    }

    Point(double x, double y) {	
        set(x, y, 0);
    }

    Point(double x, double y, long time) {
        set(x, y, time);
    }
    
    public double x() { return this.x; }
    public double y() { return this.y; }
    public long time() { return this.time; }

    public void set(double x, double y, long time) {
        this.x = x;
        this.y = y;
        this.time = time;
    }
    
    public static double cut6digit(double in) {
        return ((long)(in*1000000.0))/1000000.0;
    }

    public static int compare(Point p1, Point p2) {
        if (p1.time < p2.time)
            return -1;
        if (p1.time > p2.time)
            return 1;
        return (0);
    }
    
    public boolean equals(Point p2) {
        if ((this.x == p2.x) && (this.y == p2.y) && (this.time == p2.time))
            return true;
        return false;
    }
    
    public double distance(Point p2) {
        return Point.distance(this, p2);
    }

    public static double distance(double aLat1, double aLon1, double aLat2, double aLon2) {
        double pi80 = Math.PI / 180.0; 
        aLat1 *= pi80;
        aLon1 *= pi80;
        aLat2 *= pi80;
        aLon2 *= pi80;

        double r = 6372.797;    // mean radius of Earth in km
        double dlat = aLat2 - aLat1;
        double dlng = aLon2 - aLon1;
        double a = Math.sin(dlat / 2.0) * Math.sin(dlat / 2.0) + Math.cos(aLat1) * Math.cos(aLat2) * Math.sin(dlng / 2.0) * Math.sin(dlng / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double km = r * c;

        return (double)((int)((km * 1000.0) * 100.0))/100.0;     // in meters
        //return (miles ? (km * 0.621371192) : km);
    }

    public static double distance(Point p1, Point p2) {
        double aLat1 = p1.x;
        double aLon1 = p1.y;
        double aLat2 = p2.x;
        double aLon2 = p2.y;
        return distance(aLat1, aLon1, aLat2, aLon2);
    }

    public static Point interpolate(Point p1, Point p2, long time) {
        return interpolate(p1.x, p1.y, p1.time, p2.x, p2.y, p2.time, time);
    }
    
    public static Point interpolate(Point p1, long t1, Point p2, long t2, long time) {
        return interpolate(p1.x, p1.y, t1, p2.x, p2.y, t2, time);
    }
    
    public static Point interpolate(double x1, double y1, long t1, double x2, double y2, long t2, long tm) {
        Point res = new Point();

        if (t1 == t2) {
            res.x = (x1 + x2)/2.0;
            res.y = (y1 + y2)/2.0;
            res.time = (long)((t1 + t2)/2.0);
        }
        else if (tm >= t2) {
            res.x = x2;
            res.y = y2;
            res.time = t2;
        }
        else if (tm <= t1) {
            res.x = x1;
            res.y = y1;
            res.time = t1;
        }
        else {
            res.x = (((double)(tm - t1)*(x2 - x1))/(double)(t2 - t1)) + x1;
            res.y = (((double)(tm - t1)*(y2 - y1))/(double)(t2 - t1)) + y1;
            res.time = tm;
        }
        res.x = Point.cut6digit(res.x);
        res.y = Point.cut6digit(res.y);

        return res;
    }

    public String toString() {
        String res = String.format("x = %.6f, y = %.6f, time = %d\n", x, y, time);
        return res;
    }
}


