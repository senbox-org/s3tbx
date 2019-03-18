package org.esa.s3tbx.slstr.pdu.stitching.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.ui.OperatorMenu;
import org.esa.snap.core.gpf.ui.OperatorParameterSupport;
import org.esa.snap.core.util.ArrayUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;

import javax.swing.AbstractButton;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Tonio Fincke
 */
public class PDUStitchingDialog extends ModelessDialog {

    private final PDUStitchingModel formModel;

    public PDUStitchingDialog(final String title, AppContext appContext, final String helpID) {
        super(appContext.getApplicationWindow(), title, ID_APPLY_CLOSE, helpID);

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi("PduStitching");
        formModel = new PDUStitchingModel();
        OperatorParameterSupport parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorDescriptor(),
                formModel.getPropertySet(), formModel.getParameterMap(), null);

        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(), operatorSpi.getOperatorDescriptor(),
                parameterSupport, appContext, helpID);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
        AbstractButton button = getButton(ID_APPLY);
        button.setText("Run");
        button.setMnemonic('R');
        setContent(new PDUStitchingPanel(appContext, formModel));
    }

    @Override
    protected void onApply() {
        ProgressMonitorSwingWorker<Product, Void> swingWorker = new ProgressMonitorSwingWorker<Product, Void>(getJDialog(), "PDU Stitching") {
            @Override
            protected Product doInBackground(ProgressMonitor pm) throws Exception {
                String[] before = null;
                Product product = null;
                final File targetDir = (File) formModel.getPropertyValue(PDUStitchingModel.PROPERTY_TARGET_DIR);
                if (targetDir.exists()) {
                    before = targetDir.list();
                }
                final Map<String, Object> parameterMap = formModel.getParameterMap();
                final Product[] sourceProducts = formModel.getSourceProducts();
                Map<String, Product> sourceProductMap = new HashMap<>();
                for (int i = 0; i < sourceProducts.length; i++) {
                    sourceProductMap.put(GPF.SOURCE_PRODUCT_FIELD_NAME + "." + (i + 1), sourceProducts[i]);
                }
                Operator stitchingOperator = GPF.getDefaultInstance().createOperator("PduStitching", parameterMap,
                        sourceProductMap, null);
                stitchingOperator.execute(pm);
                if (formModel.openInApp()) {
                    final ProductReaderPlugIn sen3ReaderPlugIn = getSentinel3ReaderPlugin();
                    final String[] after = targetDir.list();
                    for (String inTargetDir : after) {
                        if (!ArrayUtils.isMemberOf(inTargetDir, before)) {
                            pm.setSubTaskName("Opening stitched SLSTR L1B product");
                            final ProductReader reader = sen3ReaderPlugIn.createReaderInstance();
                            product = reader.readProductNodes(new File(targetDir, inTargetDir), null);
                        }
                    }

                }
                pm.done();
                return product;
            }
        };
        swingWorker.executeWithBlocking();
        Product product = null;
        try {
            product = swingWorker.get();
        } catch (InterruptedException | ExecutionException e) {
            Dialogs.showError("Could not create stitched SLSTR L1B product: " + e.getMessage());
        }
        if (product != null) {
            SnapApp.getDefault().getProductManager().addProduct(product);
        }
        Dialogs.showInformation("SLSTR L1B PDU Stitching",
                "Stitched SLSTR L1B product has been successfully created in the target directory.", null);
    }

    private ProductReaderPlugIn getSentinel3ReaderPlugin() {
        final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();
        final Iterator<ProductReaderPlugIn> sen3ReaderPlugins = ioPlugInManager.getReaderPlugIns("Sen3");
        if (!sen3ReaderPlugins.hasNext()) {
            throw new IllegalStateException("No appropriate reader for reading Sentinel-3 products found");
        }
        return sen3ReaderPlugins.next();
    }

}
