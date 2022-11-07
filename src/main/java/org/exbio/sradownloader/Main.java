package org.exbio.sradownloader;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.pipeline.ExecutableStep;
import org.exbio.pipejar.pipeline.ExecutionManager;

import java.io.File;
import java.io.IOException;

public class Main {
    static File workingDirectory;

    public static void main(String[] args) throws IOException {
        workingDirectory = new File(args[0]);
        ExecutionManager.workingDirectory = new OutputFile(args[0]);
        ExecutionManager.setThreadNumber(Integer.parseInt(args[1]));
        ExecutionManager.disableHashing();

        Configs configs = new Configs();
        configs.merge(new File(args[2]));

        ExecutableStep download = new Download(configs.runTable, configs.fasterqDump, configs.excludeTreatments);

        ExecutionManager manager = new ExecutionManager(download);
        manager.run();
    }
}