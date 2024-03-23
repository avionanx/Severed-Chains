package legend.game.submap;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import legend.core.gpu.Rect4i;
import legend.core.gte.MV;
import legend.core.gte.TmdWithId;
import legend.core.memory.types.IntRef;
import legend.core.opengl.Obj;
import legend.core.opengl.TmdObjLoader;

import legend.game.inventory.Equipment;
import legend.game.inventory.EquipmentRegistryEvent;
import legend.game.scripting.ScriptFile;
import legend.game.tim.Tim;
import legend.game.tmd.UvAdjustmentMetrics14;
import legend.game.types.CContainer;
import legend.game.types.EquipmentSlot;
import legend.game.types.GsRVIEW2;
import legend.game.types.Model124;
import legend.game.types.TmdAnimationFile;
import legend.game.unpacker.FileData;
import legend.game.unpacker.Unpacker;
import static legend.game.Scus94491BpeSegment_800b.battleStage_800bb0f4;

import legend.lodmod.LodEquipment;
import legend.lodmod.LodMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.legendofdragoon.modloader.events.EventListener;
import legend.game.BossRush;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static legend.core.Async.allLoaded;
import static legend.core.GameEngine.GPU;
import static legend.core.GameEngine.REGISTRIES;
import static legend.game.Scus94491BpeSegment.orderingTableBits_1f8003c0;
import static legend.game.Scus94491BpeSegment_8003.GsSetSmapRefView2L;
import static legend.game.Scus94491BpeSegment_8003.setProjectionPlaneDistance;
import static legend.game.Scus94491BpeSegment_8005.submapCutBeforeBattle_80052c3c;
import static legend.game.Scus94491BpeSegment_8005.submapEnvState_80052c44;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.Scus94491BpeSegment_800b.previousSubmapCut_800bda08;
import static legend.game.Scus94491BpeSegment_800b.projectionPlaneDistance_800bd810;
import static legend.game.Scus94491BpeSegment_800b.rview2_800bd7e8;
import static legend.game.Scus94491BpeSegment_800b.submapId_800bd808;
import static legend.game.Scus94491BpeSegment_800c.worldToScreenMatrix_800c3548;

public class SubmapModded extends Submap {
  private static final Logger LOGGER = LogManager.getFormatterLogger(SubmapModded.class);
  private final CollisionGeometry collisionGeometry;

  private final int cut;

  private final List<Tim> pxls = new ArrayList<>();

  private final Model124 submapModel_800d4bf8 = new Model124("Submap");

  private final GsRVIEW2 rview2_800cbd10 = new GsRVIEW2();
  private Obj backgroundObj;




  public SubmapModded(final int cut, final CollisionGeometry collisionGeometry) {
    battleStage_800bb0f4 = 17;
    submapId_800bd808 = 0;
    this.cut = cut;
    this.collisionGeometry = collisionGeometry;
    this.loadCollisionAndTransitions();
    BossRush.clearEquipments();
    BossRush.clearItems();
  }

  @Override
  public void loadEnv(Runnable onLoaded) {
    Unpacker.loadFile("../assets/submap/%d/environment/collinfo".formatted(this.cut),fileData1 -> {
      Unpacker.loadFile("../assets/submap/%d/environment/collmesh".formatted(this.cut),fileData2 -> {
        this.collisionGeometry.loadCollision(new TmdWithId("SubmapColl", fileData2),fileData1);
        CameraInfo cam = cameraInfos.get(this.cut);
        updateRview2(cam.viewpoint(),cam.refpoint(),cam.rotation(),cam.projectionDistance());
        submapEnvState_80052c44 = SubmapEnvState.CHECK_TRANSITIONS_1_2;
        onLoaded.run();
      });
    });
  }

  private void updateRview2(final Vector3f viewpoint, final Vector3f refpoint, final int rotation, final int projectionDistance) {
    this.rview2_800cbd10.viewpoint_00.set(viewpoint);
    this.rview2_800cbd10.refpoint_0c.set(refpoint);
    this.rview2_800cbd10.viewpointTwist_18 = (short)rotation << 12;
    this.rview2_800cbd10.super_1c = null;
    projectionPlaneDistance_800bd810 = projectionDistance;

    this.updateCamera();
  }
  private void updateCamera() {

    setProjectionPlaneDistance(projectionPlaneDistance_800bd810);
    GsSetSmapRefView2L(this.rview2_800cbd10);
    this.clearSmallValuesFromMatrix(worldToScreenMatrix_800c3548);
    rview2_800bd7e8.set(this.rview2_800cbd10);
  }
  private void clearSmallValuesFromMatrix(final MV matrix) {
    //LAB_800e72b4
    for(int x = 0; x < 3; x++) {
      //LAB_800e72c4
      for(int y = 0; y < 3; y++) {
        if(Math.abs(matrix.get(x, y)) < 0.015625f) {
          matrix.set(x, y, 0.0f);
        }

        //LAB_800e72e8
      }
    }
  }

  @Override
  public void loadAssets(Runnable onLoaded) {
    LOGGER.info("Loading submap cut %d assets", this.cut);
    

      final AtomicInteger loadedCount = new AtomicInteger();
      final int expectedCount = 1;

      // Load sobj assets
      final List<FileData> assets = new ArrayList<>();
      final List<FileData> scripts = new ArrayList<>();
      final List<FileData> textures = new ArrayList<>();
      final AtomicInteger assetsCount = new AtomicInteger();

      final Runnable prepareSobjs = () -> this.prepareSobjs(assets, scripts, textures);
      final Runnable prepareSobjsAndComplete = () -> allLoaded(loadedCount, expectedCount, prepareSobjs, onLoaded);
      Unpacker.loadDirectory("../assets/submap/%d/assets".formatted(this.cut),files -> allLoaded(assetsCount, 3, () -> assets.addAll(files), prepareSobjsAndComplete));
      Unpacker.loadDirectory("../assets/submap/%d/scripts".formatted(this.cut),files -> allLoaded(assetsCount, 3, () -> scripts.addAll(files), prepareSobjsAndComplete));
      //loadDrgnDir(drgnIndex.get() + 2, fileIndex.get() + 1, files -> allLoaded(assetsCount, 3, () -> assets.addAll(files), prepareSobjsAndComplete));
      //loadDrgnDir(drgnIndex.get() + 2, fileIndex.get() + 2, files -> allLoaded(assetsCount, 3, () -> scripts.addAll(files), prepareSobjsAndComplete));
      Unpacker.loadDirectory("../assets/submap/%d/assets/textures".formatted(this.cut), files -> allLoaded(assetsCount, 3, () -> textures.addAll(files), prepareSobjsAndComplete));


  }
  private void prepareSobjs(final List<FileData> assets, final List<FileData> scripts, final List<FileData> textures) {
    LOGGER.info("Submap cut %d preparing sobjs", this.cut);

    final int objCount = scripts.size() - 2;

    this.script = new ScriptFile("Submap controller", scripts.get(0).getBytes());

    for(int objIndex = 0; objIndex < objCount; objIndex++) {
      final byte[] scriptData = scripts.get(objIndex + 1).getBytes();

      final FileData submapModel = assets.get(objIndex * 33);

      final IntRef drgnIndex = new IntRef();
      final IntRef fileIndex = new IntRef();


      final SubmapObject obj = new SubmapObject();
      obj.script = new ScriptFile("Submap object %d (DRGN%d/%d/%d)".formatted(objIndex, drgnIndex.get(), fileIndex.get() + 2, objIndex + 1), scriptData);

      if(submapModel.hasVirtualSize() && submapModel.real()) {
        obj.model = new CContainer("Submap object %d (DRGN%d/%d/%d)".formatted(objIndex, drgnIndex.get(), fileIndex.get() + 1, objIndex * 33), new FileData(submapModel.getBytes()));
      } else {
        obj.model = null;
      }

      for(int animIndex = objIndex * 33 + 1; animIndex < (objIndex + 1) * 33; animIndex++) {
        final FileData data = assets.get(animIndex);

        // This is a stupid fix for a stupid retail bug where almost all
        // sobj animations in DRGN24.938 are symlinked to a PXL file
        // GH#292
        if(data.readInt(0) == 0x11) {
          obj.animations.add(null);
          continue;
        }

        obj.animations.add(new TmdAnimationFile(data));
      }

      this.objects.add(obj);
    }

    // Get models that are symlinked
    for(int objIndex = 0; objIndex < objCount; objIndex++) {
      final SubmapObject obj = this.objects.get(objIndex);

      if(obj.model == null) {
        final FileData submapModel = assets.get(objIndex * 33);

        obj.model = this.objects.get(submapModel.realFileIndex() / 33).model;
      }
    }

    for(final FileData file : textures) {
      if(file != null && file.real()) {
        this.pxls.add(new Tim(file));
      } else {
        this.pxls.add(null);
      }
    }

    this.loadTextures();
  }
  private void loadTextures() {
    this.uvAdjustments.clear();

    final boolean[] usedSlots = new boolean[this.pxls.size() * 2];

    outer:
    for(int pxlIndex = 0; pxlIndex < this.pxls.size(); pxlIndex++) {
      final Tim tim = this.pxls.get(pxlIndex);

      if(tim != null) {
        final Rect4i imageRect = tim.getImageRect();
        final Rect4i clutRect = tim.getClutRect();

        final int neededSlots = imageRect.w / 16;

        // We increment by neededSlots so that wide textures only land on even slots
        for(int slotIndex = 0; slotIndex < 20; slotIndex += neededSlots) {
          boolean free = true;
          for(int i = 0; i < neededSlots; i++) {
            if(usedSlots[slotIndex + i]) {
              free = false;
              break;
            }
          }

          if(free) {
            for(int i = 0; i < neededSlots; i++) {
              usedSlots[slotIndex + i] = true;
            }

            final int x = 576 + slotIndex % 12 * 16;
            final int y = 256 + slotIndex / 12 * 128;

            imageRect.x = x;
            imageRect.y = y;
            clutRect.x = x;
            clutRect.y = y + imageRect.h;

            GPU.uploadData15(imageRect, tim.getImageData());
            GPU.uploadData15(clutRect, tim.getClutData());

            this.uvAdjustments.add(new UvAdjustmentMetrics14(pxlIndex + 1, x, y));
            continue outer;
          }
        }

        throw new RuntimeException("Failed to find available texture slot for sobj texture " + pxlIndex);
      } else {
        this.uvAdjustments.add(UvAdjustmentMetrics14.NONE);
      }
    }


  }

  @Override
  public void restoreAssets() {
    this.loadTextures();
  }

  @Override
  public void loadMusicAndSounds() {

  }

  @Override
  public void startMusic() {

  }

  @Override
  public void loadMapTransitionData(MapTransitionData4c transitionData) {
    transitionData.clear();

  }

  @Override
  public void prepareEnv() {

  }

  @Override
  public void prepareSobjModel(SubmapObject210 sobj) {
    TmdObjLoader.fromModel("SobjModel (index " + sobj.sobjIndex_12e + ')', sobj.model_00);
  }

  @Override
  public void finishLoading() {

  }

  @Override
  public void draw() {
    /*
    MV mv = new MV();
    mv.identity();
    mv.scale(1000);
    RENDERER.queueModel(this.map,mv);
     */
  }


  @Override
  public void drawEnv(MV[] sobjMatrices) {
    //requires some file from drgn0, not used rn and commented out
    //this.animateAndRenderSubmapModel(this.submapCutMatrix_800d4bb0);
    final float[] sobjZs = new float[sobjMatrices.length];
    for(int i = 0; i < sobjMatrices.length; i++) {
      sobjZs[i] = (worldToScreenMatrix_800c3548.m02 * sobjMatrices[i].transfer.x +
        worldToScreenMatrix_800c3548.m12 * sobjMatrices[i].transfer.y +
        worldToScreenMatrix_800c3548.m22 * sobjMatrices[i].transfer.z + worldToScreenMatrix_800c3548.transfer.z) / (1 << 16 - orderingTableBits_1f8003c0);
    }

  }
  @Override
  public void unload() {
    previousSubmapCut_800bda08 = this.cut;
    this.submapModel_800d4bf8.deleteModelParts();

    if(this.backgroundObj != null) {
      this.backgroundObj.delete();
      this.backgroundObj = null;
    }
  }
  private void loadCollisionAndTransitions(){
    this.collisionGeometry.clearCollisionAndTransitionInfo();

  }
  @Override
  public void calcGoodScreenOffset(float x, float y, Vector2f out) {
    if(x < -80) {
      out.x -= 80 + x;
      //LAB_800e7f80
    } else if(x > 80) {
      //LAB_800e7f9c
      out.x += 80 - x;
    }

    //LAB_800e7fa8
    if(y < -40) {
      out.y -= 40 + y;
      //LAB_800e7fbc
    } else if(y > 40) {
      //LAB_800e7fd4
      out.y += 40 - y;
    }

  }

  @Override
  public int getEncounterRate() {
    return 0;
  }

  @Override
  public void generateEncounter() {

  }

  @Override
  public void storeStateBeforeBattle() {
    submapCutBeforeBattle_80052c3c = this.cut;
  }

  @Override
  public boolean isReturningToSameMapAfterBattle() {
    return false;
  }

  private static final Int2ObjectMap<CameraInfo> cameraInfos;
  static {
    cameraInfos = new Int2ObjectOpenHashMap<>();
    cameraInfos.put(2000, new CameraInfo(new Vector3f( -1.400E+1f , -1.575E+3f, -1.226E+3f),new Vector3f( -1.400E+1f, -1.471E+3f, -1.141E+3f), 0, 1172));
  }


}