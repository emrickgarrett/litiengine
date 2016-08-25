package de.gurkenlabs.litiengine;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.AnnotationFormatError;
import java.util.logging.LogManager;

import de.gurkenlabs.core.DefaultUncaughtExceptionHandler;
import de.gurkenlabs.core.IInitializable;
import de.gurkenlabs.core.ILaunchable;
import de.gurkenlabs.litiengine.annotation.GameInfo;
import de.gurkenlabs.litiengine.configuration.GameConfiguration;
import de.gurkenlabs.litiengine.entities.ai.EntityManager;
import de.gurkenlabs.litiengine.graphics.DebugRenderer;
import de.gurkenlabs.litiengine.graphics.IRenderEngine;
import de.gurkenlabs.litiengine.graphics.RenderEngine;
import de.gurkenlabs.litiengine.gui.screens.IScreenManager;
import de.gurkenlabs.litiengine.gui.screens.ScreenManager;
import de.gurkenlabs.litiengine.input.Input;
import de.gurkenlabs.litiengine.physics.IPhysicsEngine;
import de.gurkenlabs.litiengine.physics.PhysicsEngine;
import de.gurkenlabs.litiengine.sound.ISoundEngine;
import de.gurkenlabs.litiengine.sound.PaulsSoundEngine;
import de.gurkenlabs.litiengine.tiled.tmx.IEnvironment;
import de.gurkenlabs.util.io.StreamUtilities;

public abstract class Game implements IInitializable, ILaunchable {

  private static GameInfo info;
  private static GameConfiguration configuration;
  private static IScreenManager screenManager;
  private static IRenderEngine graphicsEngine;
  private static IPhysicsEngine physicsEngine;
  private static ISoundEngine soundEngine;
  private static IGameLoop gameLoop;
  private static IEnvironment environment;

  private static GameMetrics metrics;

  private static EntityManager entityManager;
  private static RenderLoop renderLoop;

  protected Game() {
    final GameInfo inf = this.getClass().getAnnotation(GameInfo.class);
    if (inf == null) {
      throw new AnnotationFormatError("No GameInfo annotation found on game implementation " + this.getClass());
    }
    
    info = inf;

    Thread.setDefaultUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler());
    final String gameTitle = !getInfo().subTitle().isEmpty() ? getInfo().name() + " - " + getInfo().subTitle() + " " + getInfo().version() : getInfo().name() + " - " + getInfo().version();
    final ScreenManager scrMgr = new ScreenManager(gameTitle);

    // ensures that we terminate the game, when the window is closed
    scrMgr.addWindowListener(new WindowHandler());
    screenManager = scrMgr;
  }
  
  static {
    graphicsEngine = new RenderEngine();
    physicsEngine = new PhysicsEngine();
    soundEngine = new PaulsSoundEngine();
    metrics = new GameMetrics();
    entityManager = new EntityManager();
    
    // init configuration before init method in order to use configured values
    // to initialize components
    configuration = new GameConfiguration();
    getConfiguration().load();

    // setup default exception handling for render and update loop
    renderLoop = new RenderLoop();
    renderLoop.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler());
    final GameLoop updateLoop = new GameLoop(getConfiguration().CLIENT.getUpdaterate());
    updateLoop.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler());
    gameLoop = updateLoop;

    getLoop().registerForUpdate(getPhysicsEngine());
    getLoop().onUpsTracked(updateCount -> getMetrics().setUpdatesPerSecond(updateCount));
  }
  
  public static GameConfiguration getConfiguration() {
    return configuration;
  }

  public static EntityManager getEntityManager() {
    return entityManager;
  }

  public static IEnvironment getEnvironment() {
    return environment;
  }

  public static GameInfo getInfo() {
    return info;
  }

  public static IGameLoop getLoop() {
    return gameLoop;
  }

  public static GameMetrics getMetrics() {
    return metrics;
  }

  public static IPhysicsEngine getPhysicsEngine() {
    return physicsEngine;
  }

  public static IRenderEngine getRenderEngine() {
    return graphicsEngine;
  }

  public static IScreenManager getScreenManager() {
    return screenManager;
  }

  public static ISoundEngine getSoundEngine() {
    return soundEngine;
  }

  public static void loadEnvironment(final IEnvironment env) {
    environment = env;
    environment.init();
    getPhysicsEngine().setBounds(new Rectangle2D.Double(0, 0, environment.getMap().getSizeInPixels().getWidth(), environment.getMap().getSizeInPixels().getHeight()));
  }

  @Override
  public void init() {
    final String LOGGING_CONFIG_FILE = "logging.properties";
    // init logging
    final InputStream defaultLoggingConfig = ClassLoader.getSystemResourceAsStream(LOGGING_CONFIG_FILE);

    // if a specific file exists, load it
    // otherwise try to find a default logging configuration in any resource
    // folder.
    if (!new File(LOGGING_CONFIG_FILE).exists() && defaultLoggingConfig != null) {
      try {
        StreamUtilities.copy(defaultLoggingConfig, new File(LOGGING_CONFIG_FILE));
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }

    if (new File(LOGGING_CONFIG_FILE).exists()) {
      System.setProperty("java.util.logging.config.file", LOGGING_CONFIG_FILE);

      try {
        LogManager.getLogManager().readConfiguration();
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    if (Game.getConfiguration().CLIENT.showGameMetrics()) {
      Game.getScreenManager().onRendered((g) -> getMetrics().render(g));
    }

    if (Game.getConfiguration().DEBUG.isDebugEnabled()) {
      Game.getRenderEngine().onEntityRendered(e -> DebugRenderer.renderEntityDebugInfo(e.getGraphics(), e.getRenderedObject()));
    }

    Game.getRenderEngine().onMapRendered(e -> {
      DebugRenderer.renderMapDebugInfo(e.getGraphics(), e.getRenderedObject());
    });

    // init screens
    getScreenManager().init(getConfiguration().GRAPHICS.getResolutionWidth(), getConfiguration().GRAPHICS.getResolutionHeight(), getConfiguration().GRAPHICS.isFullscreen());
    getScreenManager().onFpsChanged(fps -> {
      getMetrics().setFramesPerSecond(fps);
    });

    // init sounds
    soundEngine.init(getConfiguration().SOUND.getSoundVolume());

    getScreenManager().getRenderComponent().addMouseListener(Input.MOUSE);
    getScreenManager().getRenderComponent().addMouseMotionListener(Input.MOUSE);
    getScreenManager().getRenderComponent().addMouseWheelListener(Input.MOUSE);
  }

  @Override
  public void start() {
    gameLoop.start();
    soundEngine.start();
    renderLoop.start();
  }

  @Override
  public void terminate() {
    gameLoop.terminate();

    soundEngine.terminate();
    renderLoop.terminate();
    System.exit(0);
  }

  /**
   * The Class WindowHandler.
   */
  private class WindowHandler implements WindowListener {

    /*
     * (non-Javadoc)
     *
     * @see
     * java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
     */
    @Override
    public void windowActivated(final WindowEvent event) {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
     */
    @Override
    public void windowClosed(final WindowEvent event) {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
     */
    @Override
    public void windowClosing(final WindowEvent event) {
      Game.this.terminate();
      System.exit(0);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.
     * WindowEvent)
     */
    @Override
    public void windowDeactivated(final WindowEvent event) {
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.
     * WindowEvent)
     */
    @Override
    public void windowDeiconified(final WindowEvent event) {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
     */
    @Override
    public void windowIconified(final WindowEvent event) {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
     */
    @Override
    public void windowOpened(final WindowEvent event) {
    }
  }

}
