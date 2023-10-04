/**
 * SmithWatermaAlg
 * - Smith-Waterman Algorithm
 *
 * @author      Jeongyeup Paek (jpaek@usc.edu)
 * @modified    Nov. 15, 2010
 **/
package org.example.cellidtogps;

import java.util.Vector;

class SmithWatermanAlg {

    // scoring scheme
    public double MATCH     =  1.0; // +1 for letters that match
    public double MISMATCH  = -1.0; // -1 for letters that mismatch
    public double GAP       = -1.0; // -1 for any gap

    public static final int POINTER_NONE = 0;
    public static final int POINTER_LEFT = 1;
    public static final int POINTER_UP = 2;
    public static final int POINTER_DIAGONAL = 3;

    // return the match results!!!
    public int max_i;
    public int max_j;
    public double max_score;

    public int tail_match;
    public int head_match;
    public int tail_head_match;
    public int head_tail_match;
    public int complete_match;
    public int subset_match;

    public int match_len;
    public String matchedString1;
    public String matchedString2;

    SmithWatermanAlg() {
        this.reset();
    }

    SmithWatermanAlg(int _mismatch, int _gap) {
        MISMATCH = _mismatch;
        GAP = _gap;
        this.reset();
    }

    private void reset() {
        this.max_i = 0;
        this.max_j = 0;
        this.max_score = 0;
        this.tail_match = 0;
        this.head_match = 0;
        this.tail_head_match = 0;
        this.head_tail_match = 0;
        this.complete_match = 0;
        this.subset_match = 0;
        this.matchedString1 = "";
        this.matchedString2 = "";
        this.match_len = 0;
    }

    public double run(String seq1, String seq2, boolean modified) {
        String[] seqarr1 = seq1.split("\\s+");
        String[] seqarr2 = seq2.split("\\s+");
        return run(seqarr1, seqarr2, modified);
    }

    public double run(int[] seqintarr1, int[] seqintarr2, boolean modified) {
        String[] seqarr1 = new String[seqintarr1.length];
        String[] seqarr2 = new String[seqintarr2.length];
        for (int i = 0; i < seqintarr1.length; i++) {
            seqarr1[i] = Integer.toString(seqintarr1[i]);
        }
        for (int i = 0; i < seqintarr2.length; i++) {
            seqarr2[i] = Integer.toString(seqintarr2[i]);
        }
        return run(seqarr1, seqarr2, modified);
    }
    
    public double run(String[] seqarr1, String[] seqarr2, boolean modified) {
        int len1 = seqarr1.length;
        int len2 = seqarr2.length;
        
        this.reset();

        // initialization
        double[][] matrix_score = new double[len2 + 1][len1 + 1];
        int[][] matrix_pointer = new int[len2 + 1][len1 + 1];

        matrix_score[0][0]   = 0;
        matrix_pointer[0][0] = POINTER_NONE;
        for(int j = 1; j <= len1; j++) {
            matrix_score[0][j]   = 0;
            matrix_pointer[0][j] = POINTER_NONE;
        }
        for (int i = 1; i <= len2; i++) {
            matrix_score[i][0]   = 0;
            matrix_pointer[i][0] = POINTER_NONE;
        }

        // fill
        for(int i = 1; i <= len2; i++) {
            for(int j = 1; j <= len1; j++) {
                double diagonal_score = 0;
                double left_score = 0;
                double up_score = 0;

                // calculate match score
                if (seqarr1[j-1].equals(seqarr2[i-1])) {
                    diagonal_score = matrix_score[i-1][j-1] + MATCH;
                }
                else {
                    diagonal_score = matrix_score[i-1][j-1] + MISMATCH;
                }

                // calculate gap scores
                up_score   = matrix_score[i-1][j] + GAP;
                left_score = matrix_score[i][j-1] + GAP;

                if ((diagonal_score <= 0) && (up_score <= 0) && (left_score <= 0)) {
                    matrix_score[i][j]   = 0;
                    matrix_pointer[i][j] = POINTER_NONE;
                    continue; // terminate this iteration of the loop
                }

                // choose best score
                if (diagonal_score >= up_score) {
                    if (diagonal_score >= left_score) {
                        matrix_score[i][j]   = diagonal_score;
                        matrix_pointer[i][j] = POINTER_DIAGONAL;
                    }
                    else {
                        matrix_score[i][j]   = left_score;
                        matrix_pointer[i][j] = POINTER_LEFT;
                    }
                } else {
                    if (up_score >= left_score) {
                        matrix_score[i][j]   = up_score;
                        matrix_pointer[i][j] = POINTER_UP;
                    }
                    else {
                        matrix_score[i][j]   = left_score;
                        matrix_pointer[i][j] = POINTER_LEFT;
                    }
                }

                // set maximum score
                if ((!modified) ||      // if modified, last item of seq1 must match
                    ((j == len1) && (seqarr1[j-1].equals(seqarr2[i-1])))) { //// MODIFIED !
                    if (matrix_score[i][j] > max_score) {
                        max_i     = i;
                        max_j     = j;
                        max_score = matrix_score[i][j];
                    }
                }
            }
        }

        // trace-back
        Vector<String> align1 = new Vector<String>();
        Vector<String> align2 = new Vector<String>();
        int j = max_j;
        int i = max_i;

        while (true) {
            if (matrix_pointer[i][j] == POINTER_NONE)
                break;

            if (matrix_pointer[i][j] == POINTER_DIAGONAL) {
                align1.add(0, seqarr1[j-1]);
                align2.add(0, seqarr2[i-1]);
                i--; j--;
            }
            else if (matrix_pointer[i][j] == POINTER_LEFT) {
                align1.add(0, seqarr1[j-1]);
                align2.add(0, "-"); //"-"
                j--;
            }
            else if (matrix_pointer[i][j] == POINTER_UP) {
                align1.add(0, "-"); //"-"
                align2.add(0, seqarr2[i-1]);
                i--;
            }   
        }
        //////////////////////////////////////////////////////////////////////////////////////////////
        
        this.match_len = align1.size();
        this.matchedString1 = "";
        this.matchedString2 = "";

        for (int k = 0; k < align1.size(); k++) {
            if (!matchedString1.equals("")) matchedString1 += " ";
            matchedString1 += align1.elementAt(k);
        }
        for (int k = 0; k < align2.size(); k++) {
            if (!matchedString2.equals("")) matchedString2 += " ";
            matchedString2 += align2.elementAt(k);
        }

        // check tail match
        if ((this.max_i == len2) && (this.max_j == len1)) { this.tail_match = 1; }
        // check head match
        if ((i == 0) && (j == 0)) { this.head_match = 1; }
        // check tail to head match
        if ((i == 0) && (this.max_j == len1)) { this.tail_head_match = 1; }
        // check head to tail match
        if ((this.max_i == len2) && (j == 0)) { this.head_tail_match = 1; }
        // check full/subset match 
        if ((this.max_score == len1) && (len2 == this.max_score)) { this.complete_match = 1; } 
        // forward subset match
        else if (this.max_score == len1) { this.subset_match = 1; } 
        // reverse subset match
        else if (this.max_score == len2) { this.subset_match = -1; }

        this.max_j = this.max_j - 1;
        this.max_i = this.max_i - 1;

        return max_score;
    }
    
    public double run(String seq1, String seq2) { return run(seq1, seq2, false); }
    public double runModified(String seq1, String seq2) { return run(seq1, seq2, true); }

    public double run(int[] seqarr1, int[] seqarr2) { return run(seqarr1, seqarr2, false); }
    public double runModified(int[] seqarr1, int[] seqarr2) { return run(seqarr1, seqarr2, true); }

    public double run(String[] seqarr1, String[] seqarr2) { return run(seqarr1, seqarr2, false); }
    public double runModified(String[] seqarr1, String[] seqarr2) { return run(seqarr1, seqarr2, true); }

}

