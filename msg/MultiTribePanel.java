package msg;

import java.awt.*;
import javax.swing.*;
import java.util.*;

class MultiTribePanel extends JPanel
{
    private final static int        PREF_W              = 1000;
    private final static int        PREF_H              =  360;
    private final static int        H_MARGIN            =   75;
    private final static int        GRAPH_W             = PREF_W - 2*H_MARGIN;
    private final static Color      CLUSTAL_COLOR       = Color.LIGHT_GRAY;
    private final static Color      AXIS_COLOR          = Color.BLACK;
    private final static Color      BG                  = new Color(210, 220, 255);
    private final static Color      MERGED_COLOR        = Color.RED.darker();
    private final static int        X_OF_Y_AXIS         =   35;
    private final static int        X_AXIS_TO_BOTTOM    =   25;
    private final static int        MAX_TO_TOP          =   40;
    private final static int        DFLT_N_TRIBES       =   20;
    private final static int        TRIBE_SIZE          =  100;
    private final static int        BREEDING_POOL_SIZE  =   50;
    private final static Font       BIG_FONT            = new Font("Serif", Font.PLAIN, 32);
    private final static int        TEXT_V_SPACING      =   30;
    private final static int        SCORE_PLACEHOLDER   = Integer.MIN_VALUE;

    private MSGFrame                frame;
    private Phase                   phase;
    private SequenceDataset         clustalAlignment;
    private int                     clustalScore;
    private int                     consensusWidth;
    private int                     nTribes;
    private Population[]            tribes;
    private Color[]                 tribeColors;
    private Population              mergedTribes;
    private float                   vertPixPerScoreUnit;
    private int                     nGenerationsTribePhase;
    private int                     nGenerationsCombinedPhase;
    private boolean                 darkBG;


    private enum Phase { PRIMORDIAL, TRIBES, MERGED, DONE }


    MultiTribePanel(MSGFrame frame, int nTribes)
    {
        this.frame = frame;
        this.nTribes = nTribes;
        phase = Phase.PRIMORDIAL;
        darkBG = true;
    }


    synchronized void reset(UngappedSequenceDataset seqNameToUngapped,
                            SequenceDataset clustalAlignment,
                            int clustalScore, int consensusWidth)
    {
        assert phase == Phase.PRIMORDIAL  ||  phase == Phase.DONE;
        phase = Phase.TRIBES;

        this.clustalAlignment = clustalAlignment;
        this.clustalScore = clustalScore;
        this.consensusWidth = consensusWidth;

        // All but the last tribe are ordinary.
        tribes = new Population[nTribes];
        mergedTribes = null;
        for (int i=0; i<nTribes-1; i++)
        {
            tribes[i] = new Population(TRIBE_SIZE,
                                       BREEDING_POOL_SIZE,
                                       seqNameToUngapped,
                                       consensusWidth,
                                       GRAPH_W);
        }
        int nGapsPerChromosome = tribes[0].getChromosomeLength();

        // Build 1 "kickstarted" population, initialized from the Clustal
        // alignment. If the desired width is << the clustal width, a kickstarted
        // population might be impossible; in this case the last tribe defaults
        // to an ordinary random population as above.
        tribes[nTribes-1] = new KickstartedPopulation(TRIBE_SIZE,
                                              BREEDING_POOL_SIZE,
                                              nGapsPerChromosome,
                                              seqNameToUngapped,
                                              clustalAlignment,
                                              consensusWidth,
                                              GRAPH_W);

        if (tribeColors == null)
        {
            tribeColors = new Color[nTribes];
            float deltaHue = 1f / nTribes;
            float hue = 0.05f;
            for (int i=0; i<nTribes; i++)
            {
                int hsb = Color.HSBtoRGB(hue, 1, 1);
                tribeColors[i] = new Color(hsb);
                hue += deltaHue;
            }
        }

        repaint();
    }


    public Dimension getPreferredSize()
    {
        return new Dimension(PREF_W, PREF_H);
    }


    public void paintComponent(Graphics g)
    {
        g.setColor(darkBG ? Color.BLACK : BG);
        g.fillRect(0, 0, 2000, 2000);

        switch (phase)
        {
            case PRIMORDIAL:
                return;
            case TRIBES:
                paintTribes(g);
                break;
            case MERGED:
                paintMerged(g);
                break;
            case DONE:
                paintDone(g);
                break;
        }
    }


    private void paintTribes(Graphics g)
    {
        assert phase == Phase.TRIBES;

        computeVScaleAndPaintNonHistoric(g);

        // Internal consistency is iffy during tribes->merged phase transition.
        // If tribes becomes null, it's not really serious.
        try
        {
            paintGenerationAndScore(g);
            for (int i=0; i<tribes.length; i++)
            {
                Population tribe = tribes[i];
                g.setColor(tribeColors[i]);
                PopulationHistory safeHistory = tribe.getHistory().xeroxThreadsafe();
                paintHistory(g, safeHistory);
            }
        }
        catch (NullPointerException x) { }
    }


    private void paintMerged(Graphics g)
    {
        try
        {
            computeVScaleAndPaintNonHistoric(g);
            paintGenerationAndScore(g);
            g.setColor(MERGED_COLOR);
            paintHistory(g, mergedTribes.getHistory().xeroxThreadsafe());
        }
        catch (NullPointerException x) { }
    }


    // Just paint final merged history. Won't be visible long (unless this is
    // the last consensus width we try) because thread in frame is about to
    // reset us.
    private void paintDone(Graphics g)
    {
        paintMerged(g);
    }


    // Caller should set color prior.
    private void paintHistory(Graphics g, PopulationHistory history)
    {
        if (history.size() < 2)
            return;
        int x0 = H_MARGIN;
        int y0 = scoreToPix(history.get(0));
        for (int j=1; j<history.size(); j++)
        {
            int x1 = H_MARGIN + j;
            int score = history.get(j);
            if (score == SCORE_PLACEHOLDER)
                continue;
            int y1 = scoreToPix(score);
            g.drawLine(x0, y0, x1, y1);
            x0 = x1;
            y0 = y1;
        }
    }


    private void computeVScaleAndPaintNonHistoric(Graphics g)
    {
        // Axes.
        g.setColor(AXIS_COLOR);
        Dimension size = getSize();
        g.drawLine(X_OF_Y_AXIS, 0, X_OF_Y_AXIS, 2000);

        // Compute vertical scale.
        int maxScore = Math.max(clustalScore, bestGAScore());
        float vertPixelsForGraphing = size.height - MAX_TO_TOP - X_AXIS_TO_BOTTOM;
        vertPixPerScoreUnit = vertPixelsForGraphing / maxScore;

        // ClustalScore
        g.setColor(CLUSTAL_COLOR);
        int clustalPix = scoreToPix(clustalScore);
        g.drawLine(0, clustalPix, 2000, clustalPix);
    }


    private void paintGenerationAndScore(Graphics g)
    {
        int bestGAScoreThisWidth = bestGAScore();
        if (bestGAScoreThisWidth == Integer.MIN_VALUE)
            return;

        g.setFont(BIG_FONT);
        g.setColor(darkBG ? Color.YELLOW : Color.BLACK);
        int generation = getGeneration();
        String s = "Generation = " + generation;
        int y = 250;
        if (generation != -1)
            g.drawString(s, 100, y);
        y += TEXT_V_SPACING;
        s = "Score = " + bestGAScoreThisWidth + " [w=" + consensusWidth + "]";
        g.drawString(s, 100, y);
        y += TEXT_V_SPACING;
        s = "ClustalW Score = " + clustalScore +
            " [w=" + clustalAlignment.widthOfWidestSequence() + "]";
        g.drawString(s, 100, y);
        assert frame != null;
        int bestWidth = consensusWidth;
        int bestScoreAnyWidth = bestGAScoreThisWidth;
        try
        {
            ConsensusWidthModel cwModel = frame.getConsensusWidthModel();
            if (cwModel != null)
            {
                int bestPrevWidth = cwModel.keyOfBestValue();
                int bestPrevScore = cwModel.bestValue();
                if (bestPrevScore > bestScoreAnyWidth)
                {
                    bestScoreAnyWidth = bestPrevScore;
                    bestWidth = bestPrevWidth;
                }
            }
        }
        catch (Exception x) { }
        s = "Best MSG Score so far = " + bestScoreAnyWidth + " [w=" +
            bestWidth + "]";
        y += TEXT_V_SPACING;
        g.setColor(darkBG ? Color.CYAN : Color.BLACK);
        g.drawString(s, 100, y);
    }


    // Returns -1 if internally inconsistent. This can happen during switchover
    // from tribes to merged phase.
    private int getGeneration()
    {
        try
        {
            switch (phase)
            {
                case PRIMORDIAL:
                    assert false;
                    throw new IllegalStateException();
                case TRIBES:
                    return tribes[0].getHistory().size();
                default:
                    return nGenerationsTribePhase + mergedTribes.getHistory().size();
            }
        }
        catch (NullPointerException x)
        {
            return -1;
        }
    }


    // Non-negative.
    private synchronized int bestGAScore()
    {
        int best = Integer.MIN_VALUE;
        switch (phase)
        {
            case PRIMORDIAL:
                throw new IllegalStateException("Called bestGAScore() in PRIMORDIAL phase.");
            case TRIBES:
                assert tribes != null  :  "Unexpected null tribes[]";
                for (Population tribe: tribes)
                {
                    assert tribe != null  :  "Null tribe";
                    assert tribe.getHistory() != null  :  "Tribe has null history";
                    best = Math.max(best, tribe.getMaxScore());
                }
                break;
            default:
                best = mergedTribes.getMaxScore();
                break;
        }
        return best;
    }


    private int scoreToPix(int score)
    {
        int relPix = (int)(score*vertPixPerScoreUnit);
        return getHeight() - X_AXIS_TO_BOTTOM - relPix;
    }


    private void computeMultiTribePhase()
    {
        assert phase == Phase.TRIBES;       // Set by reset()

        for (int i=0; i<nGenerationsTribePhase; i++)
        {
            for (Population tribe: tribes)
                tribe.step1Generation();    // records best score into history
            repaint();
            Thread.yield();
        }
        nGenerationsTribePhase = tribes[0].getHistory().size();
    }


    private void computeMergedPhase()
    {
        // Merge all tribes into a single diverse population. From now on,
        // tribes[] is untouchable. Add placeholders to merged population's
        // history; this will position its entries to the right of the tribes
        // when the graph is drawn.
        assert tribes != null  :  "Null tribes[] in computeMergedPhase().";
        mergedTribes = new Population(tribes);
        tribes = null;
        phase = Phase.MERGED;

        // Step the merged population.
        for (int i=0; i<nGenerationsCombinedPhase; i++)
        {
            mergedTribes.step1Generation();    // records best score into history
            repaint();
            Thread.yield();
        }

        phase = Phase.DONE;
        repaint();
    }


    // Must be called from a safe thread. Returns chromosome with best score.
    // The score is stored in the chromosome.
    Chromosome computeGAScore()
    {
        assert phase == Phase.TRIBES;
        assert consensusWidth > 0;
        computeMultiTribePhase();
        computeMergedPhase();
        return mergedTribes.getFittest();
    }


    static void sop(Object x)                   { System.out.println(x);         }
    int getNTribes()                            { return nTribes;                }
    void setNTribes(int n)                      { nTribes = n;                   }
    void setNGenerationsTribePhase(int n)       { nGenerationsTribePhase = n;    }
    void setNGenerationsCombinedPhase(int n)    { nGenerationsCombinedPhase = n; }
    void setDarkBG(boolean b)                   { darkBG = b;                    }
}
