/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
@OptionsPanelController.ContainerRegistration(
        id = "S3TBX",
        categoryName = "#LBL_S3TBXOptionsCategory_Name",
        iconBase = "org/esa/s3tbx/dataio/s3/preferences/S3TBX_icon_32.jpg",
        keywords = "#LBL_S3TBXOptionsCategory_Keywords",
        keywordsCategory = "S3TBX",
        position = 1200
)
@NbBundle.Messages(value = {
    "LBL_S3TBXOptionsCategory_Name=S3TBX",
    "LBL_S3TBXOptionsCategory_Keywords=s3tbx,slstr,olci"
})
package org.esa.s3tbx.dataio.s3.preferences.ui;

import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;