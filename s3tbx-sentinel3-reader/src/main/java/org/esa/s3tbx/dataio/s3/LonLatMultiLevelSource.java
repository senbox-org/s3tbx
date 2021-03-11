package org.esa.s3tbx.dataio.s3;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

import com.bc.ceres.core.Assert;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.GenericMultiLevelSource;
import org.esa.snap.core.util.jai.SingleBandedSampleModel;

import javax.media.jai.ImageLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Objects;

public final class LonLatMultiLevelSource extends GenericMultiLevelSource {

    private final LonLatFunction function;
    private final int targetDataType;

    public static MultiLevelSource create(MultiLevelSource lonSource, MultiLevelSource latSource,
                                          LonLatFunction function, int targetDataType) {
        Assert.argument(lonSource != null, "lonSource != null");
        Assert.argument(latSource != null, "latSource != null");
        Assert.argument(function != null, "function != null");
        Rectangle2D lonModelBounds = lonSource.getModel().getModelBounds();
        Rectangle2D latModelBounds = latSource.getModel().getModelBounds();
        Assert.argument(Objects.equals(lonModelBounds, latModelBounds), "Model bounds of lat and lon source must be equal");
        return new LonLatMultiLevelSource(lonSource, latSource, function, targetDataType);
    }

    private LonLatMultiLevelSource(MultiLevelSource lonSource, MultiLevelSource latSource, LonLatFunction function,
                                   int targetDataType) {
        super(new MultiLevelSource[]{lonSource, latSource});
        this.function = function;
        this.targetDataType = targetDataType;
    }

    @Override
    protected RenderedImage createImage(RenderedImage[] sourceImages, int level) {
        final RenderedImage lonImage = sourceImages[0];
        final RenderedImage latImage = sourceImages[1];
        final SampleModel sampleModel = new SingleBandedSampleModel(targetDataType,
                                                                    lonImage.getSampleModel().getWidth(),
                                                                    lonImage.getSampleModel().getHeight());
        final ImageLayout imageLayout = new ImageLayout(lonImage);
        imageLayout.setSampleModel(sampleModel);

        return new LonLatFunctionOpImage(lonImage, latImage, imageLayout, function);
    }
}
