package org.esa.s3tbx.processor.flh_mci;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@ActionID(category = "Processing", id = "org.esa.s3tbx.processor.flh_mci.FlhMciAction")
@ActionRegistration(displayName = "#CTL_FlhMciAction_Text")
@ActionReference(path = "Menu/Optical/Thematic Water Processing", position = 200)
@NbBundle.Messages({"CTL_FlhMciAction_Text=FLH/MCI Processor"})
public class FlhMciAction extends AbstractSnapAction {

    private static final String OPERATOR_ALIAS = "FlhMci";
    private static final String HELP_ID = "flhMciScientificTool";

    private static final String PROP_LOWER_BASELINE_BAND_NAME = "lowerBaselineBandName";
    private static final String PROP_UPPER_BASE_LINE_BAND_NAME = "upperBaselineBandName";
    private static final String PROP_SIGNAL_BAND_NAME = "signalBandName";
    private static final String PROP_LINE_HEIGHT_BAND_NAME = "lineHeightBandName";
    private static final String PROP_SLOPE_BAND_NAME = "slopeBandName";
    private static final String PROP_MASK_EXPRESSION = "maskExpression";
    private static final String PROP_PRESET = "preset";
    private static final String PROP_SLOPE = "slope";

    public FlhMciAction() {
        putValue(SHORT_DESCRIPTION, "Generates florescence line height (FLH) / maximum chlorophyll index (MCI) from spectral bands.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final AppContext appContext = getAppContext();

        final DefaultSingleTargetProductDialog dialog = new DefaultSingleTargetProductDialog(OPERATOR_ALIAS, appContext,
                                                                                             Bundle.CTL_FlhMciAction_Text(),
                                                                                             HELP_ID);
        final BindingContext bindingContext = dialog.getBindingContext();
        final PropertySet propertySet = bindingContext.getPropertySet();

        bindingContext.bindEnabledState(PROP_SLOPE_BAND_NAME, true, PROP_SLOPE, true);
        bindingContext.addPropertyChangeListener(PROP_PRESET, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Presets preset = (Presets) evt.getNewValue();
                if (preset != Presets.NONE) {
                    setValueIfValid(propertySet, PROP_LOWER_BASELINE_BAND_NAME, preset.getLowerBaselineBandName());
                    setValueIfValid(propertySet, PROP_UPPER_BASE_LINE_BAND_NAME, preset.getUpperBaselineBandName());
                    setValueIfValid(propertySet, PROP_SIGNAL_BAND_NAME, preset.getSignalBandName());
                    propertySet.setValue(PROP_LINE_HEIGHT_BAND_NAME, preset.getLineHeightBandName());
                    propertySet.setValue(PROP_SLOPE_BAND_NAME, preset.getSlopeBandName());
                    propertySet.setValue(PROP_MASK_EXPRESSION, preset.getMaskExpression());
                }
            }

            private void setValueIfValid(PropertySet propertySet, String propertyName, String bandName) {
                if (propertySet.getDescriptor(propertyName).getValueSet().contains(bandName)) {
                    propertySet.setValue(propertyName, bandName);
                }
            }
        });

        dialog.setTargetProductNameSuffix("_flhmci");
        dialog.getJDialog().pack();
        dialog.show();
    }

}
