package legend.game;

import legend.core.opengl.MeshObj;

public class Object3D {

  private int materialIndex;
  private MeshObj mesh;

  public Object3D() {

  }
  public Object3D setMesh(final MeshObj other) {
    this.mesh = other;
    return this;
  }
  public Object3D clearMesh() {
    this.mesh.delete();
    return this;
  }
  public Object3D setMaterialIndex(final int materialIndex) {
    this.materialIndex = materialIndex;
    return this;
  }
  public int getMaterialIndex() { return this.materialIndex; }
  public MeshObj getMesh() { return this.mesh; }
}
