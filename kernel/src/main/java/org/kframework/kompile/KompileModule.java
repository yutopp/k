// Copyright (c) 2014-2015 K Team. All Rights Reserved.
package org.kframework.kompile;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.apache.commons.io.FilenameUtils;
import org.kframework.main.FrontEnd;
import org.kframework.main.GlobalOptions;
import org.kframework.main.Tool;
import org.kframework.utils.errorsystem.KEMException;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.DefinitionDir;
import org.kframework.utils.file.KompiledDir;
import org.kframework.utils.file.TempDir;
import org.kframework.utils.file.WorkingDir;
import org.kframework.utils.inject.Options;
import org.kframework.utils.options.SMTOptions;

import java.io.File;
import java.util.Map;

public class KompileModule extends AbstractModule {

    public KompileModule() {
    }

    @Override
    protected void configure() {
        bind(FrontEnd.class).to(KompileFrontEnd.class);
        bind(Tool.class).toInstance(Tool.KOMPILE);

        Multibinder<Object> optionsBinder = Multibinder.newSetBinder(binder(), Object.class, Options.class);
        optionsBinder.addBinding().to(KompileOptions.class);
        Multibinder<Class<?>> experimentalOptionsBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<Class<?>>() {}, Options.class);
        experimentalOptionsBinder.addBinding().toInstance(KompileOptions.Experimental.class);
        experimentalOptionsBinder.addBinding().toInstance(SMTOptions.class);

        MapBinder.newMapBinder(
                binder(), String.class, org.kframework.kore.compile.Backend.class);
    }

    @Provides
    SMTOptions smtOptions(KompileOptions options) {
        return options.experimental.smt;
    }

    @Provides
    GlobalOptions globalOptions(KompileOptions options) {
        return options.global;
    }

    @Provides @DefinitionDir
    File definitionDir(@WorkingDir File workingDir, KompileOptions options) {
        if (options.directory == null) {
            return options.mainDefinitionFile().getParentFile();
        }
        File f = new File(options.directory);
        if (f.isAbsolute()) return f;
        return new File(workingDir, options.directory);
    }

    @Provides @KompiledDir
    File kompiledDir(@DefinitionDir File defDir, KompileOptions options, @TempDir File tempDir) {
        return new File(defDir, FilenameUtils.removeExtension(options.mainDefinitionFile().getName()) + "-kompiled");
    }

    @Provides
    org.kframework.kore.compile.Backend getKoreBackend(KompileOptions options, Map<String, org.kframework.kore.compile.Backend> map, KExceptionManager kem) {
        org.kframework.kore.compile.Backend backend = map.get(options.backend);
        if (backend == null) {
            throw KEMException.criticalError("Invalid backend: " + options.backend
                    + ". It should be one of " + map.keySet());
        }
        return backend;
    }
}
