package org.esa.s3tbx.c2rcc.ancillary;

public class AncDataFormat {

    private final String[] filenameSuffix;
    private final String bandName;
    private final double defaultValue;
    private final InterpolationBorderComputer borderComputer;

    public AncDataFormat(String[] filenameSuffix, String bandName, double defaultValue, InterpolationBorderComputer borderComputer) {
        this.filenameSuffix = filenameSuffix;
        this.bandName = bandName;
        this.defaultValue = defaultValue;
        this.borderComputer = borderComputer;
    }

    public String[] getFilenames(String prefix) {
        final String[] names = new String[filenameSuffix.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = prefix + filenameSuffix[i];
        }
        return names;
    }

    public String getBandName() {
        return bandName;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public InterpolationBorderComputer getInterpolationBorderComputer() {
        return borderComputer;
    }
}
