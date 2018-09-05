package nl.utwente.fmt.rers;

import de.learnlib.algorithms.adt.learner.ADTLearnerBuilder;
import de.learnlib.algorithms.dhc.mealy.MealyDHC;
import de.learnlib.algorithms.discriminationtree.mealy.DTLearnerMealyBuilder;
import de.learnlib.algorithms.kv.mealy.KearnsVaziraniMealyBuilder;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.algorithms.malerpnueli.MalerPnueliMealyBuilder;
import de.learnlib.algorithms.rivestschapire.RivestSchapireMealyBuilder;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealyBuilder;
import de.learnlib.api.SUL;
import de.learnlib.api.algorithm.LearningAlgorithm.MealyLearner;
import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.logging.LoggingPropertyOracle;
import de.learnlib.api.oracle.BlackBoxOracle;
import de.learnlib.api.oracle.EmptinessOracle;
import de.learnlib.api.oracle.LassoEmptinessOracle;
import de.learnlib.api.oracle.OmegaMembershipOracle;
import de.learnlib.api.oracle.PropertyOracle;
import de.learnlib.api.oracle.EquivalenceOracle.MealyEquivalenceOracle;
import de.learnlib.api.oracle.InclusionOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.oracle.SymbolQueryOracle;
import de.learnlib.filter.cache.mealy.MealyCaches;
import de.learnlib.filter.cache.mealy.SymbolQueryCache;
import de.learnlib.filter.cache.sul.SULCaches;
import de.learnlib.filter.statistic.oracle.CounterOracle;
import de.learnlib.filter.statistic.oracle.CounterSymbolQueryOracle;
import de.learnlib.filter.statistic.sul.ResetCounterObservableSUL;
import de.learnlib.filter.statistic.sul.ResetCounterSUL;
import de.learnlib.filter.statistic.sul.SymbolCounterObservableSUL;
import de.learnlib.filter.statistic.sul.SymbolCounterSUL;
import de.learnlib.oracle.emptiness.MealyBFEmptinessOracle;
import de.learnlib.oracle.emptiness.MealyLassoEmptinessOracleImpl;
import de.learnlib.oracle.equivalence.CExFirstOracle;
import de.learnlib.oracle.equivalence.DisproveFirstOracle;
import de.learnlib.oracle.equivalence.EQOracleChain;
import de.learnlib.oracle.equivalence.MealyBFInclusionOracle;
import de.learnlib.oracle.equivalence.RandomWordsEQOracle;
import de.learnlib.oracle.equivalence.WpMethodEQOracle;
import de.learnlib.oracle.membership.AbstractSULOmegaOracle;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.oracle.membership.SULSymbolQueryOracle;
import de.learnlib.oracle.property.MealyFinitePropertyOracle;
import de.learnlib.oracle.property.MealyLassoPropertyOracle;
import de.learnlib.oracle.property.PropertyOracleChain;
import de.learnlib.util.Experiment;
import lombok.Getter;
import net.automatalib.modelcheckers.ltsmin.ltl.LTSminLTLAlternatingBuilder;
import net.automatalib.modelcheckers.ltsmin.ltl.LTSminLTLIOBuilder;
import net.automatalib.modelcheckers.ltsmin.monitor.LTSminMonitorAlternatingBuilder;
import net.automatalib.modelcheckers.ltsmin.monitor.LTSminMonitorIOBuilder;
import net.automatalib.modelchecking.ModelChecker;
import net.automatalib.modelchecking.ModelCheckerLasso;
import net.automatalib.modelchecking.modelchecker.cache.SizeMealyModelCheckerCache;
import net.automatalib.modelchecking.modelchecker.cache.SizeMealyModelCheckerLassoCache;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;
import nl.utwente.fmt.rers.problems.seq.Problem;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

import static nl.utwente.fmt.rers.ProblemSUL.DEADLOCK;

/**
 * A specialization of MealyBBCExperiment. That parses LTL formulae, and instantiates the proper classes
 * (such as ModelChecker).
 */
public class RERSExperiment extends Experiment.MealyExperiment<String, String> {

    enum LEARNER {
        ADT,
        DHC,
        DiscriminationTree,
        KearnsVazirani,
        ExtensibleLStar,
        MalerPnueli,
        RivestSchapire,
        TTT
    }

    public static String TYPE;

    public static final LearnLogger LOGGER = LearnLogger.getLogger(RERSExperiment.class);

    @Getter
    private final List<PropertyOracle.MealyPropertyOracle> propertyOracles;

    @Getter
    private static ResetCounterSUL realQueryCounterSUL, learnQueryCounterSUL, eqQueryCounterSUL, emQueryCounterSUL, inQueryCounterSUL;

    @Getter
    private static SymbolCounterSUL realSymbolCounterSUL, learnSymbolCounterSUL, eqSymbolCounterSUL, emSymbolCounterSUL, inSymbolCounterSUL;

    @Getter
    private static ResetCounterObservableSUL emOQueryCounterSUL;

    @Getter
    private static SymbolCounterObservableSUL emOSymbolCounterSUL;

//    @Getter
//    private static CounterOracle realCounter, learnCounter, eqCounter, emCounter, inCounter;

    @Getter
    private static ResetCounterObservableSUL emoCounter;

    private RERSExperiment(MealyLearner learningAlgorithm,
                           MealyEquivalenceOracle equivalenceAlgorithm,
                           Alphabet inputs,
                           List<PropertyOracle.MealyPropertyOracle> propertyOracles) {
        super(learningAlgorithm, equivalenceAlgorithm, inputs);
        this.propertyOracles = propertyOracles;
    }

    /**
     * Returns a new RERSExperiment.
     *
     * @param number the {@link Problem} number to instantiate.
     * @param multiplier the multiplier used when computing the number of unrolls for a lasso.
     * @param minimumUnfolds the minimum number of times a lasso needs to be unrolled.
     * @param disproveFirst whether to use a {@link DisproveFirstOracle}.
     * @param cexFirst whether to use a {@link CExFirstOracle}.
     * @param learner the learner to instantiate.
     * @param randomWords whether to use an additional {@link RandomWordsEQOracle}.
     * @param alternate whether to use alternating semantics
     * @param monitor whether to build a monitor
     * @param cache whether to use a model checker cache.
     * @param buchi whether to build a Buchi automaton.
     * @param timeout timeout in seconds.
     *
     * @return the RERSExperiment
     *
     * @throws FileNotFoundException when the appropriate Java class can not be found.
     */
    public static RERSExperiment newExperiment(int number,
                                               double multiplier,
                                               int minimumUnfolds,
                                               boolean disproveFirst,
                                               boolean cexFirst,
                                               LEARNER learner,
                                               boolean randomWords,
                                               boolean alternate,
                                               boolean monitor,
                                               boolean cache,
                                               boolean buchi,
                                               int timeout) throws FileNotFoundException {
        assert buchi || monitor;

        assert !(disproveFirst && cexFirst);

        String mcType = monitor ? "monitor" : "";
        mcType += buchi && monitor ? "-" : "";
        mcType += buchi ? "buchi" : "";

        final String bbcType;
        if (cexFirst) bbcType = "cex-first";
        else if (disproveFirst) bbcType = "disprove-first";
        else bbcType = "none";

        final ProblemSUL problemSUL = new ProblemSUL(number);
        final Alphabet alphabet = Alphabets.fromArray(problemSUL.getInputs());

        final SymbolQueryOracle learnOracle;
        final MembershipOracle.MealyMembershipOracle eqOracle, emOracle, inOracle;
        final OmegaMembershipOracle.MealyOmegaMembershipOracle emOOracle;

        final SUL realSUL =
                //SULCaches.createDAGCache(alphabet,
                                                      realSymbolCounterSUL = new SymbolCounterSUL("real symbols", realQueryCounterSUL = new ResetCounterSUL("real queries", problemSUL))
                //)
                ;
        learnOracle = new SULSymbolQueryOracle(learnSymbolCounterSUL = new SymbolCounterSUL("learner", learnQueryCounterSUL = new ResetCounterSUL("learner", realSUL)));
        eqOracle = new SULOracle(eqSymbolCounterSUL = new SymbolCounterSUL("equivalence", eqQueryCounterSUL = new ResetCounterSUL("equivalence", realSUL)));
        emOracle = new SULOracle(emSymbolCounterSUL = new SymbolCounterSUL("emptiness", emQueryCounterSUL = new ResetCounterSUL("emptiness", realSUL)));
        inOracle = new SULOracle(inSymbolCounterSUL = new SymbolCounterSUL("inclusion", inQueryCounterSUL = new ResetCounterSUL("inclusion", realSUL)));

        emOOracle = AbstractSULOmegaOracle.newOracle(emOSymbolCounterSUL = new SymbolCounterObservableSUL("omega emptiness", emOQueryCounterSUL = new ResetCounterObservableSUL("omega emptiness", problemSUL)));

        //final SymbolQueryOracle realOracle = MealyCaches.createCache(alphabet, realCounter = new CounterSymbolQueryOracle(problemSUL, "real"));
        //learnCounter = new CounterOracle(realOracle, "learn");
        //eqCounter = new CounterOracle(realOracle, "equivalence");
        //emCounter = new CounterOracle(realOracle, "emptiness");
        //inCounter = new CounterOracle(realOracle, "inclusion");

        final MealyLearner mealyLearner;

        switch (learner) {
            case ADT:
                mealyLearner = new ADTLearnerBuilder().withAlphabet(alphabet).withOracle(learnOracle).create();
                break;
            case DHC:
                mealyLearner = new MealyDHC(alphabet, learnOracle);
                break;
            case DiscriminationTree:
                mealyLearner = new DTLearnerMealyBuilder().withAlphabet(alphabet).withOracle(learnOracle).create();
                break;
            case KearnsVazirani:
                mealyLearner = new KearnsVaziraniMealyBuilder().withAlphabet(alphabet).withOracle(learnOracle).create();
                break;
            case ExtensibleLStar:
                mealyLearner = new ExtensibleLStarMealyBuilder().withAlphabet(alphabet).withOracle(learnOracle).create();
                break;
            case MalerPnueli:
                mealyLearner = new MalerPnueliMealyBuilder().withAlphabet(alphabet).withOracle(learnOracle).create();
                break;
            case RivestSchapire:
                mealyLearner = new RivestSchapireMealyBuilder().withAlphabet(alphabet).withOracle(learnOracle).create();
                break;
            case TTT:
                mealyLearner = new TTTLearnerMealyBuilder().withAlphabet(alphabet).withOracle(learnOracle).create();
                break;
            default:
                mealyLearner = null;
                break;
        }

        assert mealyLearner != null;

        final Function<String, String> edgeParser = s -> s;
        final List<String> formulae = parseLTL(number, alternate);

        final List<PropertyOracle.MealyPropertyOracle> monitorOracles = new ArrayList<>();
        if (monitor) {
            ModelChecker.MealyModelChecker modelChecker;

            if (alternate) {
                modelChecker =
                        new LTSminMonitorAlternatingBuilder().withString2Input(edgeParser).withString2Output(edgeParser)
                                                             .withSkipOutputs(Collections.singleton(DEADLOCK))
                                                             //.withKeepFiles(true)
                                                             .create();
            } else {
                modelChecker = new LTSminMonitorIOBuilder().withString2Input(edgeParser).withString2Output(edgeParser)
                                                           .withSkipOutputs(Collections.singleton(DEADLOCK))
                                                           //.withKeepFiles(true)
                                                           .create();
            }

            if (cache) modelChecker = new SizeMealyModelCheckerCache(modelChecker);

            final EmptinessOracle.MealyEmptinessOracle emptinessOracle =
                    new MealyBFEmptinessOracle(emOracle, 1.0);

            final InclusionOracle.MealyInclusionOracle inclusionOracle =
                    new MealyBFInclusionOracle(inOracle, 1.0);

            for (int i = 0; i < formulae.size(); i++) {
                final String formula = formulae.get(i);
                final PropertyOracle.MealyPropertyOracle p = new RERSProperty(number,
                                                                              learner.toString(),
                                                                              new LoggingPropertyOracle.MealyLoggingPropertyOracle(
                                                                                      new MealyFinitePropertyOracle(
                                                                                              formula,
                                                                                              inclusionOracle,
                                                                                              emptinessOracle,
                                                                                              modelChecker)),
                                                                              i,
                                                                              mcType,
                                                                              bbcType);
                monitorOracles.add(p);
            }
        }

        final List<PropertyOracle.MealyPropertyOracle> buchiOracles = new ArrayList<>();
        if (buchi) {
            ModelCheckerLasso.MealyModelCheckerLasso modelChecker;

            if (alternate) {
                modelChecker =
                        new LTSminLTLAlternatingBuilder().withString2Input(edgeParser).withString2Output(edgeParser).withSkipOutputs(Collections.singleton(DEADLOCK))
                                                         .withMinimumUnfolds(minimumUnfolds).withMultiplier(multiplier)
                                                         //.withKeepFiles(true)
                                                         .create();
            } else {
                modelChecker = new LTSminLTLIOBuilder().withString2Input(edgeParser).withString2Output(edgeParser)
                                                       .withSkipOutputs(Collections.singleton(DEADLOCK))
                                                       .withMinimumUnfolds(minimumUnfolds).withMultiplier(multiplier)
                                                       //.withKeepFiles(true)
                                                       .create();
            }

            if (cache) modelChecker = new SizeMealyModelCheckerLassoCache(modelChecker);

            final LassoEmptinessOracle.MealyLassoEmptinessOracle lassoEmptinessOracle =
                    new MealyLassoEmptinessOracleImpl(emOOracle);

            final InclusionOracle.MealyInclusionOracle inclusionOracle =
                    new MealyBFInclusionOracle(inOracle, 1.0);

            for (int i = 0; i < formulae.size(); i++) {
                final String formula = formulae.get(i);
                final PropertyOracle.MealyPropertyOracle p = new RERSProperty(number,
                                                                              learner.toString(),
                                                                              new LoggingPropertyOracle.MealyLoggingPropertyOracle(
                                                                                      new MealyLassoPropertyOracle(
                                                                                              formula,
                                                                                              inclusionOracle,
                                                                                              lassoEmptinessOracle,
                                                                                              modelChecker)),
                                                                              i,
                                                                              mcType,
                                                                              bbcType);
                buchiOracles.add(p);
            }
        }

        final List<PropertyOracle.MealyPropertyOracle> propertyOracles;
        if (monitor && buchi) {
            assert monitorOracles.size() == buchiOracles.size();
            propertyOracles = new ArrayList<>();
            for (int i = 0; i < monitorOracles.size(); i++) {
                final PropertyOracleChain.MealyPropertyOracleChain chain
                        = new PropertyOracleChain.MealyPropertyOracleChain(monitorOracles.get(i),
                                                                           buchiOracles.get(i));
                propertyOracles.add(chain);
            }
        } else if (monitor) propertyOracles = monitorOracles;
        else if (buchi) propertyOracles = buchiOracles;
        else propertyOracles = null;

        assert propertyOracles != null;

        EQOracleChain.MealyEQOracleChain equivalenceOracle = new EQOracleChain.MealyEQOracleChain();

        final BlackBoxOracle.MealyBlackBoxOracle blackBoxOracle;
        if (disproveFirst) blackBoxOracle = new DisproveFirstOracle.MealyDisproveFirstOracle(propertyOracles);
        else if (cexFirst) blackBoxOracle = new CExFirstOracle.MealyCExFirstOracle(propertyOracles);
        else blackBoxOracle = null;

        if (blackBoxOracle != null) equivalenceOracle.addOracle(blackBoxOracle);

        equivalenceOracle.addOracle(new WpMethodEQOracle.MealyWpMethodEQOracle(eqOracle, 3));
        if (randomWords) {
            equivalenceOracle.addOracle(new EQOracleChain.MealyEQOracleChain(
                    equivalenceOracle,
                    new RandomWordsEQOracle.MealyRandomWordsEQOracle(
                            eqOracle,
                            number * 5,
                            number * 50, 1000 * 1000 * 100,
                            new Random(123456l))));
        }

        return new RERSExperiment(mealyLearner, new TimeOutEQOracle(equivalenceOracle, timeout), alphabet, propertyOracles);
    }

    /**
     * Constructs a List of LTL formulae in LTSmin format, for a given {@link Problem} number.
     *
     * @param number the Problem number.
     *
     * @return the List of LTL formulae.
     *
     * @throws FileNotFoundException when the appropriate file containing LTL formulae can not be found.
     */
    static List<String> parseLTL(int number, boolean alternate) throws FileNotFoundException {

        final List<String> result = new ArrayList();
        final InputStream is = RERSExperiment.class.getClass().getResourceAsStream(
                String.format("/constraints-Problem%d.txt", number));

        final Scanner fileScanner = new Scanner(is);

        while(fileScanner.hasNextLine()) {

            String line = fileScanner.nextLine();
            line = line.replace("(", "( ");
            line = line.replace(")", " )");

            if (!line.isEmpty()) {

                if (line.charAt(0) != '#') {
                    final Scanner lineScanner = new Scanner(line);
                    lineScanner.useDelimiter(" ");

                    final StringBuilder sb = new StringBuilder();
                    while (lineScanner.hasNext()) {
                        final String token = lineScanner.next();

                        // for spot
//                        if (token.equals("true")) sb.append("true");
//                        else if (token.equals("false")) sb.append("false");
//                        else if (token.equals("(")) sb.append("(");
//                        else if (token.equals(")")) sb.append(")");
//                        else if (token.equals("!")) sb.append("!");
//                        else if (token.equals("R")) sb.append(" R ");
//                        else if (token.equals("U")) sb.append(" U ");
//                        else if (token.equals("X")) sb.append("X ");
//                        else if (token.equals("WU")) sb.append(" W ");
//                        else if (token.equals("&")) sb.append(" && ");
//                        else if (token.equals("|")) sb.append(" || ");
//                        else if (token.matches("i[A-Z]")) {
//                                sb.append("(\"input=='"); sb.append(token.charAt(1)); sb.append("'\")"); }
//                        else if (token.matches("o[A-Z]")) {
//                            sb.append("(\"input=='"); sb.append(token.charAt(1)); sb.append("'\")"); }
//                        else throw new RuntimeException("I do not know what to do with token: " + token);

                        final String input, output;
                        if (alternate) {
                            input = "letter";
                            output = "letter";
                        } else {
                            input = "input";
                            output = "output";
                        }

                        // for LTSmin
                        if (token.equals("true")) sb.append("true");
                        else if (token.equals("false")) sb.append("false");
                        else if (token.equals("(")) sb.append("(");
                        else if (token.equals(")")) sb.append(")");
                        else if (token.equals("!")) sb.append("!");
                        else if (token.equals("R")) sb.append(" R ");
                        else if (token.equals("U")) sb.append(" U ");
                        else if (token.equals("X")) sb.append("X ");
                        else if (token.equals("WU")) sb.append(" W ");
                        else if (token.equals("&")) sb.append(" && ");
                        else if (token.equals("|")) sb.append(" || ");
                        else if (token.matches("i[A-Z]")) { sb.append("("); sb.append(input); sb.append(" == \""); sb
                                .append(token.charAt(1)); sb.append("\")"); }
                        else if (token.matches("o[A-Z]")) { sb.append("("); sb.append(output); sb.append(" == \""); sb
                                .append(token.charAt(1)); sb.append("\")"); }
                        else throw new RuntimeException("I do not know what to do with token: " + token);
                    }

                    final String formula = sb.toString();
                    result.add(formula);

                    LOGGER.info(String.format("Parsed formula #%d: %s", result.size() - 1, formula));
                }
            }
        }

        fileScanner.close();

        return result;
    }
}

