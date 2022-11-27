package org.exbio.sradownloader;

import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.pipeline.ExecutionManager;
import org.exbio.sradownloader.input.CompressFastq;
import org.exbio.sradownloader.input.SraDownload;
import org.exbio.sradownloader.rnaSeq.CreateSampleSheet;
import org.exbio.sradownloader.rnaSeq.NfCoreRnaSeq;

import java.io.File;
import java.io.IOException;

public class Main {
    public static File workingDirectory;
    public static Configs configs;

    public static void main(String[] args) throws IOException {
        workingDirectory = new File(args[0]);
        ExecutionManager.workingDirectory = new OutputFile(args[0]);
        ExecutionManager.setThreadNumber(Integer.parseInt(args[1]));

        configs = new Configs();
        configs.merge(new File(args[2]));

        SraDownload download = new SraDownload();
        CompressFastq compress = new CompressFastq(download.getOutputs());
        CreateSampleSheet createSampleSheet = new CreateSampleSheet(compress.getOutputs());
        NfCoreRnaSeq nfCoreRnaSeq = new NfCoreRnaSeq(compress.getOutputs(), createSampleSheet.sampleSheet);

        ExecutionManager manager = new ExecutionManager(download, compress, createSampleSheet, nfCoreRnaSeq);
        manager.run();
    }
}