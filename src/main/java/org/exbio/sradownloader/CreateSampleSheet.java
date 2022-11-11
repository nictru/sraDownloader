package org.exbio.sradownloader;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.pipeline.ExecutableStep;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;

public class CreateSampleSheet extends ExecutableStep {

    public final OutputFile sampleSheet;

    public CreateSampleSheet(Collection<OutputFile> dependencies) {
        super(dependencies);
        sampleSheet = addOutput("sampleSheet.csv");
    }

    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new HashSet<>() {{
            add(() -> {
                StringBuilder builder = new StringBuilder("sample,fastq_1,fastq_2,strandedness\n");
                getInputs().forEach(inputFile -> {
                    String fullName = inputFile.getName();
                    builder.append(fullName, 0, fullName.indexOf(".")).append(",");
                    builder.append(inputFile.getAbsoluteFile()).append(",,");
                    builder.append("unstranded").append("\n");
                });
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(sampleSheet))) {
                    writer.write(builder.toString());
                }
                return true;
            });
        }};
    }
}
