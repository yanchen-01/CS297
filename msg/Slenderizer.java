package msg;

import java.util.*;
import java.io.*;


/*
 * Here's the puzzle: how do you reduce the width of a gapful alignment by
 * n columns, while removing exactly g gaps? The solution here is probabilistic.
 * Each pass randomly removes n columns; the pass is feasible if the n columns
 * contained g gaps. Feasible solutions with higher scores are preferred.
 */

class Slenderizer
{
    private final static int        N_PASSES = 10000;

    private Collection<String>      originalSeqs;
    private int                     nColsBefore;
    private int                     nColsAfter;
    private int                     nGapsBefore;
    private int                     nGapsAfter;
    private int                     nColsToRemove;
    private int                     nGapsToRemove;
    private int                     scoreOfBestSolution;
    private int[]                   gapsByCol;
    private char[][]                slenderAlignment;

    Slenderizer(Collection<String> originalSeqs)
    {
        this.originalSeqs = originalSeqs;
    }


    // Returns null if no solution can be found. Otherwise returns best solution
    // found by randomization.
    int[] slenderize(int nColsAfter, int nGapsAfter)
    {
        this.nColsAfter = nColsAfter;
        this.nGapsAfter = nGapsAfter;

        // Compute # cols & gaps to remove.
        nColsBefore = originalSeqs.iterator().next().length();
        nColsToRemove = nColsBefore - nColsAfter;
        assert nColsToRemove > 0 :
               "nColsBefore=" + nColsBefore + ", nColsAfter=" + nColsAfter;
        nGapsBefore = 0;
        for (String seq: originalSeqs)
            for (int i=0; i<seq.length(); i++)
                if (seq.charAt(i) == '-')
                    nGapsBefore++;
        nGapsToRemove = nGapsBefore - nGapsAfter;
        assert nGapsToRemove > 0;

        // Count gaps per col.
        gapsByCol = new int[nColsBefore];
        for (int col=0; col<gapsByCol.length; col++)
            for (String seq: originalSeqs)
                if (seq.charAt(col) == '-')
                    gapsByCol[col]++;

        // Randomly generate columns.
        int[] bestSolution = null;
        int scoreOfBestSolution = Integer.MIN_VALUE;
        for (int i=0; i<N_PASSES; i++)
        {
            int[] candidate = buildRandomCandidate();
            if (!isFeasible(candidate))
                continue;
            char[][] charArr = delColsToAlignment(candidate);
            int candidateScore = AlignmentScorer.scoreAlignment(charArr);
            if (candidateScore > scoreOfBestSolution)
            {
                scoreOfBestSolution = candidateScore;
                bestSolution = candidate;
                slenderAlignment = charArr;
            }
        }

        return bestSolution;
    }

    @SuppressWarnings("unchecked")
    private int[] buildRandomCandidate()
    {
        int[] ret = new int[nColsToRemove];
        ArrayList unusedCols = new ArrayList<Integer>(nColsBefore);
        for (int i=0; i<nColsBefore; i++)
            unusedCols.add(i);
        int n = 0;
        while (n < ret.length)
        {
            int colIndex =  (int)(unusedCols.size() * Math.random());
            ret[n++] = (Integer)unusedCols.remove(colIndex);
        }
        return ret;
    }


    // A candidate is feasible if the columns it describes contain a total
    // of nGapsToRemove gaps.
    private boolean isFeasible(int[] candidate)
    {
        int nGaps = 0;
        for (int col: candidate)
            nGaps += gapsByCol[col];
        return nGaps == nGapsToRemove;
    }


    private char[][] delColsToAlignment(int[] candidate)
    {
        // Collect delete columns into a set for easier membership check.
        Set<Integer> deleteCols = new HashSet<Integer>(candidate.length);
        for (int col: candidate)
            deleteCols.add(col);

        // Retain chars from non-deleted cols.
        char[][] ret = new char[originalSeqs.size()][nColsAfter];
        int seqNum = 0;
        for (String seq: originalSeqs)
        {
            int destCol = 0;
            for (int srcCol=0; srcCol<seq.length(); srcCol++)
            {
                if (deleteCols.contains(srcCol))
                    continue;
                ret[seqNum][destCol] = seq.charAt(srcCol);
                destCol++;
            }
            assert destCol == nColsAfter;
            seqNum++;
        }

        return ret;
    }


    char[][] getAlignment()         { return slenderAlignment;    }
    int getWinnerScore()            { return scoreOfBestSolution; }
    static void sop(Object x)       { System.out.println(x);      }


    public static void main(String[] args)
    {
        try
        {
            File dirf = MSGFrame.GAG_POL_DIRF;
            File file = new File(dirf, "gag_04.clw");
            SequenceDataset gag4 = ClustalParser.parseFileToGapped(file);
            Slenderizer that = new Slenderizer(gag4.values());
            int[] winner = that.slenderize(47, 8);
            if (winner == null)
                sop("No solution found.");
            else
            {
                String s = "Delete these columns: ";
                for (int col: winner)
                    s += col + " ";
                sop(s);
            }
        }
        catch (IOException x)
        {
            sop("IO Stress: " + x.getMessage());
            x.printStackTrace(System.out);
        }
    }
}

