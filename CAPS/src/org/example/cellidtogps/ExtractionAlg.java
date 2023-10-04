/**
 * ExtractionAlg
 * - Algorithm for extracting current cellid sequence
 *   and inserting it into the database
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Nov. 28, 2010
 **/
package org.example.cellidtogps;

class ExtractionAlg {

    public int cntDiscard;
    MatchingAlg malg;
    DbSequences db;
    CurrentSequence cseq;

    public static int EXTRACT_ALG = Const.EXTRACT_ALG;
    public static int CUT_LEN = Const.CUT_LEN;

    public static LogEventInterface logInt = null;

    ExtractionAlg(MatchingAlg malg, DbSequences db, CurrentSequence cseq) {
        this.malg = malg;
        this.db = db;
        this.cseq = cseq;
        cntDiscard = 0;
    }

    public void cut_curr_sequence_to_db(int cellid) {
        // extract current sequence and put it into db
        if (EXTRACT_ALG == 1) {
            // duplicate cellid detected. abort sequence.
            if (cseq.has(cellid)) {
                sequence_extraction_alg_1(cellid);
            }
        }
        else if (EXTRACT_ALG == 2) {
            // duplicate cellid detected. abort sequence.
            if (cseq.has(cellid)) {
                sequence_extraction_alg_2(cellid);
            }
        } 
        else if (EXTRACT_ALG == 3) {
            if (cseq.has(cellid)) {
                move_curr_sequence_to_db();
            }
        }
        else if (EXTRACT_ALG == 4) {
            if (cseq.size() > 4*Const.MAX_RUN_LEN) {
                sequence_extraction_alg_4();
            }
        }
        else if (EXTRACT_ALG == 5) {
            if (cseq.size() > CUT_LEN) {
                move_curr_sequence_to_db();
            }
        }
    }

    public void move_curr_sequence_to_db() {
        Sequence curr_seq = cseq.get();

        insert_sequence_to_db(curr_seq);

        // empty current sequence and beginning of a new sequence.
        cseq.startNew();
    }


    ////////////////////////////////////////////////////////////////////////////


    private void sequence_extraction_alg_1(int aCellid) {
        Sequence curr_seq = cseq.get();
        Sequence front_seq = new Sequence();
        int cid = 0; 
        CellidPoint cp = null;

        //if (aCellid != 0) {
            while ((cid != aCellid) && (curr_seq.size() > 0)) {
                cp = curr_seq.shift();
                front_seq.add(cp);
            }

            insert_sequence_to_db(front_seq);
            front_seq = null;

            if ((cp != null) && (cid != 0)) {
                // put it back at the head
                curr_seq.unshift(cp);
            }
        //}

        insert_sequence_to_db(curr_seq);
        
        // empty current sequence and beginning of a new sequence.
        cseq.startNew();
    }

    private void sequence_extraction_alg_4() {
        Sequence curr_seq = cseq.get();
        Sequence next_seq = new Sequence();
        CellidPoint cp;;

        int i = curr_seq.size() - Const.MAX_RUN_LEN + 1;

        while (i < curr_seq.size()) {
            cp = curr_seq.get(i);
            next_seq.add(cp);
            i++;
        }

        insert_sequence_to_db(curr_seq);
        
        // swap current sequence
        cseq.replace(next_seq);
    }

    private void sequence_extraction_alg_2(int aCellid) {
        Sequence curr_seq = cseq.get();
        Sequence next_seq = new Sequence();
        CellidPoint cp;;

        int i = 0;
        while ((curr_seq.cellid(i) != aCellid) && (i < curr_seq.size())) {
            i++;
        }
        //i++;  //TODO: include dup cellid or not?
        while (i < curr_seq.size()) {
            cp = curr_seq.get(i);
            next_seq.add(cp);
            i++;
        }

        insert_sequence_to_db(curr_seq);
        
        // swap current sequence
        cseq.replace(next_seq);
    }

    private boolean insert_sequence_to_db(Sequence curr_seq) {

        if (curr_seq.size() >= MatchingAlg.MIN_MATCH_LEN) {
            String save_sequence = curr_seq.key();
            println("INSERT(C): " + save_sequence);
            
            long ltime = curr_seq.getLast().time();

            int should_insert = malg.check_matching_for_db_insert(curr_seq, ltime);

            if (should_insert >= 0) {
                curr_seq.cnt = should_insert;
                curr_seq.cnt += 1;

                this.db.add(curr_seq);   ////////////////

                if (curr_seq.cnt == 1) {
                    curr_seq.seqno = this.db.cntInsert;
                    this.db.cntInsert += 1;
                }
                curr_seq.rate = 0;
                int tmp = curr_seq.cnt;
                println(" ==> Insert with cnt " + tmp + "!!");
                return true;
            } else {
                println(" ==> Cancel insert!!\n");
                cntDiscard += 1;
            }
        } else {
            cntDiscard += 1;
        }
        return false;
    }

    public void println(String str) { if (logInt != null) logInt.println(str); }
}

