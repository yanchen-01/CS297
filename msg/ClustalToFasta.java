package msg;

import java.io.*;
import java.util.*;


class ClustalToFasta
{
    static void convertFile(File dirf, String clwFilename) throws IOException
    {
        File clustalFile = new File(dirf, clwFilename);
        SequenceDataset clustalAlignment = ClustalParser.parseFileToGapped(clustalFile);
        UngappedSequenceDataset ungapped = clustalAlignment.removeGaps();
        String fastaFilename = clwFilename.substring(0, clwFilename.length()-4);
        fastaFilename += ".fasta";
        File fastaFile = new File(dirf, fastaFilename);
        FileWriter fw = new FileWriter(fastaFile);
        PrintWriter pw = new PrintWriter(fw, true);     // true for autoflush
        for (String k: ungapped.keySet())
        {
            pw.write(">" + k + "\r\n");
            pw.write(ungapped.get(k) + "\r\n");
        }
        pw.close();
        fw.close();
    }


    static void convertDir(File dirf) throws IOException
    {
        String[] contents = dirf.list();
        for (String kid: contents)
            if (kid.endsWith(".clw"))
                convertFile(dirf, kid);
    }


    static void sop(Object x)       { System.out.println(x); }


    public static void main(String[] args)
    {
        try
        {
            File dirf = MSGFrame.PDGH_DIRF;
            convertDir(dirf);
            System.out.println("Done");
        }
        catch (IOException x)
        {
            System.out.println("IO Stress: " + x.getMessage());
            x.printStackTrace(System.out);
        }
    }
}
