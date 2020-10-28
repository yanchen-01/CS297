package msg;

import java.util.*;
import java.io.*;


class SizeAdjustingChromosome extends Chromosome
{
    // Constructs a chromosome that represents the result of adjusting the
    // width of src by deltaWidth. If widening, columns of gaps are added
    // fore & aft. If narrowing, columns are deleted.
    SizeAdjustingChromosome(SequenceDataset src, int deltaWidth, int nGapsAfter)
    {
        assert src.isUniformWidth();

        this.maxGapIndex = nGapsAfter - 1;

        if (deltaWidth >= 0)
        {
            // Convert source to array of StringBuilder!s.
            StringBuilder[] sbs = new StringBuilder[src.size()];
            int n = 0;
            for (String s: src.values())
                sbs[n++] = new StringBuilder(s);

            // Add columns.
            bookendNGapCols(sbs, deltaWidth);

            // Compute gap locations.
            this.maxGapIndex = sbs[0].length() - 1;
            int nGaps = 0;
            for (StringBuilder sb: sbs)
                for (int i=0; i<sb.length(); i++)
                    if (sb.charAt(i) == '-')
                        nGaps++;
            assert nGaps == nGapsAfter;
            this.gapLocations = new int[nGaps];
            n = 0;
            for (StringBuilder sb: sbs)
                for (int i=0; i<sb.length(); i++)
                    if (sb.charAt(i) == '-')
                        gapLocations[n++] = i;
            assert n == nGapsAfter;

            // Compute score.
            UngappedSequenceDataset ungapped = src.removeGaps();
            evaluate(ungapped, sbs[0].length());
        }

        else
        {
            // Try to slenderize. If slenderizer can't find a solution,
            // revert to random.
            int nColsBefore = src.widthOfWidestSequence();
            deltaWidth = Math.abs(deltaWidth);
            int nColsAfter = nColsBefore - deltaWidth;
            assert nColsAfter > 0;
            Slenderizer slen = new Slenderizer(src.values());
            this.gapLocations = new int[nGapsAfter];
            this.maxGapIndex = nColsAfter - 1;
            if (slen.slenderize(nColsAfter, nGapsAfter) != null)
            {
                // Slenderizer found a solution.
                char[][] reducedAlignment = slen.getAlignment();
                int n = 0;
                for (char[] seq: reducedAlignment)
                    for (int col=0; col<seq.length; col++)
                        if (seq[col] == '-')
                            gapLocations[n++] = col;
                assert n == nGapsAfter;
                UngappedSequenceDataset ungapped = src.removeGaps();
                evaluate(ungapped, nColsAfter);
            }
            else
            {
                // Slenderizer couldn't find a solution. Revert to random.
                randomize();
                UngappedSequenceDataset ungapped = src.removeGaps();
                evaluate(ungapped, nColsAfter);
            }
        }
    }


    private void adjustSBs(StringBuilder[] sbs, int deltaWidth)
    {
        if (deltaWidth == 0)
            return;
        else if (deltaWidth > 0)
            bookendNGapCols(sbs, deltaWidth);
        else
            deleteNGapCols(sbs, deltaWidth);
    }


    private void bookendNGapCols(StringBuilder[] sbs, int deltaWidth)
    {
        boolean atEnd = true;

        while (deltaWidth-- > 0)
        {
            if (atEnd)
            {
                for (StringBuilder sb: sbs)
                    sb.append('-');
            }
            else
            {
                for (StringBuilder sb: sbs)
                    sb.insert(0, '-');
            }
            atEnd = !atEnd;
        }
    }


    // Delete columns with most gaps.
    private void deleteNGapCols(StringBuilder[] sbs, int deltaWidth)
    {
        // Collect column #s by gap count.
        Map<Integer, Vector<Integer>> gapCountToColNums =
            new TreeMap<Integer, Vector<Integer>>();
        for (int colNum=0; colNum<sbs[0].length(); colNum++)
        {
            int gapCount = nGapsInCol(sbs, colNum);
            Vector<Integer> colsForGapCount = gapCountToColNums.get(gapCount);
            if (colsForGapCount == null)
            {
                colsForGapCount = new Vector<Integer>();
                gapCountToColNums.put(gapCount, colsForGapCount);
            }
            colsForGapCount.add(colNum);
        }

        // Collect columns to be deleted. When the time comes, these need to
        // be deleted in descending order, so as to preserve integrity of col #s.
        Set<Integer> deleteUs = new TreeSet<Integer>();
        Vector<Integer> sortedGapCounts = new Vector<Integer>(gapCountToColNums.keySet());
        outer: for (int i=sortedGapCounts.size()-1; i>=0; i--)
        {
            Integer gapCount = sortedGapCounts.get(i);
            Vector<Integer> colsMatchingGapCount = gapCountToColNums.get(gapCount);
            for (Integer col: colsMatchingGapCount)
            {
                deleteUs.add(col);
                if (deleteUs.size() == Math.abs(deltaWidth))
                    break outer;
            }
        }
        Vector<Integer> vec = new Vector<Integer>(deleteUs);

        // Delete columns in reverse order of appearance in vec.
        for (int i=vec.size()-1; i>=0; i--)
        {
            int col = vec.get(i);
            for (StringBuilder sb: sbs)
                sb.deleteCharAt(col);
        }
    }


    private int nGapsInCol(StringBuilder[] sbs, int col)
    {
        int n = 0;
        for (StringBuilder sb: sbs)
            if (sb.charAt(col) == '-')
                n++;
        return n;
    }


    public static void main(String[] args)
    {
        try
        {
            File dirf = MSGFrame.GAG_POL_DIRF;
            File file = new File(dirf, "gag_04.clw");
            SequenceDataset gag4 = ClustalParser.parseFileToGapped(file);
            SizeAdjustingChromosome saChro = new SizeAdjustingChromosome(gag4, -2, 8);
            sop(saChro);
        }
        catch (IOException x)
        {
            sop("IO Stress: " + x.getMessage());
            x.printStackTrace(System.out);
        }
    }
}
