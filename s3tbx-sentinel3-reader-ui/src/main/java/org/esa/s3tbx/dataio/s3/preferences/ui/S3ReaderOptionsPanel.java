/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.s3tbx.dataio.s3.preferences.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.s3tbx.dataio.s3.meris.MerisProductFactory;
import org.esa.s3tbx.dataio.s3.olci.OlciLevel1ProductFactory;
import org.esa.s3tbx.dataio.s3.olci.OlciProductFactory;
import org.esa.s3tbx.dataio.s3.slstr.SlstrLevel1ProductFactory;
import org.esa.s3tbx.dataio.s3.slstr.SlstrSstProductFactory;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.preferences.PreferenceUtils;
import org.esa.snap.runtime.Config;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.Insets;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class S3ReaderOptionsPanel extends javax.swing.JPanel {

    private JCheckBox slstrL1BPixelGeocodingsCheckBox;
    private JCheckBox slstrL1BOrphanPixelsCheckBox;
    private JCheckBox slstrL1BCalibrationCheckBox;
    private JCheckBox slstrL1BS3MPCRecommendationCheckBox;
    private JCheckBox slstrL2SSTPixelGeocodingsCheckBox;
    private JCheckBox olciPixelGeocodingsCheckBox;
    private JCheckBox olciL1CalibrationCheckBox;
    private JCheckBox merisPixelGeocodingsCheckBox;

    S3ReaderOptionsPanel(final S3ReaderOptionsPanelController controller) {
        initComponents();
        // listen to changes in form fields and call controller.changed()
        slstrL1BPixelGeocodingsCheckBox.addItemListener(e -> controller.changed());
        slstrL1BOrphanPixelsCheckBox.addItemListener(e -> controller.changed());
        slstrL1BCalibrationCheckBox.addItemListener(e -> controller.changed());
        slstrL1BS3MPCRecommendationCheckBox.addItemListener(e -> controller.changed());
        slstrL2SSTPixelGeocodingsCheckBox.addItemListener(e -> controller.changed());
        olciPixelGeocodingsCheckBox.addItemListener(e -> controller.changed());
        olciL1CalibrationCheckBox.addItemListener(e -> controller.changed());
        merisPixelGeocodingsCheckBox.addItemListener(e -> controller.changed());
    }

    private void initComponents() {
        slstrL1BPixelGeocodingsCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(slstrL1BPixelGeocodingsCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.slstrL1BPixelGeocodingsCheckBox.text")); // NOI18N
        slstrL1BOrphanPixelsCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(slstrL1BOrphanPixelsCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.slstrL1BOrphanPixelsCheckBox.text")); // NOI18N
        slstrL1BCalibrationCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(slstrL1BCalibrationCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.slstrL1BCalibrationFactorCheckBox.text")); // NOI18N
        slstrL1BS3MPCRecommendationCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(slstrL1BS3MPCRecommendationCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.slstrL1BS3MPCRecommendationCheckBox.text")); // NOI18N
        slstrL2SSTPixelGeocodingsCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(slstrL2SSTPixelGeocodingsCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.slstrL2SSTPixelGeocodingsCheckBox.text")); // NOI18N
        olciPixelGeocodingsCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(olciPixelGeocodingsCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.olciPixelGeocodingsCheckBox.text")); // NOI18N
        olciL1CalibrationCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(olciL1CalibrationCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.olciL1CalibrationCheckBox.text")); // NOI18N
        merisPixelGeocodingsCheckBox = new JCheckBox();
        Mnemonics.setLocalizedText(merisPixelGeocodingsCheckBox,
                                   NbBundle.getMessage(S3ReaderOptionsPanel.class,
                                                       "S3TBXReaderOptionsPanel.merisPixelGeocodingsCheckBox.text")); // NOI18N

        JPanel slstrLabel = PreferenceUtils.createTitleLabel("SLSTR");
        JPanel olciLabel = PreferenceUtils.createTitleLabel("OLCI");
        JPanel merisLabel = PreferenceUtils.createTitleLabel("MERIS");
        JLabel commentLabel = new JLabel("<html><b>NOTE:</b> For configuring the behaviour of geo-coding, please \n" +
                                                 "have also a look at the general Geo-Location panel, too.");
        final JSeparator separator = new JSeparator();

        TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTablePadding(new Insets(4, 10, 0, 0));
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setColumnWeightX(0, 1.0);
        this.setLayout(tableLayout);

        add(slstrLabel);
        add(slstrL1BPixelGeocodingsCheckBox);
        add(slstrL1BOrphanPixelsCheckBox);
        add(slstrL1BCalibrationCheckBox);
        add(slstrL1BS3MPCRecommendationCheckBox);
        add(slstrL2SSTPixelGeocodingsCheckBox);
        add(slstrL2SSTPixelGeocodingsCheckBox);
        add(olciLabel);
        add(olciPixelGeocodingsCheckBox);
        add(olciL1CalibrationCheckBox);
        add(merisLabel);
        add(merisPixelGeocodingsCheckBox);
        add(separator);
        add(commentLabel);


//        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
//        this.setLayout(layout);
//        layout.setHorizontalGroup(
//                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
//                        .addGroup(layout.createSequentialGroup()
//                                          .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
//                                                            .addComponent(slstrLabel).addComponent(separator)
//                                                            .addGap(0, 512, Short.MAX_VALUE)
//                                                            .addComponent(slstrL1BPixelGeocodingsCheckBox)
//                                                            .addGap(0, 512, Short.MAX_VALUE)
//                                                            .addComponent(slstrL1BOrphanPixelsCheckBox)
//                                                            .addGap(0, 512, Short.MAX_VALUE)
//                                                            .addComponent(slstrL1BCalibrationCheckBox)
//                                                            .addGap(0, 512, Short.MAX_VALUE)
//                                                            .addComponent(slstrL1BS3MPCRecommendationCheckBox)
//                                                            .addGap(0, 512, Short.MAX_VALUE)
//                                                            .addComponent(slstrL2SSTPixelGeocodingsCheckBox)
//                                                            .addGap(0, 512, Short.MAX_VALUE)
//                                                            .addComponent(olciLabel)
//                                                            .addGap(0, 512, Short.MAX_VALUE)
//                                                            .addComponent(olciPixelGeocodingsCheckBox)
//                                                            .addGap(0, 512, Short.MAX_VALUE)
//                                                            .addComponent(olciL1CalibrationCheckBox)
//                                                            .addGap(0, 512, Short.MAX_VALUE)
//                                                            .addComponent(merisLabel)
//                                                            .addGap(0, 512, Short.MAX_VALUE)
//                                                            .addComponent(merisPixelGeocodingsCheckBox)
//                                                            .addComponent(commentLabel))
//                                          .addContainerGap())
//        );
//        layout.setVerticalGroup(
//                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
//                        .addGroup(layout.createSequentialGroup()
//                                          .addComponent(slstrLabel)
//                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
//                                          .addComponent(slstrL1BPixelGeocodingsCheckBox)
//                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
//                                          .addComponent(slstrL1BOrphanPixelsCheckBox)
//                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
//                                          .addComponent(slstrL1BCalibrationCheckBox)
//                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
//                                          .addComponent(slstrL1BS3MPCRecommendationCheckBox)
//                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
//                                          .addComponent(slstrL2SSTPixelGeocodingsCheckBox)
//                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
//                                          .addComponent(olciLabel)
//                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
//                                          .addComponent(olciPixelGeocodingsCheckBox)
//                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
//                                          .addComponent(olciL1CalibrationCheckBox)
//                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
//                                          .addComponent(merisLabel)
//                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
//                                          .addComponent(merisPixelGeocodingsCheckBox)
//                                          .addComponent(commentLabel)
//                                          .addContainerGap())
//        );
    }

    void load() {
        final Preferences preferences = Config.instance("s3tbx").load().preferences();
        slstrL1BPixelGeocodingsCheckBox.setSelected(
                preferences.getBoolean(SlstrLevel1ProductFactory.SLSTR_L1B_USE_PIXELGEOCODINGS, true));
        slstrL1BOrphanPixelsCheckBox.setSelected(
                preferences.getBoolean(SlstrLevel1ProductFactory.SLSTR_L1B_LOAD_ORPHAN_PIXELS, false));
        slstrL1BCalibrationCheckBox.setSelected(
                preferences.getBoolean(SlstrLevel1ProductFactory.SLSTR_L1B_CUSTOM_CALIBRATION, false));
        slstrL1BS3MPCRecommendationCheckBox.setSelected(
                preferences.getBoolean(SlstrLevel1ProductFactory.SLSTR_L1B_S3MPC_CALIBRATION, false));
        slstrL2SSTPixelGeocodingsCheckBox.setSelected(
                preferences.getBoolean(SlstrSstProductFactory.SLSTR_L2_SST_USE_PIXELGEOCODINGS, true));
        olciPixelGeocodingsCheckBox.setSelected(
                preferences.getBoolean(OlciProductFactory.OLCI_USE_PIXELGEOCODING, true));
        olciL1CalibrationCheckBox.setSelected(
                preferences.getBoolean(OlciLevel1ProductFactory.OLCI_L1_CUSTOM_CALIBRATION, false));
        merisPixelGeocodingsCheckBox.setSelected(
                preferences.getBoolean(MerisProductFactory.MERIS_SAFE_USE_PIXELGEOCODING, true));
    }

    void store() {
        final Preferences preferences = Config.instance("s3tbx").load().preferences();
        preferences.putBoolean(SlstrLevel1ProductFactory.SLSTR_L1B_USE_PIXELGEOCODINGS,
                               slstrL1BPixelGeocodingsCheckBox.isSelected());
        preferences.putBoolean(SlstrLevel1ProductFactory.SLSTR_L1B_LOAD_ORPHAN_PIXELS,
                               slstrL1BOrphanPixelsCheckBox.isSelected());
        preferences.putBoolean(SlstrLevel1ProductFactory.SLSTR_L1B_CUSTOM_CALIBRATION,
                               slstrL1BCalibrationCheckBox.isSelected());
        preferences.putBoolean(SlstrLevel1ProductFactory.SLSTR_L1B_S3MPC_CALIBRATION,
                               slstrL1BS3MPCRecommendationCheckBox.isSelected());
        preferences.putBoolean(SlstrSstProductFactory.SLSTR_L2_SST_USE_PIXELGEOCODINGS,
                               slstrL2SSTPixelGeocodingsCheckBox.isSelected());
        preferences.putBoolean(OlciProductFactory.OLCI_USE_PIXELGEOCODING, olciPixelGeocodingsCheckBox.isSelected());
        preferences.putBoolean(OlciLevel1ProductFactory.OLCI_L1_CUSTOM_CALIBRATION, olciL1CalibrationCheckBox.isSelected());
        preferences.putBoolean(MerisProductFactory.MERIS_SAFE_USE_PIXELGEOCODING, merisPixelGeocodingsCheckBox.isSelected());
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            SnapApp.getDefault().getLogger().severe(e.getMessage());
        }
    }

    boolean valid() {
        // Check whether form is consistent and complete
        return true;
    }

}
