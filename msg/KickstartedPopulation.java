package msg;

import java.util.*;


class KickstartedPopulation extends Population
{
    private SequenceDataset         clustalSolution;


    KickstartedPopulation(int nChromosomes, int breedingPoolSize,
                          int nGapsPerChromosome,
                          UngappedSequenceDataset ungappedDataset,
                          SequenceDataset clustalSolution,
                          int consensusWidth, int historySize)
    {
        // Construct.
        this.nChromosomes = nChromosomes;
        this.breedingPoolSize = breedingPoolSize;
        this.ungappedDataset = ungappedDataset;
        this.consensusWidth = consensusWidth;
        this.history = new PopulationHistory(historySize);
        this.clustalSolution = clustalSolution;
        this.chromosomes = new ArrayList<Chromosome>(nChromosomes);

        // Build a chromosome to represent the ClustalW solution, adjusted
        // to the desired consensus width.
        assert clustalSolution.isUniformWidth();
        int deltaWidth = consensusWidth - clustalSolution.widthOfWidestSequence();
        SizeAdjustingChromosome starterChromo =
            new SizeAdjustingChromosome(clustalSolution, deltaWidth, nGapsPerChromosome);
        chromosomes.add(starterChromo);

        // Mutate the starter chromosome enough times to fill out the population.
        while (chromosomes.size() < nChromosomes)
        {
            Chromosome chromo = new Chromosome(starterChromo);
            chromo.mutate((float)Math.random());
            chromo.evaluate(ungappedDataset, consensusWidth);
            chromosomes.add(chromo);
        }

        initOperatorRates();
    }
}
