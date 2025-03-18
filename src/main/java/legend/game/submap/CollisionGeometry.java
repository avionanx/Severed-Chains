package legend.game.submap;

import org.joml.Vector3f;

public abstract class CollisionGeometry {
  public float playerRotationAfterCollision_800d1a84;

  public int playerRotationWasUpdated_800d1a8c;

  public boolean playerRunning;

  public abstract int checkCollision(final boolean isNpc, final Vector3f position, final Vector3f movement, final boolean updatePlayerRotationInterpolation);

  public abstract int getCollisionAndTransitionInfo(final int collisionPrimitiveIndex);

  public abstract void getMiddleOfCollisionPrimitive(final int primitiveIndex, final Vector3f out);

  public abstract int getCollisionPrimitiveAtPoint(final float x, final float y, final float z, final boolean checkSteepness, final boolean checkY);

  public abstract void unloadCollision();

  public abstract void tick();

  public abstract void setCollisionAndTransitionInfo(int i);
}
