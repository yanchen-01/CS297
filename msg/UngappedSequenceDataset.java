package msg;

import java.util.*;


class UngappedSequenceDataset extends SequenceDataset
{
    UngappedSequenceDataset(String name)        { super(name); }


    public String put(String k, String v)
    {
        assert v.indexOf('-') < 0;
        return super.put(k, v);
    }


    int nCharsOverall()
    {
        int n = 0;
        for (String s: values())
            n += s.length();
        return n;
    }


    int[] getSequenceLengths()
    {
        int[] ret = new int[size()];
        int n = 0;
        for (String seq: values())
            ret[n++] = seq.length();
        return ret;
    }
}
