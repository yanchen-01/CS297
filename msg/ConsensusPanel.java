package msg;

import java.awt.*;
import javax.swing.*;
import java.util.*;


class ConsensusPanel extends JPanel
{
    private final static Font       MSA_FONT            = new Font("Monospaced", Font.BOLD, 16);
    private final static Font       SCORE_FONT          = new Font("Serif", Font.BOLD, 26);
    private final static int        TOP_BASELINE        =   25;
    private final static int        LEFT_MARGIN         =   25;
    private final static int        LINE_V_SPACING      =   20;
    private final static int        CONSEN_BOX_W        =   10;
    private final static int        CONSEN_BOX_H        =   16;
    private final static Color[]    CONSEN_COLORS       =
    {
        Color.CYAN, new Color(25, 255, 25), Color.YELLOW, Color.ORANGE, Color.PINK, Color.RED
    };
    private final static Color[]    CONSEN_COLORS_4     =
    {
        Color.CYAN, Color.YELLOW, Color.ORANGE, Color.RED
    };
    private final static Color[]    CONSEN_COLORS_6     =
    {
        Color.CYAN, new Color(25, 255, 25), Color.YELLOW, Color.ORANGE, Color.PINK, Color.RED
    };
    private final static Color[]    CONSEN_COLORS_8     =
    {
        Color.CYAN, new Color(25, 255, 25), new Color(25, 255, 25),
        Color.YELLOW, Color.YELLOW, Color.ORANGE, Color.PINK, Color.RED
    };
    private final static Color[]    CONSEN_COLORS_10    =
    {
        Color.CYAN, new Color(25, 255, 25), new Color(25, 255, 25),
        Color.YELLOW, Color.YELLOW, Color.ORANGE, Color.ORANGE,
        Color.PINK, Color.RED, Color.RED
    };
    private final static Color[]    CONSEN_COLORS_12    =
    {
        Color.CYAN, new Color(25, 255, 25), new Color(25, 255, 25),
        Color.YELLOW, Color.YELLOW, Color.ORANGE, Color.ORANGE,
        Color.PINK, Color.PINK, Color.RED, Color.RED, Color.RED
    };
    private final static Map<Integer, Color[]>
                                    DATA_SET_SIZE_TO_CONSEN_COLORS;

    static
    {
        DATA_SET_SIZE_TO_CONSEN_COLORS = new HashMap<Integer, Color[]>();
        DATA_SET_SIZE_TO_CONSEN_COLORS.put(4, CONSEN_COLORS_4);
        DATA_SET_SIZE_TO_CONSEN_COLORS.put(6, CONSEN_COLORS_6);
        DATA_SET_SIZE_TO_CONSEN_COLORS.put(8, CONSEN_COLORS_8);
        DATA_SET_SIZE_TO_CONSEN_COLORS.put(10, CONSEN_COLORS_10);
        DATA_SET_SIZE_TO_CONSEN_COLORS.put(12, CONSEN_COLORS_12);
        for (int i: DATA_SET_SIZE_TO_CONSEN_COLORS.keySet())
            assert DATA_SET_SIZE_TO_CONSEN_COLORS.get(i).length == i;
    }


    private ArrayList<String>       msgAlignment;
    private Collection<String>      clustalAlignment;
    private int                     msgScore;
    private int                     clustalScore;
    private char[]                  gaOrClustalConsensus;           // evil global
    private int[]                   gaOrClustalConsensusDissention; // evil global


    ConsensusPanel(ArrayList<String> msgAlignment, Collection<String> clustalAlignment,
                   int msgScore, int clustalScore)
    {
        setParams(msgAlignment, clustalAlignment, msgScore, clustalScore);
    }


    public Dimension getPreferredSize()
    {
        return new Dimension(1000, 400);
    }


    void setParams(ArrayList<String> msgAlignment, Collection<String> clustalAlignment,
                   int msgScore, int clustalScore)
    {
        this.msgAlignment = msgAlignment;
        this.clustalAlignment = clustalAlignment;
        this.msgScore = msgScore;
        this.clustalScore = clustalScore;
        repaint();
    }


    private void paintConsensus(Graphics g, int y, Collection<String> text)
    {
        g.setFont(MSA_FONT);
        int x = LEFT_MARGIN;
        Color[] bgByNDissenters = DATA_SET_SIZE_TO_CONSEN_COLORS.get(text.size());

        for (int col=0; col<gaOrClustalConsensus.length; col++)
        {
            char ch = gaOrClustalConsensus[col];
            int nDissenters = gaOrClustalConsensusDissention[col];
            Color boxColor = (bgByNDissenters == null) ?
                             Color.BLACK :
                             bgByNDissenters[nDissenters];
            g.setColor(boxColor);
            g.fillRect(x, y-CONSEN_BOX_H+8, CONSEN_BOX_W, CONSEN_BOX_H);
            g.setColor(Color.BLACK);
            g.drawString(""+ch, x, y+4);
            x += CONSEN_BOX_W;
        }
    }


    public void paintComponent(Graphics g)
    {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 2000, 2000);

        // G.A. alignment.
        int y = paintMSAWithConsensus(g, TOP_BASELINE, msgAlignment, false);

        // ClustalW alignment.
        paintMSAWithConsensus(g, y+2*LINE_V_SPACING, clustalAlignment, true);
    }


    // Returns next y.
    private int paintMSAWithConsensus(Graphics g, int y,
                                      Collection<String> text,
                                      boolean consensusOnTop)
    {
        // Generate gaOrClustalConsensus[] & gaOrClustalConsensusDissention[]
        // for immediate use only.
        computeConsensus(text);

        // Paint consensus if it's on top.
        if (consensusOnTop)
        {
            paintConsensus(g, y, text);
            y += LINE_V_SPACING+3;
        }

        // Paint text.
        g.setFont(MSA_FONT);
        g.setColor(Color.BLACK);
        for (String s: text)
        {
            g.drawString(s, LEFT_MARGIN, y);
            y += LINE_V_SPACING;
        }

        // Paint consensus if it's on bottom.
        if (!consensusOnTop)
        {
            y += 3;
            paintConsensus(g, y, text);
        }

        // Paint score in large font.
        String s = text.iterator().next();
        int sw = g.getFontMetrics().stringWidth(s);
        int x = LEFT_MARGIN + sw + 35;
        boolean msgNotClustal = !consensusOnTop;
        paintScore(g, x, y, msgNotClustal);

        return y;
    }


    private void paintScore(Graphics g, int x, int baseline, boolean isMsg)
    {
        if (!isMsg)
            baseline -= 22;
        g.setColor(Color.BLACK);
        g.setFont(SCORE_FONT);
        int score = isMsg ? msgScore : clustalScore;
        String s = isMsg ? "Our" : "ClustalW";
        s += " score = " + score;
        g.drawString(s, x, baseline);
    }


    // Sets global consensus string and dissenter counts.
    private void computeConsensus(Collection<String> text)
    {
        int nRows = text.size();
        int nCols = text.iterator().next().length();
        gaOrClustalConsensus = new char[nCols];
        gaOrClustalConsensusDissention = new int[nCols];
        for (int col=0; col<nCols; col++)
        {
            int[] counts = new int[26];
            int dashCount = 0;
            int winnerCount = 0;
            char winner = 0;
            for (String s: text)
            {
                char ch = s.charAt(col);
                int newCount = -12345;
                if (ch == '-')
                    newCount = ++dashCount;
                else
                    newCount = ++counts[ch-'A'];
                if (newCount > winnerCount)
                {
                    winnerCount = newCount;
                    winner = ch;
                }
            }
            gaOrClustalConsensus[col] = winner;
            gaOrClustalConsensusDissention[col] = nRows - winnerCount;
        }
    }


    private void paintConsensus(Graphics g, int y)
    {
        assert msgAlignment != null  &&  msgAlignment.size() >= 4;

        g.setFont(MSA_FONT);
        int nCols = msgAlignment.get(0).length();
        int x = LEFT_MARGIN;
        for (int col=0; col<nCols; col++)
        {
            // Determine consensus char and degree of conservation.
            char majorityChar = 0;
            Map<Character, int[]> colCensus = new HashMap<Character, int[]>();
            int bestCount = 0;
            for (String s: msgAlignment)
            {
                char ch = s.charAt(col);
                int[] wrappedCount = colCensus.get(ch);
                if (wrappedCount == null)
                {
                    wrappedCount = new int[]{0};
                    colCensus.put(ch, wrappedCount);
                }
                wrappedCount[0]++;
                if (wrappedCount[0] > bestCount)
                {
                    bestCount = wrappedCount[0];
                    majorityChar = ch;
                }
            }

            // Paint consensus char on bg.
            int badness = msgAlignment.size() - bestCount;
            badness = Math.min(badness, CONSEN_COLORS.length-1);
            if (badness < 0)
            {
                String s = "alignment size = " + msgAlignment.size() +
                           ", best count = " + bestCount;
                assert false : s;
            }
            Color boxColor = CONSEN_COLORS[badness];
            g.setColor(boxColor);
            g.fillRect(x, y-CONSEN_BOX_H+8, CONSEN_BOX_W, CONSEN_BOX_H);
            g.setColor(Color.BLACK);
            g.drawString(""+majorityChar, x, y+4);
            x += CONSEN_BOX_W;
        }
    }


}
