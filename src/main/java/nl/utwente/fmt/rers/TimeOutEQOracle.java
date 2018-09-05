package nl.utwente.fmt.rers;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

import javax.annotation.Nullable;

import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.query.DefaultQuery;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Word;

public class TimeOutEQOracle implements EquivalenceOracle.MealyEquivalenceOracle<String, String> {

    public static final LearnLogger LOGGER = LearnLogger.getLogger(TimeOutEQOracle.class);

    private final int seconds;

    private Instant start;

    private final MealyEquivalenceOracle<String, String> eqOracle;

    public TimeOutEQOracle(MealyEquivalenceOracle<String, String> eqOracle, int seconds) {
        this.eqOracle = eqOracle;
        this.seconds = seconds;
    }

    @Nullable
    @Override
    public DefaultQuery<String, Word<String>> findCounterExample(MealyMachine<?, String, ?, String> hypothesis,
                                                                 Collection<? extends String> inputs) {
        if (start == null) start = Instant.now();

        long queries = RERSExperiment.getEqQueryCounterSUL().getStatisticalData().getCount();
        long symbols = RERSExperiment.getEqSymbolCounterSUL().getStatisticalData().getCount();

        long realQueries = RERSExperiment.getRealQueryCounterSUL().getStatisticalData().getCount();
        long realSymbols = RERSExperiment.getRealSymbolCounterSUL().getStatisticalData().getCount();

        DefaultQuery<String, Word<String>> ce = eqOracle.findCounterExample(hypothesis, inputs);

        final Instant end = Instant.now();
        final Duration timeElapsed = Duration.between(start, end);

        if (timeElapsed.getSeconds() > seconds && seconds != -1) {
            LOGGER.info("Timeout reached.");
            if (ce != null) LOGGER.info("Not using counter example!");
            ce = null;
        }

        if (ce == null) {
            queries = RERSExperiment.getEqQueryCounterSUL().getStatisticalData().getCount() - queries;
            LOGGER.info("Useless equivalence queries: " + queries);
            symbols = RERSExperiment.getEqSymbolCounterSUL().getStatisticalData().getCount() - symbols;
            LOGGER.info("Useless equivalence symbols: " + symbols);
            RERSExperiment.getEqQueryCounterSUL().getStatisticalData().increment(-queries);
            RERSExperiment.getEqSymbolCounterSUL().getStatisticalData().increment(-symbols);

            realQueries = RERSExperiment.getRealQueryCounterSUL().getStatisticalData().getCount() - realQueries;
            LOGGER.info("Real useless queries: " + realQueries);
            realSymbols = RERSExperiment.getRealSymbolCounterSUL().getStatisticalData().getCount() - realSymbols;
            LOGGER.info("Real useless symbols: " + realSymbols);
            RERSExperiment.getRealQueryCounterSUL().getStatisticalData().increment(-realQueries);
            RERSExperiment.getRealSymbolCounterSUL().getStatisticalData().increment(-realSymbols);
        }

        return ce;
    }
}
