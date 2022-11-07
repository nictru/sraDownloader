package org.exbio.sradownloader;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;
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

public class Download extends ExecutableStep {
    public final RequiredConfig<File> runTable;
    public final RequiredConfig<String> fasterqDump;
    public final OptionalConfig<List> excludeTreatments;

    public Download(InputConfig<File> runTable, InputConfig<String> fasterqDump, InputConfig<List> excludeTreatments,
                    OutputFile... dependencies) {
        super(dependencies);
        this.runTable = new RequiredConfig<>(runTable);
        this.excludeTreatments = new OptionalConfig<>(excludeTreatments, false);
        this.fasterqDump = new RequiredConfig<>(fasterqDump);
    }

    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new ArrayList<>() {{
            try {
                readLines(runTable.get()).stream().skip(1).map(line -> line.split(",")).filter(splittedLine -> {
                    if (!excludeTreatments.isSet()) {
                        return true;
                    }
                    return excludeTreatments.get().stream().noneMatch(
                            excludedTreatment -> excludedTreatment.equals(splittedLine[28]));
                }).map(splittedLine -> splittedLine[0]).forEach(srr -> {
                    String command = fasterqDump.get() + " -p --split-spot --concatenate-reads -O " +
                            Main.workingDirectory.getAbsolutePath() + " " + srr;

                    add(() -> {
                        executeAndWait(command);
                        return true;
                    });
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }};
    }
}
