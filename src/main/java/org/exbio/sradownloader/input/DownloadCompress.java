package org.exbio.sradownloader.input;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.OptionalConfig;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.RequiredConfig;
import org.exbio.pipejar.pipeline.ExecutableStep;
import org.exbio.sradownloader.Configs;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static org.exbio.pipejar.util.FileManagement.readLines;
import static org.exbio.pipejar.util.ScriptExecution.executeAndWait;

public class DownloadCompress extends ExecutableStep {
    public final RequiredConfig<File> runTable = new RequiredConfig<>(Configs.inputConfigs.sraConfigs.runTable);
    public final RequiredConfig<String> fasterqDump = new RequiredConfig<>(Configs.inputConfigs.sraConfigs.fasterqDump);
    public final RequiredConfig<String> gzip = new RequiredConfig<>(Configs.inputConfigs.sraConfigs.gzipExecutable);
    public final OptionalConfig<List<String>> excludeTreatments =
            new OptionalConfig<>(Configs.inputConfigs.sraConfigs.excludeTreatments, false);
    private final Map<String, OutputFile> srr_outputDirectory = new HashMap<>();
    private final Map<String, Boolean> srr_paired = new HashMap<>();

    public DownloadCompress() {
        super();
        addInput(runTable);

        // Add outputs for required srr ids
        try {
            readLines(runTable.get()).stream().filter(line -> line.startsWith("SRR")).map(
                                             // Split, but not at commas which are inside of quotes
                                             line -> line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                                     // Check if treatment is not excluded
                                     .filter(splittedLine -> {
                                         // TODO: write a more general filter option
                                         //  maybe create a map from column index to allowed/excluded values
                                         if (!excludeTreatments.isSet()) {
                                             return true;
                                         }
                                         return excludeTreatments.get().stream().noneMatch(
                                                 excludedTreatment -> excludedTreatment.equals(splittedLine[28]));
                                     }).forEach(splitted -> {
                                         // TODO: Create an option for determining which columns should be included in name
                                         String srr = splitted[0];
                                         String stage = splitted[15].replaceAll("[ ._]", "-");
                                         String group = splitted[24].replaceAll("[ ._]", "-");
                                         String fileName = srr + "_" + stage + "_" + group;
                                         OutputFile outputDirectory = addOutput(fileName);
                                         srr_outputDirectory.put(srr, outputDirectory);
                                         srr_paired.put(srr, splitted[21].equalsIgnoreCase("paired"));
                                     });
        } catch (IOException e) {
            throw new RuntimeException("Could not read runTable.");
        }
    }

    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new ArrayList<>() {{
            srr_outputDirectory.forEach((srr, outputDirectory) -> {
                String download = fasterqDump.get() +
                        // Show progress
                        " -p " +
                        // Split depending on library layout
                        (srr_paired.get(srr) ? "--split-files" : "--split-spot") +
                        // Set the directory for temp files
                        " -t " + outputDirectory.getPath() +
                        // Set the output directory
                        " -O " + outputDirectory.getPath() + " " + srr;
                String compress = gzip.get() + " -r " + outputDirectory.getPath();
                add(() -> {
                    boolean successfull = false;
                    int failedAttempts = 0;

                    while (!successfull) {
                        try {
                            logger.debug(download);
                            executeAndWait(download, true);
                            logger.debug(compress);
                            executeAndWait(compress, true);
                            successfull = true;
                        } catch (IOException e) {
                            failedAttempts++;

                            if (failedAttempts >= 3) {
                                throw new RuntimeException("Download/Compress failed 3 times.");
                            } else {
                                logger.warn("Download/Compress failed. Attempt " + failedAttempts + " of 3.");
                                e.printStackTrace();
                            }
                        }
                    }
                    return true;
                });
            });
        }};
    }

    @Override
    protected boolean doCreateFiles() {
        return false;
    }
}
