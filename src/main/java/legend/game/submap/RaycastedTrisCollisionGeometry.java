package legend.game.submap;

import legend.core.MathHelper;
import org.joml.Vector3f;

public class RaycastedTrisCollisionGeometry extends CollisionGeometry {

  private final double EPSILON = 1e-6;

  private boolean playerCollisionLatch_800cbe34;
  private final Vector3f cachedPlayerMovement_800cbd98 = new Vector3f();
  private int collidedPrimitiveIndex_800cbd94;

  private final Vector3f[][] triangles = {
    new Vector3f[] {
      new Vector3f(-256.0f, 0.0f, -256.0f),
      new Vector3f(256.0f, 0.0f, -256.0f),
      new Vector3f(-256.0f, 0.0f, 256.0f)
    },
    new Vector3f[] {
      new Vector3f(256.0f, 0.0f, -256.0f),
      new Vector3f(-256.0f, 0.0f, 256.0f),
      new Vector3f(256.0f, 100.0f, 256.0f)
    }
  };

  @Override
  public int checkCollision(boolean isNpc, Vector3f position, Vector3f movement, boolean updatePlayerRotationInterpolation) {
    if(isNpc) {
      return this.meshIntersection(new Vector3f(position).add(movement).sub(0,128,0), new Vector3f(0.0f, 256.0f, 0.0f), movement);
    }

    if(!this.playerCollisionLatch_800cbe34) {
      this.playerCollisionLatch_800cbe34 = true;

      this.playerRunning = movement.x * movement.x + movement.z * movement.z > 64.0f;

      this.collidedPrimitiveIndex_800cbd94 = this.meshIntersection(new Vector3f(position).add(movement).sub(0,128,0), new Vector3f(0.0f, 256.0f, 0.0f), movement);
      this.cachedPlayerMovement_800cbd98.set(movement);

      if(this.collidedPrimitiveIndex_800cbd94 != -1 && this.playerRotationWasUpdated_800d1a8c == 0 && updatePlayerRotationInterpolation) {
        this.playerRotationWasUpdated_800d1a8c = 2;
        this.playerRotationAfterCollision_800d1a84 = MathHelper.floorMod(MathHelper.atan2(movement.x, movement.z) + MathHelper.PI, MathHelper.TWO_PI);
      }
    } else {
      //LAB_800e8954
      movement.set(this.cachedPlayerMovement_800cbd98);
    }
    return this.collidedPrimitiveIndex_800cbd94;
  }

  @Override
  public int getCollisionAndTransitionInfo(int collisionPrimitiveIndex) {
    return 0;
  }

  @Override
  public void getMiddleOfCollisionPrimitive(int primitiveIndex, Vector3f out) {
    //new Vector3f(this.triangles[primitiveIndex][0]).add(this.triangles[primitiveIndex][1]).add(this.triangles[primitiveIndex][2]).div(3.0f, out);
  }

  @Override
  public int getCollisionPrimitiveAtPoint(float x, float y, float z, boolean checkSteepness, boolean checkY) {
    return this.meshIntersection(new Vector3f(x, y, z).sub(0,128,0), new Vector3f(0.0f, 256.0f, 0.0f), new Vector3f());
  }

  @Override
  public void unloadCollision() {

  }

  @Override
  public void tick() {
    this.playerCollisionLatch_800cbe34 = false;

    if(this.playerRotationWasUpdated_800d1a8c > 0) {
      this.playerRotationWasUpdated_800d1a8c--;
    }
  }

  @Override
  public void setCollisionAndTransitionInfo(int i) {

  }

  private Vector3f triangleIntersection(final Vector3f rayOrigin,final Vector3f rayTarget,final Vector3f vertex0,final Vector3f vertex1,final Vector3f vertex2){
    final Vector3f edge0 = new Vector3f();
    final Vector3f edge1 = new Vector3f();

    final Vector3f normal = new Vector3f();

    vertex1.sub(vertex0, edge0);
    vertex2.sub(vertex0, edge1);

    rayTarget.cross(edge1, normal);
    final float determinant = edge0.dot(normal);

    if(Math.abs(determinant) < this.EPSILON) {
      return null;
    }

    final float inverseDeterminant = 1 / determinant;

    final Vector3f originToV0 = new Vector3f();
    rayOrigin.sub(vertex0,originToV0);
    final float u = inverseDeterminant * originToV0.dot(normal);

    if(u < 0.0f || u > 1.0f) {
      return null;
    }

    final Vector3f perpendicularVector = new Vector3f();
    originToV0.cross(edge0, perpendicularVector);
    final float v = inverseDeterminant * rayTarget.dot(perpendicularVector);

    if(v < 0.0f || u + v > 1.0f) {
      return null;
    }

    final float t = inverseDeterminant * edge1.dot(perpendicularVector);
    if(t > this.EPSILON) {
      return new Vector3f(rayTarget).mul(t).add(rayOrigin);
    } else {
      return null;
    }
  }
  private int meshIntersection(final Vector3f origin, final Vector3f target, final Vector3f movement){
    Vector3f closest = null;
    float min = Float.MAX_VALUE;
    int index = -1;

    for(int i = 0; i < this.triangles.length; i++) {
      final Vector3f point = this.triangleIntersection(origin, target, this.triangles[i][0], this.triangles[i][1], this.triangles[i][2]);
      if(point != null){
        final float distance = point.distance(origin);
        if(distance < min) {
          min = distance;
          closest = point;
          index = i;
        }
      }
    }
    if(closest != null) {
      movement.y = closest.y;
    }
    return index;
  }
}
