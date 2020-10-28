package msg;

import java.util.*;


class Population
{
    private final static float          DFLT_CROSSOVER_RATE     = .07f;
    private final static float          DFLT_MUTATION_RATE      = .20f;
    private final static int            ACTIVITY_CHECKIN_PERIOD = 20;
    private final static float          OPERATOR_SPEEDUP        = 1.1f;

    protected int                       nChromosomes;
    protected ArrayList<Chromosome>     chromosomes;
    protected UngappedSequenceDataset   ungappedDataset;
    protected int                       consensusWidth;
    protected int                       breedingPoolSize;
    protected PopulationHistory         history;
    private float                       mutationRate;
    private float                       crossoverRate;


    Population() { }


    // Randomizes. Default crossover & mutation rates.
    Population(int nChromosomes, int breedingPoolSize,
               UngappedSequenceDataset ungappedDataset,
               int consensusWidth, int historySize)
    {
        this.nChromosomes = nChromosomes;
        this.breedingPoolSize = breedingPoolSize;
        this.ungappedDataset = ungappedDataset;
        this.consensusWidth = consensusWidth;
        chromosomes = new ArrayList<Chromosome>(nChromosomes);

        history = new PopulationHistory(historySize);

        // Build, evaluate, and add randomized chromosomes.
        int nSeqs = ungappedDataset.size();
        int nGapsTotal = consensusWidth*nSeqs - ungappedDataset.nCharsOverall();
        for (int i=0; i<nChromosomes; i++)
        {
            Chromosome chr = new Chromosome(nGapsTotal, consensusWidth-1);
            chr.randomize();
            chr.evaluate(ungappedDataset, consensusWidth);
            chromosomes.add(chr);
        }

        // Record best score into history.
        history.add(getFittest().score);

        initOperatorRates();
    }


    // Merges top performers in tribes[] into a single diverse population.
    Population(Population[] tribes)
    {
        assert tribes != null  :  "Null tribes[] in Population ctor.";
        for (Population tribe: tribes)
            assert tribe != null : "Null tribe in Population ctor.";

        // Copy instance vars from one of the tribes.
        this.nChromosomes = tribes[0].nChromosomes;
        this.ungappedDataset = tribes[0].ungappedDataset;
        this.consensusWidth = tribes[0].consensusWidth;
        this.breedingPoolSize = tribes[0].breedingPoolSize;

        // Collect top members of each tribe. Cache single best member in each
        // tribe, in case we need filler.
        int nRepresentativesPerTribe = nChromosomes / tribes.length;
        chromosomes = new ArrayList<Chromosome>(nChromosomes);
        Vector<Chromosome> filler = new Vector<Chromosome>();
        for (Population tribe: tribes)
        {
            List<Chromosome> topN = tribe.topNChromosomes(nRepresentativesPerTribe);
            chromosomes.addAll(topN);
            filler.add(topN.get(0));
        }

        // Might need a few more chromosomes, due to rounding error in nRepresentativesPerTribe.
        int fillerIndex = 0;
        while (chromosomes.size() < nChromosomes)
        {
            chromosomes.add(filler.get(fillerIndex));
            fillerIndex = (fillerIndex + 1) % filler.size();
        }

        // Record best score into history.
        history = new PopulationHistory(tribes[0].getHistory().getMaxSize());
        history.add(getFittest().score);

        initOperatorRates();
    }


    protected void initOperatorRates()
    {
        mutationRate = DFLT_MUTATION_RATE;
        crossoverRate = DFLT_CROSSOVER_RATE;
    }


    protected Chromosome[] sortDescending()
    {
        TreeSet<Chromosome> ascending = new TreeSet<Chromosome>(chromosomes);
        Chromosome[] descending = new Chromosome[ascending.size()];
        int n = descending.length - 1;
        for (Chromosome chro: ascending)
            descending[n--] = chro;
        return descending;
    }


    // Records top score in history.
    void step1Generation()
    {
        // Collect chromosomes in descending fitness order (best is at [0]).
        Chromosome[] sortedDescending = sortDescending();
        assert sortedDescending.length == chromosomes.size() :
               "Unexpected length of sorted = " + sortedDescending.length +
               " != chromosomes.size() = " + chromosomes.size();

        // Clear out main collection (safe, because all members are in sortedDescending[]).
        chromosomes.clear();

        // Elitism: best 2 members bypass breeding pool and go directly into
        // next generation. They may also breed if the wheel choses them.
        chromosomes.add(sortedDescending[0]);
        chromosomes.add(sortedDescending[1]);

        // Spin roulette wheel to determine indices of mating pairs.
        int[] randomizedBreederIndices = RouletteWheel.spin(nChromosomes);

        // Breed.
        int n = 0;
        while (chromosomes.size() < nChromosomes)
        {
            // Get parents.
            int maIndex = randomizedBreederIndices[n++];
            Chromosome ma = sortedDescending[maIndex];
            int paIndex = randomizedBreederIndices[n++];
            Chromosome pa = sortedDescending[paIndex];
            // Crossover.
            Chromosome[] kids = Chromosome.crossover(ma, pa, crossoverRate);
            // Mutate.
            kids[0].mutate(mutationRate);
            kids[1].mutate(mutationRate);
            // Evaluate.
            kids[0].evaluate(ungappedDataset, consensusWidth);
            kids[1].evaluate(ungappedDataset, consensusWidth);
            // Collect.
            chromosomes.add(kids[0]);
            chromosomes.add(kids[1]);
        }

        // Record best score into history.
        int currentScore = getFittest().score;
        history.add(currentScore);

        // Adjust operator rates.
        int histoSize = history.size();
        if (histoSize >= ACTIVITY_CHECKIN_PERIOD  &&
            histoSize % ACTIVITY_CHECKIN_PERIOD  == 0)
        {
            int earlierScore = history.get(histoSize-ACTIVITY_CHECKIN_PERIOD);
            if (earlierScore == currentScore)
            {
                // Flatlined => increase operator likelihoods.
                mutationRate *= OPERATOR_SPEEDUP;
                crossoverRate *= OPERATOR_SPEEDUP;
            }
            else if (currentScore-earlierScore > OPERATOR_SPEEDUP)
            {
                // Don't sustain high operator rates for too long.
                mutationRate /= OPERATOR_SPEEDUP;
                crossoverRate /= OPERATOR_SPEEDUP;
            }

        }
    }


    List<Chromosome> topNChromosomes(int n)
    {
        assert chromosomes.size() >= n;
        Chromosome[] allSorted = sortDescending();
        assert allSorted.length >= n;
        ArrayList<Chromosome> ret = new ArrayList<Chromosome>(n);
        for (int i=0; i<n; i++)
            ret.add(allSorted[i]);
        return ret;
    }


    Chromosome getFittest()
    {
        return sortDescending()[0];
    }


    int getChromosomeLengthThrowIfNotUniform() throws IllegalStateException
    {
        int w = chromosomes.iterator().next().length();
        for (Chromosome chromo: chromosomes)
            if (chromo.length() != w)
                throw new IllegalStateException(chromo.length() + " != " + w);
        return w;
    }


    int getChromosomeLength()
    {
        return chromosomes.get(0).length();
    }


    PopulationHistory getHistory()        { return history;               }
    int getMaxScore()                     { return history.getMaxScore(); }
    static void sop(Object x)             { System.out.println(x);        }
}
