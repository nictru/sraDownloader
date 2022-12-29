package org.exbio.sradownloader.input;

import org.apache.commons.lang3.tuple.Pair;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.InputFile;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.RequiredConfig;
import org.exbio.pipejar.pipeline.ExecutableStep;
import org.exbio.sradownloader.Configs;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.exbio.pipejar.util.FileManagement.softLink;
import static org.exbio.pipejar.util.ScriptExecution.executeAndWait;

public class ImportDirectory extends ExecutableStep {
    public final Map<String, Pair<OutputFile, OutputFile>> outputFiles = new HashMap<>();
    private final RequiredConfig<File> inputDir = new RequiredConfig<>(Configs.inputConfigs.directory);
    private final RequiredConfig<String> gzip = new RequiredConfig<>(Configs.inputConfigs.sraConfigs.gzipExecutable);
    private final InputFile input = addInput(inputDir);
    private final Map<File, OutputFile> bridge = new HashMap<>();


    public ImportDirectory() {
        super();

        Collection<File> fastqFiles = getFastqFiles(input);

        fastqFiles.stream().map(file -> {
            String name = file.getName();
            return Pair.of(name.substring(0, name.indexOf(".")), file);
        }).collect(Collectors.groupingBy(pair -> pair.getLeft().substring(0, pair.getLeft().indexOf("_")))).forEach(
                (sample, pairs) -> {
                    if (pairs.size() > 2) {
                        throw new RuntimeException("More than two fastq files for sample " + sample);
                    }

                    OutputFile outDir = new OutputFile(outputDirectory, sample);

                    if (pairs.size() == 1) {
                        OutputFile out = addOutput(outDir, sample + ".fastq.gz");
                        File first = pairs.get(0).getRight();
                        bridge.put(first, out);
                        outputFiles.put(sample, Pair.of(out, null));
                    } else {
                        pairs.sort(Comparator.comparing(Pair::getLeft));

                        OutputFile out1 = addOutput(outDir, sample + "_1.fastq.gz");
                        OutputFile out2 = addOutput(outDir, sample + "_2.fastq.gz");

                        File first = pairs.get(0).getRight();
                        File second = pairs.get(1).getRight();

                        bridge.put(first, out1);
                        bridge.put(second, out2);

                        outputFiles.put(sample, Pair.of(out1, out2));
                    }
                });
    }

    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new HashSet<>() {{
            bridge.forEach((inputFile, outputFile) -> add(() -> {
                if (inputFile.getName().endsWith(".gz")) {
                    softLink(outputFile, inputFile);
                } else {
                    String command =
                            gzip.get() + " -c " + inputFile.getAbsolutePath() + " > " + outputFile.getAbsolutePath();

                    executeAndWait(command, true);
                }
                return true;
            }));
        }};
    }

    private Collection<File> getFastqFiles(File directory) {
        return Arrays.stream(Objects.requireNonNull(directory.listFiles())).map(File::getAbsoluteFile).filter(
                file -> file.isDirectory() || file.getName().endsWith(".fastq") ||
                        file.getName().endsWith(".fastq.gz")).map(file -> {
            if (file.isDirectory()) {
                return getFastqFiles(file);
            } else {
                return new HashSet<File>() {{
                    add(file);
                }};
            }
        }).flatMap(Collection::stream).collect(Collectors.toSet());
    }
}
