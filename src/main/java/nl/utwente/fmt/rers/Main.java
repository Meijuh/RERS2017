package nl.utwente.fmt.rers;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import de.learnlib.api.logging.LearnLogger;
import net.automatalib.modelcheckers.ltsmin.LTSminUtil;
import nl.utwente.fmt.rers.RERSExperiment.LEARNER;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

    public static final LearnLogger LOGGER = LearnLogger.getLogger(Main.class);

    public static void main(String[] args) throws ParseException, FileNotFoundException {

        try {
            LOGGER.info("hostname: " + InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
        }

        LOGGER.info("commandline is: " + String.join(" ", args));

        final CommandLineParser parser = new DefaultParser();
        final CommandLine line = parser.parse(getOptions(), args);

        final String[] lineArgs = line.getArgs();

        final int exit;
        if (lineArgs.length == 2) {
            if (line.hasOption('h')) {
                printUsage();
                exit = 0;
            } else {
                final int problem = Integer.parseInt(lineArgs[0]);
                final double multiplier = Double.parseDouble(line.getOptionValue('m', "1.0"));
                LOGGER.info("multiplier is: " + multiplier);

                final int minimumUnfolds = Integer.parseInt(line.getOptionValue('u', "3"));
                LOGGER.info("minimum unfolds is : " + minimumUnfolds);

                final boolean disproveFirst = line.hasOption('D');

                final boolean cexFirst = line.hasOption('C');

                final boolean randomWords = !line.hasOption('r');

                final boolean alternate = !line.hasOption('a');

                final boolean monitor = line.hasOption('M');

                final boolean cache = line.hasOption('c');

                final boolean buchi = line.hasOption('B');

                if (cexFirst && disproveFirst) {
                    exit = 1;
                    System.out.printf("--cex-first and --disprove-first are mutually exclusive%n");
                    printUsage();
                } else if (!monitor && !buchi) {
                    exit = 1;
                    System.out.printf("You have to supply at least one of --buchi and --monitor%n");
                    printUsage();
                } else {
                    if (line.hasOption('s')) LTSminUtil.setCheckVersion(false);

                    int timeout = Integer.parseInt(line.getOptionValue('t', "-1"));

                    final RERSExperiment experiment = RERSExperiment.newExperiment(problem,
                                                                                   multiplier,
                                                                                   minimumUnfolds,
                                                                                   disproveFirst,
                                                                                   cexFirst,
                                                                                   LEARNER.valueOf(lineArgs[1]),
                                                                                   randomWords,
                                                                                   alternate,
                                                                                   monitor,
                                                                                   cache,
                                                                                   buchi,
                                                                                   timeout);
                    System.out.println(
                            "problem," +
                            "learner," +
                            "aut," +
                            "bbo," +
                            "property," +
                            "size," +
                            "realsymbols," +
                            "learnsymbols," +
                            "eqsymbols," +
                            "emsymbols," +
                            "emosymbols," +
                            "insymbols," +
                            "realqueries," +
                            "learnqueries," +
                            "eqqueries," +
                            "emqueries," +
                            "emoqueries," +
                            "inqueries," +
                            "length," +
                            "multiplier," +
                            "refinements");

                    experiment.run();
                    LOGGER.info("final states: " + experiment.getFinalHypothesis().getStates().size());

                    experiment.getPropertyOracles().stream().filter(p -> !p.isDisproved())
                              .forEach(p -> p.disprove(experiment.getFinalHypothesis(), experiment.getInputs()));

                    LOGGER.info("Final learning queries: " + RERSExperiment.getLearnQueryCounterSUL().getStatisticalData().getCount());

                    LOGGER.info("Properties disproved: " + experiment.getPropertyOracles().stream().filter(p -> p.isDisproved()).count());

                    exit = 0;
                }
            }
        } else {
            printUsage();
            exit = 1;
        }

        if (exit != 0) System.exit(exit);
    }

    static void printUsage() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + Main.class.getCanonicalName() + " [problem number] [learner]",
                            "You can apply both the option --monitor, and --buchi, then first monitors will be used " +
                            "disprove properties. You have to supply at least one of --monitor and --buchi. " +
                            "Also, you have to choose between --disprove-first, and --cex-first, but they are " +
                            "mutually exclusive.\n",
                            getOptions(), "\nCreated by Jeroen Meijer", true);
    }

    static Options getOptions() {
        final Options options = new Options();

        options.addOption("t", "timeout", true, "timeout in seconds");
        options.addOption("m", "multiplier", true, "multiplier for unrolls");
        options.addOption("u", "minimum-unfolds", true, "minimum number of unfolds");
        options.addOption("D", "disprove-first", false, "use disprove first black-box oracle");
        options.addOption("C", "cex-first", false, "use counter example first black-box oracle");
        options.addOption("r", "no-random-words", false, "do not use an additional random words equivalence oracle");
        options.addOption("a", "no-alternate", false, "do not use alternating edge semantics");
        options.addOption("M", "monitor", false, "create a Monitor");
        options.addOption("B", "buchi", false, "create a Büchi automaton");
        options.addOption("h", "help", false, "prints help");
        options.addOption("c", "cache", false, "use a model checker cache");
        options.addOption("s", "ltsmin-skip-version-check", false, "skip the LTSmin version check");

        return options;
    }
}
