package msg;

import java.io.*;


class RandomFastaGenerator
{
    private final static int[]      SKEWS       = { 0, 2, 5, 7 };
    private final static String[]   FNAMES      = { "Uniform", "LightSkew",
                                                    "ModerateSkew", "IntenseSkew" };


    public static void generate(File dirf) throws IOException
    {
        assert dirf.exists() : "No such dir: " + dirf.getAbsolutePath();
        for (int i=0; i<SKEWS.length; i++)
            generate(new File(dirf, FNAMES[i]+".fasta"), 6, SKEWS[i]);
    }


    private static void generate(File file, int nSeqs, int skew) throws IOException
    {
        FileWriter fw = new FileWriter(file);
        PrintWriter pw = new PrintWriter(fw, true);     // true for autoflush

        for (int i=0; i<nSeqs; i++)
        {
            String name = "Random_" + i;
            pw.write(">" + name + "\r\n");
            int len = 50 - skew*i;
            assert len > 0;
            StringBuilder sb = new StringBuilder();
            for (int j=0; j<len; j++)
                sb.append(randomAA());
            pw.print(sb + "\r\n");
        }

        pw.close();
        fw.close();
    }


    private final static String AA_CHARS = "ACDEFGHIKLMNPQRSTVWY";


    private static char randomAA()
    {
        int index = (int)(AA_CHARS.length() * Math.random());
        return AA_CHARS.charAt(index);
    }


    public static void main(String[] args)
    {
        File dirf = MSGFrame.RANDOM_DIRF;
        try
        {
            generate(dirf);
            System.out.println("Done");
        }
        catch (IOException x)
        {
            System.out.println("IO Stress: " + x.getMessage());
            x.printStackTrace(System.out);
        }
    }
}
