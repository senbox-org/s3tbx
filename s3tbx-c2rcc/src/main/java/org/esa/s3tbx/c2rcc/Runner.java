package org.esa.s3tbx.c2rcc;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.internal.OperatorExecutor;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.SystemUtils;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 */
public class Runner {
    private static final Logger LOGGER = Logger.getLogger(Runner.class.getName());
    static {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
        SystemUtils.initJAI((Class<?>) null);
    }

    public static void main(String[] args) throws IOException {
        File sourceFile = new File(args[0]);
        File sensor = new File(args[1]);
        LOGGER.info("Source: " + sourceFile.getName());

        Product source = ProductIO.readProduct(sourceFile);
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("useDefaultSolarFlux", true);
        Product target = GPF.createProduct("c2rcc." + sensor, parameters, source);

        writeWithGPF(target, args[1]);
    }

    private static void writeWithGPF(Product target, String targetFilePath) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        JAI defaultInstance = JAI.getDefaultInstance();
        LOGGER.info("GPF Parallelism: " + defaultInstance.getTileScheduler().getParallelism());
        LOGGER.info("GPF Cache-Size: " + defaultInstance.getTileCache().getMemoryCapacity() / (1000*1000));
        GPF.writeProduct(target, new File(targetFilePath), ProductIO.DEFAULT_FORMAT_NAME,
                         false, ProgressMonitor.NULL);

        stopWatch.stop();
        LOGGER.info("GPF TIME: " + stopWatch.getTimeDiffString());
    }

    private static void writeWithProductIO(Product target, String targetFilePath) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ProductIO.writeProduct(target, targetFilePath, ProductIO.DEFAULT_FORMAT_NAME);

        stopWatch.stop();
        LOGGER.info("PIO TIME: " + stopWatch.getTimeDiffString());
    }
}
