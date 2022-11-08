package org.exbio.sradownloader;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.OptionalConfig;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.RequiredConfig;
import org.exbio.pipejar.pipeline.ExecutableStep;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import static org.exbio.pipejar.util.FileManagement.readLines;
import static org.exbio.pipejar.util.ScriptExecution.executeAndWait;

public class SraDownload extends ExecutableStep {
    public final RequiredConfig<File> runTable = new RequiredConfig<>(Main.configs.runTable);
    public final RequiredConfig<String> fasterqDump = new RequiredConfig<>(Main.configs.fasterqDump);
    public final OptionalConfig<List> excludeTreatments = new OptionalConfig<>(Main.configs.excludeTreatments, false);

    public SraDownload(OutputFile... dependencies) {
        super(dependencies);
        addInput(runTable);

        // Add outputs for required srr ids
        try {
            readLines(runTable.get()).stream().skip(1).map(line -> line.split(",")).filter(splittedLine -> {
                if (!excludeTreatments.isSet()) {
                    return true;
                }
                return excludeTreatments.get().stream().noneMatch(
                        excludedTreatment -> excludedTreatment.equals(splittedLine[28]));
            }).map(splittedLine -> splittedLine[0]).forEach(srr -> {
                addOutput(srr + ".fastq");
            });
        } catch (IOException e) {
            throw new RuntimeException("Could not read runTable.");
        }
    }

    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new ArrayList<>() {{
            getOutputs().forEach(fastqFile -> {
                String srr = fastqFile.getName().replace(".fastq", "");
                String command =
                        fasterqDump.get() + " -p --split-spot --concatenate-reads -O " + fastqFile.getParent() + " " +
                                srr;
                add(() -> {
                    executeAndWait(command, true);
                    fastqFile.setState(OutputFile.states.Created);
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
