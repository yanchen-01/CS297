package msg;

import java.util.*;


class ConsensusWidthModel extends TreeMap<Integer, Integer>
{
    private ConsensusWidthPanel     view;
    private Integer                 lastKeyAdded;


    private ConsensusWidthModel(ConsensusWidthModel src)
    {
        super(src);
    }


    ConsensusWidthModel(ConsensusWidthPanel view)
    {
        this.view = view;
    }


    synchronized ConsensusWidthModel xerox()
    {
        return new ConsensusWidthModel(this);
    }


    // Repaints the view.
    public synchronized Integer put(Integer k, Integer v)
    {
        super.put(k, v);
        lastKeyAdded = k;
        view.repaint();
        return 0;
    }


    public String toString()
    {
        String s = "BinarySearchModel";
        for (int key: keySet())
            s += "\n  " + kvToString(key);
        return s;
    }


    private String kvToString(int k)
    {
        return "(" + k + ") = " + get(k);
    }


    synchronized int keyOfBestValue()
    {
        int bestVal = Integer.MIN_VALUE;
        int bestKey = Integer.MIN_VALUE;
        for (int key: keySet())
        {
            if (get(key) > bestVal)
            {
                bestKey = key;
                bestVal = get(key);
            }
        }
        return bestKey;
    }


    private int[] getBracketingXs(int x)
    {
        assert containsKey(x);
        assert x > getMinX()  &&  x < getMaxX();
        ArrayList<Integer> keys = new ArrayList<Integer>(keySet());
        int indexOfKey = keys.indexOf(x);
        int prevKey = keys.get(indexOfKey-1);
        int nextKey = keys.get(indexOfKey+1);
        return new int[] {prevKey, nextKey};
    }


    // Utility for binary searching. Inserts newly computed (x,y). Returns next
    // x to be computed, or min int if search has completed. Repaints the view.
    synchronized int insertAndGetNextXForBinarySearch(int x, int y)
    {
        put(x, y);
        int[] bracketingXs = getBracketingXs(x);

        // If yprev > ynext, next search is in [xprev .. x], otherwise next
        // search is in [x .. xnext]. In case of tie, favor xprev because smaller
        // x really means smaller consensus width, which is faster to compute.
        int[] nextRange = new int[2];
        if (get(bracketingXs[0]) > get(bracketingXs[1]))
        {
            nextRange[0] = bracketingXs[0];
            nextRange[1] = x;
        }
        else
        {
            nextRange[0] = x;
            nextRange[1] = bracketingXs[1];
        }

        assert nextRange[1] > nextRange[0];
        if (nextRange[1] - nextRange[0] == 1)
            return Integer.MIN_VALUE;
        else
            return (int)((nextRange[1] + nextRange[0]) / 2);
    }


    synchronized boolean isFinished()
    {
        int k = keyOfBestValue();
        return containsKey(k-1) && containsKey(k+1);
    }


    synchronized int getMaxX()
    {
        return (Integer)keySet().toArray(new Integer[0])[size()-1];
    }


    synchronized int bestValue()             { return get(keyOfBestValue());      }
    synchronized int getMinX()               { return keySet().iterator().next(); }
    synchronized Integer getLastKeyAdded()   { return lastKeyAdded;               }
    static void sop(Object x)                { System.out.println(x);             }
}
