package org.exbio.sradownloader.input;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.configs.ConfigTypes.UsageTypes.RequiredConfig;
import org.exbio.pipejar.pipeline.ExecutableStep;
import org.exbio.pipejar.util.FileManagement;
import org.exbio.sradownloader.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import static org.exbio.pipejar.util.ScriptExecution.executeAndWait;

public class CompressFastq extends ExecutableStep {
    public final RequiredConfig<String> gzipExecutable =
            new RequiredConfig<>(Main.configs.inputConfigs.sraConfigs.gzipExecutable);

    public CompressFastq(Collection<OutputFile> fastqFiles) {
        super(true, fastqFiles);

        fastqFiles.forEach(dependency -> addOutput(dependency.getName() + ".gz"));
    }

    @Override
    protected Collection<Callable<Boolean>> getCallables() {
        return new ArrayList<>() {{
            getInputs().forEach(inputFile -> {
                File inputCopy = new File(outputDirectory, inputFile.getName());
                String commmand = gzipExecutable.get() + " " + inputCopy.getAbsolutePath();

                add(() -> {
                    // Copy input because symbolic links have too many levels for gzip
                    // Copied files are automatically deleted by gzip after compressing
                    FileManagement.copyFile(inputFile, inputCopy);
                    executeAndWait(commmand, true);
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
