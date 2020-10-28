package msg;

import java.util.*;


class PopulationHistory extends ArrayList<Integer>
{
    private int             maxSize;


    PopulationHistory(int maxSize)            { this.maxSize = maxSize; }
    PopulationHistory(PopulationHistory src)  { super(src);             }
    int getMaxSize()                          { return maxSize;         }


    public synchronized boolean add(Integer addMe)
    {
        // If full, remove all odd-indexed elements.
        if (size() == maxSize)
            for (int n=size()-1; n>0; n-=2)
                remove(n);

        return super.add(addMe);
    }


    public synchronized PopulationHistory xeroxThreadsafe()
    {
        return new PopulationHistory(this);
    }


    synchronized int getMaxScore()
    {
        int score = Integer.MIN_VALUE;
        for (int i: this)
            score = Math.max(i, score);
        return score;
    }
}
