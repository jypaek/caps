/**
 * EstimationAlg
 * - Algorithm for estimating current position using
 *   currently running cellid sequence and the database
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

import java.util.Vector;
import java.util.Collections;

// estimate current location 
class EstimationAlg {

    MatchingAlg malg;
    DbSequences db;

    // for estimation of current location
    public Sequence curr_matched_seq; 
    public double curr_match_score;
    public int curr_cellid_idx;
    public int curr_match_len;

    // my estimate of current position
    public int expected_next_cellid;

    // stats
    public int succ_cnt; 
    public int fail_cnt; 
    public double sum_estimate_err; 
    Vector<Double> err_array;

    public static LogEventInterface logInt = null;

    EstimationAlg(MatchingAlg malg, DbSequences db) {
        this.curr_matched_seq = null;
        this.curr_cellid_idx = -1;
        this.expected_next_cellid = 0;
        this.malg = malg;
        this.db = db;

        this.succ_cnt = 0; 
        this.fail_cnt = 0; 
        this.sum_estimate_err = 0; 
        this.err_array = new Vector<Double>();
    }

    ///////////////////////////////////////////////////////

    public Point estimate_with_new_matching(RunCellidSeq run_seq, long curr_time) {
        Point p;

        // look for new match
        double match_score = malg.check_matching_for_estimation(run_seq, curr_time);

        if (match_score > 0.0) {
            
            // we have a new match!!
            curr_matched_seq = malg.max_match_seq;
            curr_cellid_idx = malg.max_match_idx;
            curr_match_len = malg.max_match_len;
            curr_match_score = malg.max_max_score;

            println(String.format("DECIDED ON A MATCH FOR %s : %s (score %.1f, len %d)",
                                    run_seq.toString(), curr_matched_seq.key(), 
                                    match_score, malg.max_match_len));

            // predict next cellid
            if (curr_cellid_idx + 1 < curr_matched_seq.size()) {
                expected_next_cellid = curr_matched_seq.get(curr_cellid_idx + 1).cellid();
            } else {
                expected_next_cellid = 0;
            }
            p = curr_matched_seq.get(curr_cellid_idx).get(0);
        } else {
            curr_matched_seq = null;
            expected_next_cellid = 0;
            p = null;
            // TODO: println("NO_MATCH, TURN ON GPS\n");
        }
        return p;
    }

    ///////////////////////////////////////////////////////

    public Point estimate_with_previous_matching(long time_in_cellid) {
        Point p = null;

        if (this.curr_matched_seq != null) {
            println("USING PREVIOUS MATCH : " + curr_matched_seq.key());
            p = curr_matched_seq.followTrace(curr_cellid_idx, time_in_cellid);
        }
        return p;
    }
    
    ///////////////////////////////////////////////////////

    public Point estimate_position(RunCellidSeq run_seq, boolean run_has_changed, long time_in_cellid, long curr_time) {
        Point p = null;

        if (run_has_changed == true) {
            p = estimate_with_new_matching(run_seq, curr_time);
        } else if (curr_matched_seq != null) {
            p = estimate_with_previous_matching(time_in_cellid);
        } else {
            // cannot estimate
        }
        return p;
    }
        
    ///////////////////////////////////////////////////////

    public boolean cellid_change_as_expected(int cellid) {
        if (expected_next_cellid > 0) {
            if (expected_next_cellid == cellid) {
                println("EXPECTED CELLID MATCH (" + expected_next_cellid + "): prev prediction was correct (mrate + 1)");
                curr_matched_seq.increment_rate();
                return true;
            } else {
                println("EXPECTED CELLID MISMATCH (" + expected_next_cellid + "): prev prediction was wrong (mrate - 1)");
                curr_matched_seq.decrement_rate();
                return false;
            }
        }
        return true;
    }

    ///////////////////////////////////////////////////////

    public void add_error(double dist_gps_vs_est) {
        succ_cnt += 1;
        sum_estimate_err += dist_gps_vs_est;
        err_array.add(dist_gps_vs_est);
    }

    public void increment_fail_cnt() {
        fail_cnt += 1;
    }

    public double median_error() {
        if (err_array.size() > 0) {
            Collections.sort(err_array);
            int idx = err_array.size() / 2;
            return (double)err_array.get(idx);
        }
        return 0.0;
    }

    public double average_error() {
        // calculate average estimation error
        if (succ_cnt > 0)
            return (sum_estimate_err * 1000.0 / (double)succ_cnt)/1000.0;
        return 0.0;
    }
    
    public double max_error() {
        if (err_array.size() > 0)
            return (double)Collections.max(err_array);
        return 0.0;
    }
    
    public void println(String str) { if (logInt != null) logInt.println(str); }
}

