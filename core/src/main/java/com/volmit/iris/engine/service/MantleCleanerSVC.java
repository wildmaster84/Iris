package com.volmit.iris.engine.service;

import com.volmit.iris.Iris;
import com.volmit.iris.core.IrisSettings;
import com.volmit.iris.engine.framework.Engine;
import com.volmit.iris.engine.object.IrisEngineService;
import com.volmit.iris.util.format.C;
import com.volmit.iris.util.math.M;
import com.volmit.iris.util.misc.getHardware;
import com.volmit.iris.util.scheduling.Looper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import static com.volmit.iris.engine.service.EngineStatusSVC.getEngineCount;

public class MantleCleanerSVC extends IrisEngineService {
    private static final AtomicInteger tectonicLimit = new AtomicInteger(30);
    private Ticker trimmer;
    private Ticker unloader;

    public MantleCleanerSVC(Engine engine) {
        super(engine);
    }

    @Override
    public void onEnable(boolean hotload) {
        if (engine.isStudio() && !IrisSettings.get().getPerformance().trimMantleInStudio)
            return;
        if (trimmer == null || !trimmer.isAlive())
            trimmer = createTrimmer(engine);
        if (unloader == null || !unloader.isAlive())
            unloader = createUnloader(engine);
    }

    @Override
    public void onDisable(boolean hotload) {
        if (hotload) return;
        if (trimmer != null) trimmer.await();
        if (unloader != null) unloader.await();
    }

    static {
        tectonicLimit.set(2);
        long t = getHardware.getProcessMemory();
        while (t > 200) {
            tectonicLimit.incrementAndGet();
            t = t - 200;
        }
    }

    public static int getTectonicLimit() {
        return tectonicLimit.get();
    }

    private static Ticker createTrimmer(Engine engine) {
        return new Ticker(() -> {
            if (engine.isClosed()) return -1;
            long start = M.ms();
            try {
                engine.getMantle().trim(tectonicLimit.get() / getEngineCount());
            } catch (Throwable e) {
                Iris.debug(C.RED + "Mantle: Failed to trim.");
                Iris.reportError(e);
                e.printStackTrace();
            }

            if (engine.isClosed()) return -1;
            int size = getEngineCount();
            return Math.max(1000 / size - (M.ms() - start), 0);
        }, "Iris Mantle Trimmer - " + engine.getWorld().name());
    }

    private static Ticker createUnloader(Engine engine) {
        return new Ticker(() -> {
            if (engine.isClosed()) return -1;
            long start = M.ms();
            try {
                engine.getMantle().unloadTectonicPlate(tectonicLimit.get() / getEngineCount());
            } catch (Throwable e) {
                Iris.debug(C.RED + "Mantle: Failed to unload.");
                Iris.reportError(e);
                e.printStackTrace();
            }

            if (engine.isClosed()) return -1;
            int size = getEngineCount();
            return Math.max(1000 / size - (M.ms() - start), 0);
        }, "Iris Mantle Unloader - " + engine.getWorld().name());
    }

    private static class Ticker extends Looper {
        private final LongSupplier supplier;
        private final CountDownLatch exit = new CountDownLatch(1);

        private Ticker(LongSupplier supplier, String name) {
            this.supplier = supplier;
            setPriority(Thread.MIN_PRIORITY);
            setName(name);
            start();
        }

        @Override
        protected long loop() {
            long wait = -1;
            try {
                wait = supplier.getAsLong();
            } catch (Throwable ignored) {}
            if (wait < 0) exit.countDown();
            return wait;
        }

        public void await() {
            try {
                exit.await();
            } catch (InterruptedException ignored) {}
        }
    }
}
