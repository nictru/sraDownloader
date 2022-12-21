package org.exbio.sradownloader.input;

import org.exbio.pipejar.configs.ConfigModule;
import org.exbio.pipejar.configs.ConfigTypes.InputTypes.ExternalConfig;
import org.exbio.pipejar.configs.ConfigValidators.ExactlyOneTrueValidator;

import java.io.File;
import java.util.function.Supplier;

public class InputConfigs extends ConfigModule {
    public final ExternalConfig<Boolean> useSra = new ExternalConfig<>(Boolean.class);
    public final ExternalConfig<Boolean> useDirectory = new ExternalConfig<>(Boolean.class);
    public final ExternalConfig<File> directory = new ExternalConfig<>(File.class);
    protected final Supplier<Boolean> exactlyOneInputMethod =
            () -> new ExactlyOneTrueValidator().validate(useDirectory, useSra);
    public SraConfigs sraConfigs;
}
