package org.exbio.sradownloader.input;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.OptionalConfig;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.RequiredConfig;
import org.exbio.pipejar.pipeline.ExecutableStep;
import org.exbio.sradownloader.Main;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static org.exbio.pipejar.util.FileManagement.readLines;
import static org.exbio.pipejar.util.ScriptExecution.executeAndWait;

public class SraDownload extends ExecutableStep {
    public final RequiredConfig<File> runTable = new RequiredConfig<>(Main.configs.runTable);
    public final RequiredConfig<String> fasterqDump = new RequiredConfig<>(Main.configs.fasterqDump);
    public final OptionalConfig<List> excludeTreatments = new OptionalConfig<>(Main.configs.excludeTreatments, false);
    private final Map<String, OutputFile> srr_outputFile = new HashMap<>();

    public SraDownload(OutputFile... dependencies) {
        super(dependencies);
        addInput(runTable);

        // Add outputs for required srr ids
        try {
            readLines(runTable.get()).stream().filter(line -> line.startsWith("SRR")).map(
                    line -> line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")).filter(splittedLine -> {
                // TODO: write a more general filter option
                //  maybe create a map from column index to allowed values
                if (!excludeTreatments.isSet()) {
                    return true;
                }
                return excludeTreatments.get().stream().noneMatch(
                        excludedTreatment -> excludedTreatment.equals(splittedLine[28]));
            }).forEach(splitted -> {
                // TODO: Create an option for determining which columns should be included in name
                String srr = splitted[0];
                String stage = splitted[15].replace(" ", "-").replace("_", "-");
                String group = splitted[24].replace(".", "-").replace(" ", "-").replace("_", "-");
                String fileName = srr + "_" + stage + "_" + group + ".fastq";
                OutputFile outputFile = addOutput(fileName);
                srr_outputFile.put(srr, outputFile);
            });
        } catch (IOException e) {
            throw new RuntimeException("Could not read runTable.");
        }
    }

    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new ArrayList<>() {{
            srr_outputFile.entrySet().forEach(entry -> {
                String srr = entry.getKey();
                OutputFile outputFile = entry.getValue();
                // TODO: Allow either --split-spot or --split-files as parameter. For paired-end reads,
                //  --split-files is the right option. This is, because for paired-end SRAs, there are always exactly
                //  two reads per spot.
                String command = fasterqDump.get() + " -p --split-spot -t " + outputFile.getParent() + " -o " +
                        outputFile.getPath() + " " + srr;
                add(() -> {
                    executeAndWait(command, true);
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
