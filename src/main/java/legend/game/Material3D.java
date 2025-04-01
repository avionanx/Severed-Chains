package legend.game;

public class Material3D {

  private boolean hasTexture;
  private int textureIndex;
  public Material3D() {}

  public Material3D setTexture(final int index) {
    this.textureIndex = index;
    this.hasTexture = true;
    return this;
  }
  public Material3D clearTexture() {
    this.hasTexture = false;
    return this;
  }
  public boolean hasTexture() { return this.hasTexture; }
  public int getTextureIndex() { return this.textureIndex; }
}
