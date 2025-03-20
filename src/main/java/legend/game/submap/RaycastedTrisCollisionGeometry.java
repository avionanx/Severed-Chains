package legend.game.submap;

import org.joml.Vector3f;

public class RaycastedTrisCollisionGeometry extends CollisionGeometry {

  private final double MIN_THRESHOLD = 1e-6;
  private final float SCALE = 64;

  private final Vector3f[] verts = {
    new Vector3f(0,0,-1),
    new Vector3f(1,0,0),
    new Vector3f(-1,0,0)
  };

  @Override
  public int checkCollision(boolean isNpc, Vector3f position, Vector3f movement, boolean updatePlayerRotationInterpolation) {
    return 0;
  }

  @Override
  public int getCollisionAndTransitionInfo(int collisionPrimitiveIndex) {
    return 0;
  }

  @Override
  public void getMiddleOfCollisionPrimitive(int primitiveIndex, Vector3f out) {

  }

  @Override
  public int getCollisionPrimitiveAtPoint(float x, float y, float z, boolean checkSteepness, boolean checkY) {
    return 0;
  }

  @Override
  public void unloadCollision() {

  }

  @Override
  public void tick() {

  }

  @Override
  public void setCollisionAndTransitionInfo(int i) {

  }
}
