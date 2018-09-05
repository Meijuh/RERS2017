package nl.utwente.fmt.rers;

import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.oracle.PropertyOracle;
import de.learnlib.api.query.DefaultQuery;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.exception.ModelCheckingException;
import net.automatalib.words.Word;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

/**
 * Wrapper around a MealyBlackBoxProperty for several features:
 *
 *  - write a CSV line when a property is falsified,
 *  - also try to falsify a property with a fixed number of loop unrolls,
 *  - also try to falsify a property without a LassoEmptinessOracle.
 */
@ParametersAreNonnullByDefault
public class RERSProperty implements PropertyOracle.MealyPropertyOracle<String, String, String> {

    public static final LearnLogger LOGGER = LearnLogger.getLogger(RERSProperty.class);

    private final MealyPropertyOracle<String, String, String> propertyOracle;

    private int fixedFalseNegatives = 0;

    private int relativeFalseNegatives = 0;

    private final int propertyNumber;

    private final int problem;

    private final String learner;

    private final String mcType;

    private final String bbo;

    public RERSProperty(int problem,
                        String learner,
                        MealyPropertyOracle p,
                        int propertyNumber,
                        String mcType,
                        String bbo) {
        this.problem = problem;
        this.learner = learner;
        this.propertyOracle = p;
        this.propertyNumber = propertyNumber;
        this.mcType = mcType;
        this.bbo = bbo;
    }

    @Override
    public boolean isDisproved() {
        return propertyOracle.isDisproved();
    }

    @Override
    public void setProperty(String s) {
        propertyOracle.setProperty(s);
    }

    @Override
    public String getProperty() {
        return propertyOracle.getProperty();
    }

    @Nullable
    @Override
    public DefaultQuery getCounterExample() {
        return propertyOracle.getCounterExample();
    }

    /**
     * Disproves this property. Also try to disprove this property by unrolling the lasso a fixed number of times,
     * and without an LassoEmptinessOracle.
     *
     * @param hypothesis the current hypothesis.
     * @param inputs the alphabet
     * @return the query that disproves this property.
     *
     * @throws ModelCheckingException
     */
    @Nullable
    @Override
    public DefaultQuery disprove(MealyMachine hypothesis, Collection inputs) throws ModelCheckingException {

        final DefaultQuery<String, Word<String>> result = propertyOracle.disprove(hypothesis, inputs);

        //{
        //    mealyModelCheckerLasso.setMinimumUnfolds(3);
        //    mealyModelCheckerLasso.setMultiplier(0.0);
        //    final MealyLasso testLasso =
        //            mealyModelCheckerLasso.findCounterExample(hypothesis, inputs, property.getProperty());
        //    final DefaultQuery<String, Word<String>> test;
        //    if (testLasso != null) test = eo.findCounterExample(testLasso, inputs);
        //    else test = null;

        //    if (test != null && result == null) {
        //        fixedFalseNegatives++;
        //        LOGGER.info(
        //                String.format("possibly false: #%d, %s (%d times, fixed)", propertyNumber, property.getProperty(), fixedFalseNegatives));
        //        LOGGER.logQuery("query: " + test);
        //    }
        //}

        //{
        //    mealyModelCheckerLasso.setMinimumUnfolds(3);
        //    mealyModelCheckerLasso.setMultiplier(1.0);
        //    final MealyLasso testLasso =
        //            mealyModelCheckerLasso.findCounterExample(hypothesis, inputs, property.getProperty());
        //    final DefaultQuery<String, Word<String>> test;
        //    if (testLasso != null) test = eo.findCounterExample(testLasso, inputs);
        //    else test = null;

        //    if (test != null && result == null) {
        //        relativeFalseNegatives++;
        //        LOGGER.info(
        //                String.format("possibly false: #%d, %s (%d times, relative)", propertyNumber, property.getProperty(), fixedFalseNegatives));
        //        LOGGER.logQuery("query: " + test);
        //    }
        //}

        // write the CSV line.
        if (result != null) {
            System.out.printf(
                    "%d,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
                    problem,
                    learner,
                    mcType,
                    bbo,
                    propertyNumber,
                    hypothesis.getStates().size(),
                    RERSExperiment.getRealSymbolCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getLearnSymbolCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getEqSymbolCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getEmSymbolCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getEmOSymbolCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getInSymbolCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getRealQueryCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getLearnQueryCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getEqQueryCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getEmQueryCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getEmOQueryCounterSUL().getStatisticalData().getCount(),
                    RERSExperiment.getInQueryCounterSUL().getStatisticalData().getCount(),
                    result.getInput().length());
        }

        return result;

    }

    @Nullable
    @Override
    public DefaultQuery findCounterExample(MealyMachine hypothesis, Collection inputs)
            throws ModelCheckingException {

        return propertyOracle.findCounterExample(hypothesis, inputs);
    }
}
