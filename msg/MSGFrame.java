package msg;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


// Contains a BinarySearchPanel for top-level monitoring. Displays a non-modal
// dialog with a MultiTribePanel for monitoring run with current consensus width/
class MSGFrame extends JFrame implements ActionListener, ItemListener
{
    final static String     SEQUENCES_PARENT_PATH = "data";
    final static File       SEQUENCES_DIRF = new File(SEQUENCES_PARENT_PATH);
    final static String     BALIBASE_DIR_NAME = "Balibase_bb3_release";
    final static File       BALIBASE_DIRF = new File(SEQUENCES_DIRF, BALIBASE_DIR_NAME);
    final static String     GAG_POL_DIR_NAME = "GagPol";
    final static File       GAG_POL_DIRF = new File(SEQUENCES_DIRF, GAG_POL_DIR_NAME);
    final static String     PDGH_DIR_NAME = "PDGH";
    final static File       PDGH_DIRF = new File(SEQUENCES_DIRF, PDGH_DIR_NAME);
    final static String     RANDOM_DIR_NAME = "Random";
    final static File       RANDOM_DIRF = new File(SEQUENCES_DIRF, RANDOM_DIR_NAME);
    final static String     ORDER_DIR_NAME = "Order";
    final static File       ORDER_DIRF = new File(SEQUENCES_DIRF, ORDER_DIR_NAME);

    private final static int[]      N_GENS_OPTIONS   = { 3, 100, 200, 300, 500,
                                                         750, 1000, 2000 };
    private final static int[]      N_TRIBES_OPTIONS = { 1, 10, 20 };

    private JMenuItem               quitMI;
    private ConsensusWidthPanel     conWidthPan;
    private MultiTribePanel         multiTribePan;
    private SequenceDataset         clustalGappedDataset;
    private UngappedSequenceDataset ungappedDataset;
    private int                     clustalScore;
    private MSGFrame                outerThis;
    private JComboBox               tribeGenerationsCombo;
    private JComboBox               combinedGenerationsCombo;
    private JComboBox               nTribesCombo;
    private ConsensusDialog         consensusDia;
    private JCheckBox               darkBGCbox;


    public MSGFrame()
    {
        JMenuBar mbar = new JMenuBar();
        try
        {
            mbar.add(buildFileMenu());
        }
        catch (IOException x)
        {
            sop("Can't open dataset dir: " + x.getMessage());
            x.printStackTrace(System.out);
        }
        setJMenuBar(mbar);

        setLayout(new BorderLayout());
        JPanel controlsAndConWidth = new JPanel(new BorderLayout());
        JPanel controls = new JPanel();
        controls.add(new JLabel("# Tribes: "));
        nTribesCombo = buildIntCombo(N_TRIBES_OPTIONS);
        nTribesCombo.setSelectedIndex(1);
        controls.add(nTribesCombo);
        controls.add(new JLabel("# Gens Tribe Phase: "));
        tribeGenerationsCombo = buildNGensCombo();
        tribeGenerationsCombo.setSelectedItem(750);
        controls.add(tribeGenerationsCombo);
        controls.add(new JLabel("# Gens Combined Phase: "));
        combinedGenerationsCombo = buildNGensCombo();
        combinedGenerationsCombo.setSelectedItem(500);
        controls.add(combinedGenerationsCombo);
        darkBGCbox = new JCheckBox("Dark bgnd", true);
        darkBGCbox.addItemListener(this);
        controls.add(darkBGCbox);
        controlsAndConWidth.add(controls);
        conWidthPan = new ConsensusWidthPanel();
        controlsAndConWidth.add(conWidthPan, BorderLayout.SOUTH);
        add(controlsAndConWidth, BorderLayout.NORTH);
        multiTribePan = new MultiTribePanel(this, getNTribes());
        multiTribePan.setNGenerationsTribePhase(getNGensTribe());
        multiTribePan.setNGenerationsCombinedPhase(getNGensCombined());
        add(multiTribePan, BorderLayout.SOUTH);

        pack();

        outerThis = this;
    }


    private JComboBox buildNGensCombo()
    {
        return buildIntCombo(N_GENS_OPTIONS);
    }

    @SuppressWarnings("unchecked")
    JComboBox buildIntCombo(int[] vals)
    {
        JComboBox combo = new JComboBox();
        for (int i: vals)
            combo.addItem(i);
        combo.addItemListener(this);
        return combo;
    }


    private int getNGensTribe()
    {
        return (Integer)tribeGenerationsCombo.getSelectedItem();
    }


    private int getNGensCombined()
    {
        return (Integer)combinedGenerationsCombo.getSelectedItem();
    }


    private int getNTribes()
    {
        return (Integer)nTribesCombo.getSelectedItem();
    }


    private JMenu buildFileMenu() throws IOException
    {
        JMenu menu = new JMenu("File");

        // OPEN submenu.
        JMenu openMenu = new JMenu("Open");
        String[] subsubs = { "Balibase", "Gag/Pol", "PDGH", "Random", "Order" };
        File[] dirfs = { BALIBASE_DIRF, GAG_POL_DIRF, PDGH_DIRF, RANDOM_DIRF, ORDER_DIRF };
        for (int i=0; i<dirfs.length; i++)
        {
            JMenu subMenu = new JMenu(subsubs[i]);
            File dirf = dirfs[i];
			assert dirf.exists() : "No such directory: " + dirf.getAbsolutePath();
            String[] contents = dirf.list();
            Set<String> clwSorter = new TreeSet<String>();
            for (String kid: contents)
                if (kid.endsWith(".clw"))
                    clwSorter.add(kid);
            for (String kid: clwSorter)
            {
                String title = kid.substring(0, kid.length()-4);
                File kidFile = new File(dirf, kid);
                subMenu.add(new DatasetMenuItem(title, kidFile));
            }
            openMenu.add(subMenu);
        }
        menu.add(openMenu);

        // EXIT item.
        menu.addSeparator();
        quitMI = new JMenuItem("Exit");
        quitMI.addActionListener(this);
        menu.add(quitMI);
        return menu;
    }


    private class DatasetMenuItem extends JMenuItem implements ActionListener
    {
        String      displayName;
        File        file;

        DatasetMenuItem(String displayName, File file)
        {
            super(displayName);
            this.displayName = displayName;
            this.file = file;
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e)
        {
            try
            {
                assert file.exists();
                clustalGappedDataset = ClustalParser.parseFileToGapped(file);
                ungappedDataset = clustalGappedDataset.removeGaps();
                clustalScore = AlignmentScorer.scoreAlignment(clustalGappedDataset.values());
                conWidthPan.resetForNewDataSet();
                conWidthPan.setClustalScore(clustalScore);
                setTitle("Analyzing " + ungappedDataset.getName());
                (new TopLevelThread()).start();
              }
              catch (IOException x)
              {
                  sop("Can't open dataset " + getText() + ": " + x.getMessage());
                  x.printStackTrace(System.out);
              }
          }
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == quitMI)
            System.exit(0);
    }


    public void itemStateChanged(ItemEvent e)
    {
        if (e.getSource() == tribeGenerationsCombo)
        {
            if (multiTribePan != null)
                multiTribePan.setNGenerationsTribePhase(getNGensTribe());
        }

        else if (e.getSource() == tribeGenerationsCombo)
        {
            if (multiTribePan != null)
                multiTribePan.setNGenerationsCombinedPhase(getNGensCombined());
        }

        else if (e.getSource() == darkBGCbox)
        {
            multiTribePan.setDarkBG(darkBGCbox.isSelected());
            multiTribePan.repaint();
        }
    }


    static void snooze(int msecs)
    {
        try
        {
            Thread.sleep(msecs);
        }
        catch (InterruptedException x) { }
    }


    // For consistency checking. Returns true if compo is ultimately contained
    // in this JFrame object.
    private boolean isUltimateChild(Component compo)
    {
        while (compo != null  &&  compo != outerThis)
            compo = compo.getParent();
        return compo == outerThis;
    }


    private Vector<Integer> getWidthProgram()
    {
        int wClustal = clustalGappedDataset.widthOfWidestSequence();
        Stack<Integer> stack = new Stack<Integer>();
        int min = Math.max(wClustal-10, ungappedDataset.widthOfWidestSequence());
        stack.add(min);
        stack.add(wClustal + 10);
        stack.add(wClustal);
        for (int w=min+1; w<wClustal+10; w++)
            if (w != wClustal)
                stack.add(w);
        return stack;
    }


    private class TopLevelThread extends Thread
    {
        public void run()
        {
            assert multiTribePan != null;
            assert isUltimateChild(multiTribePan)  :
                   "multiTribePan isn't ultimately contained by frame";

            setPriority(2); // Don't crowd out the GUI thread or the GC
            Vector<Integer> widths = getWidthProgram();
            ConsensusWidthModel conWidthModel = conWidthPan.getModel();
            Chromosome fittestChromo = null;
            int fittestScore = Integer.MIN_VALUE;
            for (int width: widths)
            {
                Chromosome fittestChromoForWidth = computeGAScoreForWidth(width);
                conWidthModel.put(width, fittestChromoForWidth.score);
                if (fittestChromoForWidth.score > fittestScore)
                {
                    fittestScore = fittestChromoForWidth.score;
                    fittestChromo = fittestChromoForWidth;
                }
            }

            // Display & print our & clustal alignments.
            String dataset = ungappedDataset.getName();
            int nTribes = multiTribePan.getNTribes();
            int width = conWidthModel.keyOfBestValue();
            sop("Finished analysis of " + dataset);
            ArrayList<String> msgAlignment = fittestChromo.toMSAStrings(ungappedDataset, width);
            Collection<String> clustalAlignment = clustalGappedDataset.values();
            if (consensusDia == null)
                consensusDia = new ConsensusDialog(msgAlignment, clustalAlignment,
                                                   fittestScore, clustalScore);
            else
                consensusDia.setParams(msgAlignment, clustalAlignment, fittestScore, clustalScore);
            consensusDia.setVisible(true);
            sop("\nMSG Alignment (score = " + fittestScore + "):");
            for (String s: msgAlignment)
                sop(s);
            sop("\nClustal-W Alignment (score = " + clustalScore + "):");
            for (String s: clustalAlignment)
                sop(s);
        }

        private Chromosome computeGAScoreForWidth(int consensusWidth)
        {
            // Reset the multi-tribe panel for next consensus width.
            multiTribePan.setNTribes(getNTribes());
            multiTribePan.setNGenerationsTribePhase(getNGensTribe());
            multiTribePan.setNGenerationsCombinedPhase(getNGensCombined());
            multiTribePan.setNTribes(getNTribes());
            multiTribePan.reset(ungappedDataset, clustalGappedDataset,
                                clustalScore, consensusWidth);
            return multiTribePan.computeGAScore();
        }
    }

    ConsensusWidthModel getConsensusWidthModel()
    {
        try
        {
            return conWidthPan.getModel();
        }
        catch (Exception x)
        {
            // Easier than synchronizing.
            return null;
        }
    }
    static void sop(Object x)       { System.out.println(x); }
    static void snooze()            { snooze(1000);          }


    public static void main(String[] args) {
        MSGFrame frame = new MSGFrame();
        frame.setLocation(10, 10);
        frame.setVisible(true);
    }
}
