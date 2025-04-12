package legend.game;

import legend.core.QueuedModelTmd;
import legend.core.gpu.Bpp;
import legend.core.opengl.PolyBuilder;
import legend.core.opengl.Texture;
import legend.game.submap.RaycastedTrisCollisionGeometry;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMaterialProperty;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static legend.core.GameEngine.GPU;
import static legend.core.GameEngine.GTE;
import static legend.core.GameEngine.RENDERER;
import static legend.game.Scus94491BpeSegment_800c.lightColourMatrix_800c3508;
import static legend.game.Scus94491BpeSegment_800c.lightDirectionMatrix_800c34e8;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;

public class Scene {

  private final ArrayList<Object3D> objects = new ArrayList<>();
  private final ArrayList<Texture> textures = new ArrayList<>();
  private final ArrayList<Material3D> materials = new ArrayList<>();

  private Sky sky;

  public Scene(final Path path, final RaycastedTrisCollisionGeometry collisionGeometryPtr) {
    final AIScene scene = Assimp.aiImportFile(path.resolve("map.glb").toString(), 0);

    for(int i = 0; i < scene.mNumMaterials(); i++) {
      final Material3D material3D = new Material3D();

      final AIMaterial material = AIMaterial.create(scene.mMaterials().get(i));
      final AIString texturePath = AIString.calloc();
      final int status = Assimp.aiGetMaterialTexture(material, Assimp.aiTextureType_DIFFUSE, 0, texturePath, (IntBuffer)null, null, null, null, null, null);

      if(status == 0) {
        final int textureIndex = Integer.parseInt(texturePath.dataString().substring(1));
        material3D.setTexture(textureIndex);
      }
      texturePath.free();
      final PointerBuffer materialPropertyBuffer = material.mProperties();
      while(materialPropertyBuffer.hasRemaining()) {
        final AIMaterialProperty property = AIMaterialProperty.create(materialPropertyBuffer.get());
        if("$clr.diffuse".equals(property.mKey().dataString())) {
          final FloatBuffer colorBuffer = property.mData().asFloatBuffer();
          material3D.color = new Vector3f(colorBuffer.get(), colorBuffer.get(), colorBuffer.get());
          //material3D.color.div(2.0f);
          material3D.alpha = colorBuffer.get();
        }
      }


      this.materials.add(material3D);
    }
    this.loadSkybox(path.resolve("sky.png"));
    this.getTextures(scene);
    this.loadCollision(collisionGeometryPtr, scene.mRootNode(), scene);

    for(int meshIndex = 0; meshIndex < scene.mNumMeshes(); meshIndex++) {
      final PolyBuilder builder = new PolyBuilder("MapBuilder", GL_TRIANGLES);
      final Object3D object = new Object3D();

      final AIMesh mesh = AIMesh.create(scene.mMeshes().get(meshIndex));
      object.setMaterialIndex(mesh.mMaterialIndex());

      if(this.materials.get(object.getMaterialIndex()).hasTexture()) {
        builder.bpp(Bpp.BITS_24);
      }

      final AIFace.Buffer faces = mesh.mFaces();
      final AIVector3D.Buffer vertices = mesh.mVertices();
      final AIVector3D.Buffer normals = mesh.mNormals();
      final AIVector3D.Buffer uvs = mesh.mTextureCoords(0);

      while(faces.hasRemaining()) {
        final AIFace face = faces.get();

        for(int indexIndex = 0; indexIndex < face.mNumIndices(); indexIndex++) {
          final int vertexIndex = face.mIndices().get(indexIndex);
          final AIVector3D vertex = vertices.get(vertexIndex);
          final AIVector3D normal = normals.get(vertexIndex);
          builder.addVertex(vertex.x(), vertex.y(), vertex.z());
          builder.normal(normal.x(), normal.y(), normal.z());
          builder.disableBackfaceCulling();
          builder.rgb(this.materials.get(object.getMaterialIndex()).color);
          /*
          if(colours != null) {
            final AIColor4D colour = colours.get(vertexIndex);
            builder.rgb(colour.r() / 2, colour.g() / 2, colour.b() / 2);
          } else {
            builder.monochrome(0.5f);
          }
          */
          if(this.materials.get(object.getMaterialIndex()).hasTexture() && uvs != null) {
            final AIVector3D uv = uvs.get(vertexIndex);
            builder.uv(uv.x(), uv.y());
          }
        }
      }

      object.setMesh(builder.build());
      this.objects.add(object);
    }


    Assimp.aiReleaseImport(scene);
  }

  private void loadSkybox(final Path path) {
    if(!Files.exists(path)) return;
    this.sky = new Sky(path);
  }

  private void getTextures(final AIScene scene) {
    final int textureCount = scene.mNumTextures();

    for(int i = 0; i < textureCount; i++) {
      final AITexture texture = AITexture.create(scene.mTextures().get(i));

      final IntBuffer width = BufferUtils.createIntBuffer(1);
      final IntBuffer height = BufferUtils.createIntBuffer(1);
      final IntBuffer channels = BufferUtils.createIntBuffer(1);
      final ByteBuffer img = STBImage.stbi_load_from_memory(texture.pcDataCompressed(), width, height, channels, 3);
      this.textures.add(Texture.create(builder -> builder.data(img, width.get(), height.get())));
      STBImage.stbi_image_free(img);
    }
  }

  public void unload() {
    this.objects.forEach(Object3D::clearMesh);
    this.textures.forEach(Texture::delete);
    this.sky.unload();
  }

  private void loadCollision(final RaycastedTrisCollisionGeometry collisionGeometryPtr, final AINode root, final AIScene scene) {
    final AINode collision = findNode(root, "collision");
    if(collision == null) { return; }

    final int meshCount = collision.mNumMeshes();
    final IntBuffer meshPtr = collision.mMeshes();

    final ArrayList<Vector3f> triangles = new ArrayList<>();

    for(int i = 0; i < meshCount; i++) {
      final AIMesh mesh = AIMesh.create(scene.mMeshes().get(meshPtr.get(i)));

      final AIVector3D.Buffer vertices = mesh.mVertices();
      final AIFace.Buffer faces = mesh.mFaces();
      while(faces.hasRemaining()) {
        final var faceIndices = faces.get().mIndices();
        while(faceIndices.hasRemaining()) {
          final var vertexData = vertices.get(faceIndices.get());
          triangles.add(new Vector3f(vertexData.x(), vertexData.y(), vertexData.z()));
        }
      }
    }
    collisionGeometryPtr.vertices = triangles;
  }
  public void render() {
    for(final Object3D object3D : this.objects) {
      final var queued = RENDERER.queueModel(object3D.getMesh(), QueuedModelTmd.class)
        .screenspaceOffset(GPU.getOffsetX() + GTE.getScreenOffsetX() - 184, GPU.getOffsetY() + GTE.getScreenOffsetY() - 120)
        .lightDirection(lightDirectionMatrix_800c34e8)
        .lightColour(lightColourMatrix_800c3508)
        .backgroundColour(GTE.backgroundColour)
      ;
      if(this.materials.get(object3D.getMaterialIndex()).hasTexture()) {
        queued.texture(this.textures.get(this.materials.get(object3D.getMaterialIndex()).getTextureIndex()));
      }
    }
    this.sky.render();
  }

  private static AINode findNode(final AINode node, final String name) {
    if(node.mName().dataString().equals(name)) {
      return node;
    } else {
      final int childCount = node.mNumChildren();
      final PointerBuffer childrenPtr = node.mChildren();

      for(int i = 0; i < childCount; i++) {
        final AINode next = findNode(AINode.create(childrenPtr.get(i)), name);
        if(next != null) { return next; }
      }

      return null;
    }
  }
}
