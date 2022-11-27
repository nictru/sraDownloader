package org.exbio.sradownloader.rnaSeq;

import org.exbio.pipejar.configs.ConfigModule;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.ExternalConfig;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

public class RnaSeqConfigs extends ConfigModule {
    public final InputConfig<String> nextflowExecutable = new ExternalConfig<>(String.class);
}
