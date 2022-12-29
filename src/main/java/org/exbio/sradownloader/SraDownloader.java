package org.exbio.sradownloader;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.exbio.pipejar.configs.ConfigTypes.FileTypes.OutputFile;
import org.exbio.pipejar.pipeline.Workflow;
import org.exbio.sradownloader.input.DownloadCompress;
import org.exbio.sradownloader.input.ImportDirectory;
import org.exbio.sradownloader.rnaSeq.NfCoreRnaSeq;

import java.io.IOException;
import java.util.Map;

public class SraDownloader extends Workflow<Configs> {
    public SraDownloader(String[] args) throws IOException, ParseException {
        super(args);
    }

    public static void main(String[] args) throws IOException, ParseException {
        new SraDownloader(args);
    }

    @Override
    protected Configs createConfigs() {
        return new Configs();
    }

    @Override
    protected void buildFlow() {
        final Map<String, Pair<OutputFile, OutputFile>> fastqFiles;

        if (Configs.inputConfigs.useSra.get()) {
            logger.info("Downloading SRA files");
            DownloadCompress downloadCompress = add(new DownloadCompress());
            fastqFiles = downloadCompress.outputFiles;
        } else {
            logger.info("Importing fastq files");
            ImportDirectory importDirectory = add(new ImportDirectory());
            fastqFiles = importDirectory.outputFiles;
        }

        add(new NfCoreRnaSeq(fastqFiles));
    }
}