package de.gurkenlabs.litiengine.physics;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import de.gurkenlabs.litiengine.IUpdateable;
import de.gurkenlabs.litiengine.entities.IEntityProvider;
import de.gurkenlabs.litiengine.entities.IMobileEntity;

public interface IMovementController extends IUpdateable, IEntityProvider {
  
  public void apply(Force force);

  public List<Force> getActiceForces();

  public void onMovementCheck(Predicate<IMobileEntity> predicate);

  public void onMoved(Consumer<Point2D> cons);
}
