package org.exbio.sradownloader;

import org.exbio.pipejar.configs.ConfigModuleCollection;
import org.exbio.sradownloader.input.InputConfigs;
import org.exbio.sradownloader.rnaSeq.RnaSeqConfigs;

public class Configs extends ConfigModuleCollection {
    public final InputConfigs inputConfigs = new InputConfigs();
    public final RnaSeqConfigs rnaSeqConfigs = new RnaSeqConfigs();
}
