package msg;

import java.io.*;
import java.util.*;


//
// Parses an alignment pasted from the ClustalW results panel.
//


class ClustalParser
{
    private final static int    MAX_SEQUENCES       =  8;
    private final static int    MAX_SEQUENCE_LEN    = 50;
    private       static File   file;


    // Returns a map from sequence name to sequence, with gaps inserted by
    // ClustalW. All sequences should be same length.
    static SequenceDataset parseFileToGapped(File f) throws IOException
    {
        file = f;
        FileReader fr = new FileReader(file);
        LineNumberReader lnr = new LineNumberReader(fr);
        String datasetName = f.getName();
        assert datasetName.endsWith(".clw");
        datasetName = datasetName.substring(0, datasetName.length()-4);
        SequenceDataset ret = new SequenceDataset(datasetName);
        String line;
        while ((line = lnr.readLine()) != null)
        {
            line = line.trim();
            if (line.length() == 0)
                continue;
            // Remove consensus value line (:.*)
            if (isConsensusLine(line))
                continue;
            String[] pieces = line.split("\\s+");   // @ least 1 whitespace
            String name = pieces[0];
            String aligned = ret.get(name);
            if (aligned == null)
                ret.put(name, pieces[1]);
            else
                ret.put(name, aligned+pieces[1]);
            if (ret.size() == MAX_SEQUENCES)
                break;
        }
        lnr.close();
        fr.close();
        int len = ret.values().iterator().next().length();
        for (String s: ret.values())
        {
            assert s.length() == len;
            String err = isAAaOrGaps(s);
            assert err == null : err;
        }

        Collection<String> clonedKeys = new HashSet<String>(ret.keySet());
        for (String k: clonedKeys)
        {
            if (ret.get(k).length() > MAX_SEQUENCE_LEN)
            {
                String v = ret.get(k).substring(0, MAX_SEQUENCE_LEN);
                ret.remove(k);
                ret.put(k, v);
            }
        }

        return ret;
    }


    private static boolean isConsensusLine(String s)
    {
        if (s.indexOf('|') >= 0)
            return false;

        return s.indexOf(':') >= 0  ||
               s.indexOf('.') >= 0  ||
               s.indexOf('*') >= 0;
    }


    // Returns null if string only contains amino acid codes or gaps. Otherwise
    // returns an error message.
    private final static String AAS_AND_GAP     = "ACDEFGHIKLMNPQRSTVWY-";
    static String isAAaOrGaps(String s)
    {
        for (int i=0; i<s.length(); i++)
            if (AAS_AND_GAP.indexOf(s.charAt(i)) < 0)
                return "Illegal char in file " + file.getName() + ": |" + s.charAt(i) + "| in " + s;
        return null;
    }

    static void sop(Object x)          { System.out.println(x); }
}
