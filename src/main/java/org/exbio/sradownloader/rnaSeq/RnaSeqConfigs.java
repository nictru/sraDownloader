package org.exbio.sradownloader.rnaSeq;

import org.exbio.pipejar.configs.ConfigModule;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.ExternalConfig;

import java.io.File;

public class RnaSeqConfigs extends ConfigModule {
    public final ExternalConfig<String> nextflowExecutable = new ExternalConfig<>(String.class);
    public final ExternalConfig<File> genomeFasta = new ExternalConfig<>(File.class);
    public final ExternalConfig<File> gtf = new ExternalConfig<>(File.class);
    public final ExternalConfig<File> starIndex = new ExternalConfig<>(File.class);
    public final ExternalConfig<String> genome = new ExternalConfig<>(String.class);
    public final ExternalConfig<File> bed = new ExternalConfig<>(File.class);
    public final ExternalConfig<File> cache = new ExternalConfig<>(File.class);
    public final ExternalConfig<String> profile = new ExternalConfig<>(String.class);
}
