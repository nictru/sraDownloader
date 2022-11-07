package org.exbio.sradownloader;

import org.exbio.pipejar.configs.ConfigModule;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.ExternalConfig;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.InputConfig;

import java.io.File;
import java.util.List;

public class Configs extends ConfigModule {
    public final InputConfig<File> runTable = new ExternalConfig<>(File.class);
    public final InputConfig<String> fasterqDump = new ExternalConfig<>(String.class);
    public final InputConfig<List> excludeTreatments = new ExternalConfig<>(List.class);

    public Configs() {
        super.init();
    }
}
