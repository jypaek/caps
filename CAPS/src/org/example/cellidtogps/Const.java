/**
 * Const
 * - Define global constants
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Aug. 3, 2010
 **/
package org.example.cellidtogps;

class Const {
    // everything is in seconds !!
    static final int SEC = 1;
    static final int MIN = 60 * SEC;
    static final int HOUR = 60 * MIN;
    
    static final int ACC_THRESH = 50;

    static int EXTRACT_ALG = 4;
    static int CUT_LEN = 20;
    static int MAX_RUN_LEN = 5;
    static double MIN_MATCH_LEN = 1.0;

    static int OUTTIME = 180;
    static int OUTTIME2 = 90;
    
    static double PENALTY = -0.5;
    
    static final int TURN_OFF_COUNT = 1;
    static final double TURN_OFF_ERR_THRESH = 80.0;
    
    static final long TIME_JUMP_THRESH = 20*MIN;
    static final long TIME_STAY_THRESH = 5*MIN;
    static final int DIST_JUMP_THRESH = 2000;
}

