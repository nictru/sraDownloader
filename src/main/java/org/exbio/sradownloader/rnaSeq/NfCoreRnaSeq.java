package org.exbio.sradownloader.rnaSeq;

import org.apache.commons.lang3.tuple.Pair;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.InputFile;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.RequiredConfig;
import org.exbio.pipejar.pipeline.ExecutableStep;
import org.exbio.sradownloader.Configs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.exbio.pipejar.util.ScriptExecution.executeAndWait;

public class NfCoreRnaSeq extends ExecutableStep {
    public final OutputFile results;
    public final OutputFile sampleSheet;
    private final RequiredConfig<String> nextflowExecutable =
            new RequiredConfig<>(Configs.rnaSeqConfigs.nextflowExecutable);
    private final Map<String, Pair<InputFile, InputFile>> fastqFiles = new HashMap<>();

    public NfCoreRnaSeq(Map<String, Pair<OutputFile, OutputFile>> fastqFiles) {
        super(false, fastqFiles.values().stream().map(pair -> new HashSet<OutputFile>() {{
            add(pair.getLeft());
            add(pair.getRight());
        }}).flatMap(Collection::stream).toList());

        fastqFiles.forEach((sample, pair) -> {
            OutputFile sampleDirectory = new OutputFile(outputDirectory, sample);
            InputFile first = addInput(sampleDirectory, pair.getLeft());
            InputFile second = pair.getRight() == null ? null : addInput(sampleDirectory, pair.getRight());
            this.fastqFiles.put(sample, Pair.of(first, second));
        });

        sampleSheet = addOutput("sampleSheet.csv");
        results = addOutput("results");
    }

    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new HashSet<>() {{
            add(() -> {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(sampleSheet))) {
                    writer.write("sample,fastq_1,fastq_2,strandedness");
                    writer.newLine();

                    for (Map.Entry<String, Pair<InputFile, InputFile>> entry : fastqFiles.entrySet()) {
                        if (entry.getValue().getRight() == null) {
                            writer.write(
                                    String.format("%s,%s,,unstranded", entry.getKey(), entry.getValue().getLeft()));
                        } else {
                            writer.write(
                                    String.format("%s,%s,%s,unstranded", entry.getKey(), entry.getValue().getLeft(),
                                            entry.getValue().getRight()));
                        }
                        writer.newLine();
                    }
                }

                String command = nextflowExecutable.get() + " run nf-core/rnaseq -profile docker --input " +
                        sampleSheet.getAbsolutePath() + " --outdir " + results.getAbsolutePath();
                executeAndWait(command, true);
                return true;
            });
        }};
    }
}
