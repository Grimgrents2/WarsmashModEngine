package com.etheller.warsmash.parsers.fdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.fdfparser.FDFParser;
import com.etheller.warsmash.fdfparser.FrameDefinitionVisitor;
import com.etheller.warsmash.parsers.fdf.datamodel.AnchorDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.BackdropCornerFlags;
import com.etheller.warsmash.parsers.fdf.datamodel.ControlStyle;
import com.etheller.warsmash.parsers.fdf.datamodel.FontDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FrameClass;
import com.etheller.warsmash.parsers.fdf.datamodel.FrameDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.datamodel.FrameTemplateEnvironment;
import com.etheller.warsmash.parsers.fdf.datamodel.SetPointDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.TextJustify;
import com.etheller.warsmash.parsers.fdf.datamodel.Vector2Definition;
import com.etheller.warsmash.parsers.fdf.datamodel.Vector4Definition;
import com.etheller.warsmash.parsers.fdf.datamodel.fields.StringPairFrameDefinitionField;
import com.etheller.warsmash.parsers.fdf.frames.AbstractUIFrame;
import com.etheller.warsmash.parsers.fdf.frames.BackdropFrame;
import com.etheller.warsmash.parsers.fdf.frames.ControlFrame;
import com.etheller.warsmash.parsers.fdf.frames.EditBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.FilterModeTextureFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.ListBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.SimpleButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;
import com.etheller.warsmash.parsers.fdf.frames.SimpleStatusBarFrame;
import com.etheller.warsmash.parsers.fdf.frames.SpriteFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextureFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.units.custom.WTS;
import com.etheller.warsmash.util.ImageUtils;
import com.etheller.warsmash.util.StringBundle;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.handlers.AbstractMdxModelViewer;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxModel;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.FocusableFrame;
import com.hiveworkshop.rms.parsers.mdlx.MdlxLayer.FilterMode;

public final class GameUI extends AbstractUIFrame implements UIFrame {
	public static final boolean DEBUG = false;
	private static final boolean PIN_FAIL_IS_FATAL = true;
	private final DataSource dataSource;
	private final Element skin;
	private final Viewport viewport;
	private final Scene uiScene;
	private final AbstractMdxModelViewer modelViewer;
	private final int racialCommandIndex;
	private final FrameTemplateEnvironment templates;
	private final Map<String, Texture> pathToTexture = new HashMap<>();
	private final boolean autoPosition = false;
	private final FreeTypeFontGenerator fontGenerator;
	private final FreeTypeFontParameter fontParam;
	private final Map<String, UIFrame> nameToFrame = new HashMap<>();
	private final Viewport fdfCoordinateResolutionDummyViewport;
	private final DataTable skinData;
	private final Element errorStrings;
	private final GlyphLayout glyphLayout;
	private final WTS mapStrings;
	private final BitmapFont font;
	private final BitmapFont font20;
	private final DynamicFontGeneratorHolder dynamicFontGeneratorHolder;

	public GameUI(final DataSource dataSource, final GameSkin skin, final Viewport viewport, final Scene uiScene,
			final AbstractMdxModelViewer modelViewer, final int racialCommandIndex, final WTS mapStrings) {
		super("GameUI", null);
		this.dataSource = dataSource;
		this.skin = skin.getSkin();
		this.viewport = viewport;
		this.uiScene = uiScene;
		this.modelViewer = modelViewer;
		this.racialCommandIndex = racialCommandIndex;
		if (viewport instanceof ExtendViewport) {
			this.renderBounds.set(0, 0, ((ExtendViewport) viewport).getMinWorldWidth(),
					((ExtendViewport) viewport).getMinWorldHeight());
		}
		else {
			this.renderBounds.set(0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
		}
		this.templates = new FrameTemplateEnvironment();

		this.dynamicFontGeneratorHolder = new DynamicFontGeneratorHolder(this.modelViewer.dataSource, this.skin);
		this.fontGenerator = this.dynamicFontGeneratorHolder.getFontGenerator("MasterFont");
		final FreeTypeFontParameter fontParam = new FreeTypeFontParameter();
		fontParam.size = 32;
		this.font = this.fontGenerator.generateFont(fontParam);
		fontParam.size = 20;
		this.font20 = this.fontGenerator.generateFont(fontParam);
		this.fontParam = new FreeTypeFontParameter();
		this.fdfCoordinateResolutionDummyViewport = new FitViewport(0.8f, 0.6f);
		this.skinData = skin.getSkinsTable();
		this.errorStrings = this.skinData.get("Errors");
		this.glyphLayout = new GlyphLayout();
		this.mapStrings = mapStrings;
	}

	public static GameSkin loadSkin(final DataSource dataSource, final String skin) {
		final DataTable skinsTable = new DataTable(StringBundle.EMPTY);
		try (InputStream stream = dataSource.getResourceAsStream("UI\\war3skins.txt")) {
			skinsTable.readTXT(stream, true);
			try (InputStream miscDataTxtStream = dataSource.getResourceAsStream("Units\\CommandFunc.txt")) {
				skinsTable.readTXT(miscDataTxtStream, true);
			}
			try (InputStream miscDataTxtStream = dataSource.getResourceAsStream("Units\\CommandStrings.txt")) {
				skinsTable.readTXT(miscDataTxtStream, true);
			}
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
		// TODO eliminate duplicate read of skin TXT!!
		if (dataSource.has("war3mapSkin.txt")) {
			try (InputStream miscDataTxtStream = dataSource.getResourceAsStream("war3mapSkin.txt")) {
				skinsTable.readTXT(miscDataTxtStream, true);
			}
			catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
//		final Element main = skinsTable.get("Main");
//		final String skinsField = main.getField("Skins");
//		final String[] skins = skinsField.split(",");
		final Element defaultSkin = skinsTable.get("Default");
		final Element userSkin = skinsTable.get(skin);
		final Element customSkin = skinsTable.get("CustomSkin");
		for (final String key : defaultSkin.keySet()) {
			if (!userSkin.hasField(key)) {
				userSkin.setField(key, defaultSkin.getField(key));
			}
		}
		if (customSkin != null) {
			for (final String key : customSkin.keySet()) {
				userSkin.setField(key, customSkin.getField(key));
			}
		}
		return new GameSkin(userSkin, skinsTable);
	}

	public static GameSkin loadSkin(final DataSource dataSource, final int skinIndex) {
		final DataTable skinsTable = new DataTable(StringBundle.EMPTY);
		try (InputStream stream = dataSource.getResourceAsStream("UI\\war3skins.txt")) {
			skinsTable.readTXT(stream, true);
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
		// TODO eliminate duplicate read of skin TXT!!
		if (dataSource.has("war3mapSkin.txt")) {
			try (InputStream miscDataTxtStream = dataSource.getResourceAsStream("war3mapSkin.txt")) {
				skinsTable.readTXT(miscDataTxtStream, true);
			}
			catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
		final Element main = skinsTable.get("Main");
		final String skinsField = main.getField("Skins");
		final String[] skins = skinsField.split(",");
		final Element defaultSkin = skinsTable.get("Default");
		final Element userSkin;
		if ((skinIndex >= 0) && (skinIndex < skins.length)) {
			userSkin = skinsTable.get(skins[skinIndex]);
		}
		else {
			userSkin = new Element("UserSkin", skinsTable);
		}
		final Element customSkin = skinsTable.get("CustomSkin");
		for (final String key : defaultSkin.keySet()) {
			if (!userSkin.hasField(key)) {
				userSkin.setField(key, defaultSkin.getField(key));
			}
		}
		if (customSkin != null) {
			for (final String key : customSkin.keySet()) {
				userSkin.setField(key, customSkin.getField(key));
			}
		}
		return new GameSkin(userSkin, skinsTable);
	}

	public void loadTOCFile(final String tocFilePath) throws IOException {
		final DataSourceFDFParserBuilder dataSourceFDFParserBuilder = new DataSourceFDFParserBuilder(this.dataSource);
		final FrameDefinitionVisitor fdfVisitor = new FrameDefinitionVisitor(this.templates,
				dataSourceFDFParserBuilder);
		System.err.println("Loading TOC file: " + tocFilePath);
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(this.dataSource.getResourceAsStream(tocFilePath)))) {
			String line;
			int tocLines = 0;
			while ((line = reader.readLine()) != null) {
				final FDFParser firstFileParser = dataSourceFDFParserBuilder.build(line);
				fdfVisitor.visit(firstFileParser.program());
				tocLines++;
			}
			System.out.println("TOC file loaded " + tocLines + " lines");
		}
	}

	public boolean hasSkinField(final String file) {
		return (file != null) && this.skin.hasField(file);
	}

	public String getSkinField(String file) {
		if ((file != null) && this.skin.hasField(file)) {
			file = this.skin.getField(file);
		}
		else {
			throw new IllegalStateException("Decorated file name lookup not available: " + file);
		}
		return file;
	}

	public String trySkinField(String file) {
		if ((file != null) && this.skin.hasField(file)) {
			file = this.skin.getField(file);
		}
		return file;
	}

	public DataTable getSkinData() {
		return this.skinData;
	}

	public UIFrame createFrame(final String name, final UIFrame owner, final int priority, final int createContext) {
		final FrameDefinition frameDefinition = this.templates.getFrame(name);
		final UIFrame inflatedFrame = inflate(frameDefinition, owner, null, frameDefinition.has("DecorateFileNames"));
		if (owner == this) {
			add(inflatedFrame);
		}
		return inflatedFrame;
	}

	public UIFrame createSimpleFrame(final String name, final UIFrame owner, final int createContext) {
		final FrameDefinition frameDefinition = this.templates.getFrame(name);
		if (frameDefinition == null) {
			final SimpleFrame simpleFrame = new SimpleFrame(name, owner);
			add(simpleFrame);
			return simpleFrame;
		}
		else if (frameDefinition.getFrameClass() == FrameClass.Frame) {
			final UIFrame inflated = inflate(frameDefinition, owner, null, frameDefinition.has("DecorateFileNames"));
			if (this.autoPosition) {
				inflated.positionBounds(this, this.viewport);
			}
			add(inflated);
			return inflated;
		}
		return null;
	}

	public TextureFrame createTextureFrame(final String name, final UIFrame parent, final boolean decorateFileNames,
			final Vector4Definition texCoord) {
		final TextureFrame textureFrame = new TextureFrame(name, parent, decorateFileNames, texCoord);
		this.nameToFrame.put(name, textureFrame);
		add(textureFrame);
		return textureFrame;
	}

	public TextureFrame createTextureFrame(final String name, final UIFrame parent, final boolean decorateFileNames,
			final Vector4Definition texCoord, final FilterMode filterMode) {
		final FilterModeTextureFrame textureFrame = new FilterModeTextureFrame(name, parent, decorateFileNames,
				texCoord);
		textureFrame.setFilterMode(filterMode);
		this.nameToFrame.put(name, textureFrame);
		add(textureFrame);
		return textureFrame;
	}

	public StringFrame createStringFrame(final String name, final UIFrame parent, final Color color,
			final TextJustify justifyH, final TextJustify justifyV, final float fdfFontSize) {
		this.fontParam.size = (int) convertY(this.viewport, fdfFontSize);
		if (this.fontParam.size == 0) {
			this.fontParam.size = 128;
		}
		final BitmapFont frameFont = this.fontGenerator.generateFont(this.fontParam);
		final StringFrame stringFrame = new StringFrame(name, parent, color, justifyH, justifyV, frameFont, name, null,
				null);
		this.nameToFrame.put(name, stringFrame);
		add(stringFrame);
		return stringFrame;
	}

	public UIFrame inflate(final FrameDefinition frameDefinition, final UIFrame parent,
			final FrameDefinition parentDefinitionIfAvailable, final boolean inDecorateFileNames) {
		UIFrame inflatedFrame = null;
		BitmapFont frameFont = null;
		Viewport viewport2 = this.viewport;
		switch (frameDefinition.getFrameClass()) {
		case Frame:
			if ("SIMPLEFRAME".equals(frameDefinition.getFrameType())) {
				final SimpleFrame simpleFrame = new SimpleFrame(frameDefinition.getName(), parent);
				// TODO: we should not need to put ourselves in this map 2x, but we do
				// since there are nested inflate calls happening before the general case
				// mapping
				this.nameToFrame.put(frameDefinition.getName(), simpleFrame);
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					simpleFrame.add(inflate(childDefinition, simpleFrame, frameDefinition,
							inDecorateFileNames || childDefinition.has("DecorateFileNames")));
				}
				inflatedFrame = simpleFrame;
			}
			else if ("SIMPLESTATUSBAR".equals(frameDefinition.getFrameType())) {
				final boolean decorateFileNames = frameDefinition.has("DecorateFileNames")
						|| ((parentDefinitionIfAvailable != null)
								&& parentDefinitionIfAvailable.has("DecorateFileNames"));
				final SimpleStatusBarFrame simpleStatusBarFrame = new SimpleStatusBarFrame(frameDefinition.getName(),
						parent, decorateFileNames, false, 0.0f);
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					simpleStatusBarFrame.add(inflate(childDefinition, simpleStatusBarFrame, frameDefinition,
							inDecorateFileNames || childDefinition.has("DecorateFileNames")));
				}
				final String barTexture = frameDefinition.getString("BarTexture");
				if (barTexture != null) {
					simpleStatusBarFrame.getBarFrame().setTexture(barTexture, this);
					simpleStatusBarFrame.getBorderFrame().setTexture(barTexture + "Border", this);
				}
				inflatedFrame = simpleStatusBarFrame;
			}
			else if ("SPRITE".equals(frameDefinition.getFrameType())) {
				final SpriteFrame spriteFrame = new SpriteFrame(frameDefinition.getName(), parent, this.uiScene,
						viewport2);
				String backgroundArt = frameDefinition.getString("BackgroundArt");
				if (frameDefinition.has("DecorateFileNames") || inDecorateFileNames) {
					if (backgroundArt != null) {
						if (this.skin.hasField(backgroundArt)) {
							backgroundArt = this.skin.getField(backgroundArt);
						}
					}
				}
				if (backgroundArt != null) {
					setSpriteFrameModel(spriteFrame, backgroundArt);
				}
				viewport2 = this.viewport; // TODO was fdfCoordinateResolutionDummyViewport here previously, but is that
											// a good idea?
				this.nameToFrame.put(frameDefinition.getName(), spriteFrame);
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					spriteFrame.add(inflate(childDefinition, spriteFrame, frameDefinition,
							inDecorateFileNames || childDefinition.has("DecorateFileNames")));
				}
				inflatedFrame = spriteFrame;
			}
			else if ("FRAME".equals(frameDefinition.getFrameType())
					|| "DIALOG".equals(frameDefinition.getFrameType())) {
				final SimpleFrame simpleFrame = new SimpleFrame(frameDefinition.getName(), parent);
				// TODO: we should not need to put ourselves in this map 2x, but we do
				// since there are nested inflate calls happening before the general case
				// mapping
				this.nameToFrame.put(frameDefinition.getName(), simpleFrame);
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					simpleFrame.add(inflate(childDefinition, simpleFrame, frameDefinition,
							inDecorateFileNames || childDefinition.has("DecorateFileNames")));
				}
				inflatedFrame = simpleFrame;
			}
			else if ("TEXT".equals(frameDefinition.getFrameType())) {
				final Float textLength = frameDefinition.getFloat("TextLength");
				TextJustify justifyH = frameDefinition.getTextJustify("FontJustificationH");
				if (justifyH == null) {
					justifyH = TextJustify.LEFT;
				}
				TextJustify justifyV = frameDefinition.getTextJustify("FontJustificationV");
				if (justifyV == null) {
					justifyV = TextJustify.MIDDLE;
				}
				for (final SetPointDefinition setPoint : frameDefinition.getSetPoints()) {
					if (((setPoint.getMyPoint() == FramePoint.TOP) && (setPoint.getOtherPoint() == FramePoint.TOP))
							|| ((setPoint.getMyPoint() == FramePoint.BOTTOM)
									&& (setPoint.getOtherPoint() == FramePoint.BOTTOM))) {
						justifyH = TextJustify.CENTER;
					}
				}

				Color fontColor;
				final Vector4Definition fontColorDefinition = frameDefinition.getVector4("FontColor");
				if (fontColorDefinition == null) {
					fontColor = Color.WHITE;
				}
				else {
					fontColor = new Color(fontColorDefinition.getX(), fontColorDefinition.getY(),
							fontColorDefinition.getZ(), fontColorDefinition.getW());
				}

				Color fontHighlightColor;
				final Vector4Definition fontHighlightColorDefinition = frameDefinition.getVector4("FontHighlightColor");
				if (fontHighlightColorDefinition == null) {
					fontHighlightColor = null;
				}
				else {
					fontHighlightColor = new Color(fontHighlightColorDefinition.getX(),
							fontHighlightColorDefinition.getY(), fontHighlightColorDefinition.getZ(),
							fontHighlightColorDefinition.getW());
				}

				Color fontDisabledColor;
				final Vector4Definition fontDisabledColorDefinition = frameDefinition.getVector4("FontDisabledColor");
				if (fontDisabledColorDefinition == null) {
					fontDisabledColor = null;
				}
				else {
					fontDisabledColor = new Color(fontDisabledColorDefinition.getX(),
							fontDisabledColorDefinition.getY(), fontDisabledColorDefinition.getZ(),
							fontDisabledColorDefinition.getW());
				}

				Color fontShadowColor;
				final Vector4Definition fontShadowColorDefinition = frameDefinition.getVector4("FontShadowColor");
				if (fontShadowColorDefinition == null) {
					fontShadowColor = null;
				}
				else {
					fontShadowColor = new Color(fontShadowColorDefinition.getX(), fontShadowColorDefinition.getY(),
							fontShadowColorDefinition.getZ(), fontShadowColorDefinition.getW());
				}
				final FontDefinition font = frameDefinition.getFont("FrameFont");
				final Float height = frameDefinition.getFloat("Height");
				this.fontParam.size = (int) convertY(viewport2,
						(font == null ? (height == null ? 0.06f : height) : font.getFontSize()));
				if (this.fontParam.size == 0) {
					this.fontParam.size = 24;
				}
				frameFont = this.dynamicFontGeneratorHolder.getFontGenerator(font.getFontName())
						.generateFont(this.fontParam);
				String textString = frameDefinition.getName();
				String text = frameDefinition.getString("Text");
				if (text != null) {
					final String decoratedString = this.templates.getDecoratedString(text);
					if (decoratedString != text) {
						text = decoratedString;
					}
					textString = text;
				}
				final StringFrame stringFrame = new StringFrame(frameDefinition.getName(), parent, fontColor, justifyH,
						justifyV, frameFont, textString, fontHighlightColor, fontDisabledColor);
				if (fontShadowColor != null) {
					final Vector2Definition shadowOffset = frameDefinition.getVector2("FontShadowOffset");
					stringFrame.setFontShadowColor(fontShadowColor);
					stringFrame.setFontShadowOffsetX(convertX(viewport2, shadowOffset.getX()));
					stringFrame.setFontShadowOffsetY(convertY(viewport2, shadowOffset.getY()));
				}
				inflatedFrame = stringFrame;
			}
			else if ("GLUETEXTBUTTON".equals(frameDefinition.getFrameType())) {
				// ButtonText & ControlBackdrop
				final GlueTextButtonFrame glueButtonFrame = new GlueTextButtonFrame(frameDefinition.getName(), parent);
				// TODO: we should not need to put ourselves in this map 2x, but we do
				// since there are nested inflate calls happening before the general case
				// mapping
				this.nameToFrame.put(frameDefinition.getName(), glueButtonFrame);
				final String controlBackdropKey = frameDefinition.getString("ControlBackdrop");
				final String controlPushedBackdropKey = frameDefinition.getString("ControlPushedBackdrop");
				final String controlDisabledBackdropKey = frameDefinition.getString("ControlDisabledBackdrop");
				final String controlMouseOverHighlightKey = frameDefinition.getString("ControlMouseOverHighlight");
				final String buttonTextKey = frameDefinition.getString("ButtonText");
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					if (childDefinition.getName().equals(controlBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlBackdrop(inflatedChild);
					}
					else if (childDefinition.getName().equals(controlPushedBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlPushedBackdrop(inflatedChild);
					}
					else if (childDefinition.getName().equals(controlDisabledBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlDisabledBackdrop(inflatedChild);
					}
					else if (childDefinition.getName().equals(controlMouseOverHighlightKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlMouseOverHighlight(inflatedChild);
					}
					else if (childDefinition.getName().equals(buttonTextKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setButtonText(inflatedChild);
					}
				}
				final EnumSet<ControlStyle> controlStyle = ControlStyle
						.parseControlStyle(frameDefinition.getString("ControlStyle"));
				if (controlStyle.contains(ControlStyle.AUTOTRACK)
						&& controlStyle.contains(ControlStyle.HIGHLIGHTONMOUSEOVER)) {
					glueButtonFrame.setHighlightOnMouseOver(true);
				}
				inflatedFrame = glueButtonFrame;
			}
			else if ("SIMPLEBUTTON".equals(frameDefinition.getFrameType())) {
				// ButtonText & ControlBackdrop
				final SimpleButtonFrame simpleButtonFrame = new SimpleButtonFrame(frameDefinition.getName(), parent);
				// TODO: we should not need to put ourselves in this map 2x, but we do
				// since there are nested inflate calls happening before the general case
				// mapping
				this.nameToFrame.put(frameDefinition.getName(), simpleButtonFrame);
				final StringPairFrameDefinitionField normalTextDefinition = frameDefinition.getStringPair("NormalText");
				final StringPairFrameDefinitionField disabledTextDefinition = frameDefinition
						.getStringPair("DisabledText");
				final StringPairFrameDefinitionField highlightTextDefinition = frameDefinition
						.getStringPair("HighlightText");
				final String normalTextureDefinition = frameDefinition.getString("NormalTexture");
				final String pushedTextureDefinition = frameDefinition.getString("PushedTexture");
				final String disabledTextureDefinition = frameDefinition.getString("DisabledTexture");
				final String useHighlightDefinition = frameDefinition.getString("UseHighlight");

				final boolean decorateFileNamesOnThisFrame = frameDefinition.has("DecorateFileNames")
						|| inDecorateFileNames;
				final UIFrame normalText = inflate(this.templates.getFrame(normalTextDefinition.getFirst()),
						simpleButtonFrame, frameDefinition, decorateFileNamesOnThisFrame);
				setDecoratedText((StringFrame) normalText, normalTextDefinition.getSecond());
				normalText.setSetAllPoints(true);
				final UIFrame disabledText = inflate(this.templates.getFrame(disabledTextDefinition.getFirst()),
						simpleButtonFrame, frameDefinition, decorateFileNamesOnThisFrame);
				setDecoratedText((StringFrame) disabledText, disabledTextDefinition.getSecond());
				disabledText.setSetAllPoints(true);
				final UIFrame highlightText = inflate(this.templates.getFrame(highlightTextDefinition.getFirst()),
						simpleButtonFrame, frameDefinition, decorateFileNamesOnThisFrame);
				setDecoratedText((StringFrame) highlightText, highlightTextDefinition.getSecond());
				highlightText.setSetAllPoints(true);
				final UIFrame normalTexture = inflate(this.templates.getFrame(normalTextureDefinition),
						simpleButtonFrame, frameDefinition, decorateFileNamesOnThisFrame);
				normalTexture.setSetAllPoints(true);
				final UIFrame pushedTexture = inflate(this.templates.getFrame(pushedTextureDefinition),
						simpleButtonFrame, frameDefinition, decorateFileNamesOnThisFrame);
				pushedTexture.setSetAllPoints(true);
				final UIFrame disabledTexture = inflate(this.templates.getFrame(disabledTextureDefinition),
						simpleButtonFrame, frameDefinition, decorateFileNamesOnThisFrame);
				disabledTexture.setSetAllPoints(true);
				final UIFrame useHighlight = inflate(this.templates.getFrame(useHighlightDefinition), simpleButtonFrame,
						frameDefinition, decorateFileNamesOnThisFrame);
				useHighlight.setSetAllPoints(true);
				simpleButtonFrame.setButtonText(normalText);
				simpleButtonFrame.setDisabledText(disabledText);
				simpleButtonFrame.setHighlightText(highlightText);
				simpleButtonFrame.setControlBackdrop(normalTexture);
				simpleButtonFrame.setControlDisabledBackdrop(disabledTexture);
				simpleButtonFrame.setControlMouseOverHighlight(useHighlight);
				simpleButtonFrame.setControlPushedBackdrop(pushedTexture);

				final Vector2Definition pushedTextOffset = frameDefinition.getVector2("ButtonPushedTextOffset");
				if (pushedTextOffset != null) {
					final UIFrame pushedNormalText = inflate(this.templates.getFrame(normalTextDefinition.getFirst()),
							simpleButtonFrame, frameDefinition, decorateFileNamesOnThisFrame);
					setDecoratedText((StringFrame) pushedNormalText, normalTextDefinition.getSecond());
					final UIFrame pushedHighlightText = inflate(
							this.templates.getFrame(highlightTextDefinition.getFirst()), simpleButtonFrame,
							frameDefinition, decorateFileNamesOnThisFrame);
					setDecoratedText((StringFrame) pushedHighlightText, highlightTextDefinition.getSecond());
					pushedNormalText.addSetPoint(new SetPoint(FramePoint.TOPLEFT, simpleButtonFrame, FramePoint.TOPLEFT,
							GameUI.convertX(viewport2, pushedTextOffset.getX()),
							GameUI.convertY(viewport2, pushedTextOffset.getY())));
					pushedNormalText.addSetPoint(new SetPoint(FramePoint.BOTTOMRIGHT, simpleButtonFrame,
							FramePoint.BOTTOMRIGHT, GameUI.convertX(viewport2, pushedTextOffset.getX()),
							GameUI.convertY(viewport2, pushedTextOffset.getY())));
					pushedHighlightText.addSetPoint(new SetPoint(FramePoint.TOPLEFT, simpleButtonFrame,
							FramePoint.TOPLEFT, GameUI.convertX(viewport2, pushedTextOffset.getX()),
							GameUI.convertY(viewport2, pushedTextOffset.getY())));
					pushedHighlightText.addSetPoint(new SetPoint(FramePoint.BOTTOMRIGHT, simpleButtonFrame,
							FramePoint.BOTTOMRIGHT, GameUI.convertX(viewport2, pushedTextOffset.getX()),
							GameUI.convertY(viewport2, pushedTextOffset.getY())));
					simpleButtonFrame.setPushedText(pushedNormalText);
					simpleButtonFrame.setPushedHighlightText(pushedHighlightText);
				}
				else {
					simpleButtonFrame.setPushedText(normalText);
					simpleButtonFrame.setPushedHighlightText(highlightText);
				}

				inflatedFrame = simpleButtonFrame;
			}
			else if ("GLUEBUTTON".equals(frameDefinition.getFrameType())) {
				// ButtonText & ControlBackdrop
				final GlueButtonFrame glueButtonFrame = new GlueButtonFrame(frameDefinition.getName(), parent);
				// TODO: we should not need to put ourselves in this map 2x, but we do
				// since there are nested inflate calls happening before the general case
				// mapping
				this.nameToFrame.put(frameDefinition.getName(), glueButtonFrame);
				final String controlBackdropKey = frameDefinition.getString("ControlBackdrop");
				final String controlPushedBackdropKey = frameDefinition.getString("ControlPushedBackdrop");
				final String controlDisabledBackdropKey = frameDefinition.getString("ControlDisabledBackdrop");
				final String controlMouseOverHighlightKey = frameDefinition.getString("ControlMouseOverHighlight");
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					if (childDefinition.getName().equals(controlBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlBackdrop(inflatedChild);
					}
					else if (childDefinition.getName().equals(controlPushedBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlPushedBackdrop(inflatedChild);
					}
					else if (childDefinition.getName().equals(controlDisabledBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlDisabledBackdrop(inflatedChild);
					}
					else if (childDefinition.getName().equals(controlMouseOverHighlightKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlMouseOverHighlight(inflatedChild);
					}
				}
				final EnumSet<ControlStyle> controlStyle = ControlStyle
						.parseControlStyle(frameDefinition.getString("ControlStyle"));
				if (controlStyle.contains(ControlStyle.AUTOTRACK)
						&& controlStyle.contains(ControlStyle.HIGHLIGHTONMOUSEOVER)) {
					glueButtonFrame.setHighlightOnMouseOver(true);
				}
				inflatedFrame = glueButtonFrame;
			}
			else if ("TEXTBUTTON".equals(frameDefinition.getFrameType())) {
				// ButtonText & ControlBackdrop
				final TextButtonFrame glueButtonFrame = new TextButtonFrame(frameDefinition.getName(), parent);
				// TODO: we should not need to put ourselves in this map 2x, but we do
				// since there are nested inflate calls happening before the general case
				// mapping
				this.nameToFrame.put(frameDefinition.getName(), glueButtonFrame);
				final String controlBackdropKey = frameDefinition.getString("ControlBackdrop");
				final String controlPushedBackdropKey = frameDefinition.getString("ControlPushedBackdrop");
				final String controlDisabledBackdropKey = frameDefinition.getString("ControlDisabledBackdrop");
				final String controlMouseOverHighlightKey = frameDefinition.getString("ControlMouseOverHighlight");
				final Vector2Definition pushedTextOffset = frameDefinition.getVector2("ButtonPushedTextOffset");
				if (pushedTextOffset != null) {
					glueButtonFrame.setButtonPushedTextOffsetX(pushedTextOffset.getX());
					glueButtonFrame.setButtonPushedTextOffsetY(pushedTextOffset.getY());
				}
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					if (childDefinition.getName().equals(controlBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlBackdrop(inflatedChild);
					}
					else if (childDefinition.getName().equals(controlPushedBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlPushedBackdrop(inflatedChild);
					}
					else if (childDefinition.getName().equals(controlDisabledBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlDisabledBackdrop(inflatedChild);
					}
					else if (childDefinition.getName().equals(controlMouseOverHighlightKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, glueButtonFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						glueButtonFrame.setControlMouseOverHighlight(inflatedChild);
					}
				}
				final EnumSet<ControlStyle> controlStyle = ControlStyle
						.parseControlStyle(frameDefinition.getString("ControlStyle"));
				if (controlStyle.contains(ControlStyle.AUTOTRACK)
						&& controlStyle.contains(ControlStyle.HIGHLIGHTONMOUSEOVER)) {
					glueButtonFrame.setHighlightOnMouseOver(true);
				}
				inflatedFrame = glueButtonFrame;
			}
			else if ("EDITBOX".equals(frameDefinition.getFrameType())) {
				final float editBorderSize = convertX(viewport2, frameDefinition.getFloat("EditBorderSize"));
				final Vector4Definition editCursorColorDefinition = frameDefinition.getVector4("EditCursorColor");
				Color editCursorColor;
				if (editCursorColorDefinition == null) {
					editCursorColor = Color.WHITE;
				}
				else {
					editCursorColor = new Color(editCursorColorDefinition.getX(), editCursorColorDefinition.getY(),
							editCursorColorDefinition.getZ(), editCursorColorDefinition.getW());
				}
				final EditBoxFrame editBoxFrame = new EditBoxFrame(frameDefinition.getName(), parent, editBorderSize,
						editCursorColor);
				// TODO: we should not need to put ourselves in this map 2x, but we do
				// since there are nested inflate calls happening before the general case
				// mapping
				this.nameToFrame.put(frameDefinition.getName(), editBoxFrame);
				final String controlBackdropKey = frameDefinition.getString("ControlBackdrop");
				final String editTextFrameKey = frameDefinition.getString("EditTextFrame");
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					if (childDefinition.getName().equals(controlBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, editBoxFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						editBoxFrame.setControlBackdrop(inflatedChild);
					}
					else if (childDefinition.getName().equals(editTextFrameKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, editBoxFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						final StringFrame editTextFrame = (StringFrame) inflatedChild;
						inflatedChild.setSetAllPoints(true, editBorderSize);
						editBoxFrame.setEditTextFrame(editTextFrame);
						setText(editTextFrame, "");
					}
				}
				inflatedFrame = editBoxFrame;
			}
			else if ("CONTROL".equals(frameDefinition.getFrameType())) {
				final ControlFrame controlFrame = new ControlFrame(frameDefinition.getName(), parent);
				// TODO: we should not need to put ourselves in this map 2x, but we do
				// since there are nested inflate calls happening before the general case
				// mapping
				this.nameToFrame.put(frameDefinition.getName(), controlFrame);
				final String controlBackdropKey = frameDefinition.getString("ControlBackdrop");
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					if (childDefinition.getName().equals(controlBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, controlFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						controlFrame.setControlBackdrop(inflatedChild);
					}
				}
				inflatedFrame = controlFrame;
			}
			else if ("LISTBOX".equals(frameDefinition.getFrameType())) {
				// TODO advanced components here
				final ListBoxFrame controlFrame = new ListBoxFrame(frameDefinition.getName(), parent, viewport2);
				// TODO: we should not need to put ourselves in this map 2x, but we do
				// since there are nested inflate calls happening before the general case
				// mapping
				this.nameToFrame.put(frameDefinition.getName(), controlFrame);
				final String controlBackdropKey = frameDefinition.getString("ControlBackdrop");
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					if (childDefinition.getName().equals(controlBackdropKey)) {
						final UIFrame inflatedChild = inflate(childDefinition, controlFrame, frameDefinition,
								inDecorateFileNames || childDefinition.has("DecorateFileNames"));
						inflatedChild.setSetAllPoints(true);
						controlFrame.setControlBackdrop(inflatedChild);
					}
				}
				inflatedFrame = controlFrame;
			}
			else if ("HIGHLIGHT".equals(frameDefinition.getFrameType())) {
				final String highlightType = frameDefinition.getString("HighlightType");
				if (!"FILETEXTURE".equals(highlightType)) {
					throw new IllegalStateException(
							"Our engine does not know how to handle a non-FILETEXTURE highlight");
				}
				final String highlightAlphaFile = frameDefinition.getString("HighlightAlphaFile");
				final String highlightAlphaMode = frameDefinition.getString("HighlightAlphaMode");
				final FilterModeTextureFrame textureFrame = new FilterModeTextureFrame(frameDefinition.getName(),
						parent, inDecorateFileNames || frameDefinition.has("DecorateFileNames"), null);
				textureFrame.setTexture(highlightAlphaFile, this);
				if ("ADD".equals(highlightAlphaMode)) {
					textureFrame.setFilterMode(FilterMode.ADDALPHA);
				}
				return textureFrame;
			}
			else if ("BACKDROP".equals(frameDefinition.getFrameType())) {
				final boolean tileBackground = frameDefinition.has("BackdropTileBackground");
				final boolean mirrored = frameDefinition.has("BackdropMirrored");
				String backgroundString = frameDefinition.getString("BackdropBackground");
				String cornerFlagsString = frameDefinition.getString("BackdropCornerFlags");
				if (cornerFlagsString == null) {
					cornerFlagsString = "";
				}
				final EnumSet<BackdropCornerFlags> cornerFlags = BackdropCornerFlags
						.parseCornerFlags(cornerFlagsString);
				final Float cornerSizeNullable = frameDefinition.getFloat("BackdropCornerSize");
				final float cornerSize = GameUI.convertX(viewport2,
						cornerSizeNullable == null ? 0.0f : cornerSizeNullable);
				final Float backgroundSizeNullable = frameDefinition.getFloat("BackdropBackgroundSize");
				final float backgroundSize = GameUI.convertX(viewport2,
						backgroundSizeNullable == null ? 0.0f : backgroundSizeNullable);
				Vector4Definition backgroundInsets = frameDefinition.getVector4("BackdropBackgroundInsets");
				if (backgroundInsets != null) {
					backgroundInsets = new Vector4Definition(GameUI.convertX(viewport2, backgroundInsets.getX()),
							GameUI.convertY(viewport2, backgroundInsets.getY()),
							GameUI.convertX(viewport2, backgroundInsets.getZ()),
							GameUI.convertY(viewport2, backgroundInsets.getW()));
				}
				else {
					backgroundInsets = new Vector4Definition(0, 0, 0, 0);
				}
				final boolean decorateFileNames = frameDefinition.has("DecorateFileNames") || inDecorateFileNames;
				String edgeFileString = frameDefinition.getString("BackdropEdgeFile");
				System.out.println(frameDefinition.getName() + " wants edge file: " + edgeFileString);
				if (decorateFileNames && (edgeFileString != null)) {
					edgeFileString = trySkinField(edgeFileString);
				}
				if (decorateFileNames && (edgeFileString != null)) {
					backgroundString = trySkinField(backgroundString);
				}
				final Texture background = backgroundString == null ? null : loadTexture(backgroundString);
				final Texture edgeFile = edgeFileString == null ? null : loadTexture(edgeFileString);
				System.out.println(frameDefinition.getName() + " got edge file: " + edgeFile);

				final BackdropFrame backdropFrame = new BackdropFrame(frameDefinition.getName(), parent,
						decorateFileNames, tileBackground, background, cornerFlags, cornerSize, backgroundSize,
						backgroundInsets, edgeFile, mirrored);
				this.nameToFrame.put(frameDefinition.getName(), backdropFrame);
				for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
					backdropFrame.add(inflate(childDefinition, backdropFrame, frameDefinition, decorateFileNames));
				}
				inflatedFrame = backdropFrame;
			}
			break;
		case Layer:
			final SimpleFrame simpleFrame = new SimpleFrame(frameDefinition.getName(), parent);
			simpleFrame.setSetAllPoints(true);
			this.nameToFrame.put(frameDefinition.getName(), simpleFrame);
			for (final FrameDefinition childDefinition : frameDefinition.getInnerFrames()) {
				simpleFrame.add(inflate(childDefinition, simpleFrame, frameDefinition,
						inDecorateFileNames || childDefinition.has("DecorateFileNames")));
			}
			inflatedFrame = simpleFrame;
			break;
		case String:
			final Float textLength = frameDefinition.getFloat("TextLength");
			TextJustify justifyH = frameDefinition.getTextJustify("FontJustificationH");
			if (justifyH == null) {
				justifyH = TextJustify.CENTER;
			}
			TextJustify justifyV = frameDefinition.getTextJustify("FontJustificationV");
			if (justifyV == null) {
				justifyV = TextJustify.MIDDLE;
			}

			Color fontColor;
			final Vector4Definition fontColorDefinition = frameDefinition.getVector4("FontColor");
			if (fontColorDefinition == null) {
				fontColor = Color.WHITE;
			}
			else {
				fontColor = new Color(fontColorDefinition.getX(), fontColorDefinition.getY(),
						fontColorDefinition.getZ(), fontColorDefinition.getW());
			}
			final FontDefinition font = frameDefinition.getFont("Font");
			this.fontParam.size = (int) convertY(viewport2, font.getFontSize());
			if (this.fontParam.size == 0) {
				this.fontParam.size = 24;
			}
			frameFont = this.dynamicFontGeneratorHolder.getFontGenerator(font.getFontName())
					.generateFont(this.fontParam);
			String textString = frameDefinition.getName();
			String text = frameDefinition.getString("Text");
			if (text != null) {
				final String decoratedString = this.templates.getDecoratedString(text);
				if (decoratedString != text) {
					text = decoratedString;
				}
				textString = text;
			}
			final StringFrame stringFrame = new StringFrame(frameDefinition.getName(), parent, fontColor, justifyH,
					justifyV, frameFont, textString, null, null);
			inflatedFrame = stringFrame;
			break;
		case Texture:
			final String file = frameDefinition.getString("File");
			final boolean decorateFileNames = frameDefinition.has("DecorateFileNames") || inDecorateFileNames;
			final Vector4Definition texCoord = frameDefinition.getVector4("TexCoord");
			TextureFrame textureFrame;
			final String alphaMode = frameDefinition.getString("AlphaMode");
			if ((alphaMode != null) && alphaMode.equals("ADD")) {
				final FilterModeTextureFrame filterModeTextureFrame = new FilterModeTextureFrame(
						frameDefinition.getName(), parent, decorateFileNames, texCoord);
				filterModeTextureFrame.setFilterMode(FilterMode.ADDALPHA);
				textureFrame = filterModeTextureFrame;
			}
			else {
				textureFrame = new TextureFrame(frameDefinition.getName(), parent, decorateFileNames, texCoord);
			}
			textureFrame.setTexture(file, this);
			inflatedFrame = textureFrame;
			break;
		default:
			break;
		}
		if (inflatedFrame != null) {
			if (frameDefinition.has("SetAllPoints")) {
				inflatedFrame.setSetAllPoints(true);
			}
			Float width = frameDefinition.getFloat("Width");
			if (width != null) {
				inflatedFrame.setWidth(convertX(viewport2, width));
			}
			else {
				width = frameDefinition.getFloat("TextLength");
				if (width != null) {
					if (frameFont != null) {
						inflatedFrame.setWidth(convertX(viewport2, width * frameFont.getSpaceWidth()));
					}
				}
			}
			final Float height = frameDefinition.getFloat("Height");
			if (height != null) {
				inflatedFrame.setHeight(convertY(viewport2, height));
			}
			for (final AnchorDefinition anchor : frameDefinition.getAnchors()) {
				inflatedFrame.addAnchor(new AnchorDefinition(anchor.getMyPoint(),
						convertX(this.viewport, anchor.getX()), convertY(this.viewport, anchor.getY())));
			}
			for (final SetPointDefinition setPointDefinition : frameDefinition.getSetPoints()) {
				final UIFrame otherFrameByName = getFrameByName(setPointDefinition.getOther(),
						0 /* TODO: createContext */);
				if (otherFrameByName == null) {
					System.err.println("Failing to pin " + frameDefinition.getName() + " to "
							+ setPointDefinition.getOther() + " because it was null!");
					if (PIN_FAIL_IS_FATAL) {
						throw new IllegalStateException("Failing to pin " + frameDefinition.getName() + " to "
								+ setPointDefinition.getOther() + " because it was null!");
					}
				}
				else {
					inflatedFrame.addSetPoint(new SetPoint(setPointDefinition.getMyPoint(), otherFrameByName,
							setPointDefinition.getOtherPoint(), convertX(this.viewport, setPointDefinition.getX()),
							convertY(this.viewport, setPointDefinition.getY())));
				}
			}
			this.nameToFrame.put(frameDefinition.getName(), inflatedFrame);
		}
		else {
			// TODO in production throw some kind of exception here
		}
		return inflatedFrame;
	}

	public void setSpriteFrameModel(final SpriteFrame spriteFrame, String backgroundArt) {
		if (backgroundArt.toLowerCase().endsWith(".mdl") || backgroundArt.toLowerCase().endsWith(".mdx")) {
			backgroundArt = backgroundArt.substring(0, backgroundArt.length() - 4);
		}
		backgroundArt += ".mdx";
		final MdxModel model = (MdxModel) this.modelViewer.load(backgroundArt, this.modelViewer.mapPathSolver,
				this.modelViewer.solverParams);
		spriteFrame.setModel(model);
	}

	public UIFrame createFrameByType(final String typeName, final String name, final UIFrame owner,
			final String inherits, final int createContext) {
		// TODO idk what inherits is doing yet, and I didn't implement createContext yet
		// even though it looked like just mapping/indexing on int
		final FrameDefinition baseTemplateDef = this.templates.getFrame(name);
		final FrameDefinition frameDefinition = new FrameDefinition(FrameClass.Frame, typeName, name);
		if (baseTemplateDef != null) {
			frameDefinition.inheritFrom(baseTemplateDef, "WITHCHILDREN".equals(inherits));
		}
		final UIFrame inflatedFrame = inflate(frameDefinition, owner, null, frameDefinition.has("DecorateFileNames"));
		if (this == owner) {
			add(inflatedFrame);
		}
		return inflatedFrame;
	}

	public UIFrame getFrameByName(final String name, final int createContext) {
		return this.nameToFrame.get(name);
	}

	public static float convertX(final Viewport viewport, final float fdfX) {
		if (viewport instanceof ExtendViewport) {
			return (fdfX / 0.8f) * ((ExtendViewport) viewport).getMinWorldWidth();
		}
		return (fdfX / 0.8f) * viewport.getWorldWidth();
	}

	public static float convertY(final Viewport viewport, final float fdfY) {
		if (viewport instanceof ExtendViewport) {
			return (fdfY / 0.6f) * ((ExtendViewport) viewport).getMinWorldHeight();
		}
		return (fdfY / 0.6f) * viewport.getWorldHeight();
	}

	public static float unconvertX(final Viewport viewport, final float nonFdfX) {
		if (viewport instanceof ExtendViewport) {
			return (nonFdfX / ((ExtendViewport) viewport).getMinWorldWidth()) * 0.8f;
		}
		return (nonFdfX / viewport.getWorldWidth()) * 0.8f;
	}

	public static float unconvertY(final Viewport viewport, final float nonFdfY) {
		if (viewport instanceof ExtendViewport) {
			return (nonFdfY / ((ExtendViewport) viewport).getMinWorldHeight()) * 0.6f;
		}
		return (nonFdfY / viewport.getWorldHeight()) * 0.6f;
	}

	public Texture loadTexture(String path) {
		final int lastDotIndex = path.lastIndexOf('.');
		if (lastDotIndex == -1) {
			path = path + ".blp";
		}
		else {
			path = path.substring(0, lastDotIndex) + ".blp";
		}
		Texture texture = this.pathToTexture.get(path);
		if (texture == null) {
			try {
				texture = ImageUtils.getAnyExtensionTexture(this.dataSource, path);
				this.pathToTexture.put(path, texture);
			}
			catch (final Exception exc) {

			}
		}
		return texture;
	}

	@Override
	public final void positionBounds(final GameUI gameUI, final Viewport viewport) {
		innerPositionBounds(this, viewport);
	}

	@Override
	protected void internalRender(final SpriteBatch batch, final BitmapFont baseFont, final GlyphLayout glyphLayout) {
		super.internalRender(batch, baseFont, glyphLayout);
	}

	@Override
	public void add(final UIFrame childFrame) {
		super.add(childFrame);
		this.nameToFrame.put(childFrame.getName(), childFrame);
	}

	public Scene getUiScene() {
		return this.uiScene;
	}

	public FrameTemplateEnvironment getTemplates() {
		return this.templates;
	}

	public String getErrorString(final String key) {
		String errorString = this.errorStrings.getField(key, this.racialCommandIndex);
		if (errorString.startsWith("TRIGSTR_")) {
			errorString = this.mapStrings.get(Integer.parseInt(errorString.substring(8)));
		}
		return errorString;
	}

	public GlyphLayout getGlyphLayout() {
		return this.glyphLayout;
	}

	public void setText(final StringFrame stringFrame, final String text) {
		stringFrame.setText(text, this, this.viewport);
	}

	public void setDecoratedText(final StringFrame stringFrame, final String text) {
		stringFrame.setText(this.templates.getDecoratedString(text), this, this.viewport);
	}

	public BitmapFont getFont() {
		return this.font;
	}

	public BitmapFont getFont20() {
		return this.font20;
	}

	public FreeTypeFontGenerator getFontGenerator() {
		return this.fontGenerator;
	}

	public void dispose() {
		this.dynamicFontGeneratorHolder.dispose();
	}

	public FocusableFrame getNextFocusFrame() {
		// TODO to support tabbing thru menus and stuff, we will have to implement this
		return null;
	}
}
