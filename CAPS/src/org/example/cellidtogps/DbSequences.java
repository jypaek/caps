/**
 * DbSequences
 * - Database of cellid sequences
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Nov. 28, 2010
 **/
package org.example.cellidtogps;

import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;


class DbSequences {

    Vector<Sequence> sequences;
    HashMap<String, Sequence> seqMap;

    int cntInsert;
    int cntDelete;

    public static LogEventInterface logInt = null;

    DbSequences() {
        sequences = new Vector<Sequence>();
        seqMap = new HashMap<String, Sequence>();
        cntInsert = 0;
        cntDelete = 0;
    }

    public void add(Sequence s) {
        String key = s.key();
        seqMap.put(key, s);
        sequences.add(s);
    }

    public Sequence get(String key) {
        Sequence s = seqMap.get(key);
        return s;
    }

    public Sequence get(int index) {
        Sequence s = sequences.elementAt(index);
        return s;
    }

    public boolean contains(String key) {
        return seqMap.containsKey(key);
    }

    public boolean contains(Sequence s) {
        return seqMap.containsValue(s);
    }

    public boolean remove(Sequence s) {
        if (seqMap.containsValue(s)) {
            seqMap.remove(s.key());
            s.clear();
            return sequences.remove(s);
        }
        return false;
    }

    public boolean remove(String key) {
        if (seqMap.containsKey(key)) {
            Sequence s = seqMap.remove(key);
            s.clear();
            return sequences.remove(s);
        }
        return false;
    }

    public void clear() {
        seqMap.clear();
        sequences.clear();
    }

    public void printSequencesCellid() {
        Sequence v;
        println("\n## cellid sequences\n");
        for (int i = 0; i < sequences.size(); i++) {
            v = sequences.elementAt(i);
            println("KEY: " + v.key() + "(" + v.cnt + ")");
        }
    }

    public void printSequencesLatLon() {
        int tmpcnt = 0;
        
        println("");

        Sequence v;
        for (int i = 0; i < sequences.size(); i++) {
            //foreach $key (sort{$db_sequence_hash{$a}{"seqno"} <=> $db_sequence_hash{$b}{"seqno"}} keys %db_sequence_hash) {
            v = sequences.elementAt(i);
            tmpcnt++;

            println("GPS_SEQ START " + tmpcnt);
            for(int j = 0; j < v.length(); j++) {
                CellidPoint cp = v.get(j);
                long dtime = (v.time(j) - v.time(0)) / Const.SEC;
                String str = String.format("GPS_SEQ %.6f %.6f %d %d %d %d",
                                    cp.x(), cp.y(), cp.cellid(), cp.time(), cp.dist, dtime);
                println(str);

                for (int k = 0; k < cp.length(); k++) {
                    Point p = cp.get(k);
                    String str2 = String.format("GPS_SEQ %.6f %.6f %d %d", 
                                    p.x, p.y, cp.cellid(), p.time);
                    println(str2);
                }
            }
        }
        println("");
    }

    public void sort() {
        Collections.sort(sequences);
    }

    public void printDbToDbFile(String dbfilename) { printDbToDbFile(dbfilename, null); }

    public void printDbToDbFile(String dbfilename, VirtualCellid vcid) {
        try {
            FileOutputStream fout = new FileOutputStream(dbfilename, false);
            OutputStreamWriter osw = new OutputStreamWriter(fout);

            Sequence v;
            for (int i = 0; i < this.sequences.size(); i++) {
                v = this.sequences.elementAt(i);
                
                String str2 = String.format("%d %d %d %d SEQ_START", i, v.length(), v.cnt, v.rate);
                osw.write(str2 + "\n");

                for(int j = 0; j < v.length(); j++) {
                    CellidPoint cp = v.get(j);
                    
                    String str;
                    for (int k = 0; k < cp.length(); k++) {
                        Point p = cp.get(k);
                        int cellid = cp.cellid();
                        if (vcid != null)
                            cellid = vcid.getOriginal(cellid);

                        if (k == 0)
                            str = String.format("%.6f %.6f %d %d CELL_START", cp.x(), cp.y(), cp.time(), cellid);
                        else
                            str = String.format("%.6f %.6f %d %d", p.x, p.y, p.time, cellid);
                        osw.write(str + "\n");
                    }
                }
            }
            osw.flush();
            osw.close();
            fout.close();
        } catch (Exception e) {
            println("Cannot print to db file");
        }    
    }

    public void constructDbFromDbFile(String dbfilename) throws Exception { constructDbFromDbFile(dbfilename, null); }

    public void constructDbFromDbFile(String dbfilename, VirtualCellid vcid) throws Exception {
        FileInputStream fin = new FileInputStream(dbfilename);
        BufferedReader inRd = new BufferedReader(new InputStreamReader(fin));

        String text;
        double lat; double lon; long time; int cellid;
        
        Sequence curr_seq = null;
        CellidPoint curr_cp = null;

        while ((text = inRd.readLine()) != null)
        {
            String line = text.trim();
            if (line.contains("#") == true) continue;
            if ((line.matches("\\s+") == true) || (line.equals("") == true)) continue;

            String[] splited = line.split("\\s+");

            if ((splited.length == 5) && (splited[4].matches("SEQ_START"))) {
                curr_seq = new Sequence();
                this.add(curr_seq);

                curr_seq.cnt = Integer.parseInt(splited[2]);
                curr_seq.rate = Integer.parseInt(splited[3]);
            }
            else if (splited.length >= 4) {
                lat = Double.parseDouble(splited[0]);
                lon = Double.parseDouble(splited[1]);
                time = Long.parseLong(splited[2]);
                cellid = Integer.parseInt(splited[3]);
                if (vcid != null) cellid = vcid.get(cellid);
            
                if ((splited.length == 5) && (splited[4].matches("CELL_START"))) {
                    curr_cp = curr_seq.add(cellid, lat, lon, time);
                } else if (splited.length == 4) {
                    curr_cp.add(lat, lon, time);
                } else {
                    println("ERROR 11");
                }
            }
        }
        //this.sort();
        fin.close();
    }

    public void constructDbFromLogFile(String infilename, MatchingAlg malg, VirtualCellid vcid) throws Exception {
        FileInputStream fin = new FileInputStream(infilename);
        BufferedReader inRd = new BufferedReader(new InputStreamReader(fin));

        String line;
        int log;
        int cellid = 0;
        double lat;
        double lon;
        long time;
        
        FilterParseLog parser = new FilterParseLog();
        PrevLogPoint prevlog = new PrevLogPoint();
        CurrentSequence cseq = new CurrentSequence();
        ExtractionAlg extalg = new ExtractionAlg(malg, this, cseq);

        while ((line = inRd.readLine()) != null)
        {
            try {
                log = parser.run(line);
            } catch (Exception e) {
                println("parser.run error at : " + line);
                println("caught exception: " + e.getMessage());
                throw e;
            }

            if (log == FilterParseLog.FILTER_NEXT)  continue;
            else if (log == FilterParseLog.FILTER_BREAK) break;
            else if (log == FilterParseLog.FILTER_SKIP)  continue;

            cellid = vcid.get(parser.cellid);
            lat = parser.lat;
            lon = parser.lon;
            time = parser.time;

            if ((time - prevlog.time() > 0) && (time - prevlog.time() < 2*Const.SEC)) continue;

            prevlog.detect_change_from_prev_log(cellid, lat, lon, time);

            boolean new_seq_detected = prevlog.new_seq_detected;
            boolean new_diff_cell_detected = prevlog.new_diff_cell_detected;
            boolean new_same_cell_detected = prevlog.new_same_cell_detected;
            boolean old_cell_detected = prevlog.old_cell_detected;

            // push current location:
            // TODO: how often should we store this information? 1 sec?
            // important to do this regardless of cellid so that we don't need a dummy cellid at the end of a sequence
            // push current location:
            if (prevlog.jump_time >= Const.SEC) {
                //if (!cellid_change_detected)
                    cseq.addPointToCurrentCellidPoint(lat, lon, time);
            }

            // let's select an algorithm for sequence extraction from current sequence.
            if (new_seq_detected == true) {
                extalg.move_curr_sequence_to_db();

                // re-add the new values into the current sequence to start a new sequence
                cseq.addCellidPoint(cellid, lat, lon, time);
            }
            else if (new_diff_cell_detected == true) {
                // cell-id change has been detected
                extalg.cut_curr_sequence_to_db(cellid);

                // add the new values into the current sequence to start a new sequence
                cseq.addCellidPoint(cellid, lat, lon, time);
            }
            else if (new_same_cell_detected == true) {

                // add the same cellid into the current sequence
                cseq.addCellidPoint(cellid, lat, lon, time);
            }

            // update previous info and go to next
            prevlog.set(cellid, lat, lon, time);
            if ((new_seq_detected == true) || (new_diff_cell_detected == true) || (new_same_cell_detected == true)) {
                prevlog.setFirstInCellid(lat, lon, time);
            }
        }
        println("process last (" + cellid + ")");
        extalg.move_curr_sequence_to_db();

        //this.sort();
        this.printSequencesCellid();
        //this.printSequencesLatLon();
        fin.close();
    }

    public int size() { return sequences.size(); }
    public int length() { return size(); }

    public static void println(String str) { if (logInt != null) logInt.println(str); }
}

