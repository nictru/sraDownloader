package org.exbio.sradownloader.input;

import org.exbio.pipejar.configs.ClassGetter;
import org.exbio.pipejar.configs.ConfigModule;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.ExternalConfig;

import java.io.File;
import java.util.List;

public class SraConfigs extends ConfigModule {
    public final ExternalConfig<File> runTable = new ExternalConfig<>(File.class);
    public final ExternalConfig<String> fasterqDump = new ExternalConfig<>(String.class);
    public final ExternalConfig<List<String>> excludeTreatments = new ExternalConfig<>(ClassGetter.getList());
    public final ExternalConfig<String> gzipExecutable = new ExternalConfig<>(String.class);
}
