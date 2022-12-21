package org.exbio.sradownloader.rnaSeq;

import org.exbio.pipejar.configs.ConfigModule;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.ExternalConfig;

public class RnaSeqConfigs extends ConfigModule {
    public final ExternalConfig<String> nextflowExecutable = new ExternalConfig<>(String.class);
}
