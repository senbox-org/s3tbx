package org.esa.s3tbx.dataio.landsat.geotiff;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by obarrile on 12/03/2019.
 */
public class LandsatColorIterator {
    private ArrayList<Color> colors;
    private Iterator<Color> colorIterator;

    public LandsatColorIterator () {
        colors = new ArrayList<Color>();
        colors.add(Color.red);
        colors.add(Color.red.darker());
        colors.add(Color.red.darker().darker());
        colors.add(Color.blue);
        colors.add(Color.blue.darker());
        colors.add(Color.blue.darker().darker());
        colors.add(Color.green);
        colors.add(Color.green.darker());
        colors.add(Color.green.darker().darker());
        colors.add(Color.yellow);
        colors.add(Color.yellow.darker());
        colors.add(Color.yellow.darker().darker());
        colors.add(Color.magenta);
        colors.add(Color.magenta.darker());
        colors.add(Color.magenta.darker().darker());
        colors.add(Color.pink);
        colors.add(Color.pink.darker());
        colors.add(Color.pink.darker().darker());

        colorIterator = colors.iterator();
    }


    public Color next() {
        if(colorIterator.hasNext()) return colorIterator.next();
        colorIterator = colors.iterator();
        return this.next();
    }

}
