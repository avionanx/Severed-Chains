package legend.game;

import legend.core.QueuedModelStandard;
import legend.core.QueuedModelTmd;
import legend.core.gpu.Bpp;
import legend.core.gte.MV;
import legend.core.opengl.MeshObj;
import legend.core.opengl.PolyBuilder;
import legend.core.opengl.Texture;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;

import java.nio.file.Path;

import static legend.core.GameEngine.GPU;
import static legend.core.GameEngine.GTE;
import static legend.core.GameEngine.RENDERER;
import static legend.game.Scus94491BpeSegment_800c.lightColourMatrix_800c3508;
import static legend.game.Scus94491BpeSegment_800c.lightDirectionMatrix_800c34e8;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;

public class Sky {

  private final Texture skyTexture;
  private final MeshObj mesh;

  public Sky(final Path skyPath) {
    this.skyTexture = Texture.png(skyPath);

    final AIScene scene = Assimp.aiImportFile(Path.of("gfx","cube.glb").toString(), 0);

    final AIMesh mesh = AIMesh.create(scene.mMeshes().get(0));

    final AIFace.Buffer faces = mesh.mFaces();
    final AIVector3D.Buffer vertices = mesh.mVertices();
    final AIVector3D.Buffer uvs = mesh.mTextureCoords(0);
    final PolyBuilder builder = new PolyBuilder("Sky Builder", GL_TRIANGLES);
    builder.bpp(Bpp.BITS_24);
    while(faces.hasRemaining()) {
      final AIFace face = faces.get();

      for(int indexIndex = 0; indexIndex < face.mNumIndices(); indexIndex++) {
        final int vertexIndex = face.mIndices().get(indexIndex);
        final AIVector3D vertex = vertices.get(vertexIndex);
        builder.addVertex(vertex.x(), vertex.y(), vertex.z());
        builder.disableBackfaceCulling();
        if(uvs != null) {
          final AIVector3D uv = uvs.get(vertexIndex);
          builder.uv(uv.x(), uv.y());
        }
      }
    }
    Assimp.aiReleaseImport(scene);
    this.mesh = builder.build();
  }
  public void unload() {
    this.skyTexture.delete();
    this.mesh.delete();
  }
  public void render() {
    MV mv = new MV();
    mv.scaling(18000.0f, 12000.0f, 18000.0f);
    RENDERER.queueModel(this.mesh, mv, QueuedModelStandard.class)
      .screenspaceOffset(GPU.getOffsetX() + GTE.getScreenOffsetX() - 184, GPU.getOffsetY() + GTE.getScreenOffsetY() - 120)
      .texture(this.skyTexture)
      ;
  }
}
