package org.esa.s2tbx.tooladapter;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.OperatorMenu;
import org.esa.beam.framework.gpf.ui.OperatorParameterSupport;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author Lucian Barbulescu.
 */
public class S2tbxToolAdapterDialog extends SingleTargetProductDialog {

    /** Operator identifier. */
    private String alias;
    /** Parameters related info. */
    private OperatorParameterSupport parameterSupport;
    /** The form used to get the user's input */
    private S2tbxToolAdapterForm form;

    /** Constructor.
     *
     * @param alias
     * @param appContext
     * @param title
     * @param helpID
     */
    protected S2tbxToolAdapterDialog(String alias, AppContext appContext, String title, String helpID) {
        super(appContext, title, helpID);
        this.alias = alias;
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(alias);

        this.parameterSupport = new OperatorParameterSupport(operatorSpi.getOperatorDescriptor());

        form = new S2tbxToolAdapterForm(appContext, operatorSpi, parameterSupport.getPropertySet(),
                getTargetProductSelector());
        OperatorMenu operatorMenu = new OperatorMenu(this.getJDialog(),
                operatorSpi.getOperatorDescriptor(),
                parameterSupport,
                appContext,
                helpID);
        getJDialog().setJMenuBar(operatorMenu.createDefaultMenu());
    }

    @Override
    protected void onApply() {
        if (validateUserInput()) {
            super.onApply();
        }
    }

    private boolean validateUserInput() {
        return true;
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }


    /**
     * Creates the desired target product.
     * Usually, this method will be implemented by invoking one of the multiple {@link org.esa.beam.framework.gpf.GPF GPF}
     * {@code createProduct} methods.
     * <p/>
     * The method should throw a {@link org.esa.beam.framework.gpf.OperatorException} in order to signal "nominal" processing errors,
     * other exeption types are treated as internal errors.
     *
     * @return The target product.
     * @throws Exception if an error occurs, an {@link org.esa.beam.framework.gpf.OperatorException} is signaling "nominal" processing errors.
     */
    @Override
    protected Product createTargetProduct() throws Exception {
//        Product sourceProduct = getAppContext().getSelectedProduct();

        //Get the target product definition
//        TargetProductSelectorModel targetProductDef = getTargetProductSelector().getModel();


//        Map<String, Object> parameters = new HashMap<String, Object>();
//        parameters.put("toolFile", new File("C:\\Windows\\System32\\cmd.exe"));
//        parameters.put("toolWorkingDirectory", new File("D:\\Workspaces\\ESA\\Toolbox\\workingDir"));
//        parameters.put("commandLineTemplate", "copy-cmdLineTemplate.tpl");
//        parameters.put("command", "COPY");

        //Update the targetProductFile
        //parameterSupport.getParameterMap().put(S2tbxToolAdapterConstants.TOOL_TARGET_PRODUCT_FILE_ID, getTargetProductSelector().getModel().getProductFile());

        //Get the selected product.
        final Product sourceProduct = form.getSourceProduct();
        Map<String, Product> sourceProducts = new HashMap<String, Product>();
        sourceProducts.put("sourceProduct", sourceProduct);

        Operator op = GPF.getDefaultInstance().createOperator(this.alias,parameterSupport.getParameterMap(),sourceProducts, null);

        // set the output consumer
        ((S2tbxToolAdapterOp)op).setConsumer(new LogOutputConsumer());
//
        return op.getTargetProduct();
    }

    @Override
    protected boolean canApply() {
        return true;
    }

    /**
     * Add the output of the tool to the log.
     */
    private class LogOutputConsumer implements ProcessOutputCounsumer {

        /**
         * Consume a line of output obtained from a tool.
         *
         * @param line a line of output text.
         */
        @Override
        public void consumeOutpuLine(String line) {
            BeamLogManager.getSystemLogger().log(Level.INFO, line);
        }
    }
}
