package msg;

import java.util.*;


// Extends a map from sequence name to sequence.
class SequenceDataset extends TreeMap<String, String>
{
    private String      name;


    SequenceDataset(String name)        { this.name = name; }


    UngappedSequenceDataset removeGaps()
    {
        UngappedSequenceDataset ret = new UngappedSequenceDataset(name);
        for (String k: keySet())
        {
            String gapped = get(k);
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<gapped.length(); i++)
                if (gapped.charAt(i) != '-')
                    sb.append(gapped.charAt(i));
            ret.put(k, sb.toString());
        }
        return ret;
    }


    public String toString()
    {
        String s = "Sequence Dataset " + name;
        int longestKeyLen = -1;
        for (String k: keySet())
            longestKeyLen= Math.max(longestKeyLen, k.length());
        for (String k: keySet())
        {
            int nSpaces = longestKeyLen - k.length();
            s += "\n" + k + ": ";
            for (int i=0; i<nSpaces; i++)
                s += " ";
            s += get(k);
        }
        return s;
    }


    // With a proper global alignment all sequences are the same width, but
    // you never can tell.
    int widthOfWidestSequence()
    {
        int w = -1;
        for (String seq: values())
            w = Math.max(w, seq.length());
        return w;
    }


    int countGaps()
    {
        int n = 0;
        for (String s: values())
        {
            for (int i=0; i<s.length(); i++)
            {
                if (s.charAt(i) == '-')
                    n++;
            }
        }
        return n;
    }


    boolean isUniformWidth()
    {
        String s = values().iterator().next();
        int w = s.length();
        for (String s1: values())
            if (s1.length() != w)
                return false;
        return true;
    }


    String getName()        { return name; }
}
