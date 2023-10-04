/**
 * FilterParseLog
 * - Tool for parsing the raw GPS logfile
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

class FilterParseLog {

    public int linecnt;
    public int skip;
    public int filtered;
    public int acc_filtered;

    public static final int FILTER_VALID = 0;
    public static final int FILTER_NEXT = 1;
    public static final int FILTER_BREAK = 2;
    public static final int FILTER_SKIP = 3;

    String cdate; String ctime;
    double lat; double lon; long time; int acc;
    int cellid; int lac; int rssi;
    double netlat; double netlon; int netacc;

    FilterParseLog() {
        reset();
        skip = 1;
    }

    public void reset() {
        linecnt = 0;
        filtered = 0;
        acc_filtered = 0;
        resetData();
    }

    public int run(String text) {
        String line = text.trim();
        // skip all lines containing // 
        if (line.contains("#") == true) {
            return FILTER_NEXT;
        }
        if (line.contains("BREAK") == true) {
            return FILTER_BREAK;
        }
        if ((line.matches("\\s+") == true) ||
            (line.equals("") == true)) {
            return FILTER_NEXT;
        }

        //// get GPS position
        String[] splited = line.split("\\s+");

        try {
            if ((splited.length >= 12) &&
                (splited[0].matches("\\d+-\\d+-\\d+") || splited[1].matches("\\d+:\\d+:\\d+") 
                || splited[11].matches("RAWLOG"))) {
                //2010-07-03 07:31:30 37.362957 -122.002096 1278167490 32.0 29313 40495 2 37.366053 -122.000463 1077.0
                this.setData(
                    splited[0],
                    splited[1],
                    Double.parseDouble(splited[2]),
                    Double.parseDouble(splited[3]),
                    Long.parseLong(splited[4]),
                    (int)(Double.parseDouble(splited[5])),
                    Integer.parseInt(splited[6]),
                    Integer.parseInt(splited[7]),
                    Integer.parseInt(splited[8]),
                    Double.parseDouble(splited[9]),
                    Double.parseDouble(splited[10]),
                    (int)(Double.parseDouble(splited[11]))
                );
                if ((this.netlat == 0.0) || (this.netlon == 0.0)) { 
                    filtered += 1;
                    return FILTER_NEXT; 
                }
            } 
            else if ((splited.length == 10) &&
                (splited[0].contains("\\d+.\\d+") && splited[2].contains("\\d+.\\d+"))) {
                //+34.020991 -118.289408 257.7 63427271253563 14 3 1 0 57 7
                //($lat, $lon, $acc, $time, $vsat, $usat, $cellid, $mode, $rssi, $bar) = split(/\s+/, $line, 10);
                this.setData(
                    Double.parseDouble(splited[0]),
                    Double.parseDouble(splited[1]),
                    Long.parseLong(splited[3]),
                    (int)(Double.parseDouble(splited[2])),
                    Integer.parseInt(splited[6]),
                    Integer.parseInt(splited[8])
                );
                this.netlat = Double.parseDouble(splited[0]);
                this.netlon = Double.parseDouble(splited[1]);
            } else {
                filtered += 1;
                return FILTER_NEXT;
            }
        } catch (Exception e) {
            System.out.println("Exception during parsing line: " + line + "\n - " + e.getMessage());
            return FILTER_NEXT;
        }

        if ((this.lat == 0.0) || (this.lon == 0.0)) { 
            filtered += 1;
            return FILTER_NEXT;
        }

        if (this.acc > Const.ACC_THRESH) { 
            acc_filtered += 1;
            filtered += 1;
            return FILTER_NEXT;
        }
        if ((this.cellid == 65535) || (this.cellid == 0)) { 
            filtered += 1;
            return FILTER_NEXT;
        }
        
        linecnt += 1;
        if ((linecnt % skip) != 0) { 
            return FILTER_NEXT; 
        }

        return FILTER_VALID;
    }

    public void resetData() {
        cdate = ""; ctime = "";
        lat = 0.0; lon = 0.0; time = 0; acc = 0;
        cellid = 0; lac = 0; rssi = 0;
        netlat = 0.0; netlon = 0.0; netacc = 0;
    }

    public void setData(String cdate, String ctime, 
                double lat, double lon, long time, int acc, 
                int cellid, int lac, int rssi, 
                double netlat, double netlon, int netacc) {
        resetData();
        this.cdate = cdate; this.ctime = ctime;
        this.lat = lat; this.lon = lon; this.time = time; this.acc = acc;
        this.cellid = cellid; this.lac = lac; this.rssi = rssi;
        this.netlat = netlat; this.netlon = netlon; this.netacc = netacc;
    }
    
    public void setData(double lat, double lon, long time, int acc, 
                int cellid, int rssi) {
        resetData();
        this.lat = lat; this.lon = lon; this.time = time; this.acc = acc;
        this.cellid = cellid; this.rssi = rssi;
    }
}

