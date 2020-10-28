package msg;

class RouletteWheel
{
    // Assumes generation size = 100. Index is rank (0 is best).
    private final static int[]      RELATIVE_ODDS =
        { 5, 5, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };

    static int[]                    THE_WHEEL;

    static
    {
        THE_WHEEL = new int[50];
        int n = 0;
        for (int winnerIndex=0; winnerIndex<RELATIVE_ODDS.length; winnerIndex++)
            for (int i=0; i<RELATIVE_ODDS[winnerIndex]; i++)
                THE_WHEEL[n++] = winnerIndex;
        assert n == THE_WHEEL.length;
    }


    static int[] spin(int matingPoolSize)
    {
        int[] winners = new int[matingPoolSize];
        for (int i=0; i<matingPoolSize; i++)
        {
            int randy = (int)(THE_WHEEL.length * Math.random());
            winners[i] = THE_WHEEL[randy];
        }
        return winners;
    }


    public static void main(String[] args)
    {
        int[] winners = spin(50);
        String s = "";
        for (int i: winners)
            s += i + ",";
        System.out.println(s);
    }
}
