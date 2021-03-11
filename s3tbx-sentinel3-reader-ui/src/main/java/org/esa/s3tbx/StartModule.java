package org.esa.s3tbx;

import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.openide.modules.OnStart;

public class StartModule {

    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            ResourceUtils.installGraphs(this.getClass(), "auxdata/graphs/");
        }
    }
}