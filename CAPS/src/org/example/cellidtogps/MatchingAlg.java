/**
 * MatchingAlg
 * - Algorithm for cellid sequence matching
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Nov. 15, 2010
 **/

class MatchingAlg {

    SmithWatermanAlg smithwaterman;
    DbSequences db;

    public static double MIN_MATCH_LEN = Const.MIN_MATCH_LEN;

    public Sequence max_match_seq;
    public double max_max_score;
    public int max_match_len;
    public int max_match_idx;

    private int num_match;
    private int max_match_cnt;
    private int max_match_rate;
    private int max_match_head; // whether it is tail to head match
    private long max_match_time;

    public static LogEventInterface logInt = null;

    MatchingAlg(DbSequences db) {
        smithwaterman = new SmithWatermanAlg();
        smithwaterman.MISMATCH = Const.PENALTY;
        smithwaterman.GAP = Const.PENALTY;
        this.db = db;
        reset();
    }

    private void reset() {
        num_match = 0;
        max_max_score = 0.0;
        max_match_len = 0;
        max_match_seq = null;
        max_match_idx = -1;     // matched cellid index within the max matched sequence
        max_match_cnt = 0;      // cnt of the max matched sequence
        max_match_time = 0;
        max_match_rate = 0;
        max_match_head = 0;
    }

    public int check_matching_for_db_insert(Sequence curr_seq, long curr_time) {
        int sum_cnt = 0;
        int should_insert = 0;
        int curr_len = curr_seq.size();
        int[] curr_arr = curr_seq.intArray();
        
        reset();

        if (this.db.size() == 0) { return 0; }

        println("CHECK_MATCHING_WITH_DB_FOR_INSERT\t-- " + curr_seq.key());

        for (int i = 0; i < this.db.size(); i++) {
            Sequence sequence = this.db.get(i);
            int seq_len = sequence.size();
            int[] seq_arr = sequence.intArray();
        
            double max_score = smithwaterman.run(curr_arr, seq_arr);
            int match_len = smithwaterman.match_len;

            if (max_score > 0.0)
            {
                print("  + Compare: " + sequence.key());

                num_match += 1;
                println("-- Match found(" + num_match + ")!! (score " + max_score 
                        + ", len " + smithwaterman.match_len 
                        + ", max_i " + smithwaterman.max_i
                        + ", max_j " + smithwaterman.max_j
                        + ")");
                println("     # " + smithwaterman.matchedString1 + "");
                if (max_score != (double)match_len)
                    println("     # " + smithwaterman.matchedString2 + "");

                // find longest match
                if (max_score > max_max_score) {
                    max_max_score = max_score;
                    max_match_len = match_len;
                    max_match_seq = sequence;
                }

                if (smithwaterman.complete_match == 1) {
                    // if complete match, delete the original
                    println("    ==> complete match!!");
                    if (true) {
                        println("DELETE(C)!!!: \"" + sequence.key() + "\"");
                        sum_cnt += sequence.cnt;
                        this.db.remove(sequence);
                        sequence = null;
                    }
                    if ((curr_len != match_len) || ((double)seq_len != max_score)) {
                        println("ERROR 1\n"); System.exit(1);
                    }
                }
                else if (smithwaterman.subset_match > 0) {
                    // curr_seq is subset of a sequence in the db
                    println("    ==> full subset match!!");
                    if ((curr_len != match_len) || ((double)curr_len != max_score)) {
                        println("ERROR 2\n"); System.exit(1);
                    }
                    sequence.cnt += 1;

                    // TODO: insert if time is different
                    if (false) {
                        long match_time = sequence.get(smithwaterman.max_i).time();
                        int mtime_diff = HourOfDay.diff(match_time, curr_time);
                        if (mtime_diff <= 1) { should_insert = -1; }
                    } else {
                        if (true) {
                            should_insert = -1;
                        }
                    }
                }
                else if (smithwaterman.subset_match < 0) {
                    // curr_seq is superset of a sequence in the db
                    // the sequence in the db is subset of curr_seq
                    println("    ==> reverse subset match!!");
                    
                    // TODO: insert if time is different
                    if (false) {
                        long match_time = sequence.get(smithwaterman.max_i).time();
                        long my_time = curr_seq.get(smithwaterman.max_j).time();
                        int mtime_diff = HourOfDay.diff(match_time, my_time);
                        if (mtime_diff <= 1) {
                            println("DELETE(R)!!!: \"" + sequence.key() + "\"");
                            sum_cnt += sequence.cnt;
                            this.db.remove(sequence);
                            sequence = null;
                        }
                    } else {
                        if (true) {
                            println("DELETE(R)!!!: \"" + sequence.key() + "\"");
                            sum_cnt += sequence.cnt;
                            this.db.remove(sequence);
                            sequence = null;
                        }
                    }
                    if (seq_len != match_len) {
                        println("ERROR 3\n"); System.exit(1);
                    }
                }
            } else {
                //println("-- No match !\n");
            }
        }
        if (max_max_score == 0.0) {
            println(" > No match found");
        } else {
            if (max_match_seq == null) println("ERROR, max_match_seq == null\n");
            String str = String.format(" > Max. match: %s, score = %.1f, len = %d",
                                    max_match_seq.key(), max_max_score, max_match_len);
            println(str);
        }
        
        if (should_insert >= 0) { should_insert = sum_cnt; }
        return should_insert;
    }
    
    public double check_matching_for_estimation(RunCellidSeq curr_seq, long curr_time) {
        int[] curr_arr = curr_seq.intArray();
        int curr_len = curr_seq.size();
        
        reset();

        if (this.db.size() == 0) { return 0; }

        println("CHECK_MATCHING_WITH_DB_FOR_ESTIMATION\t-- " + curr_seq.toString());

        for (int i = 0; i < this.db.size(); i++) {
            Sequence sequence = this.db.get(i);
            int seq_len = sequence.size();
            int[] seq_arr = sequence.intArray();
        
            double max_score = smithwaterman.runModified(curr_arr, seq_arr);   // modified
            int match_len = smithwaterman.match_len;

            // last id of the match must match current cellid
            if ((max_score > 0.0) && (smithwaterman.max_j != curr_len - 1)) {
                print("ERROR, max_j must equal (curr_len - 1) in a match, if exists\n");
                System.exit(1);
            }

            // NOTE that we might only have len-1 match
            if ((max_score >= MIN_MATCH_LEN) || 
                ((max_score > 0) && ((smithwaterman.subset_match == 1) || (smithwaterman.subset_match == -1)))) {

                print("  + Compare: " + sequence.key());

                num_match += 1;
                println("-- Match found(" + num_match + ")!! (score " + max_score 
                        + ", len " + smithwaterman.match_len 
                        + ", max_i " + smithwaterman.max_i
                        + ", max_j " + smithwaterman.max_j
                        + ")");
                println("     # " + smithwaterman.matchedString1 + "");
                if (max_score != match_len)
                    println("     # " + smithwaterman.matchedString2 + "");

                if (smithwaterman.complete_match == 1) { println("    ==> complete match!!"); }
                else if (smithwaterman.subset_match == 1) { println("    ==> subset match!!"); }
                else if (smithwaterman.subset_match == -1) { println("    ==> reverse subset match!!"); }
                else {
                    if (smithwaterman.tail_match == 1) { println("    ==> tail match!!"); } 
                    if (smithwaterman.head_match == 1) { println("    ==> head match!!"); }
                    if (smithwaterman.tail_head_match == 1) { println("    ==> tail to head match!!"); } 
                    if (smithwaterman.head_tail_match == 1) { println("    ==> head to tail match!!"); }
                }

                // TODO: using first-found-match algorithm for now
                //       how should we rate same length matches?
                //       need some tie breakers....

                int result = 0;

                // Use TOD -- if same time of day, increase score!
                if (HourOfDay.is_closer_time_of_day(match_time, this.max_match_time, curr_time) > 0) {
                    max_score = max_score + 1.0;
                }

                //long match_time = sequence.get(smithwaterman.max_i).getLast().time;
                long match_time = sequence.get(smithwaterman.max_i).time();

                while (true) {
                    // find best match
                    if (result < 0) { break; } else if (result == 0) { result = is_better_match(max_score, this.max_max_score); }
                    // find longest match
                    if (result < 0) { break; } else if (result == 0) { result = is_longer_match(match_len, this.max_match_len); }
                    // prefer all to head match
                    if (result < 0) { break; } else if (result == 0) { 
                        if ((double)curr_len == max_score)
                            result = is_tail_head_match(smithwaterman.tail_head_match, this.max_match_head); 
                    }
                    // choose same time of day
                    if (result < 0) { break; } else if (result == 0) { 
                        if (HourOfDay.is_closer_time_of_day(match_time, this.max_match_time, curr_time) > 0) {
                            result = HourOfDay.is_same_time_of_day(match_time, curr_time); 
                        }
                    }
                    // prefer tail to head match
                    if (result < 0) { break; } else if (result == 0) { 
                        result = is_tail_head_match(smithwaterman.tail_head_match, this.max_match_head); 
                    }
                    // choose newer sequence
                    if (result < 0) { break; } else if (result == 0) {
                        result = is_newer_time(match_time, this.max_match_time); 
                    }
                    // choose better positive rate
                    if (result < 0) { break; } else if (result == 0) { 
                        result = is_better_positive_rate(sequence.rate, this.max_match_rate); 
                    }
                    // choose closer time of day
                    if (result < 0) { break; } else if (result == 0) { 
                        result = HourOfDay.is_closer_time_of_day(match_time, this.max_match_time, curr_time); 
                    }
                    // choose more frequent sequence
                    if (result < 0) { break; } else if (result == 0) { 
                        result = is_more_frequent(sequence.cnt, this.max_match_cnt); 
                    }
                    // choose one closer to the head --> why?
                    //if (result < 0) { break; } else if (result == 0) { result = is_smaller_index(smithwaterman.max_i, this.max_match_idx); }
                    // choose better rate
                    if (result < 0) { break; } else if (result == 0) { 
                        result = is_better_rate(sequence.rate, this.max_match_rate); 
                    }
                    
                    if (result <= 0) { if (max_score > 0.0 && this.max_max_score == 0.0) { result = 1; }}
                    if (result > 0) {
                        println("SELECTED");
                        max_max_score = max_score; 
                        max_match_len = match_len; 
                        max_match_seq = sequence;
                        max_match_idx = smithwaterman.max_i; 
                        max_match_head = smithwaterman.tail_head_match;
                        max_match_time = match_time;
                        max_match_rate = sequence.rate;
                        max_match_cnt = sequence.cnt;
                    }
                    break;
                }
            }
            else {
                //println("\n");
            }
        }
        if (max_max_score == 0.0) {
            println(" > No match found");
        } else {
            String str = String.format(" > Max. match: %s, score = %.1f, len = %d, idx = %d",
                                    max_match_seq.key(), max_max_score, max_match_len, max_match_idx);
            println(str);
        }

        return max_max_score;
    }

    public static int is_better_match(double max_score, double max_max_score) {
        if (max_score > max_max_score) {
            if (max_max_score > 0.0)
                println("   --> better match (max_score " + max_score + " > max_max_score " + max_max_score + ")");
            return 1;
        } else if (max_score < max_max_score) {
            return -1;
        }
        return 0;
    }

    public static int is_longer_match(int match_len, int max_match_len) {
        if (match_len > max_match_len) {
            if (max_match_len > 0)
                println("   --> longer match (match_len " + match_len + " > max_len " + max_match_len + ")");
            return 1;
        } else if (match_len < max_match_len) {
            return -1;
        }
        return 0;
    }

    public static int is_better_rate (int mrate, int max_match_rate) {
        if (mrate > max_match_rate) {
            println("   --> better rate (mrate " + mrate + " > max_match_rate " + max_match_rate + ")");
            return 1;
        } else if (mrate < max_match_rate) {
            return -1;
        }
        return 0;
    }

    public static int is_better_positive_rate(int mrate, int max_match_rate) {
        if (mrate > 0) {
            if (mrate > max_match_rate) {
                println("   --> better positive rate (mrate " + mrate + " > max_match_rate " + max_match_rate + ")");
                return 1;
            } else if (mrate < max_match_rate) {
                return -1;
            }
        }
        return 0;
    }

    public static int is_tail_head_match(int tail_head_match, int max_match_head) {
        if (tail_head_match > max_match_head) {
            println("   --> prefer tail-to-head match (tail_head_match > max_match_head)");
            return 1;
        } else if (tail_head_match < max_match_head) {
            return -1;
        }
        return 0;
    }

    public static int is_smaller_index(int max_i, int max_match_idx) {
        if (max_i < max_match_idx) {
            println(String.format("   --> smaller idx (%d < %d)", max_i, max_match_idx));
            return 1;
        } else if (max_i > max_match_idx) {
            return -1;
        }
        return 0;
    }

    public static int is_newer_time(long mtime, long max_match_time) {
        if (mtime > max_match_time) {
            println(String.format("   --> newer time (%d > %d)", mtime, max_match_time));
            return 1;
        } else if (mtime < max_match_time) {
            return -1;
        }
        return 0;
    }

    public static int is_more_frequent(int mcnt, int max_match_cnt) {
        if (mcnt > max_match_cnt) {
            println(String.format("   --> more frequent cnt (%d > %d)", mcnt, max_match_cnt));
            return 1;
        } else if (mcnt < max_match_cnt) {
            return -1;
        }
        return 0;
    }

    public static void println(String str) { if (logInt != null) logInt.println(str); }
    public static void print(String str) { if (logInt != null) logInt.print(str); }
}


