package org.esa.s3tbx.dataio.landsat.geotiff.c2;

public class FlagCodingArgs {
    final String name;
    final int flagMask;
    final String description;

    FlagCodingArgs(String name, int flagMask, String description) {
        this.name = name;
        this.flagMask = flagMask;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getFlagMask() {
        return flagMask;
    }

    public String getDescription() {
        return description;
    }
}