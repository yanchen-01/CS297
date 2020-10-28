package msg;

import java.util.*;


/*
 * A chromosome contains 1 value for each gap to be inserted into any sequence of
 * the dataset. The value is the index (from 0) of the gap. Coincident gaps are
 * dealt with elsewhere. Gap locations (genes of the chromosome) are associated
 * with individual sequences, but not in any way that matters to the G.A. operators,
 * so the gap-to-containing-sequence mapping is supported elsewhere.
 */


public class Chromosome implements Comparable<Chromosome>
{
    private static int      nextSn;

    protected int           maxGapIndex;                // = consensus width - 1
    protected int[]         gapLocations;
    protected int           sn = nextSn++;
    private String          sval;                       // cached by toString()
            int             score = Integer.MIN_VALUE;  // for faster access


    Chromosome()        { }


    Chromosome(int nGaps, int maxGapIndex)
    {
        this.maxGapIndex = maxGapIndex;
        gapLocations = new int[nGaps];
    }


    Chromosome(Chromosome src)
    {
        this.maxGapIndex = src.maxGapIndex;
        this.gapLocations = new int[src.gapLocations.length];
        System.arraycopy(src.gapLocations, 0,
                         this.gapLocations, 0, this.gapLocations.length);
        this.sval = src.sval;  // immutable strings, so no risk
        this.score = score;
    }


    // Arg is a string extracted from database file. Format is e.g. "12_34_56".
    Chromosome(String dbString, int maxGapIndex)
    {
        this.maxGapIndex = maxGapIndex;

        // Major delimiter is '_'
        String[] pieces = dbString.split("_");
        gapLocations = new int[pieces.length];

        // Each piece is a gap location.
        for (int i=0; i<gapLocations.length; i++)
            gapLocations[i] = Integer.parseInt(pieces[i]);
    }


    String toStringForDatabase()
    {
        String s = "";
        for (int x: gapLocations)
            s += x + "_";
        if (s.length() > 0)
            s = s.substring(0, s.length()-1);
        return s;
    }


    public String toString()
    {
        if (sval == null)
        {
            sval = "Chromosome: /" + gapsToString() +
                   " score=" + (isEvaluated() ? (" " + score) : " UNSCORED");
        }
        return sval;
    }


    String gapsToString()
    {
        String s = "";
        for (int gloc: gapLocations)
            s += gloc + "/";
        return s;
    }


    // Not valid unless chromosome has been evaluated.
    public int compareTo(Chromosome that)
    {
        assert score != Integer.MIN_VALUE : "Compared before score was set.";
        assert that.score != Integer.MIN_VALUE : "Compared before score was set.";

        if (score != that.score)
            return score - that.score;
        else if (!(this.toString().equals(that.toString())))
            return this.toString().compareTo(that.toString());
        else
            return this.sn - that.sn;
    }


    public boolean equals(Object x)
    {
        Chromosome that = (Chromosome)x;
        return this.toString().equals(that.toString());
    }


    public int hashCode()
    {
        assert score != Integer.MIN_VALUE : "Hashed before score was set.";
        return score;
    }


    private final static int randomFrom0ThruN(int n)
    {
        return (int)((n+1) * Math.random());
    }


    void randomize()
    {
        for (int i=0; i<gapLocations.length; i++)
            gapLocations[i] = (int)((maxGapIndex+1) * Math.random());
    }


    // Tribe of origin is automatically maintained during the exchange, since
    // it's stored in the upper bits of the chromosome's values.
    static Chromosome[] crossover(Chromosome ma, Chromosome pa, float oddsPerSite)
    {
        assert ma.length() == pa.length() :
               "Unequal lengths: " + ma.length() + " != " + pa.length();

        // Build a template.
        boolean[] template = new boolean[ma.length()];
        boolean b = true;
        for (int i=0; i<template.length; i++)
        {
            if (Math.random() <= oddsPerSite)
                b = !b;
            template[i] = b;
        }

        // Produce offspring.
        Chromosome[] kids = new Chromosome[2];
        for (int i=0; i<2; i++)
            kids[i] = new Chromosome(ma.gapLocations.length, ma.maxGapIndex);
        for (int i=0; i<template.length; i++)
        {
            kids[0].gapLocations[i] = template[i] ? ma.gapLocations[i] : pa.gapLocations[i];
            kids[1].gapLocations[i] = template[i] ? pa.gapLocations[i] : ma.gapLocations[i];
        }
        return kids;
    }


    // Mutation rate = odds of changing 1 gap.
    void mutate(float oddsPerSite)
    {
        for (int i=0; i<gapLocations.length; i++)
        {
            if (Math.random() < oddsPerSite)
            {
                gapLocations[i] = (int)((1+maxGapIndex)*Math.random());
            }
        }
    }


    int getGapLocation(int n)
    {
        assert n >= 0  &&  n <= gapLocations.length-1  :
               "Bad gap location " + n + " (max = " + (gapLocations.length-1) + ").";
        return gapLocations[n];
    }


    // Sets score to != Integer.MIN_VALUE.
    void evaluate(UngappedSequenceDataset ungapped, int alignmentWidth)
    {
        char[][] charArrs = toCharArrays(ungapped, alignmentWidth);
        score = AlignmentScorer.scoreAlignment(charArrs);
    }


    char[][] toCharArrays(UngappedSequenceDataset ungapped, int alignmentWidth)
    {
        // Build empty alignment.
        int nSeqs = ungapped.size();
        char[][] gappedAlignment = new char[nSeqs][alignmentWidth];

        // Place gaps.
        int gapIndexInEntireChromosome = 0;
        int seqNum = 0;
        for (String ungappedSeq: ungapped.values())
        {
            int nGapsThisSeq = alignmentWidth - ungappedSeq.length();
            for (int i=0; i<nGapsThisSeq; i++)
            {
                int gapLocation = gapLocations[gapIndexInEntireChromosome];
                while (gappedAlignment[seqNum][gapLocation] == '-')
                    gapLocation = (gapLocation + 1) % alignmentWidth;
                gappedAlignment[seqNum][gapLocation] = '-';
                gapIndexInEntireChromosome++;
            }
            seqNum++;
        }

        // Place chars in non-gap locations in gappedAlignment[][].
        seqNum = 0;
        for (String ungappedSeq: ungapped.values())
        {
            int indexInUngapped = 0;
            for (int col=0; col<gappedAlignment[0].length; col++)
            {
                if (gappedAlignment[seqNum][col] == '-')
                    continue;
                gappedAlignment[seqNum][col] = ungappedSeq.charAt(indexInUngapped++);
            }
            seqNum++;
        }

        return gappedAlignment;
    }


    ArrayList<String> toMSAStrings(UngappedSequenceDataset ungapped, int alignmentWidth)
    {
        char[][] charArrs = toCharArrays(ungapped, alignmentWidth);
        ArrayList<String> ret = new ArrayList<String>();
        for (char[] charArr: charArrs)
            ret.add(new String(charArr));
        return ret;
    }


    static void sop(Object x)               { System.out.println(x);             }
    int length()                            { return gapLocations.length;        }
    int consensusWidth()                    { return maxGapIndex + 1;            }
    void setGapLocation(int index, int loc) { gapLocations[index] = loc;         }
    boolean isEvaluated()                   { return score != Integer.MIN_VALUE; }
}
