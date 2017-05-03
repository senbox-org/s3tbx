package org.esa.s3tbx.olci.mph_chl.visat;

import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;

@ActionID(category = "Processing", id = "org.esa.s3tbx.olci.mph_chl.visat.MphChlAction")
@ActionRegistration(displayName = "#CTL_MphChlAction_Text")
@ActionReference(path = "Menu/Optical/Thematic Water Processing", position = 200)
@NbBundle.Messages({"CTL_MphChlAction_Text=OLCI MPH/CHL Processor"})
public class MphChlAction extends AbstractSnapAction {

    private static final String OPERATOR_ALIAS = "OlciMphChl";
    private static final String HELP_ID = "mphChlScientificTool";

    public MphChlAction() {
        putValue(Action.SHORT_DESCRIPTION, "Calculates maximum peak height of chlorophyll.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final DefaultSingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog(OPERATOR_ALIAS,
                                                     getAppContext(),
                                                     "OLCI MPH/CHL Processor",
                                                     HELP_ID);


        dialog.setTargetProductNameSuffix("_mphchl");
        dialog.getJDialog().pack();
        dialog.show();
    }

}
