package msg;

import java.util.*;
import java.awt.*;
import javax.swing.*;


class ConsensusWidthPanel extends JPanel
{
    private final static Font           FONT = new Font("Monospaced", Font.PLAIN, 12);
    private final static Color          BG = Color.WHITE;
    private final static Color          AXIS_COLOR = Color.BLACK;
    private final static Color          AXIS_TEXT_COLOR = Color.BLACK;
    private final static Color          DOT_COLOR = Color.BLUE;
    private final static Color          DOT_JOIN_COLOR = Color.BLUE.darker();
    private final static Color          CURRENT_COLOR = Color.MAGENTA;
    private final static Color          CLUSTAL_COLOR = new Color(128, 128, 128);
    private final static Color          LATEST_COLOR = new Color(105, 150, 255);
    private final static Color          WINNER_COLOR = Color.RED.darker();
    private final static Color          GRID_COLOR = Color.LIGHT_GRAY;
    private final static int            DOT_RADIUS = 4;
    private final static int            H_MARGIN = 60;
    private final static int            TOP_MARGIN = 40;
    private final static int            BOTTOM_TO_H_AXIS_LABEL_BASELINE = 20;
    private final static int            BOTTOM_TO_H_AXIS = 41;
    private final static int            GRAPH_W_PIX_PREF = 600;
    private final static int            GRAPH_H_PIX_PREF = 280;

    private float                       xUnitsPerPixel;
    private float                       yUnitsPerPixel;
    private int                         xCurrent;       // In abstract (non-pixel) units
    private ConsensusWidthModel         model;
    private int                         clustalScore;


    ConsensusWidthPanel()
    {
        setOpaque(true);
        setBackground(BG);
        model = new ConsensusWidthModel(this);
    }


    public Dimension getPreferredSize()
    {
        int wPref = H_MARGIN + GRAPH_W_PIX_PREF + H_MARGIN;
        int hPref = TOP_MARGIN + GRAPH_H_PIX_PREF + BOTTOM_TO_H_AXIS;
        return new Dimension(wPref, hPref);
    }

    public void paintComponent(Graphics g)
    {
        // If only 0 or 1 dots, there's not enough info to set H scale. It will
        // be set to something useless, and should not be used.
        adjustHScale();
        adjustVScale();

        // Clear
        g.setColor(BG);
        g.fillRect(0, 0, 2000, 2000);

        // Use a threadsafe clone of the model.
        ConsensusWidthModel safeModel = (model == null) ? null : model.xerox();

        // Vertical grid.
        if (safeModel != null  &&  safeModel.size() >= 2  &&  xUnitsPerPixel < 0.2f)
        {
            int yTop = yToPix(clustalScore);
            int yBottom = getHeight() - BOTTOM_TO_H_AXIS;
            int xMinUnits = safeModel.getMinX();
            int xMaxUnits = safeModel.getMaxX();
            g.setColor(GRID_COLOR);
            for (int xUnits=xMinUnits; xUnits<=xMaxUnits; xUnits++)
            {
                int xPix = xToPix(xUnits);
                g.drawLine(xPix, yTop, xPix, yBottom);
            }
        }

        // Axes.
        g.setColor(AXIS_COLOR);
        int y = getHeight() - BOTTOM_TO_H_AXIS;
        int r = getWidth() - H_MARGIN;
        g.drawLine(H_MARGIN, y, r, y);
        g.drawLine(H_MARGIN, y, H_MARGIN, TOP_MARGIN);
        g.drawLine(r, y, r, TOP_MARGIN);

        // Label axes if values at both ends have been computed.
        g.setFont(FONT);
        if (safeModel != null  &&  safeModel.size() >= 2)
        {
            int xMinUnits = safeModel.getMinX();
            int xMaxUnits = safeModel.getMaxX();
            int xMinPix = xToPix(xMinUnits);
            int xMaxPix = xToPix(xMaxUnits);
            int[] xPixes = new int[] { xMinPix, xMaxPix };
            String[] ss = new String[2];
            ss[0] = "Min w = " + xMinUnits;
            ss[1] = "Max w = " + xMaxUnits;
            for (int i=0; i<2; i++)
            {
                int sw = g.getFontMetrics().stringWidth(ss[i]);
                int sx = xPixes[i] - sw/2;
                g.drawString(ss[i], sx, TOP_MARGIN-3);
            }
        }

        // Nothing else to do if no data yet.
        if (safeModel == null  ||  safeModel.isEmpty())
            return;

        // If multiple dots, join them. Draw these lines first, so they'll
        // be overlaid by the dots themselves.
        if (safeModel.size() >= 2)
        {
            g.setColor(DOT_JOIN_COLOR);
            boolean first = true;
            int x0Pix = 0;
            int y0Pix = 0;
            for (int x1: safeModel.keySet())
            {
                int x1Pix = xToPix(x1);
                int y1Pix = yToPix(safeModel.get(x1));
                if (!first)
                    g.drawLine(x0Pix, y0Pix, x1Pix, y1Pix);
                first = false;
                x0Pix = x1Pix;
                y0Pix = y1Pix;
            }
        }

        // ClustalW score.
        if (clustalScore > 0)
        {
            g.setColor(CLUSTAL_COLOR);
            y = yToPix(clustalScore);
            g.drawLine(0, y, 2000, y);
            String s = "ClustalW score = " + clustalScore;
            g.drawString(s, H_MARGIN+25, y-2);
        }

        // Mark most recent score.
        Integer xLastUnitsWrapped = safeModel.getLastKeyAdded();
        int xBestUnits = safeModel.keyOfBestValue();
        int xBestPix = xToPix(xBestUnits);
        if (xLastUnitsWrapped != null  &&  xLastUnitsWrapped != xBestUnits)
        {
            int xLastUnits = xLastUnitsWrapped;
            int yLastUnits = safeModel.get(xLastUnits);
            int xLastPix = xToPix(xLastUnits);
            int yLastPix = yToPix(yLastUnits);
            g.setColor(LATEST_COLOR);
            g.drawLine(xLastPix, getHeight()-BOTTOM_TO_H_AXIS+25, xLastPix, yLastPix);
            String s = "Last score=" + yLastUnits + " at w=" + xLastUnits;
            int sw = g.getFontMetrics().stringWidth(s);
            int xText = xLastPix - sw/2;
            xText = Math.max(xText, 0);
            xText = Math.min(xText, getWidth()-sw/2);
            g.drawString(s, xText, getHeight()-BOTTOM_TO_H_AXIS+38);
        }

        // Mark top score so far.
        int yBestUnits = safeModel.get(xBestUnits);
        int yBestPix = yToPix(yBestUnits);
        g.setColor(WINNER_COLOR);
        g.drawLine(xBestPix, getHeight()-BOTTOM_TO_H_AXIS+14, xBestPix, yBestPix);
        String s = "Top score=" + yBestUnits + " at w=" + xBestUnits;
        g.setFont(FONT);
        int sw = g.getFontMetrics().stringWidth(s);
        int xText = xBestPix - sw/2;
        xText = Math.max(xText, 5);
        xText = Math.min(xText, getWidth()-sw/2-5);
        g.drawString(s, xText, getHeight()-BOTTOM_TO_H_AXIS+25);

        // All dots. Min & max are labeled. Best is in special color.
        g.setColor(DOT_COLOR);
        FontMetrics fm = g.getFontMetrics();
        int xMinUnits = safeModel.getMinX();
        int xMaxUnits = safeModel.getMaxX();
        int stringW = fm.stringWidth(""+xMinUnits);
        Point textOffsetXMin = new Point(-DOT_RADIUS - stringW - 9, DOT_RADIUS + 2);
        Point textOffsetXMax = new Point(DOT_RADIUS + 2, DOT_RADIUS + 2);
        for (int x: safeModel.keySet())
        {
            Point textOffset = null;
            if (x == xMinUnits)
                textOffset = textOffsetXMin;
            else if (x == xMaxUnits)
                textOffset = textOffsetXMax;
            Color dotColor = DOT_COLOR;
            if (x == xBestUnits)
                dotColor = WINNER_COLOR;
            else if (xLastUnitsWrapped != null  &&  xLastUnitsWrapped != xBestUnits)
                dotColor = LATEST_COLOR;
            g.setColor(dotColor);
            paintDotAndValue(g, x, textOffset);
        }

        // X currently being computed.
        if (xCurrent > 0)
        {
            g.setColor(CURRENT_COLOR);
            int xPix = xToPix(xCurrent);
            g.drawLine(xPix, 0, xPix, 2000);
        }
    }


    private void adjustHScale()
    {
        if (model == null  ||  model.size() < 2)
        {
            xUnitsPerPixel = 12345;     // should never matter
            return;
        }

        float graphWUnits = model.getMaxX() - model.getMinX();
        float graphWPix = getWidth() - 2*H_MARGIN;
        xUnitsPerPixel = graphWUnits / graphWPix;
    }


    private void adjustVScale()
    {
        int bestGAScore = (model == null || model.isEmpty()) ? 1 : model.bestValue();
        float bestScore = Math.max(bestGAScore, clustalScore);
        float fUnitsPerPix = bestScore / GRAPH_H_PIX_PREF;
        yUnitsPerPixel = (float)Math.ceil(fUnitsPerPix);
    }


    // X is in arbitrary units, not pixels. Uses g's current color & font.
    // Labels the point if textOffset is not null.
    private void paintDotAndValue(Graphics g, int xUnits, Point textOffset)
    {
        assert model.containsKey(xUnits) : "Model has no value for " + xUnits;

        int xPix = xToPix(xUnits);
        // Special case: put max x on right vertical axis. Roundoff would put it
        // a couple of puxels to the left.
        if (model.size() >= 2  &&  xUnits == model.getMaxX())
            xPix = getWidth() - H_MARGIN;
        int yUnits = model.get(xUnits);
        int yPix = yToPix(yUnits);
        g.fillOval(xPix-DOT_RADIUS, yPix-DOT_RADIUS, 2*DOT_RADIUS, 2*DOT_RADIUS);

        if (textOffset != null)
        {
            String s = "" + yUnits;
            g.drawString(s, xPix + textOffset.x, yPix + textOffset.y);
        }
    }


    private int xToPix(int xUnits)
    {
        // Ensure max x is on right-side vertical axis. (Otherwise rounding
        // error makes ugly.)
        if (xUnits == model.getMaxX())
            return getWidth() - H_MARGIN;

        int xMin = model.getMinX();
        return H_MARGIN + Math.round((xUnits-xMin)/xUnitsPerPixel);
    }


    private int yToPix(int yUnits)
    {
        yUnits = Math.max(yUnits, 0);
        return getHeight() - BOTTOM_TO_H_AXIS - Math.round(yUnits/yUnitsPerPixel);
    }


    void resetForNewDataSet()
    {
        model = new ConsensusWidthModel(this);
        clustalScore = 0;
        repaint();
    }


    void setClustalScore(int c)
    {
        clustalScore = c;
        repaint();
    }


    ConsensusWidthModel getModel()  { return model;                }
    boolean isFinished()            { return model.isFinished();   }
    static void sop(Object x)       { System.out.println(x);       }


    public static void main(String[] args)
    {
        ConsensusWidthPanel cwp = new ConsensusWidthPanel();
        cwp.setClustalScore(350);
        ConsensusWidthModel model = cwp.getModel();
        model.put(10, 100);
        model.put(50, 500);
        model.put(20, 200);
        model.put(30, 300);
        model.put(40, 400);
        JFrame frame = new JFrame();
        frame.add(cwp, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }
}
