package org.esa.s3tbx.c2rcc.ancillary;

public class AncDataFormat {

    private final String[] filenameSuffix;
    private final String bandname;
    private final double defaultvalue;
    private final InterpolationBorderComputer borderComputer;

    public AncDataFormat(String[] filenameSuffix, String bandname, double defaultvalue, InterpolationBorderComputer borderComputer) {
        this.filenameSuffix = filenameSuffix;
        this.bandname = bandname;
        this.defaultvalue = defaultvalue;
        this.borderComputer = borderComputer;
    }

    public String[] getFilenames(String prefix) {
        final String[] names = new String[filenameSuffix.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = prefix + filenameSuffix[i];
        }
        return names;
    }

    public String getBandname() {
        return bandname;
    }

    public double getDefaultvalue() {
        return defaultvalue;
    }

    public InterpolationBorderComputer getInterpolationBorderComputer() {
        return borderComputer;
    }
}
