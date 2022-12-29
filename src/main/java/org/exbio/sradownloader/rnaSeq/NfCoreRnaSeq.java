package org.exbio.sradownloader.rnaSeq;

import org.apache.commons.lang3.tuple.Pair;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.InputFile;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.OptionalConfig;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.RequiredConfig;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.UsageConfig;
import org.exbio.pipejar.pipeline.ExecutableStep;
import org.exbio.sradownloader.Configs;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.exbio.pipejar.util.ScriptExecution.executeAndWait;

public class NfCoreRnaSeq extends ExecutableStep {
    public final OutputFile results;
    public final OutputFile sampleSheet;
    public final OutputFile workDir;
    private final RequiredConfig<String> nextflowExecutable =
            new RequiredConfig<>(Configs.rnaSeqConfigs.nextflowExecutable);
    private final Map<String, Pair<InputFile, InputFile>> fastqFiles = new HashMap<>();

    private final RequiredConfig<File> genomeFasta = new RequiredConfig<>(Configs.rnaSeqConfigs.genomeFasta);
    private final RequiredConfig<String> genome = new RequiredConfig<>(Configs.rnaSeqConfigs.genome);
    private final RequiredConfig<File> gtf = new RequiredConfig<>(Configs.rnaSeqConfigs.gtf);
    private final OptionalConfig<File> starIndex = new OptionalConfig<>(Configs.rnaSeqConfigs.starIndex, false);
    private final OptionalConfig<File> bed = new OptionalConfig<>(Configs.rnaSeqConfigs.bed, false);
    private final RequiredConfig<String> profile = new RequiredConfig<>(Configs.rnaSeqConfigs.profile);
    private final OptionalConfig<File> cache =
            new OptionalConfig<>(Configs.rnaSeqConfigs.cache, profile.isSet() && profile.get().equals("cluster"));
    private final InputFile slurmConfig = new InputFile(inputDirectory, "slurm.config");

    public NfCoreRnaSeq(Map<String, Pair<OutputFile, OutputFile>> fastqFiles) {
        super(false, fastqFiles.values().stream().map(pair -> new HashSet<OutputFile>() {{
            add(pair.getLeft());
            add(pair.getRight());
        }}).flatMap(Collection::stream).toList());

        fastqFiles.forEach((sample, pair) -> {
            OutputFile sampleDirectory = new OutputFile(inputDirectory, sample);
            InputFile first = addInput(sampleDirectory, pair.getLeft());
            InputFile second = pair.getRight() == null ? null : addInput(sampleDirectory, pair.getRight());
            this.fastqFiles.put(sample, Pair.of(first, second));
        });

        OutputFile temp = addOutput("temp");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/slurm.config"))));
             BufferedWriter writer = new BufferedWriter(new FileWriter(slurmConfig))) {

            reader.lines().map(line -> line.replace("{cache}", cache.get().getAbsolutePath()).replace("{temp}",
                    temp.getAbsolutePath())).forEach(line -> {
                try {
                    writer.write(line);
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        sampleSheet = addOutput("sampleSheet.csv");
        results = addOutput("results");
        workDir = addOutput("work");
    }

    private static File traceBackSymlinks(File linkFile) {
        Path path = linkFile.toPath();

        while (Files.isSymbolicLink(path)) {
            try {
                path = Files.readSymbolicLink(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return path.toFile();
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
                            writer.write(String.format("%s,%s,,unstranded", entry.getKey(),
                                    traceBackSymlinks(entry.getValue().getLeft())));
                        } else {
                            writer.write(String.format("%s,%s,%s,unstranded", entry.getKey(),
                                    traceBackSymlinks(entry.getValue().getLeft()),
                                    traceBackSymlinks(entry.getValue().getRight())));
                        }
                        writer.newLine();
                    }
                }

                Map<String, UsageConfig<?>> parameters = new HashMap<>() {{
                    put("--fasta", genomeFasta);
                    put("--genome", genome);
                    put("--gtf", gtf);
                    put("--star_index", starIndex);
                    put("--gene_bed", bed);
                    put("-profile", profile);
                }};

                String command = nextflowExecutable.get() + " run nf-core/rnaseq -c " + slurmConfig.getAbsolutePath() +
                        " --input " + sampleSheet.getAbsolutePath() + " --outdir " + results.getAbsolutePath() +
                        " -work-dir " + workDir.getAbsolutePath() + " " +
                        parameters.entrySet().stream().filter(entry -> entry.getValue().isSet()).map(
                                entry -> entry.getKey() + " " + entry.getValue().get()).collect(
                                Collectors.joining(" "));
                logger.debug(command);
                executeAndWait(command, true);
                return true;
            });
        }};
    }
}
