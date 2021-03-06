package com.etheller.warsmash.viewer5.handlers.mdx;

import java.util.List;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.ModelViewer;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.Texture;
import com.etheller.warsmash.viewer5.gl.DataTexture;
import com.etheller.warsmash.viewer5.gl.WebGL;
import com.etheller.warsmash.viewer5.handlers.w3x.DynamicShadowManager;
import com.etheller.warsmash.viewer5.handlers.w3x.W3xSceneLightManager;

public class BatchGroup extends GenericGroup {

	private final MdxModel model;
	public final boolean isExtended;

	public BatchGroup(final MdxModel model, final boolean isExtended) {
		this.model = model;
		this.isExtended = isExtended;
	}

	@Override
	public void render(final MdxComplexInstance instance, final Matrix4 mvp) {
		final Scene scene = instance.scene;
		final MdxModel model = this.model;
		final List<Texture> textures = model.getTextures();
		final MdxHandler handler = model.handler;
		final List<Batch> batches = model.batches;
		final List<Integer> replaceables = model.replaceables;
		final ModelViewer viewer = model.viewer;
		final GL20 gl = viewer.gl;
		final WebGL webGL = viewer.webGL;
		final boolean isExtended = this.isExtended;
		final ShaderProgram shader;
		final W3xSceneLightManager lightManager = (W3xSceneLightManager) scene.getLightManager();

		if (isExtended) {
			if (DynamicShadowManager.IS_SHADOW_MAPPING) {
				shader = handler.shaders.extendedShadowMap;
			}
			else {
				shader = handler.shaders.extended;
			}
		}
		else {
			if (DynamicShadowManager.IS_SHADOW_MAPPING) {
				shader = handler.shaders.complexShadowMap;
			}
			else {
				shader = handler.shaders.complex;
			}
		}

		webGL.useShaderProgram(shader);

		shader.setUniformMatrix("u_mvp", mvp);

		final DataTexture boneTexture = instance.boneTexture;
		final DataTexture unitLightsTexture = lightManager.getUnitLightsTexture();

		unitLightsTexture.bind(14);
		shader.setUniformi("u_lightTexture", 14);
		shader.setUniformf("u_lightCount", lightManager.getUnitLightCount());
		shader.setUniformf("u_lightTextureHeight", unitLightsTexture.getHeight());

		// Instances of models with no bones don't have a bone texture.
		if (boneTexture != null) {
			boneTexture.bind(15);

			shader.setUniformf("u_hasBones", 1);
			shader.setUniformi("u_boneMap", 15);
			shader.setUniformf("u_vectorSize", 1f / boneTexture.getWidth());
			shader.setUniformf("u_rowSize", 1);
		}
		else {
			shader.setUniformf("u_hasBones", 0);
		}

		shader.setUniformi("u_texture", 0);

		gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, model.arrayBuffer);
		gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, model.elementBuffer);

		shader.setUniform4fv("u_vertexColor", instance.vertexColor, 0, instance.vertexColor.length);

		for (final int index : this.objects) {
			final Batch batch = batches.get(index);
			final Geoset geoset = batch.geoset;
			final Layer layer = batch.layer;
			final int geosetIndex = geoset.index;
			final int layerIndex = layer.index;
			final float[] geosetColor = instance.geosetColors[geosetIndex];
			final float layerAlpha = instance.layerAlphas[layerIndex];

			if ((geosetColor[3] > 0.01) && (layerAlpha > 0.01)) {
				// BELOW: I updated it to "Math.max(0," because MDL and MDX parser for PRSCMOD
				// menu screen behaved differently,
				// the MDL case was getting "no data" for default value when unanimated, and "no
				// data" resolved to -1,
				// whereas MDX binary contained an "unused" 0 value.
				final int layerTexture = Math.max(0, instance.layerTextures[layerIndex]);
				final float[] uvAnim = instance.uvAnims[layerIndex];

				shader.setUniform4fv("u_geosetColor", geosetColor, 0, geosetColor.length);

				shader.setUniformf("u_layerAlpha", layerAlpha);
				shader.setUniformf("u_unshaded", layer.unshaded);

				shader.setUniform2fv("u_uvTrans", uvAnim, 0, 2);
				shader.setUniform2fv("u_uvRot", uvAnim, 2, 2);
				shader.setUniform1fv("u_uvScale", uvAnim, 4, 1);

				if (instance.additiveOverrideMeshMode) {
					layer.bindBlended(shader);
					gl.glBlendFunc(FilterMode.ADDITIVE_ALPHA[0], FilterMode.ADDITIVE_ALPHA[1]);
				}
				else if (instance.vertexColor[3] < 1.0f) {
					layer.bindBlended(shader);
				}
				else {
					layer.bind(shader);
				}

				final Integer replaceable = replaceables.get(layerTexture); // TODO is this OK?
				Texture texture;

				if ((replaceable > 0) && (replaceable < WarsmashConstants.REPLACEABLE_TEXTURE_LIMIT)
						&& (instance.replaceableTextures[replaceable] != null)) {
					texture = instance.replaceableTextures[replaceable];
				}
				else {
					texture = textures.get(layerTexture);

					Texture textureLookup = instance.textureMapper.get(texture);
					if (textureLookup == null) {
						textureLookup = texture;
					}
					texture = textureLookup;
				}

				viewer.webGL.bindTexture(texture, 0);

				if (isExtended) {
					geoset.bindExtended(shader, layer.coordId);
				}
				else {
					geoset.bind(shader, layer.coordId);
				}

				geoset.render();
			}
		}
	}
}
