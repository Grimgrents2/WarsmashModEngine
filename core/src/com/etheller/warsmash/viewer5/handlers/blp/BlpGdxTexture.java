package com.etheller.warsmash.viewer5.handlers.blp;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.etheller.warsmash.util.ImageUtils;
import com.etheller.warsmash.viewer5.GdxTextureResource;
import com.etheller.warsmash.viewer5.ModelViewer;
import com.etheller.warsmash.viewer5.PathSolver;
import com.etheller.warsmash.viewer5.handlers.ResourceHandler;

public class BlpGdxTexture extends GdxTextureResource {

	public BlpGdxTexture(final ModelViewer viewer, final ResourceHandler handler, final String extension,
			final PathSolver pathSolver, final String fetchUrl) {
		super(viewer, handler, extension, pathSolver, fetchUrl);
	}

	@Override
	protected void lateLoad() {

	}

	@Override
	protected void load(final InputStream src, final Object options) {
		BufferedImage img;
		try {
			img = ImageIO.read(src);
			setGdxTexture(ImageUtils.getTexture(img, true));
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

}
