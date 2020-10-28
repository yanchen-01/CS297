package msg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Stack;

public class MSG {
    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            throw new Exception("Need args: " + displayOptions());
        switch (args[0]) {
            case "base":
                printClustalScore();
                break;
            case "widths":
                printWidths();
                break;
            case "align":
                if (args.length == 1)
                    throw new Exception("Need width");
                int width = Integer.parseInt(args[1]);
                align(width);
                break;
            case "score":
                if (args.length == 1)
                    throw new Exception("Need alignment");
                String alignmentString = args[1].replace("[", "");
                alignmentString = alignmentString.replace("]", "");
                String[] array = alignmentString.split(", ");
                ArrayList<String> alignment = new ArrayList<String>();
                Collections.addAll(alignment, array);
                System.out.println(AlignmentScorer.scoreAlignment(alignment));
                break;
            default:
                throw new Exception("Wrong arg: " + args[0] + displayOptions());
        }
    }

    static int N_TRIBES = 10;
    static int N_GENS_TRIBE = 750;
    static int N_GENS_COMBINED = 500;
    static final File SEQUENCES_DIRF = new File("../msg/data");
    static final File BALIBASE_DIRF;
    static final File clwFile;
    static SequenceDataset clustalGappedDataset;
    static UngappedSequenceDataset ungappedDataset;
    private static MultiTribePanel multiTribe;
    private static int clustalScore;

    static {
        BALIBASE_DIRF = new File(SEQUENCES_DIRF, "Balibase_bb3_release");
        clwFile = new File(BALIBASE_DIRF, "BB11001.clw");

        try {
            clustalGappedDataset = ClustalParser.parseFileToGapped(clwFile);
        } catch (IOException var1) {
            var1.printStackTrace();
        }

        ungappedDataset = clustalGappedDataset.removeGaps();
        multiTribe = new MultiTribePanel((MSGFrame) null, N_TRIBES);
    }


    private static void printClustalScore() {
        clustalScore = AlignmentScorer.scoreAlignment(clustalGappedDataset.values());
        System.out.println(clustalScore);
    }

    private static void printWidths() {
        int wClustal = clustalGappedDataset.widthOfWidestSequence();
        Stack<Integer> stack = new Stack<Integer>();
        int min = Math.max(wClustal - 10, ungappedDataset.widthOfWidestSequence());
        stack.add(min);
        stack.add(wClustal + 10);
        stack.add(wClustal);
        for (int w = min + 1; w < wClustal + 10; w++)
            if (w != wClustal)
                stack.add(w);
        System.out.println(stack);
    }

    public static void align(int width) {
        ConsensusWidthPanel conWidthPan = new ConsensusWidthPanel();
        ConsensusWidthModel conWidthModel = conWidthPan.getModel();
        multiTribe.setNTribes(N_TRIBES);
        multiTribe.setNGenerationsTribePhase(N_GENS_TRIBE);
        multiTribe.setNGenerationsCombinedPhase(N_GENS_COMBINED);
        multiTribe.reset(ungappedDataset, clustalGappedDataset,
                clustalScore, width);
        Chromosome fittestChromoForWidth = multiTribe.computeGAScore();
        conWidthModel.put(width, fittestChromoForWidth.score);
        ArrayList<String> msgAlignment = fittestChromoForWidth.toMSAStrings(ungappedDataset, width);
        System.out.print(msgAlignment);
    }

    private static String displayOptions() {
        return "\n(base) for the clustal score, " +
                "\n(widths) for get width, " +
                "\n(align width) for the alignment based on the width" +
                "\n(score width) for the score of the alignment based on the width";
    }
}