package org.esa.s3tbx.c2rcc;

import org.esa.snap.framework.datamodel.Product;

public interface C2rccConfigurable {

    void setAtmosphericAuxDataPath(String atmosphericAuxDataPath);

    void setTomsomiStartProduct(Product tomsomiStartProduct);

    void setTomsomiEndProduct(Product tomsomiEndProduct);

    void setNcepStartProduct(Product ncepStartProduct);

    void setNcepEndProduct(Product ncepEndProduct);

    void setTemperature(double temperature);

    void setSalinity(double salinity);

    void setOzone(double ozone);

    void setPress(double press);

    void setValidPixelExpression(String validPixelExpression);

    void setOutputRtosa(boolean outputRtosa);
}
