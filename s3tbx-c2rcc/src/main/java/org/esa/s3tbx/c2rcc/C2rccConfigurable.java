package org.esa.s3tbx.c2rcc;

import org.esa.snap.core.datamodel.Product;

public interface C2rccConfigurable {

    void setAtmosphericAuxDataPath(String atmosphericAuxDataPath);

    void setOzoneStartProduct(Product ozoneStartProduct);

    void setOzoneEndProduct(Product ozoneEndProduct);

    void setPressureStartProduct(Product pressureStartProduct);

    void setPressureEndProduct(Product pressureEndProduct);

    void setTemperature(double temperature);

    void setSalinity(double salinity);

    void setOzone(double ozone);

    void setPress(double press);

    void setValidPixelExpression(String validPixelExpression);

    void setOutputRtosa(boolean outputRtosa);

    void setOutputAsRrs(boolean asRadianceRefl);
}
