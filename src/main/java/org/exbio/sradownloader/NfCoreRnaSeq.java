package org.exbio.sradownloader;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.InputFile;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.RequiredConfig;
import org.exbio.pipejar.pipeline.ExecutableStep;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;

import static org.exbio.pipejar.util.ScriptExecution.executeAndWait;

public class NfCoreRnaSeq extends ExecutableStep {
    public final OutputFile results;
    private final InputFile sampleSheet;
    private final RequiredConfig<String> nextflowExecutable = new RequiredConfig<>(Main.configs.nextflowExecutable);

    public NfCoreRnaSeq(Collection<OutputFile> fastqs, OutputFile sampleSheet) {
        super(fastqs, sampleSheet);
        this.sampleSheet = addInput(sampleSheet);
        results = addOutput("results");
    }

    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new HashSet<>() {{
            add(() -> {
                String command = nextflowExecutable.get() + " run nf-core/rnaseq -profile docker --input " +
                        sampleSheet.getAbsolutePath() + " --outdir " + results.getAbsolutePath();
                executeAndWait(command, true);
                return true;
            });
        }};
    }
}
