package legend.game.submap;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import legend.core.MathHelper;
import legend.core.RenderEngine;
import legend.core.gpu.Bpp;
import legend.core.gpu.GpuCommandCopyVramToVram;
import legend.core.gpu.Rect4i;
import legend.core.gpu.Renderable;
import legend.core.gpu.VramTextureLoader;
import legend.core.gpu.VramTextureSingle;
import legend.core.gte.GsCOORDINATE2;
import legend.core.gte.MV;
import legend.core.gte.ModelPart10;
import legend.core.gte.TmdWithId;
import legend.core.gte.Transforms;
import legend.core.memory.types.IntRef;
import legend.core.opengl.MeshObj;
import legend.core.opengl.Obj;
import legend.core.opengl.PolyBuilder;
import legend.core.opengl.QuadBuilder;
import legend.core.opengl.Texture;
import legend.core.opengl.TmdObjLoader;
import legend.game.combat.environment.BattleStage;
import legend.game.combat.environment.BattleStageDarkening1800;
import legend.game.modding.events.submap.SubmapEnvironmentTextureEvent;
import legend.game.scripting.ScriptFile;
import legend.game.tim.Tim;
import legend.game.tmd.Renderer;
import legend.game.tmd.UvAdjustmentMetrics14;
import legend.game.types.CContainer;
import legend.game.types.GsRVIEW2;
import legend.game.types.Keyframe0c;
import legend.game.types.McqHeader;
import legend.game.types.Model124;
import legend.game.types.TmdAnimationFile;
import legend.game.types.Translucency;
import legend.game.unpacker.FileData;
import legend.game.unpacker.Unpacker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static legend.core.Async.allLoaded;
import static legend.core.GameEngine.EVENTS;
import static legend.core.GameEngine.GPU;
import static legend.core.GameEngine.GTE;
import static legend.core.GameEngine.RENDERER;
import static legend.game.Scus94491BpeSegment.battlePreloadedEntities_1f8003f4;
import static legend.game.Scus94491BpeSegment.loadDrgnDir;
import static legend.game.Scus94491BpeSegment.loadMcq;
import static legend.game.Scus94491BpeSegment.orderingTableBits_1f8003c0;
import static legend.game.Scus94491BpeSegment.tmdGp0Tpage_1f8003ec;
import static legend.game.Scus94491BpeSegment.zOffset_1f8003e8;
import static legend.game.Scus94491BpeSegment_8002.animateModel;
import static legend.game.Scus94491BpeSegment_8002.applyModelRotationAndScale;
import static legend.game.Scus94491BpeSegment_8002.initModel;
import static legend.game.Scus94491BpeSegment_8002.initObjTable2;
import static legend.game.Scus94491BpeSegment_8002.prepareObjTable2;
import static legend.game.Scus94491BpeSegment_8003.GsGetLw;
import static legend.game.Scus94491BpeSegment_8003.GsGetLws;
import static legend.game.Scus94491BpeSegment_8003.GsInitCoordinate2;
import static legend.game.Scus94491BpeSegment_8003.GsSetLightMatrix;
import static legend.game.Scus94491BpeSegment_8003.GsSetSmapRefView2L;
import static legend.game.Scus94491BpeSegment_8003.setProjectionPlaneDistance;
import static legend.game.Scus94491BpeSegment_8005.submapCutBeforeBattle_80052c3c;
import static legend.game.Scus94491BpeSegment_8005.submapEnvState_80052c44;
import static legend.game.Scus94491BpeSegment_8007.vsyncMode_8007a3b8;
import static legend.game.Scus94491BpeSegment_800b.battleFlags_800bc960;
import static legend.game.Scus94491BpeSegment_800b.drgnBinIndex_800bc058;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.Scus94491BpeSegment_800b.previousSubmapCut_800bda08;
import static legend.game.Scus94491BpeSegment_800b.projectionPlaneDistance_800bd810;
import static legend.game.Scus94491BpeSegment_800b.rview2_800bd7e8;
import static legend.game.Scus94491BpeSegment_800b.stage_800bda0c;
import static legend.game.Scus94491BpeSegment_800b.submapId_800bd808;
import static legend.game.Scus94491BpeSegment_800c.lightColourMatrix_800c3508;
import static legend.game.Scus94491BpeSegment_800c.lightDirectionMatrix_800c34e8;
import static legend.game.Scus94491BpeSegment_800c.worldToScreenMatrix_800c3548;
import static legend.game.combat.Battle.stageDarkening_800c6958;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL12C.GL_UNSIGNED_INT_8_8_8_8_REV;

public class SubmapModded extends Submap {
  private static final Logger LOGGER = LogManager.getFormatterLogger(SubmapModded.class);
  private final CollisionGeometry collisionGeometry;

  private final int cut;

  private final List<Tim> pxls = new ArrayList<>();

  private final Model124 submapModel_800d4bf8 = new Model124("Submap");

  private final GsRVIEW2 rview2_800cbd10 = new GsRVIEW2();
  private Obj backgroundObj;

  private QuadBuilder quadBuilder;
  private MeshObj arrow;

  private Texture arrowTexture;

  private ArrayList<Note> noteList;
  private int noteProgressPointer;
  private ArrayList<LaunchedNote> launchedNotes;

  private int frameIndex = 0;

  public static boolean guitarMode = true;

  public SubmapModded(final int cut, final CollisionGeometry collisionGeometry) {
    this.cut = cut;
    this.collisionGeometry = collisionGeometry;
    this.loadCollisionAndTransitions();
    submapId_800bd808 = 57;
    if(this.cut == 1002){
      this.quadBuilder = new QuadBuilder("SoulIndicatorMaker");
      this.arrowTexture = Texture.png(Path.of("gfx", "ddr", "arrow.png"));
      this.arrow = quadBuilder
        .bpp(Bpp.BITS_24)
        .size(1.0f, 1.0f)
        .uvSize(1.0f, 1.0f)
        .pos(-0.5f, -0.5f, 0.0f)
        .build();
      this.launchedNotes = new ArrayList<>();
      this.noteProgressPointer = 0;
      this.noteList = new ArrayList<>();
      this.loadNotes();
    }
  }

  int getTrackIndexFromScriptFlags(){
    final int f1 = gameState_800babc8.scriptFlags2_bc.get(848) ? 4 : 0;
    final int f2 = gameState_800babc8.scriptFlags2_bc.get(849) ? 2 : 0;
    final int f3 = gameState_800babc8.scriptFlags2_bc.get(850) ? 1 : 0;
    return f1 + f2 + f3;
  }
  private void loadNotes(){
    final int trackIndex = getTrackIndexFromScriptFlags();
    try (BufferedReader br = new BufferedReader(new FileReader("assets/beatmaps/" + trackIndex + ".txt"))){
      String line;
      while((line = br.readLine()) != null){
        String[] parts = line.split(",");
        this.noteList.add(new Note(NoteType.values()[Integer.valueOf(parts[0])],Integer.valueOf(parts[1])));
      }
    } catch(IOException e){
      e.printStackTrace();
    }
    /*
    switch(trackIndex){
      case 0:
        this.noteList.add(new Note(NoteType.DOWN, 255));
        this.noteList.add(new Note(NoteType.DOWN, 267));
        this.noteList.add(new Note(NoteType.UP, 372));
        this.noteList.add(new Note(NoteType.UP, 384));
        this.noteList.add(new Note(NoteType.RIGHT, 487));
        this.noteList.add(new Note(NoteType.RIGHT, 500));
        this.noteList.add(new Note(NoteType.DOWN, 600));
        this.noteList.add(new Note(NoteType.DOWN, 612));
        this.noteList.add(new Note(NoteType.DOWN, 675));
        this.noteList.add(new Note(NoteType.DOWN, 695));
        this.noteList.add(new Note(NoteType.UP, 715));
        this.noteList.add(new Note(NoteType.UP, 727));
        this.noteList.add(new Note(NoteType.RIGHT, 832));
        this.noteList.add(new Note(NoteType.RIGHT, 844));
        this.noteList.add(new Note(NoteType.UP, 947));
        this.noteList.add(new Note(NoteType.UP, 960));
        this.noteList.add(new Note(NoteType.RIGHT, 1060));
        this.noteList.add(new Note(NoteType.RIGHT, 1072));
        this.noteList.add(new Note(NoteType.UP, 1127));
        this.noteList.add(new Note(NoteType.UP, 1152));
        this.noteList.add(new Note(NoteType.LEFT, 1167));
        this.noteList.add(new Note(NoteType.RIGHT, 1167));
        this.noteList.add(new Note(NoteType.RIGHT, 1180));
        this.noteList.add(new Note(NoteType.DOWN, 1224));
        this.noteList.add(new Note(NoteType.LEFT, 1287));
        this.noteList.add(new Note(NoteType.UP, 1287));
        this.noteList.add(new Note(NoteType.UP, 1304));
        this.noteList.add(new Note(NoteType.DOWN, 1340));
        this.noteList.add(new Note(NoteType.LEFT, 1407));
        this.noteList.add(new Note(NoteType.RIGHT, 1407));
        this.noteList.add(new Note(NoteType.RIGHT, 1420));
        this.noteList.add(new Note(NoteType.DOWN, 1464));
        this.noteList.add(new Note(NoteType.LEFT, 1524));
        this.noteList.add(new Note(NoteType.UP, 1524));
        this.noteList.add(new Note(NoteType.UP, 1535));
        this.noteList.add(new Note(NoteType.DOWN, 1575));
        this.noteList.add(new Note(NoteType.RIGHT, 1592));
        this.noteList.add(new Note(NoteType.RIGHT, 1615));
        this.noteList.add(new Note(NoteType.UP, 1627));
        this.noteList.add(new Note(NoteType.LEFT, 1640));
        this.noteList.add(new Note(NoteType.UP, 1640));
        this.noteList.add(new Note(NoteType.DOWN, 1684));
        this.noteList.add(new Note(NoteType.LEFT, 1752));
        this.noteList.add(new Note(NoteType.UP, 1752));
        this.noteList.add(new Note(NoteType.UP, 1763));
        this.noteList.add(new Note(NoteType.DOWN, 1807));
        this.noteList.add(new Note(NoteType.LEFT, 1860));
        this.noteList.add(new Note(NoteType.UP, 1860));
        this.noteList.add(new Note(NoteType.UP, 1872));
        this.noteList.add(new Note(NoteType.DOWN, 1920));
        this.noteList.add(new Note(NoteType.UP, 1972));
        this.noteList.add(new Note(NoteType.RIGHT, 1972));
        this.noteList.add(new Note(NoteType.RIGHT, 1984));
        this.noteList.add(new Note(NoteType.DOWN, 2024));
        this.noteList.add(new Note(NoteType.RIGHT, 2044));
        this.noteList.add(new Note(NoteType.LEFT, 2064));
        this.noteList.add(new Note(NoteType.RIGHT, 2072));
        this.noteList.add(new Note(NoteType.DOWN, 2087));
        this.noteList.add(new Note(NoteType.UP, 2087));
        this.noteList.add(new Note(NoteType.UP, 2100));
        this.noteList.add(new Note(NoteType.LEFT, 2152));
        this.noteList.add(new Note(NoteType.LEFT, 2200));
        this.noteList.add(new Note(NoteType.UP, 2200));
        this.noteList.add(new Note(NoteType.DOWN, 2212));
        this.noteList.add(new Note(NoteType.UP, 2212));
        this.noteList.add(new Note(NoteType.LEFT, 2260));
        this.noteList.add(new Note(NoteType.UP, 2324));
        this.noteList.add(new Note(NoteType.RIGHT, 2324));
        this.noteList.add(new Note(NoteType.DOWN, 2335));
        this.noteList.add(new Note(NoteType.RIGHT, 2336));
        this.noteList.add(new Note(NoteType.LEFT, 2380));
        this.noteList.add(new Note(NoteType.DOWN, 2435));
        this.noteList.add(new Note(NoteType.UP, 2435));
        this.noteList.add(new Note(NoteType.UP, 2447));
        this.noteList.add(new Note(NoteType.LEFT, 2487));
        this.noteList.add(new Note(NoteType.UP, 2504));
        this.noteList.add(new Note(NoteType.UP, 2535));
        this.noteList.add(new Note(NoteType.DOWN, 2547));
        this.noteList.add(new Note(NoteType.RIGHT, 2547));
        this.noteList.add(new Note(NoteType.LEFT, 2564));
        this.noteList.add(new Note(NoteType.RIGHT, 2564));
        this.noteList.add(new Note(NoteType.DOWN, 2604));
        this.noteList.add(new Note(NoteType.LEFT, 2664));
        this.noteList.add(new Note(NoteType.UP, 2664));
        this.noteList.add(new Note(NoteType.UP, 2675));
        this.noteList.add(new Note(NoteType.DOWN, 2724));
        this.noteList.add(new Note(NoteType.DOWN, 2780));
        this.noteList.add(new Note(NoteType.UP, 2780));
        this.noteList.add(new Note(NoteType.UP, 2792));
        this.noteList.add(new Note(NoteType.RIGHT, 2792));
        this.noteList.add(new Note(NoteType.LEFT, 2835));
        this.noteList.add(new Note(NoteType.UP, 2887));
        this.noteList.add(new Note(NoteType.DOWN, 2900));
        this.noteList.add(new Note(NoteType.UP, 2900));
        this.noteList.add(new Note(NoteType.LEFT, 2952));
        this.noteList.add(new Note(NoteType.RIGHT, 2964));
        this.noteList.add(new Note(NoteType.RIGHT, 2992));
        this.noteList.add(new Note(NoteType.UP, 3012));
        this.noteList.add(new Note(NoteType.RIGHT, 3012));
        this.noteList.add(new Note(NoteType.LEFT, 3024));
        this.noteList.add(new Note(NoteType.UP, 3024));
        this.noteList.add(new Note(NoteType.DOWN, 3060));
        this.noteList.add(new Note(NoteType.LEFT, 3120));
        this.noteList.add(new Note(NoteType.UP, 3120));
        this.noteList.add(new Note(NoteType.UP, 3132));
        this.noteList.add(new Note(NoteType.DOWN, 3180));
        this.noteList.add(new Note(NoteType.LEFT, 3240));
        this.noteList.add(new Note(NoteType.UP, 3240));
        this.noteList.add(new Note(NoteType.DOWN, 3252));
        this.noteList.add(new Note(NoteType.UP, 3252));
        this.noteList.add(new Note(NoteType.LEFT, 3295));
        this.noteList.add(new Note(NoteType.DOWN, 3352));
        this.noteList.add(new Note(NoteType.UP, 3352));
        this.noteList.add(new Note(NoteType.UP, 3364));
        this.noteList.add(new Note(NoteType.LEFT, 3404));
        this.noteList.add(new Note(NoteType.RIGHT, 3424));
        this.noteList.add(new Note(NoteType.RIGHT, 3447));
        this.noteList.add(new Note(NoteType.UP, 3464));
        this.noteList.add(new Note(NoteType.DOWN, 3475));
        this.noteList.add(new Note(NoteType.UP, 3476));
        this.noteList.add(new Note(NoteType.LEFT, 3524));
        this.noteList.add(new Note(NoteType.DOWN, 3580));
        this.noteList.add(new Note(NoteType.UP, 3580));
        this.noteList.add(new Note(NoteType.UP, 3592));
        this.noteList.add(new Note(NoteType.LEFT, 3640));
        this.noteList.add(new Note(NoteType.DOWN, 3700));
        this.noteList.add(new Note(NoteType.UP, 3700));
        this.noteList.add(new Note(NoteType.UP, 3712));
        this.noteList.add(new Note(NoteType.LEFT, 3755));
        this.noteList.add(new Note(NoteType.UP, 3807));
        this.noteList.add(new Note(NoteType.RIGHT, 3807));
        this.noteList.add(new Note(NoteType.UP, 3820));
        this.noteList.add(new Note(NoteType.DOWN, 3855));
        this.noteList.add(new Note(NoteType.LEFT, 3900));
        this.noteList.add(new Note(NoteType.DOWN, 3927));
        this.noteList.add(new Note(NoteType.RIGHT, 3927));
        this.noteList.add(new Note(NoteType.DOWN, 3940));
        this.noteList.add(new Note(NoteType.RIGHT, 3940));
        this.noteList.add(new Note(NoteType.UP, 4040));
        this.noteList.add(new Note(NoteType.UP, 4052));
        this.noteList.add(new Note(NoteType.LEFT, 4155));
        this.noteList.add(new Note(NoteType.UP, 4155));
        this.noteList.add(new Note(NoteType.LEFT, 4167));
        this.noteList.add(new Note(NoteType.UP, 4167));
        this.noteList.add(new Note(NoteType.UP, 4272));
        this.noteList.add(new Note(NoteType.UP, 4284));
        this.noteList.add(new Note(NoteType.RIGHT, 4344));
        this.noteList.add(new Note(NoteType.RIGHT, 4372));
        this.noteList.add(new Note(NoteType.DOWN, 4387));
        this.noteList.add(new Note(NoteType.UP, 4387));
        this.noteList.add(new Note(NoteType.DOWN, 4400));
        this.noteList.add(new Note(NoteType.UP, 4400));
        this.noteList.add(new Note(NoteType.UP, 4500));
        this.noteList.add(new Note(NoteType.UP, 4512));
        this.noteList.add(new Note(NoteType.DOWN, 4612));
        this.noteList.add(new Note(NoteType.RIGHT, 4612));
        this.noteList.add(new Note(NoteType.DOWN, 4624));
        this.noteList.add(new Note(NoteType.RIGHT, 4624));
        this.noteList.add(new Note(NoteType.UP, 4732));
        this.noteList.add(new Note(NoteType.RIGHT, 4732));
        this.noteList.add(new Note(NoteType.LEFT, 4744));
        this.noteList.add(new Note(NoteType.UP, 4744));
        this.noteList.add(new Note(NoteType.DOWN, 4767));
        this.noteList.add(new Note(NoteType.RIGHT, 4795));
        this.noteList.add(new Note(NoteType.LEFT, 4807));
        this.noteList.add(new Note(NoteType.RIGHT, 4824));
        this.noteList.add(new Note(NoteType.UP, 4840));
        this.noteList.add(new Note(NoteType.UP, 4852));
        break;
      case 1:
        this.noteList.add(new Note(NoteType.UP, 360));
        this.noteList.add(new Note(NoteType.RIGHT, 360));
        this.noteList.add(new Note(NoteType.RIGHT, 375));
        this.noteList.add(new Note(NoteType.UP, 390));
        this.noteList.add(new Note(NoteType.DOWN, 405));
        this.noteList.add(new Note(NoteType.LEFT, 420));
        this.noteList.add(new Note(NoteType.LEFT, 465));
        this.noteList.add(new Note(NoteType.DOWN, 480));
        this.noteList.add(new Note(NoteType.LEFT, 502));
        this.noteList.add(new Note(NoteType.RIGHT, 502));
        this.noteList.add(new Note(NoteType.DOWN, 525));
        this.noteList.add(new Note(NoteType.UP, 540));
        this.noteList.add(new Note(NoteType.LEFT, 555));
        this.noteList.add(new Note(NoteType.DOWN, 570));
        this.noteList.add(new Note(NoteType.UP, 585));
        this.noteList.add(new Note(NoteType.UP, 600));
        this.noteList.add(new Note(NoteType.DOWN, 615));
        this.noteList.add(new Note(NoteType.LEFT, 630));
        this.noteList.add(new Note(NoteType.DOWN, 645));
        this.noteList.add(new Note(NoteType.UP, 645));
        this.noteList.add(new Note(NoteType.RIGHT, 660));
        this.noteList.add(new Note(NoteType.UP, 675));
        this.noteList.add(new Note(NoteType.DOWN, 690));
        this.noteList.add(new Note(NoteType.LEFT, 705));
        this.noteList.add(new Note(NoteType.DOWN, 720));
        this.noteList.add(new Note(NoteType.LEFT, 750));
        this.noteList.add(new Note(NoteType.DOWN, 765));
        this.noteList.add(new Note(NoteType.LEFT, 787));
        this.noteList.add(new Note(NoteType.RIGHT, 787));
        this.noteList.add(new Note(NoteType.DOWN, 810));
        this.noteList.add(new Note(NoteType.UP, 825));
        this.noteList.add(new Note(NoteType.LEFT, 840));
        this.noteList.add(new Note(NoteType.DOWN, 855));
        this.noteList.add(new Note(NoteType.UP, 870));
        this.noteList.add(new Note(NoteType.UP, 885));
        this.noteList.add(new Note(NoteType.DOWN, 900));
        this.noteList.add(new Note(NoteType.LEFT, 915));
        this.noteList.add(new Note(NoteType.UP, 945));
        this.noteList.add(new Note(NoteType.RIGHT, 945));
        this.noteList.add(new Note(NoteType.RIGHT, 960));
        this.noteList.add(new Note(NoteType.UP, 975));
        this.noteList.add(new Note(NoteType.DOWN, 990));
        this.noteList.add(new Note(NoteType.LEFT, 1005));
        this.noteList.add(new Note(NoteType.LEFT, 1050));
        this.noteList.add(new Note(NoteType.DOWN, 1065));
        this.noteList.add(new Note(NoteType.LEFT, 1087));
        this.noteList.add(new Note(NoteType.RIGHT, 1087));
        this.noteList.add(new Note(NoteType.DOWN, 1110));
        this.noteList.add(new Note(NoteType.UP, 1125));
        this.noteList.add(new Note(NoteType.LEFT, 1140));
        this.noteList.add(new Note(NoteType.DOWN, 1155));
        this.noteList.add(new Note(NoteType.UP, 1170));
        this.noteList.add(new Note(NoteType.UP, 1185));
        this.noteList.add(new Note(NoteType.DOWN, 1200));
        this.noteList.add(new Note(NoteType.LEFT, 1215));
        this.noteList.add(new Note(NoteType.DOWN, 1230));
        this.noteList.add(new Note(NoteType.UP, 1230));
        this.noteList.add(new Note(NoteType.DOWN, 1245));
        this.noteList.add(new Note(NoteType.UP, 1245));
        this.noteList.add(new Note(NoteType.RIGHT, 1260));
        this.noteList.add(new Note(NoteType.UP, 1275));
        this.noteList.add(new Note(NoteType.UP, 1305));
        this.noteList.add(new Note(NoteType.RIGHT, 1305));
        this.noteList.add(new Note(NoteType.DOWN, 1342));
        this.noteList.add(new Note(NoteType.UP, 1342));
        this.noteList.add(new Note(NoteType.LEFT, 1357));
        this.noteList.add(new Note(NoteType.LEFT, 1380));
        this.noteList.add(new Note(NoteType.DOWN, 1380));
        this.noteList.add(new Note(NoteType.DOWN, 1410));
        this.noteList.add(new Note(NoteType.UP, 1410));
        this.noteList.add(new Note(NoteType.DOWN, 1425));
        this.noteList.add(new Note(NoteType.UP, 1440));
        this.noteList.add(new Note(NoteType.RIGHT, 1440));
        this.noteList.add(new Note(NoteType.DOWN, 1485));
        this.noteList.add(new Note(NoteType.LEFT, 1500));
        this.noteList.add(new Note(NoteType.LEFT, 1522));
        this.noteList.add(new Note(NoteType.DOWN, 1522));
        this.noteList.add(new Note(NoteType.DOWN, 1560));
        this.noteList.add(new Note(NoteType.RIGHT, 1560));
        this.noteList.add(new Note(NoteType.UP, 1575));
        this.noteList.add(new Note(NoteType.DOWN, 1597));
        this.noteList.add(new Note(NoteType.UP, 1597));
        this.noteList.add(new Note(NoteType.DOWN, 1627));
        this.noteList.add(new Note(NoteType.RIGHT, 1627));
        this.noteList.add(new Note(NoteType.UP, 1642));
        this.noteList.add(new Note(NoteType.UP, 1665));
        this.noteList.add(new Note(NoteType.RIGHT, 1665));
        this.noteList.add(new Note(NoteType.LEFT, 1695));
        this.noteList.add(new Note(NoteType.UP, 1695));
        this.noteList.add(new Note(NoteType.DOWN, 1710));
        this.noteList.add(new Note(NoteType.DOWN, 1740));
        this.noteList.add(new Note(NoteType.DOWN, 1755));
        this.noteList.add(new Note(NoteType.UP, 1770));
        this.noteList.add(new Note(NoteType.RIGHT, 1785));
        this.noteList.add(new Note(NoteType.LEFT, 1815));
        this.noteList.add(new Note(NoteType.DOWN, 1815));
        this.noteList.add(new Note(NoteType.DOWN, 1852));
        this.noteList.add(new Note(NoteType.RIGHT, 1852));
        this.noteList.add(new Note(NoteType.UP, 1867));
        this.noteList.add(new Note(NoteType.DOWN, 1890));
        this.noteList.add(new Note(NoteType.UP, 1890));
        this.noteList.add(new Note(NoteType.DOWN, 1920));
        this.noteList.add(new Note(NoteType.RIGHT, 1920));
        this.noteList.add(new Note(NoteType.UP, 1935));
        this.noteList.add(new Note(NoteType.UP, 1957));
        this.noteList.add(new Note(NoteType.RIGHT, 1957));
        this.noteList.add(new Note(NoteType.LEFT, 1987));
        this.noteList.add(new Note(NoteType.UP, 1987));
        this.noteList.add(new Note(NoteType.DOWN, 2002));
        this.noteList.add(new Note(NoteType.LEFT, 2030));
        this.noteList.add(new Note(NoteType.DOWN, 2030));
        this.noteList.add(new Note(NoteType.RIGHT, 2060));
        this.noteList.add(new Note(NoteType.UP, 2070));
        this.noteList.add(new Note(NoteType.DOWN, 2080));
        this.noteList.add(new Note(NoteType.LEFT, 2100));
        this.noteList.add(new Note(NoteType.DOWN, 2100));
        this.noteList.add(new Note(NoteType.DOWN, 2137));
        this.noteList.add(new Note(NoteType.RIGHT, 2137));
        this.noteList.add(new Note(NoteType.UP, 2152));
        this.noteList.add(new Note(NoteType.DOWN, 2175));
        this.noteList.add(new Note(NoteType.UP, 2175));
        this.noteList.add(new Note(NoteType.DOWN, 2205));
        this.noteList.add(new Note(NoteType.RIGHT, 2205));
        this.noteList.add(new Note(NoteType.UP, 2220));
        this.noteList.add(new Note(NoteType.UP, 2242));
        this.noteList.add(new Note(NoteType.RIGHT, 2242));
        this.noteList.add(new Note(NoteType.LEFT, 2272));
        this.noteList.add(new Note(NoteType.UP, 2272));
        this.noteList.add(new Note(NoteType.DOWN, 2287));
        this.noteList.add(new Note(NoteType.LEFT, 2315));
        this.noteList.add(new Note(NoteType.DOWN, 2315));
        this.noteList.add(new Note(NoteType.UP, 2355));
        this.noteList.add(new Note(NoteType.UP, 2370));
        this.noteList.add(new Note(NoteType.LEFT, 2392));
        this.noteList.add(new Note(NoteType.DOWN, 2392));
        this.noteList.add(new Note(NoteType.DOWN, 2430));
        this.noteList.add(new Note(NoteType.RIGHT, 2430));
        this.noteList.add(new Note(NoteType.UP, 2445));
        this.noteList.add(new Note(NoteType.DOWN, 2460));
        this.noteList.add(new Note(NoteType.UP, 2460));
        this.noteList.add(new Note(NoteType.DOWN, 2497));
        this.noteList.add(new Note(NoteType.RIGHT, 2497));
        this.noteList.add(new Note(NoteType.UP, 2512));
        this.noteList.add(new Note(NoteType.UP, 2535));
        this.noteList.add(new Note(NoteType.RIGHT, 2535));
        this.noteList.add(new Note(NoteType.LEFT, 2565));
        this.noteList.add(new Note(NoteType.UP, 2565));
        this.noteList.add(new Note(NoteType.DOWN, 2580));
        this.noteList.add(new Note(NoteType.LEFT, 2607));
        this.noteList.add(new Note(NoteType.DOWN, 2607));
        this.noteList.add(new Note(NoteType.RIGHT, 2637));
        this.noteList.add(new Note(NoteType.UP, 2647));
        this.noteList.add(new Note(NoteType.DOWN, 2657));
        this.noteList.add(new Note(NoteType.DOWN, 2677));
        this.noteList.add(new Note(NoteType.UP, 2677));
        this.noteList.add(new Note(NoteType.RIGHT, 2715));
        this.noteList.add(new Note(NoteType.UP, 2730));
        this.noteList.add(new Note(NoteType.LEFT, 2752));
        this.noteList.add(new Note(NoteType.DOWN, 2752));
        this.noteList.add(new Note(NoteType.DOWN, 2790));
        this.noteList.add(new Note(NoteType.UP, 2805));
        this.noteList.add(new Note(NoteType.DOWN, 2820));
        this.noteList.add(new Note(NoteType.RIGHT, 2820));
        this.noteList.add(new Note(NoteType.RIGHT, 2857));
        this.noteList.add(new Note(NoteType.RIGHT, 2872));
        this.noteList.add(new Note(NoteType.UP, 2887));
        this.noteList.add(new Note(NoteType.UP, 2902));
        this.noteList.add(new Note(NoteType.DOWN, 2917));
        this.noteList.add(new Note(NoteType.DOWN, 2932));
        this.noteList.add(new Note(NoteType.LEFT, 2947));
        this.noteList.add(new Note(NoteType.LEFT, 2962));
        this.noteList.add(new Note(NoteType.UP, 2962));
        this.noteList.add(new Note(NoteType.DOWN, 3000));
        this.noteList.add(new Note(NoteType.UP, 3000));
        this.noteList.add(new Note(NoteType.RIGHT, 3022));
        this.noteList.add(new Note(NoteType.UP, 3037));
        this.noteList.add(new Note(NoteType.DOWN, 3075));
        this.noteList.add(new Note(NoteType.UP, 3075));
        this.noteList.add(new Note(NoteType.UP, 3100));
        this.noteList.add(new Note(NoteType.UP, 3120));
        this.noteList.add(new Note(NoteType.RIGHT, 3120));
        this.noteList.add(new Note(NoteType.DOWN, 3150));
        this.noteList.add(new Note(NoteType.LEFT, 3165));
        this.noteList.add(new Note(NoteType.LEFT, 3180));
        this.noteList.add(new Note(NoteType.DOWN, 3195));
        this.noteList.add(new Note(NoteType.DOWN, 3210));
        this.noteList.add(new Note(NoteType.UP, 3225));
        this.noteList.add(new Note(NoteType.UP, 3240));
        this.noteList.add(new Note(NoteType.DOWN, 3255));
        this.noteList.add(new Note(NoteType.RIGHT, 3255));
        this.noteList.add(new Note(NoteType.DOWN, 3290));
        this.noteList.add(new Note(NoteType.UP, 3290));
        this.noteList.add(new Note(NoteType.RIGHT, 3312));
        this.noteList.add(new Note(NoteType.UP, 3327));
        this.noteList.add(new Note(NoteType.DOWN, 3365));
        this.noteList.add(new Note(NoteType.UP, 3365));
        this.noteList.add(new Note(NoteType.LEFT, 3382));
        this.noteList.add(new Note(NoteType.UP, 3405));
        this.noteList.add(new Note(NoteType.RIGHT, 3405));
        this.noteList.add(new Note(NoteType.RIGHT, 3435));
        this.noteList.add(new Note(NoteType.RIGHT, 3450));
        this.noteList.add(new Note(NoteType.UP, 3465));
        this.noteList.add(new Note(NoteType.UP, 3480));
        this.noteList.add(new Note(NoteType.DOWN, 3495));
        this.noteList.add(new Note(NoteType.DOWN, 3510));
        this.noteList.add(new Note(NoteType.LEFT, 3525));
        this.noteList.add(new Note(NoteType.LEFT, 3540));
        this.noteList.add(new Note(NoteType.UP, 3540));
        this.noteList.add(new Note(NoteType.DOWN, 3577));
        this.noteList.add(new Note(NoteType.UP, 3577));
        this.noteList.add(new Note(NoteType.RIGHT, 3600));
        this.noteList.add(new Note(NoteType.UP, 3615));
        this.noteList.add(new Note(NoteType.DOWN, 3652));
        this.noteList.add(new Note(NoteType.UP, 3652));
        this.noteList.add(new Note(NoteType.LEFT, 3667));
        this.noteList.add(new Note(NoteType.LEFT, 3682));
        this.noteList.add(new Note(NoteType.LEFT, 3697));
        this.noteList.add(new Note(NoteType.LEFT, 3712));
        this.noteList.add(new Note(NoteType.LEFT, 3727));
        this.noteList.add(new Note(NoteType.LEFT, 3742));
        this.noteList.add(new Note(NoteType.DOWN, 3757));
        this.noteList.add(new Note(NoteType.DOWN, 3787));
        this.noteList.add(new Note(NoteType.UP, 3810));
        this.noteList.add(new Note(NoteType.RIGHT, 3830));
        this.noteList.add(new Note(NoteType.DOWN, 3870));
        this.noteList.add(new Note(NoteType.UP, 3890));
        this.noteList.add(new Note(NoteType.RIGHT, 3910));
        this.noteList.add(new Note(NoteType.DOWN, 3940));
        this.noteList.add(new Note(NoteType.UP, 3960));
        this.noteList.add(new Note(NoteType.RIGHT, 3980));
        this.noteList.add(new Note(NoteType.RIGHT, 4020));
        this.noteList.add(new Note(NoteType.RIGHT, 4035));
        this.noteList.add(new Note(NoteType.UP, 4050));
        this.noteList.add(new Note(NoteType.UP, 4065));
        this.noteList.add(new Note(NoteType.DOWN, 4080));
        this.noteList.add(new Note(NoteType.DOWN, 4095));
        this.noteList.add(new Note(NoteType.LEFT, 4110));
        this.noteList.add(new Note(NoteType.LEFT, 4125));
        this.noteList.add(new Note(NoteType.UP, 4125));
        this.noteList.add(new Note(NoteType.DOWN, 4162));
        this.noteList.add(new Note(NoteType.UP, 4162));
        this.noteList.add(new Note(NoteType.RIGHT, 4185));
        this.noteList.add(new Note(NoteType.UP, 4200));
        this.noteList.add(new Note(NoteType.DOWN, 4230));
        this.noteList.add(new Note(NoteType.UP, 4230));
        this.noteList.add(new Note(NoteType.LEFT, 4252));
        this.noteList.add(new Note(NoteType.UP, 4275));
        this.noteList.add(new Note(NoteType.RIGHT, 4275));
        this.noteList.add(new Note(NoteType.LEFT, 4320));
        this.noteList.add(new Note(NoteType.DOWN, 4335));
        this.noteList.add(new Note(NoteType.UP, 4350));
        this.noteList.add(new Note(NoteType.DOWN, 4365));
        this.noteList.add(new Note(NoteType.UP, 4380));
        this.noteList.add(new Note(NoteType.DOWN, 4395));
        this.noteList.add(new Note(NoteType.RIGHT, 4410));
        this.noteList.add(new Note(NoteType.LEFT, 4447));
        this.noteList.add(new Note(NoteType.DOWN, 4477));
        this.noteList.add(new Note(NoteType.UP, 4515));
        this.noteList.add(new Note(NoteType.RIGHT, 4530));
        this.noteList.add(new Note(NoteType.RIGHT, 4545));
        this.noteList.add(new Note(NoteType.UP, 4560));
        this.noteList.add(new Note(NoteType.DOWN, 4590));
        this.noteList.add(new Note(NoteType.LEFT, 4635));
        this.noteList.add(new Note(NoteType.DOWN, 4672));
        this.noteList.add(new Note(NoteType.LEFT, 4710));
        this.noteList.add(new Note(NoteType.RIGHT, 4710));
        this.noteList.add(new Note(NoteType.DOWN, 4740));
        this.noteList.add(new Note(NoteType.UP, 4740));
        this.noteList.add(new Note(NoteType.RIGHT, 4762));
        this.noteList.add(new Note(NoteType.UP, 4777));
        this.noteList.add(new Note(NoteType.DOWN, 4815));
        this.noteList.add(new Note(NoteType.UP, 4815));
        this.noteList.add(new Note(NoteType.DOWN, 4830));
        this.noteList.add(new Note(NoteType.UP, 4830));
        this.noteList.add(new Note(NoteType.UP, 4850));
        this.noteList.add(new Note(NoteType.RIGHT, 4850));
        this.noteList.add(new Note(NoteType.LEFT, 4900));
        this.noteList.add(new Note(NoteType.DOWN, 4915));
        this.noteList.add(new Note(NoteType.DOWN, 4945));
        this.noteList.add(new Note(NoteType.UP, 4967));
        this.noteList.add(new Note(NoteType.RIGHT, 4987));
        this.noteList.add(new Note(NoteType.DOWN, 5027));
        this.noteList.add(new Note(NoteType.UP, 5047));
        this.noteList.add(new Note(NoteType.RIGHT, 5067));
        this.noteList.add(new Note(NoteType.DOWN, 5097));
        this.noteList.add(new Note(NoteType.UP, 5117));
        this.noteList.add(new Note(NoteType.LEFT, 5137));
        this.noteList.add(new Note(NoteType.DOWN, 5152));
        this.noteList.add(new Note(NoteType.UP, 5167));
        this.noteList.add(new Note(NoteType.RIGHT, 5190));
        this.noteList.add(new Note(NoteType.UP, 5212));
        this.noteList.add(new Note(NoteType.UP, 5227));
        this.noteList.add(new Note(NoteType.DOWN, 5242));
        this.noteList.add(new Note(NoteType.LEFT, 5257));
        this.noteList.add(new Note(NoteType.DOWN, 5280));
        this.noteList.add(new Note(NoteType.UP, 5280));
        this.noteList.add(new Note(NoteType.DOWN, 5295));
        this.noteList.add(new Note(NoteType.UP, 5295));
        this.noteList.add(new Note(NoteType.RIGHT, 5310));
        this.noteList.add(new Note(NoteType.UP, 5325));
        this.noteList.add(new Note(NoteType.UP, 5355));
        this.noteList.add(new Note(NoteType.RIGHT, 5355));
        this.noteList.add(new Note(NoteType.DOWN, 5392));
        this.noteList.add(new Note(NoteType.UP, 5392));
        this.noteList.add(new Note(NoteType.LEFT, 5407));
        this.noteList.add(new Note(NoteType.LEFT, 5430));
        this.noteList.add(new Note(NoteType.DOWN, 5430));
        this.noteList.add(new Note(NoteType.DOWN, 5460));
        this.noteList.add(new Note(NoteType.UP, 5460));
        this.noteList.add(new Note(NoteType.DOWN, 5475));
        this.noteList.add(new Note(NoteType.UP, 5490));
        this.noteList.add(new Note(NoteType.RIGHT, 5490));
        this.noteList.add(new Note(NoteType.DOWN, 5535));
        this.noteList.add(new Note(NoteType.LEFT, 5550));
        this.noteList.add(new Note(NoteType.LEFT, 5572));
        this.noteList.add(new Note(NoteType.DOWN, 5572));
        this.noteList.add(new Note(NoteType.DOWN, 5610));
        this.noteList.add(new Note(NoteType.RIGHT, 5610));
        this.noteList.add(new Note(NoteType.UP, 5625));
        this.noteList.add(new Note(NoteType.DOWN, 5647));
        this.noteList.add(new Note(NoteType.UP, 5647));
        this.noteList.add(new Note(NoteType.DOWN, 5677));
        this.noteList.add(new Note(NoteType.RIGHT, 5677));
        this.noteList.add(new Note(NoteType.UP, 5692));
        this.noteList.add(new Note(NoteType.UP, 5715));
        this.noteList.add(new Note(NoteType.RIGHT, 5715));
        this.noteList.add(new Note(NoteType.LEFT, 5745));
        this.noteList.add(new Note(NoteType.UP, 5745));
        this.noteList.add(new Note(NoteType.DOWN, 5760));
        this.noteList.add(new Note(NoteType.DOWN, 5790));
        this.noteList.add(new Note(NoteType.DOWN, 5805));
        this.noteList.add(new Note(NoteType.UP, 5820));
        this.noteList.add(new Note(NoteType.RIGHT, 5835));
        this.noteList.add(new Note(NoteType.LEFT, 5865));
        this.noteList.add(new Note(NoteType.DOWN, 5865));
        this.noteList.add(new Note(NoteType.DOWN, 5902));
        this.noteList.add(new Note(NoteType.RIGHT, 5902));
        this.noteList.add(new Note(NoteType.UP, 5917));
        this.noteList.add(new Note(NoteType.DOWN, 5940));
        this.noteList.add(new Note(NoteType.UP, 5940));
        this.noteList.add(new Note(NoteType.DOWN, 5970));
        this.noteList.add(new Note(NoteType.RIGHT, 5970));
        this.noteList.add(new Note(NoteType.UP, 5985));
        this.noteList.add(new Note(NoteType.UP, 6007));
        this.noteList.add(new Note(NoteType.RIGHT, 6007));
        this.noteList.add(new Note(NoteType.LEFT, 6037));
        this.noteList.add(new Note(NoteType.UP, 6037));
        this.noteList.add(new Note(NoteType.DOWN, 6052));
        this.noteList.add(new Note(NoteType.LEFT, 6080));
        this.noteList.add(new Note(NoteType.DOWN, 6080));
        this.noteList.add(new Note(NoteType.RIGHT, 6110));
        this.noteList.add(new Note(NoteType.UP, 6120));
        this.noteList.add(new Note(NoteType.DOWN, 6130));
        this.noteList.add(new Note(NoteType.LEFT, 6150));
        this.noteList.add(new Note(NoteType.DOWN, 6150));
        this.noteList.add(new Note(NoteType.DOWN, 6187));
        this.noteList.add(new Note(NoteType.RIGHT, 6187));
        this.noteList.add(new Note(NoteType.UP, 6202));
        this.noteList.add(new Note(NoteType.DOWN, 6225));
        this.noteList.add(new Note(NoteType.UP, 6225));
        this.noteList.add(new Note(NoteType.DOWN, 6255));
        this.noteList.add(new Note(NoteType.RIGHT, 6255));
        this.noteList.add(new Note(NoteType.UP, 6270));
        this.noteList.add(new Note(NoteType.UP, 6292));
        this.noteList.add(new Note(NoteType.RIGHT, 6292));
        this.noteList.add(new Note(NoteType.LEFT, 6322));
        this.noteList.add(new Note(NoteType.UP, 6322));
        this.noteList.add(new Note(NoteType.DOWN, 6337));
        this.noteList.add(new Note(NoteType.LEFT, 6365));
        this.noteList.add(new Note(NoteType.DOWN, 6365));
        this.noteList.add(new Note(NoteType.UP, 6405));
        this.noteList.add(new Note(NoteType.UP, 6420));
        this.noteList.add(new Note(NoteType.LEFT, 6442));
        this.noteList.add(new Note(NoteType.DOWN, 6442));
        this.noteList.add(new Note(NoteType.DOWN, 6480));
        this.noteList.add(new Note(NoteType.RIGHT, 6480));
        this.noteList.add(new Note(NoteType.UP, 6495));
        this.noteList.add(new Note(NoteType.DOWN, 6510));
        this.noteList.add(new Note(NoteType.UP, 6510));
        this.noteList.add(new Note(NoteType.DOWN, 6547));
        this.noteList.add(new Note(NoteType.RIGHT, 6547));
        this.noteList.add(new Note(NoteType.UP, 6562));
        this.noteList.add(new Note(NoteType.UP, 6585));
        this.noteList.add(new Note(NoteType.RIGHT, 6585));
        this.noteList.add(new Note(NoteType.LEFT, 6615));
        this.noteList.add(new Note(NoteType.UP, 6615));
        this.noteList.add(new Note(NoteType.DOWN, 6630));
        this.noteList.add(new Note(NoteType.LEFT, 6657));
        this.noteList.add(new Note(NoteType.DOWN, 6657));
        this.noteList.add(new Note(NoteType.RIGHT, 6687));
        this.noteList.add(new Note(NoteType.UP, 6697));
        this.noteList.add(new Note(NoteType.DOWN, 6707));
        this.noteList.add(new Note(NoteType.DOWN, 6727));
        this.noteList.add(new Note(NoteType.UP, 6727));
        this.noteList.add(new Note(NoteType.RIGHT, 6765));
        this.noteList.add(new Note(NoteType.UP, 6780));
        this.noteList.add(new Note(NoteType.LEFT, 6802));
        this.noteList.add(new Note(NoteType.DOWN, 6802));
        this.noteList.add(new Note(NoteType.DOWN, 6840));
        this.noteList.add(new Note(NoteType.UP, 6855));
        this.noteList.add(new Note(NoteType.DOWN, 6870));
        this.noteList.add(new Note(NoteType.RIGHT, 6870));
        this.noteList.add(new Note(NoteType.RIGHT, 6907));
        this.noteList.add(new Note(NoteType.RIGHT, 6922));
        this.noteList.add(new Note(NoteType.UP, 6937));
        this.noteList.add(new Note(NoteType.UP, 6952));
        this.noteList.add(new Note(NoteType.DOWN, 6967));
        this.noteList.add(new Note(NoteType.DOWN, 6982));
        this.noteList.add(new Note(NoteType.LEFT, 6997));
        this.noteList.add(new Note(NoteType.LEFT, 7012));
        this.noteList.add(new Note(NoteType.UP, 7012));
        this.noteList.add(new Note(NoteType.DOWN, 7050));
        this.noteList.add(new Note(NoteType.UP, 7050));
        this.noteList.add(new Note(NoteType.RIGHT, 7072));
        this.noteList.add(new Note(NoteType.UP, 7087));
        this.noteList.add(new Note(NoteType.DOWN, 7125));
        this.noteList.add(new Note(NoteType.UP, 7125));
        this.noteList.add(new Note(NoteType.UP, 7150));
        this.noteList.add(new Note(NoteType.UP, 7170));
        this.noteList.add(new Note(NoteType.RIGHT, 7170));
        this.noteList.add(new Note(NoteType.DOWN, 7200));
        this.noteList.add(new Note(NoteType.LEFT, 7215));
        this.noteList.add(new Note(NoteType.LEFT, 7230));
        this.noteList.add(new Note(NoteType.DOWN, 7245));
        this.noteList.add(new Note(NoteType.DOWN, 7260));
        this.noteList.add(new Note(NoteType.UP, 7275));
        this.noteList.add(new Note(NoteType.UP, 7290));
        this.noteList.add(new Note(NoteType.DOWN, 7305));
        this.noteList.add(new Note(NoteType.RIGHT, 7305));
        this.noteList.add(new Note(NoteType.DOWN, 7340));
        this.noteList.add(new Note(NoteType.UP, 7340));
        this.noteList.add(new Note(NoteType.RIGHT, 7362));
        this.noteList.add(new Note(NoteType.UP, 7377));
        this.noteList.add(new Note(NoteType.DOWN, 7415));
        this.noteList.add(new Note(NoteType.UP, 7415));
        this.noteList.add(new Note(NoteType.LEFT, 7432));
        this.noteList.add(new Note(NoteType.UP, 7455));
        this.noteList.add(new Note(NoteType.RIGHT, 7455));
        this.noteList.add(new Note(NoteType.RIGHT, 7485));
        this.noteList.add(new Note(NoteType.RIGHT, 7500));
        this.noteList.add(new Note(NoteType.UP, 7515));
        this.noteList.add(new Note(NoteType.UP, 7530));
        this.noteList.add(new Note(NoteType.DOWN, 7545));
        this.noteList.add(new Note(NoteType.DOWN, 7560));
        this.noteList.add(new Note(NoteType.LEFT, 7575));
        this.noteList.add(new Note(NoteType.LEFT, 7590));
        this.noteList.add(new Note(NoteType.UP, 7590));
        this.noteList.add(new Note(NoteType.DOWN, 7627));
        this.noteList.add(new Note(NoteType.UP, 7627));
        this.noteList.add(new Note(NoteType.RIGHT, 7650));
        this.noteList.add(new Note(NoteType.UP, 7665));
        this.noteList.add(new Note(NoteType.DOWN, 7702));
        this.noteList.add(new Note(NoteType.UP, 7702));
        this.noteList.add(new Note(NoteType.LEFT, 7717));
        this.noteList.add(new Note(NoteType.LEFT, 7732));
        this.noteList.add(new Note(NoteType.LEFT, 7747));
        this.noteList.add(new Note(NoteType.LEFT, 7762));
        this.noteList.add(new Note(NoteType.LEFT, 7777));
        this.noteList.add(new Note(NoteType.LEFT, 7792));
        this.noteList.add(new Note(NoteType.DOWN, 7807));
        this.noteList.add(new Note(NoteType.DOWN, 7837));
        this.noteList.add(new Note(NoteType.UP, 7860));
        this.noteList.add(new Note(NoteType.RIGHT, 7880));
        this.noteList.add(new Note(NoteType.DOWN, 7920));
        this.noteList.add(new Note(NoteType.UP, 7940));
        this.noteList.add(new Note(NoteType.RIGHT, 7960));
        this.noteList.add(new Note(NoteType.DOWN, 7990));
        this.noteList.add(new Note(NoteType.UP, 8010));
        this.noteList.add(new Note(NoteType.RIGHT, 8030));
        this.noteList.add(new Note(NoteType.RIGHT, 8070));
        this.noteList.add(new Note(NoteType.RIGHT, 8085));
        this.noteList.add(new Note(NoteType.UP, 8100));
        this.noteList.add(new Note(NoteType.UP, 8115));
        this.noteList.add(new Note(NoteType.DOWN, 8130));
        this.noteList.add(new Note(NoteType.DOWN, 8145));
        this.noteList.add(new Note(NoteType.LEFT, 8160));
        this.noteList.add(new Note(NoteType.LEFT, 8175));
        this.noteList.add(new Note(NoteType.UP, 8175));
        this.noteList.add(new Note(NoteType.DOWN, 8212));
        this.noteList.add(new Note(NoteType.UP, 8212));
        this.noteList.add(new Note(NoteType.RIGHT, 8235));
        this.noteList.add(new Note(NoteType.UP, 8250));
        this.noteList.add(new Note(NoteType.DOWN, 8280));
        this.noteList.add(new Note(NoteType.UP, 8280));
        this.noteList.add(new Note(NoteType.LEFT, 8302));
        this.noteList.add(new Note(NoteType.UP, 8325));
        this.noteList.add(new Note(NoteType.RIGHT, 8325));
        this.noteList.add(new Note(NoteType.UP, 8355));
        this.noteList.add(new Note(NoteType.RIGHT, 8355));
        this.noteList.add(new Note(NoteType.LEFT, 8370));
        this.noteList.add(new Note(NoteType.DOWN, 8385));
        this.noteList.add(new Note(NoteType.UP, 8400));
        this.noteList.add(new Note(NoteType.DOWN, 8415));
        this.noteList.add(new Note(NoteType.UP, 8430));
        this.noteList.add(new Note(NoteType.DOWN, 8445));
        this.noteList.add(new Note(NoteType.RIGHT, 8460));
        this.noteList.add(new Note(NoteType.LEFT, 8497));
        this.noteList.add(new Note(NoteType.DOWN, 8527));
        this.noteList.add(new Note(NoteType.UP, 8565));
        this.noteList.add(new Note(NoteType.RIGHT, 8580));
        this.noteList.add(new Note(NoteType.RIGHT, 8595));
        this.noteList.add(new Note(NoteType.UP, 8610));
        this.noteList.add(new Note(NoteType.DOWN, 8640));
        this.noteList.add(new Note(NoteType.LEFT, 8685));
        this.noteList.add(new Note(NoteType.DOWN, 8722));
        this.noteList.add(new Note(NoteType.LEFT, 8760));
        this.noteList.add(new Note(NoteType.RIGHT, 8760));
        this.noteList.add(new Note(NoteType.DOWN, 8790));
        this.noteList.add(new Note(NoteType.UP, 8790));
        this.noteList.add(new Note(NoteType.RIGHT, 8812));
        this.noteList.add(new Note(NoteType.UP, 8827));
        this.noteList.add(new Note(NoteType.DOWN, 8865));
        this.noteList.add(new Note(NoteType.UP, 8865));
        this.noteList.add(new Note(NoteType.DOWN, 8880));
        this.noteList.add(new Note(NoteType.UP, 8880));
        this.noteList.add(new Note(NoteType.UP, 8900));
        this.noteList.add(new Note(NoteType.RIGHT, 8900));
        this.noteList.add(new Note(NoteType.LEFT, 8950));
        this.noteList.add(new Note(NoteType.DOWN, 8965));
        this.noteList.add(new Note(NoteType.DOWN, 8995));
        this.noteList.add(new Note(NoteType.UP, 9017));
        this.noteList.add(new Note(NoteType.RIGHT, 9037));
        this.noteList.add(new Note(NoteType.DOWN, 9077));
        this.noteList.add(new Note(NoteType.UP, 9097));
        this.noteList.add(new Note(NoteType.RIGHT, 9117));
        this.noteList.add(new Note(NoteType.DOWN, 9147));
        this.noteList.add(new Note(NoteType.UP, 9167));
        this.noteList.add(new Note(NoteType.LEFT, 9187));
        this.noteList.add(new Note(NoteType.RIGHT, 9187));
        break;
      case 2:
        this.noteList.add(new Note(NoteType.DOWN, 504));
        this.noteList.add(new Note(NoteType.DOWN, 514));
        this.noteList.add(new Note(NoteType.DOWN, 526));
        this.noteList.add(new Note(NoteType.LEFT, 540));
        this.noteList.add(new Note(NoteType.UP, 552));
        this.noteList.add(new Note(NoteType.UP, 564));
        this.noteList.add(new Note(NoteType.UP, 574));
        this.noteList.add(new Note(NoteType.DOWN, 586));
        this.noteList.add(new Note(NoteType.RIGHT, 600));
        this.noteList.add(new Note(NoteType.RIGHT, 612));
        this.noteList.add(new Note(NoteType.RIGHT, 624));
        this.noteList.add(new Note(NoteType.UP, 634));
        this.noteList.add(new Note(NoteType.RIGHT, 660));
        this.noteList.add(new Note(NoteType.UP, 674));
        this.noteList.add(new Note(NoteType.DOWN, 688));
        this.noteList.add(new Note(NoteType.UP, 706));
        this.noteList.add(new Note(NoteType.UP, 720));
        this.noteList.add(new Note(NoteType.LEFT, 732));
        this.noteList.add(new Note(NoteType.UP, 732));
        this.noteList.add(new Note(NoteType.RIGHT, 754));
        this.noteList.add(new Note(NoteType.RIGHT, 766));
        this.noteList.add(new Note(NoteType.DOWN, 780));
        this.noteList.add(new Note(NoteType.RIGHT, 780));
        this.noteList.add(new Note(NoteType.LEFT, 814));
        this.noteList.add(new Note(NoteType.LEFT, 826));
        this.noteList.add(new Note(NoteType.LEFT, 840));
        this.noteList.add(new Note(NoteType.UP, 840));
        this.noteList.add(new Note(NoteType.DOWN, 874));
        this.noteList.add(new Note(NoteType.UP, 886));
        this.noteList.add(new Note(NoteType.RIGHT, 900));
        this.noteList.add(new Note(NoteType.DOWN, 912));
        this.noteList.add(new Note(NoteType.DOWN, 924));
        this.noteList.add(new Note(NoteType.DOWN, 934));
        this.noteList.add(new Note(NoteType.LEFT, 946));
        this.noteList.add(new Note(NoteType.UP, 960));
        this.noteList.add(new Note(NoteType.UP, 972));
        this.noteList.add(new Note(NoteType.UP, 984));
        this.noteList.add(new Note(NoteType.DOWN, 994));
        this.noteList.add(new Note(NoteType.LEFT, 1020));
        this.noteList.add(new Note(NoteType.LEFT, 1032));
        this.noteList.add(new Note(NoteType.LEFT, 1044));
        this.noteList.add(new Note(NoteType.UP, 1044));
        this.noteList.add(new Note(NoteType.RIGHT, 1066));
        this.noteList.add(new Note(NoteType.UP, 1082));
        this.noteList.add(new Note(NoteType.DOWN, 1096));
        this.noteList.add(new Note(NoteType.RIGHT, 1114));
        this.noteList.add(new Note(NoteType.RIGHT, 1126));
        this.noteList.add(new Note(NoteType.DOWN, 1140));
        this.noteList.add(new Note(NoteType.RIGHT, 1140));
        this.noteList.add(new Note(NoteType.UP, 1174));
        this.noteList.add(new Note(NoteType.UP, 1186));
        this.noteList.add(new Note(NoteType.LEFT, 1200));
        this.noteList.add(new Note(NoteType.UP, 1200));
        this.noteList.add(new Note(NoteType.LEFT, 1234));
        this.noteList.add(new Note(NoteType.LEFT, 1246));
        this.noteList.add(new Note(NoteType.LEFT, 1260));
        this.noteList.add(new Note(NoteType.UP, 1260));
        this.noteList.add(new Note(NoteType.DOWN, 1284));
        this.noteList.add(new Note(NoteType.UP, 1294));
        this.noteList.add(new Note(NoteType.RIGHT, 1306));
        this.noteList.add(new Note(NoteType.LEFT, 1332));
        this.noteList.add(new Note(NoteType.LEFT, 1344));
        this.noteList.add(new Note(NoteType.LEFT, 1354));
        this.noteList.add(new Note(NoteType.UP, 1354));
        this.noteList.add(new Note(NoteType.RIGHT, 1380));
        this.noteList.add(new Note(NoteType.RIGHT, 1392));
        this.noteList.add(new Note(NoteType.DOWN, 1404));
        this.noteList.add(new Note(NoteType.RIGHT, 1404));
        this.noteList.add(new Note(NoteType.LEFT, 1440));
        this.noteList.add(new Note(NoteType.LEFT, 1452));
        this.noteList.add(new Note(NoteType.LEFT, 1464));
        this.noteList.add(new Note(NoteType.UP, 1464));
        this.noteList.add(new Note(NoteType.LEFT, 1486));
        this.noteList.add(new Note(NoteType.DOWN, 1500));
        this.noteList.add(new Note(NoteType.UP, 1512));
        this.noteList.add(new Note(NoteType.RIGHT, 1534));
        this.noteList.add(new Note(NoteType.RIGHT, 1546));
        this.noteList.add(new Note(NoteType.DOWN, 1560));
        this.noteList.add(new Note(NoteType.RIGHT, 1560));
        this.noteList.add(new Note(NoteType.LEFT, 1584));
        this.noteList.add(new Note(NoteType.LEFT, 1594));
        this.noteList.add(new Note(NoteType.LEFT, 1606));
        this.noteList.add(new Note(NoteType.UP, 1606));
        this.noteList.add(new Note(NoteType.RIGHT, 1644));
        this.noteList.add(new Note(NoteType.RIGHT, 1654));
        this.noteList.add(new Note(NoteType.DOWN, 1666));
        this.noteList.add(new Note(NoteType.RIGHT, 1666));
        this.noteList.add(new Note(NoteType.UP, 1690));
        this.noteList.add(new Note(NoteType.DOWN, 1706));
        this.noteList.add(new Note(NoteType.LEFT, 1720));
        this.noteList.add(new Note(NoteType.LEFT, 1740));
        this.noteList.add(new Note(NoteType.LEFT, 1752));
        this.noteList.add(new Note(NoteType.LEFT, 1764));
        this.noteList.add(new Note(NoteType.UP, 1764));
        this.noteList.add(new Note(NoteType.RIGHT, 1786));
        this.noteList.add(new Note(NoteType.RIGHT, 1800));
        this.noteList.add(new Note(NoteType.DOWN, 1812));
        this.noteList.add(new Note(NoteType.RIGHT, 1812));
        this.noteList.add(new Note(NoteType.LEFT, 1846));
        this.noteList.add(new Note(NoteType.LEFT, 1860));
        this.noteList.add(new Note(NoteType.LEFT, 1872));
        this.noteList.add(new Note(NoteType.UP, 1872));
        this.noteList.add(new Note(NoteType.LEFT, 1898));
        this.noteList.add(new Note(NoteType.DOWN, 1914));
        this.noteList.add(new Note(NoteType.UP, 1934));
        this.noteList.add(new Note(NoteType.RIGHT, 1954));
        this.noteList.add(new Note(NoteType.RIGHT, 1966));
        this.noteList.add(new Note(NoteType.DOWN, 1980));
        this.noteList.add(new Note(NoteType.RIGHT, 1980));
        this.noteList.add(new Note(NoteType.LEFT, 2004));
        this.noteList.add(new Note(NoteType.LEFT, 2014));
        this.noteList.add(new Note(NoteType.LEFT, 2026));
        this.noteList.add(new Note(NoteType.UP, 2026));
        this.noteList.add(new Note(NoteType.RIGHT, 2052));
        this.noteList.add(new Note(NoteType.RIGHT, 2064));
        this.noteList.add(new Note(NoteType.DOWN, 2074));
        this.noteList.add(new Note(NoteType.RIGHT, 2074));
        this.noteList.add(new Note(NoteType.UP, 2100));
        this.noteList.add(new Note(NoteType.DOWN, 2116));
        this.noteList.add(new Note(NoteType.LEFT, 2130));
        this.noteList.add(new Note(NoteType.LEFT, 2156));
        this.noteList.add(new Note(NoteType.DOWN, 2208));
        this.noteList.add(new Note(NoteType.UP, 2230));
        this.noteList.add(new Note(NoteType.RIGHT, 2260));
        this.noteList.add(new Note(NoteType.UP, 2306));
        this.noteList.add(new Note(NoteType.DOWN, 2336));
        this.noteList.add(new Note(NoteType.LEFT, 2366));
        this.noteList.add(new Note(NoteType.LEFT, 2410));
        this.noteList.add(new Note(NoteType.DOWN, 2426));
        this.noteList.add(new Note(NoteType.UP, 2440));
        this.noteList.add(new Note(NoteType.UP, 2456));
        this.noteList.add(new Note(NoteType.UP, 2470));
        this.noteList.add(new Note(NoteType.UP, 2486));
        this.noteList.add(new Note(NoteType.DOWN, 2508));
        this.noteList.add(new Note(NoteType.UP, 2524));
        this.noteList.add(new Note(NoteType.RIGHT, 2538));
        this.noteList.add(new Note(NoteType.RIGHT, 2554));
        this.noteList.add(new Note(NoteType.RIGHT, 2568));
        this.noteList.add(new Note(NoteType.RIGHT, 2584));
        this.noteList.add(new Note(NoteType.LEFT, 2614));
        this.noteList.add(new Note(NoteType.DOWN, 2628));
        this.noteList.add(new Note(NoteType.UP, 2644));
        this.noteList.add(new Note(NoteType.UP, 2658));
        this.noteList.add(new Note(NoteType.UP, 2674));
        this.noteList.add(new Note(NoteType.UP, 2688));
        this.noteList.add(new Note(NoteType.DOWN, 2724));
        this.noteList.add(new Note(NoteType.UP, 2734));
        this.noteList.add(new Note(NoteType.RIGHT, 2746));
        this.noteList.add(new Note(NoteType.UP, 2760));
        this.noteList.add(new Note(NoteType.RIGHT, 2772));
        this.noteList.add(new Note(NoteType.UP, 2784));
        this.noteList.add(new Note(NoteType.RIGHT, 2794));
        this.noteList.add(new Note(NoteType.UP, 2806));
        this.noteList.add(new Note(NoteType.RIGHT, 2820));
        this.noteList.add(new Note(NoteType.UP, 2832));
        this.noteList.add(new Note(NoteType.RIGHT, 2844));
        this.noteList.add(new Note(NoteType.UP, 2854));
        this.noteList.add(new Note(NoteType.DOWN, 2866));
        this.noteList.add(new Note(NoteType.UP, 2880));
        this.noteList.add(new Note(NoteType.DOWN, 2892));
        this.noteList.add(new Note(NoteType.UP, 2904));
        this.noteList.add(new Note(NoteType.DOWN, 2914));
        this.noteList.add(new Note(NoteType.LEFT, 2926));
        this.noteList.add(new Note(NoteType.DOWN, 2940));
        this.noteList.add(new Note(NoteType.LEFT, 2952));
        this.noteList.add(new Note(NoteType.DOWN, 2974));
        this.noteList.add(new Note(NoteType.DOWN, 2986));
        this.noteList.add(new Note(NoteType.DOWN, 3000));
        this.noteList.add(new Note(NoteType.UP, 3000));
        this.noteList.add(new Note(NoteType.RIGHT, 3024));
        this.noteList.add(new Note(NoteType.RIGHT, 3034));
        this.noteList.add(new Note(NoteType.DOWN, 3046));
        this.noteList.add(new Note(NoteType.RIGHT, 3060));
        this.noteList.add(new Note(NoteType.RIGHT, 3072));
        this.noteList.add(new Note(NoteType.RIGHT, 3084));
        this.noteList.add(new Note(NoteType.DOWN, 3094));
        this.noteList.add(new Note(NoteType.DOWN, 3106));
        this.noteList.add(new Note(NoteType.LEFT, 3120));
        this.noteList.add(new Note(NoteType.RIGHT, 3132));
        this.noteList.add(new Note(NoteType.UP, 3144));
        this.noteList.add(new Note(NoteType.UP, 3154));
        this.noteList.add(new Note(NoteType.LEFT, 3166));
        this.noteList.add(new Note(NoteType.DOWN, 3180));
        this.noteList.add(new Note(NoteType.DOWN, 3192));
        this.noteList.add(new Note(NoteType.DOWN, 3204));
        this.noteList.add(new Note(NoteType.UP, 3214));
        this.noteList.add(new Note(NoteType.RIGHT, 3226));
        this.noteList.add(new Note(NoteType.RIGHT, 3240));
        this.noteList.add(new Note(NoteType.DOWN, 3252));
        this.noteList.add(new Note(NoteType.RIGHT, 3264));
        this.noteList.add(new Note(NoteType.RIGHT, 3274));
        this.noteList.add(new Note(NoteType.RIGHT, 3286));
        this.noteList.add(new Note(NoteType.DOWN, 3300));
        this.noteList.add(new Note(NoteType.LEFT, 3312));
        this.noteList.add(new Note(NoteType.RIGHT, 3334));
        this.noteList.add(new Note(NoteType.UP, 3346));
        this.noteList.add(new Note(NoteType.LEFT, 3360));
        this.noteList.add(new Note(NoteType.DOWN, 3384));
        this.noteList.add(new Note(NoteType.DOWN, 3394));
        this.noteList.add(new Note(NoteType.DOWN, 3406));
        this.noteList.add(new Note(NoteType.UP, 3420));
        this.noteList.add(new Note(NoteType.RIGHT, 3432));
        this.noteList.add(new Note(NoteType.RIGHT, 3444));
        this.noteList.add(new Note(NoteType.DOWN, 3454));
        this.noteList.add(new Note(NoteType.RIGHT, 3466));
        this.noteList.add(new Note(NoteType.RIGHT, 3480));
        this.noteList.add(new Note(NoteType.DOWN, 3492));
        this.noteList.add(new Note(NoteType.DOWN, 3504));
        this.noteList.add(new Note(NoteType.LEFT, 3514));
        this.noteList.add(new Note(NoteType.RIGHT, 3526));
        this.noteList.add(new Note(NoteType.UP, 3540));
        this.noteList.add(new Note(NoteType.UP, 3552));
        this.noteList.add(new Note(NoteType.LEFT, 3574));
        this.noteList.add(new Note(NoteType.DOWN, 3586));
        this.noteList.add(new Note(NoteType.DOWN, 3600));
        this.noteList.add(new Note(NoteType.UP, 3624));
        this.noteList.add(new Note(NoteType.RIGHT, 3634));
        this.noteList.add(new Note(NoteType.RIGHT, 3646));
        this.noteList.add(new Note(NoteType.DOWN, 3660));
        this.noteList.add(new Note(NoteType.RIGHT, 3672));
        this.noteList.add(new Note(NoteType.RIGHT, 3684));
        this.noteList.add(new Note(NoteType.DOWN, 3694));
        this.noteList.add(new Note(NoteType.DOWN, 3706));
        this.noteList.add(new Note(NoteType.LEFT, 3720));
        this.noteList.add(new Note(NoteType.RIGHT, 3744));
        this.noteList.add(new Note(NoteType.UP, 3754));
        this.noteList.add(new Note(NoteType.DOWN, 3766));
        this.noteList.add(new Note(NoteType.LEFT, 3780));
        this.noteList.add(new Note(NoteType.DOWN, 3792));
        this.noteList.add(new Note(NoteType.DOWN, 3802));
        this.noteList.add(new Note(NoteType.DOWN, 3814));
        this.noteList.add(new Note(NoteType.LEFT, 3826));
        this.noteList.add(new Note(NoteType.UP, 3840));
        this.noteList.add(new Note(NoteType.UP, 3852));
        this.noteList.add(new Note(NoteType.UP, 3862));
        this.noteList.add(new Note(NoteType.DOWN, 3874));
        this.noteList.add(new Note(NoteType.RIGHT, 3886));
        this.noteList.add(new Note(NoteType.RIGHT, 3900));
        this.noteList.add(new Note(NoteType.RIGHT, 3912));
        this.noteList.add(new Note(NoteType.UP, 3922));
        this.noteList.add(new Note(NoteType.RIGHT, 3946));
        this.noteList.add(new Note(NoteType.UP, 3962));
        this.noteList.add(new Note(NoteType.DOWN, 3976));
        this.noteList.add(new Note(NoteType.UP, 3994));
        this.noteList.add(new Note(NoteType.UP, 4006));
        this.noteList.add(new Note(NoteType.LEFT, 4020));
        this.noteList.add(new Note(NoteType.UP, 4020));
        this.noteList.add(new Note(NoteType.RIGHT, 4042));
        this.noteList.add(new Note(NoteType.RIGHT, 4054));
        this.noteList.add(new Note(NoteType.DOWN, 4066));
        this.noteList.add(new Note(NoteType.RIGHT, 4066));
        this.noteList.add(new Note(NoteType.LEFT, 4102));
        this.noteList.add(new Note(NoteType.LEFT, 4114));
        this.noteList.add(new Note(NoteType.LEFT, 4126));
        this.noteList.add(new Note(NoteType.UP, 4126));
        this.noteList.add(new Note(NoteType.DOWN, 4160));
        this.noteList.add(new Note(NoteType.UP, 4172));
        this.noteList.add(new Note(NoteType.RIGHT, 4184));
        this.noteList.add(new Note(NoteType.DOWN, 4200));
        this.noteList.add(new Note(NoteType.DOWN, 4212));
        this.noteList.add(new Note(NoteType.DOWN, 4222));
        this.noteList.add(new Note(NoteType.LEFT, 4234));
        this.noteList.add(new Note(NoteType.UP, 4246));
        this.noteList.add(new Note(NoteType.UP, 4260));
        this.noteList.add(new Note(NoteType.UP, 4272));
        this.noteList.add(new Note(NoteType.DOWN, 4282));
        this.noteList.add(new Note(NoteType.LEFT, 4306));
        this.noteList.add(new Note(NoteType.LEFT, 4320));
        this.noteList.add(new Note(NoteType.LEFT, 4332));
        this.noteList.add(new Note(NoteType.UP, 4332));
        this.noteList.add(new Note(NoteType.RIGHT, 4354));
        this.noteList.add(new Note(NoteType.UP, 4370));
        this.noteList.add(new Note(NoteType.DOWN, 4384));
        this.noteList.add(new Note(NoteType.RIGHT, 4402));
        this.noteList.add(new Note(NoteType.RIGHT, 4414));
        this.noteList.add(new Note(NoteType.DOWN, 4426));
        this.noteList.add(new Note(NoteType.RIGHT, 4426));
        this.noteList.add(new Note(NoteType.UP, 4462));
        this.noteList.add(new Note(NoteType.UP, 4474));
        this.noteList.add(new Note(NoteType.LEFT, 4486));
        this.noteList.add(new Note(NoteType.UP, 4486));
        this.noteList.add(new Note(NoteType.LEFT, 4522));
        this.noteList.add(new Note(NoteType.LEFT, 4534));
        this.noteList.add(new Note(NoteType.LEFT, 4546));
        this.noteList.add(new Note(NoteType.UP, 4546));
        this.noteList.add(new Note(NoteType.DOWN, 4572));
        this.noteList.add(new Note(NoteType.UP, 4582));
        this.noteList.add(new Note(NoteType.RIGHT, 4594));
        this.noteList.add(new Note(NoteType.LEFT, 4620));
        this.noteList.add(new Note(NoteType.LEFT, 4632));
        this.noteList.add(new Note(NoteType.LEFT, 4642));
        this.noteList.add(new Note(NoteType.UP, 4642));
        this.noteList.add(new Note(NoteType.RIGHT, 4666));
        this.noteList.add(new Note(NoteType.RIGHT, 4680));
        this.noteList.add(new Note(NoteType.DOWN, 4692));
        this.noteList.add(new Note(NoteType.RIGHT, 4692));
        this.noteList.add(new Note(NoteType.LEFT, 4726));
        this.noteList.add(new Note(NoteType.LEFT, 4740));
        this.noteList.add(new Note(NoteType.LEFT, 4752));
        this.noteList.add(new Note(NoteType.UP, 4752));
        this.noteList.add(new Note(NoteType.LEFT, 4774));
        this.noteList.add(new Note(NoteType.DOWN, 4786));
        this.noteList.add(new Note(NoteType.UP, 4800));
        this.noteList.add(new Note(NoteType.RIGHT, 4822));
        this.noteList.add(new Note(NoteType.RIGHT, 4834));
        this.noteList.add(new Note(NoteType.DOWN, 4846));
        this.noteList.add(new Note(NoteType.RIGHT, 4846));
        this.noteList.add(new Note(NoteType.LEFT, 4872));
        this.noteList.add(new Note(NoteType.LEFT, 4882));
        this.noteList.add(new Note(NoteType.LEFT, 4894));
        this.noteList.add(new Note(NoteType.UP, 4894));
        this.noteList.add(new Note(NoteType.RIGHT, 4932));
        this.noteList.add(new Note(NoteType.RIGHT, 4942));
        this.noteList.add(new Note(NoteType.DOWN, 4954));
        this.noteList.add(new Note(NoteType.RIGHT, 4954));
        this.noteList.add(new Note(NoteType.UP, 4978));
        this.noteList.add(new Note(NoteType.DOWN, 4994));
        this.noteList.add(new Note(NoteType.LEFT, 5008));
        this.noteList.add(new Note(NoteType.LEFT, 5026));
        this.noteList.add(new Note(NoteType.LEFT, 5040));
        this.noteList.add(new Note(NoteType.LEFT, 5052));
        this.noteList.add(new Note(NoteType.UP, 5052));
        this.noteList.add(new Note(NoteType.RIGHT, 5074));
        this.noteList.add(new Note(NoteType.RIGHT, 5086));
        this.noteList.add(new Note(NoteType.DOWN, 5100));
        this.noteList.add(new Note(NoteType.RIGHT, 5100));
        this.noteList.add(new Note(NoteType.LEFT, 5134));
        this.noteList.add(new Note(NoteType.LEFT, 5146));
        this.noteList.add(new Note(NoteType.LEFT, 5160));
        this.noteList.add(new Note(NoteType.UP, 5160));
        this.noteList.add(new Note(NoteType.LEFT, 5186));
        this.noteList.add(new Note(NoteType.DOWN, 5202));
        this.noteList.add(new Note(NoteType.UP, 5222));
        this.noteList.add(new Note(NoteType.RIGHT, 5242));
        this.noteList.add(new Note(NoteType.RIGHT, 5254));
        this.noteList.add(new Note(NoteType.DOWN, 5266));
        this.noteList.add(new Note(NoteType.RIGHT, 5266));
        this.noteList.add(new Note(NoteType.LEFT, 5292));
        this.noteList.add(new Note(NoteType.LEFT, 5302));
        this.noteList.add(new Note(NoteType.LEFT, 5314));
        this.noteList.add(new Note(NoteType.UP, 5314));
        this.noteList.add(new Note(NoteType.RIGHT, 5340));
        this.noteList.add(new Note(NoteType.RIGHT, 5352));
        this.noteList.add(new Note(NoteType.DOWN, 5362));
        this.noteList.add(new Note(NoteType.RIGHT, 5362));
        this.noteList.add(new Note(NoteType.UP, 5388));
        this.noteList.add(new Note(NoteType.DOWN, 5404));
        this.noteList.add(new Note(NoteType.LEFT, 5418));
        this.noteList.add(new Note(NoteType.LEFT, 5444));
        this.noteList.add(new Note(NoteType.DOWN, 5496));
        this.noteList.add(new Note(NoteType.UP, 5518));
        this.noteList.add(new Note(NoteType.RIGHT, 5548));
        this.noteList.add(new Note(NoteType.UP, 5594));
        this.noteList.add(new Note(NoteType.DOWN, 5624));
        this.noteList.add(new Note(NoteType.LEFT, 5654));
        this.noteList.add(new Note(NoteType.LEFT, 5698));
        this.noteList.add(new Note(NoteType.DOWN, 5714));
        this.noteList.add(new Note(NoteType.UP, 5728));
        this.noteList.add(new Note(NoteType.UP, 5744));
        this.noteList.add(new Note(NoteType.UP, 5758));
        this.noteList.add(new Note(NoteType.UP, 5774));
        this.noteList.add(new Note(NoteType.DOWN, 5796));
        this.noteList.add(new Note(NoteType.UP, 5812));
        this.noteList.add(new Note(NoteType.RIGHT, 5826));
        this.noteList.add(new Note(NoteType.RIGHT, 5842));
        this.noteList.add(new Note(NoteType.RIGHT, 5856));
        this.noteList.add(new Note(NoteType.RIGHT, 5872));
        this.noteList.add(new Note(NoteType.LEFT, 5902));
        this.noteList.add(new Note(NoteType.DOWN, 5916));
        this.noteList.add(new Note(NoteType.UP, 5932));
        this.noteList.add(new Note(NoteType.UP, 5946));
        this.noteList.add(new Note(NoteType.UP, 5962));
        this.noteList.add(new Note(NoteType.UP, 5976));
        this.noteList.add(new Note(NoteType.DOWN, 6012));
        this.noteList.add(new Note(NoteType.UP, 6022));
        this.noteList.add(new Note(NoteType.RIGHT, 6034));
        this.noteList.add(new Note(NoteType.UP, 6046));
        this.noteList.add(new Note(NoteType.RIGHT, 6060));
        this.noteList.add(new Note(NoteType.UP, 6072));
        this.noteList.add(new Note(NoteType.RIGHT, 6082));
        this.noteList.add(new Note(NoteType.UP, 6094));
        this.noteList.add(new Note(NoteType.RIGHT, 6106));
        this.noteList.add(new Note(NoteType.UP, 6120));
        this.noteList.add(new Note(NoteType.RIGHT, 6132));
        this.noteList.add(new Note(NoteType.UP, 6142));
        this.noteList.add(new Note(NoteType.DOWN, 6154));
        this.noteList.add(new Note(NoteType.UP, 6166));
        this.noteList.add(new Note(NoteType.DOWN, 6180));
        this.noteList.add(new Note(NoteType.UP, 6192));
        this.noteList.add(new Note(NoteType.DOWN, 6202));
        this.noteList.add(new Note(NoteType.LEFT, 6214));
        this.noteList.add(new Note(NoteType.DOWN, 6226));
        this.noteList.add(new Note(NoteType.LEFT, 6240));
        this.noteList.add(new Note(NoteType.DOWN, 6262));
        this.noteList.add(new Note(NoteType.DOWN, 6274));
        this.noteList.add(new Note(NoteType.DOWN, 6286));
        this.noteList.add(new Note(NoteType.UP, 6286));
        this.noteList.add(new Note(NoteType.RIGHT, 6312));
        this.noteList.add(new Note(NoteType.RIGHT, 6322));
        this.noteList.add(new Note(NoteType.DOWN, 6334));
        this.noteList.add(new Note(NoteType.RIGHT, 6346));
        this.noteList.add(new Note(NoteType.RIGHT, 6360));
        this.noteList.add(new Note(NoteType.RIGHT, 6372));
        this.noteList.add(new Note(NoteType.DOWN, 6382));
        this.noteList.add(new Note(NoteType.DOWN, 6394));
        this.noteList.add(new Note(NoteType.LEFT, 6406));
        this.noteList.add(new Note(NoteType.RIGHT, 6420));
        this.noteList.add(new Note(NoteType.UP, 6432));
        this.noteList.add(new Note(NoteType.UP, 6442));
        this.noteList.add(new Note(NoteType.LEFT, 6454));
        this.noteList.add(new Note(NoteType.DOWN, 6466));
        this.noteList.add(new Note(NoteType.DOWN, 6480));
        this.noteList.add(new Note(NoteType.DOWN, 6492));
        this.noteList.add(new Note(NoteType.UP, 6502));
        this.noteList.add(new Note(NoteType.RIGHT, 6514));
        this.noteList.add(new Note(NoteType.RIGHT, 6526));
        this.noteList.add(new Note(NoteType.DOWN, 6540));
        this.noteList.add(new Note(NoteType.RIGHT, 6552));
        this.noteList.add(new Note(NoteType.RIGHT, 6562));
        this.noteList.add(new Note(NoteType.RIGHT, 6574));
        this.noteList.add(new Note(NoteType.DOWN, 6586));
        this.noteList.add(new Note(NoteType.LEFT, 6600));
        this.noteList.add(new Note(NoteType.RIGHT, 6622));
        this.noteList.add(new Note(NoteType.UP, 6634));
        this.noteList.add(new Note(NoteType.LEFT, 6646));
        this.noteList.add(new Note(NoteType.DOWN, 6672));
        this.noteList.add(new Note(NoteType.DOWN, 6682));
        this.noteList.add(new Note(NoteType.DOWN, 6694));
        this.noteList.add(new Note(NoteType.UP, 6706));
        this.noteList.add(new Note(NoteType.RIGHT, 6720));
        this.noteList.add(new Note(NoteType.RIGHT, 6732));
        this.noteList.add(new Note(NoteType.DOWN, 6742));
        this.noteList.add(new Note(NoteType.RIGHT, 6754));
        this.noteList.add(new Note(NoteType.RIGHT, 6766));
        this.noteList.add(new Note(NoteType.DOWN, 6780));
        this.noteList.add(new Note(NoteType.DOWN, 6792));
        this.noteList.add(new Note(NoteType.LEFT, 6802));
        this.noteList.add(new Note(NoteType.RIGHT, 6814));
        this.noteList.add(new Note(NoteType.UP, 6826));
        this.noteList.add(new Note(NoteType.UP, 6840));
        this.noteList.add(new Note(NoteType.LEFT, 6862));
        this.noteList.add(new Note(NoteType.DOWN, 6874));
        this.noteList.add(new Note(NoteType.DOWN, 6886));
        this.noteList.add(new Note(NoteType.UP, 6912));
        this.noteList.add(new Note(NoteType.RIGHT, 6922));
        this.noteList.add(new Note(NoteType.RIGHT, 6934));
        this.noteList.add(new Note(NoteType.DOWN, 6946));
        this.noteList.add(new Note(NoteType.RIGHT, 6960));
        this.noteList.add(new Note(NoteType.RIGHT, 6972));
        this.noteList.add(new Note(NoteType.DOWN, 6982));
        this.noteList.add(new Note(NoteType.DOWN, 6994));
        this.noteList.add(new Note(NoteType.LEFT, 7006));
        this.noteList.add(new Note(NoteType.RIGHT, 7032));
        this.noteList.add(new Note(NoteType.UP, 7042));
        this.noteList.add(new Note(NoteType.DOWN, 7054));
        this.noteList.add(new Note(NoteType.LEFT, 7066));
        break;
      case 5:
        this.noteList.add(new Note(NoteType.LEFT, 300));
        this.noteList.add(new Note(NoteType.DOWN, 312));
        this.noteList.add(new Note(NoteType.UP, 322));
        this.noteList.add(new Note(NoteType.RIGHT, 334));
        this.noteList.add(new Note(NoteType.RIGHT, 360));
        this.noteList.add(new Note(NoteType.LEFT, 480));
        this.noteList.add(new Note(NoteType.RIGHT, 504));
        this.noteList.add(new Note(NoteType.DOWN, 526));
        this.noteList.add(new Note(NoteType.UP, 552));
        this.noteList.add(new Note(NoteType.LEFT, 574));
        this.noteList.add(new Note(NoteType.RIGHT, 612));
        this.noteList.add(new Note(NoteType.UP, 684));
        this.noteList.add(new Note(NoteType.DOWN, 744));
        this.noteList.add(new Note(NoteType.LEFT, 814));
        this.noteList.add(new Note(NoteType.RIGHT, 886));
        this.noteList.add(new Note(NoteType.UP, 900));
        this.noteList.add(new Note(NoteType.DOWN, 912));
        this.noteList.add(new Note(NoteType.LEFT, 924));
        this.noteList.add(new Note(NoteType.RIGHT, 934));
        this.noteList.add(new Note(NoteType.UP, 946));
        this.noteList.add(new Note(NoteType.DOWN, 960));
        this.noteList.add(new Note(NoteType.LEFT, 972));
        this.noteList.add(new Note(NoteType.UP, 984));
        this.noteList.add(new Note(NoteType.UP, 994));
        this.noteList.add(new Note(NoteType.DOWN, 1066));
        this.noteList.add(new Note(NoteType.LEFT, 1126));
        this.noteList.add(new Note(NoteType.DOWN, 1246));
        this.noteList.add(new Note(NoteType.UP, 1246));
        this.noteList.add(new Note(NoteType.DOWN, 1260));
        this.noteList.add(new Note(NoteType.UP, 1260));
        this.noteList.add(new Note(NoteType.RIGHT, 1380));
        this.noteList.add(new Note(NoteType.UP, 1392));
        this.noteList.add(new Note(NoteType.DOWN, 1404));
        this.noteList.add(new Note(NoteType.LEFT, 1464));
        this.noteList.add(new Note(NoteType.DOWN, 1486));
        this.noteList.add(new Note(NoteType.RIGHT, 1524));
        this.noteList.add(new Note(NoteType.UP, 1584));
        this.noteList.add(new Note(NoteType.DOWN, 1644));
        this.noteList.add(new Note(NoteType.LEFT, 1714));
        this.noteList.add(new Note(NoteType.DOWN, 1740));
        this.noteList.add(new Note(NoteType.UP, 1774));
        this.noteList.add(new Note(NoteType.RIGHT, 1800));
        this.noteList.add(new Note(NoteType.RIGHT, 1812));
        this.noteList.add(new Note(NoteType.RIGHT, 1824));
        this.noteList.add(new Note(NoteType.RIGHT, 1834));
        this.noteList.add(new Note(NoteType.RIGHT, 1846));
        this.noteList.add(new Note(NoteType.RIGHT, 1860));
        this.noteList.add(new Note(NoteType.RIGHT, 1872));
        this.noteList.add(new Note(NoteType.RIGHT, 1884));
        this.noteList.add(new Note(NoteType.RIGHT, 1894));
        this.noteList.add(new Note(NoteType.DOWN, 1906));
        this.noteList.add(new Note(NoteType.UP, 1906));
        this.noteList.add(new Note(NoteType.LEFT, 1932));
        this.noteList.add(new Note(NoteType.RIGHT, 1932));
        this.noteList.add(new Note(NoteType.DOWN, 1942));
        this.noteList.add(new Note(NoteType.UP, 1942));
        this.noteList.add(new Note(NoteType.DOWN, 1954));
        this.noteList.add(new Note(NoteType.UP, 1954));
        this.noteList.add(new Note(NoteType.DOWN, 1966));
        this.noteList.add(new Note(NoteType.UP, 1966));
        this.noteList.add(new Note(NoteType.LEFT, 2004));
        this.noteList.add(new Note(NoteType.RIGHT, 2004));
        this.noteList.add(new Note(NoteType.DOWN, 2040));
        this.noteList.add(new Note(NoteType.UP, 2040));
        this.noteList.add(new Note(NoteType.LEFT, 2064));
        this.noteList.add(new Note(NoteType.RIGHT, 2064));
        this.noteList.add(new Note(NoteType.DOWN, 2076));
        this.noteList.add(new Note(NoteType.UP, 2076));
        this.noteList.add(new Note(NoteType.DOWN, 2088));
        this.noteList.add(new Note(NoteType.UP, 2088));
        this.noteList.add(new Note(NoteType.DOWN, 2100));
        this.noteList.add(new Note(NoteType.UP, 2100));
        this.noteList.add(new Note(NoteType.LEFT, 2172));
        this.noteList.add(new Note(NoteType.RIGHT, 2172));
        this.noteList.add(new Note(NoteType.LEFT, 2194));
        this.noteList.add(new Note(NoteType.RIGHT, 2194));
        this.noteList.add(new Note(NoteType.DOWN, 2206));
        this.noteList.add(new Note(NoteType.UP, 2206));
        this.noteList.add(new Note(NoteType.DOWN, 2220));
        this.noteList.add(new Note(NoteType.UP, 2220));
        this.noteList.add(new Note(NoteType.DOWN, 2232));
        this.noteList.add(new Note(NoteType.UP, 2232));
        this.noteList.add(new Note(NoteType.DOWN, 2254));
        this.noteList.add(new Note(NoteType.UP, 2254));
        this.noteList.add(new Note(NoteType.DOWN, 2292));
        this.noteList.add(new Note(NoteType.UP, 2302));
        this.noteList.add(new Note(NoteType.DOWN, 2314));
        this.noteList.add(new Note(NoteType.UP, 2326));
        this.noteList.add(new Note(NoteType.DOWN, 2340));
        this.noteList.add(new Note(NoteType.UP, 2352));
        this.noteList.add(new Note(NoteType.DOWN, 2364));
        this.noteList.add(new Note(NoteType.UP, 2374));
        this.noteList.add(new Note(NoteType.DOWN, 2386));
        this.noteList.add(new Note(NoteType.UP, 2400));
        this.noteList.add(new Note(NoteType.DOWN, 2412));
        this.noteList.add(new Note(NoteType.UP, 2424));
        this.noteList.add(new Note(NoteType.RIGHT, 2604));
        this.noteList.add(new Note(NoteType.RIGHT, 2746));
        this.noteList.add(new Note(NoteType.UP, 2760));
        this.noteList.add(new Note(NoteType.RIGHT, 2772));
        this.noteList.add(new Note(NoteType.UP, 2784));
        this.noteList.add(new Note(NoteType.RIGHT, 2794));
        this.noteList.add(new Note(NoteType.UP, 2806));
        this.noteList.add(new Note(NoteType.RIGHT, 2820));
        this.noteList.add(new Note(NoteType.UP, 2832));
        this.noteList.add(new Note(NoteType.RIGHT, 2844));
        this.noteList.add(new Note(NoteType.UP, 2854));
        this.noteList.add(new Note(NoteType.RIGHT, 2866));
        this.noteList.add(new Note(NoteType.UP, 2880));
        this.noteList.add(new Note(NoteType.RIGHT, 2892));
        this.noteList.add(new Note(NoteType.DOWN, 2904));
        this.noteList.add(new Note(NoteType.UP, 2904));
        this.noteList.add(new Note(NoteType.RIGHT, 2914));
        this.noteList.add(new Note(NoteType.LEFT, 2926));
        this.noteList.add(new Note(NoteType.UP, 2926));
        this.noteList.add(new Note(NoteType.RIGHT, 2940));
        this.noteList.add(new Note(NoteType.UP, 2952));
        this.noteList.add(new Note(NoteType.RIGHT, 2964));
        this.noteList.add(new Note(NoteType.UP, 2974));
        this.noteList.add(new Note(NoteType.RIGHT, 2986));
        this.noteList.add(new Note(NoteType.UP, 3000));
        this.noteList.add(new Note(NoteType.RIGHT, 3012));
        this.noteList.add(new Note(NoteType.UP, 3024));
        this.noteList.add(new Note(NoteType.RIGHT, 3034));
        this.noteList.add(new Note(NoteType.UP, 3046));
        this.noteList.add(new Note(NoteType.RIGHT, 3060));
        this.noteList.add(new Note(NoteType.LEFT, 3072));
        this.noteList.add(new Note(NoteType.UP, 3072));
        this.noteList.add(new Note(NoteType.RIGHT, 3084));
        this.noteList.add(new Note(NoteType.DOWN, 3094));
        this.noteList.add(new Note(NoteType.UP, 3094));
        this.noteList.add(new Note(NoteType.RIGHT, 3106));
        this.noteList.add(new Note(NoteType.UP, 3120));
        this.noteList.add(new Note(NoteType.RIGHT, 3132));
        this.noteList.add(new Note(NoteType.UP, 3144));
        this.noteList.add(new Note(NoteType.RIGHT, 3154));
        this.noteList.add(new Note(NoteType.UP, 3166));
        this.noteList.add(new Note(NoteType.RIGHT, 3180));
        this.noteList.add(new Note(NoteType.UP, 3192));
        this.noteList.add(new Note(NoteType.RIGHT, 3204));
        this.noteList.add(new Note(NoteType.UP, 3214));
        this.noteList.add(new Note(NoteType.RIGHT, 3226));
        this.noteList.add(new Note(NoteType.UP, 3240));
        this.noteList.add(new Note(NoteType.RIGHT, 3252));
        this.noteList.add(new Note(NoteType.UP, 3264));
        this.noteList.add(new Note(NoteType.RIGHT, 3274));
        this.noteList.add(new Note(NoteType.UP, 3286));
        this.noteList.add(new Note(NoteType.DOWN, 3300));
        this.noteList.add(new Note(NoteType.RIGHT, 3300));
        this.noteList.add(new Note(NoteType.UP, 3312));
        this.noteList.add(new Note(NoteType.RIGHT, 3324));
        this.noteList.add(new Note(NoteType.UP, 3334));
        this.noteList.add(new Note(NoteType.RIGHT, 3346));
        this.noteList.add(new Note(NoteType.UP, 3360));
        this.noteList.add(new Note(NoteType.RIGHT, 3372));
        this.noteList.add(new Note(NoteType.UP, 3384));
        this.noteList.add(new Note(NoteType.LEFT, 3394));
        this.noteList.add(new Note(NoteType.RIGHT, 3394));
        this.noteList.add(new Note(NoteType.UP, 3406));
        this.noteList.add(new Note(NoteType.RIGHT, 3420));
        this.noteList.add(new Note(NoteType.DOWN, 3432));
        this.noteList.add(new Note(NoteType.UP, 3432));
        this.noteList.add(new Note(NoteType.RIGHT, 3444));
        this.noteList.add(new Note(NoteType.UP, 3454));
        this.noteList.add(new Note(NoteType.RIGHT, 3466));
        this.noteList.add(new Note(NoteType.UP, 3480));
        this.noteList.add(new Note(NoteType.DOWN, 3492));
        this.noteList.add(new Note(NoteType.LEFT, 3504));
        this.noteList.add(new Note(NoteType.DOWN, 3514));
        this.noteList.add(new Note(NoteType.LEFT, 3526));
        this.noteList.add(new Note(NoteType.DOWN, 3540));
        this.noteList.add(new Note(NoteType.LEFT, 3552));
        this.noteList.add(new Note(NoteType.DOWN, 3564));
        this.noteList.add(new Note(NoteType.LEFT, 3574));
        this.noteList.add(new Note(NoteType.DOWN, 3586));
        this.noteList.add(new Note(NoteType.LEFT, 3600));
        this.noteList.add(new Note(NoteType.DOWN, 3612));
        this.noteList.add(new Note(NoteType.LEFT, 3624));
        this.noteList.add(new Note(NoteType.DOWN, 3634));
        this.noteList.add(new Note(NoteType.LEFT, 3646));
        this.noteList.add(new Note(NoteType.DOWN, 3660));
        this.noteList.add(new Note(NoteType.LEFT, 3672));
        this.noteList.add(new Note(NoteType.DOWN, 3684));
        this.noteList.add(new Note(NoteType.LEFT, 3694));
        this.noteList.add(new Note(NoteType.DOWN, 3706));
        this.noteList.add(new Note(NoteType.LEFT, 3720));
        this.noteList.add(new Note(NoteType.DOWN, 3732));
        this.noteList.add(new Note(NoteType.UP, 3732));
        this.noteList.add(new Note(NoteType.LEFT, 3744));
        this.noteList.add(new Note(NoteType.DOWN, 3754));
        this.noteList.add(new Note(NoteType.RIGHT, 3754));
        this.noteList.add(new Note(NoteType.LEFT, 3766));
        this.noteList.add(new Note(NoteType.DOWN, 3780));
        this.noteList.add(new Note(NoteType.LEFT, 3792));
        this.noteList.add(new Note(NoteType.DOWN, 3804));
        this.noteList.add(new Note(NoteType.LEFT, 3814));
        this.noteList.add(new Note(NoteType.DOWN, 3826));
        this.noteList.add(new Note(NoteType.LEFT, 3840));
        this.noteList.add(new Note(NoteType.DOWN, 3852));
        this.noteList.add(new Note(NoteType.RIGHT, 3852));
        this.noteList.add(new Note(NoteType.LEFT, 3864));
        this.noteList.add(new Note(NoteType.DOWN, 3874));
        this.noteList.add(new Note(NoteType.LEFT, 3886));
        this.noteList.add(new Note(NoteType.DOWN, 3900));
        this.noteList.add(new Note(NoteType.LEFT, 3912));
        this.noteList.add(new Note(NoteType.DOWN, 3924));
        this.noteList.add(new Note(NoteType.LEFT, 3934));
        this.noteList.add(new Note(NoteType.DOWN, 3946));
        this.noteList.add(new Note(NoteType.LEFT, 3960));
        this.noteList.add(new Note(NoteType.DOWN, 3972));
        this.noteList.add(new Note(NoteType.LEFT, 3984));
        this.noteList.add(new Note(NoteType.DOWN, 3994));
        this.noteList.add(new Note(NoteType.UP, 3994));
        this.noteList.add(new Note(NoteType.LEFT, 4006));
        this.noteList.add(new Note(NoteType.DOWN, 4020));
        this.noteList.add(new Note(NoteType.RIGHT, 4020));
        this.noteList.add(new Note(NoteType.LEFT, 4032));
        this.noteList.add(new Note(NoteType.DOWN, 4044));
        this.noteList.add(new Note(NoteType.LEFT, 4054));
        this.noteList.add(new Note(NoteType.DOWN, 4066));
        this.noteList.add(new Note(NoteType.LEFT, 4080));
        this.noteList.add(new Note(NoteType.UP, 4080));
        this.noteList.add(new Note(NoteType.DOWN, 4092));
        this.noteList.add(new Note(NoteType.LEFT, 4104));
        this.noteList.add(new Note(NoteType.DOWN, 4114));
        this.noteList.add(new Note(NoteType.LEFT, 4126));
        this.noteList.add(new Note(NoteType.RIGHT, 4126));
        this.noteList.add(new Note(NoteType.DOWN, 4140));
        this.noteList.add(new Note(NoteType.LEFT, 4152));
        this.noteList.add(new Note(NoteType.DOWN, 4164));
        this.noteList.add(new Note(NoteType.LEFT, 4174));
        this.noteList.add(new Note(NoteType.DOWN, 4186));
        this.noteList.add(new Note(NoteType.LEFT, 4200));
        this.noteList.add(new Note(NoteType.DOWN, 4224));
        this.noteList.add(new Note(NoteType.UP, 4224));
        this.noteList.add(new Note(NoteType.UP, 4306));
        this.noteList.add(new Note(NoteType.DOWN, 4404));
        this.noteList.add(new Note(NoteType.RIGHT, 4572));
        this.noteList.add(new Note(NoteType.UP, 4584));
        this.noteList.add(new Note(NoteType.DOWN, 4594));
        this.noteList.add(new Note(NoteType.LEFT, 4774));
        this.noteList.add(new Note(NoteType.DOWN, 4824));
        this.noteList.add(new Note(NoteType.UP, 4846));
        this.noteList.add(new Note(NoteType.RIGHT, 4894));
        this.noteList.add(new Note(NoteType.DOWN, 4966));
        this.noteList.add(new Note(NoteType.UP, 4966));
        this.noteList.add(new Note(NoteType.RIGHT, 5052));
        this.noteList.add(new Note(NoteType.UP, 5146));
        this.noteList.add(new Note(NoteType.LEFT, 5304));
        this.noteList.add(new Note(NoteType.DOWN, 5316));
        this.noteList.add(new Note(NoteType.UP, 5328));
        this.noteList.add(new Note(NoteType.RIGHT, 5520));
        this.noteList.add(new Note(NoteType.UP, 5554));
        this.noteList.add(new Note(NoteType.DOWN, 5580));
        this.noteList.add(new Note(NoteType.LEFT, 5626));
        this.noteList.add(new Note(NoteType.DOWN, 5700));
        this.noteList.add(new Note(NoteType.UP, 5700));
        this.noteList.add(new Note(NoteType.RIGHT, 5892));
        this.noteList.add(new Note(NoteType.RIGHT, 5926));
        this.noteList.add(new Note(NoteType.RIGHT, 5940));
        this.noteList.add(new Note(NoteType.RIGHT, 5952));
        this.noteList.add(new Note(NoteType.UP, 5974));
        this.noteList.add(new Note(NoteType.UP, 6024));
        this.noteList.add(new Note(NoteType.UP, 6034));
        this.noteList.add(new Note(NoteType.UP, 6046));
        this.noteList.add(new Note(NoteType.DOWN, 6072));
        this.noteList.add(new Note(NoteType.DOWN, 6120));
        this.noteList.add(new Note(NoteType.DOWN, 6132));
        this.noteList.add(new Note(NoteType.DOWN, 6144));
        this.noteList.add(new Note(NoteType.LEFT, 6166));
      default:
        break;
    }
    */
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
    if(this.cut != 1002) return;
    this.renderIndicators();
    this.launchNotes();
    this.renderNotes();
    this.clearNotes();
    this.updateNotes();
  }

  private void launchNotes(){
    if(noteProgressPointer == noteList.size()) return;
    Note nextNote = noteList.get(noteProgressPointer);
    if(nextNote.frameIndex + 104 + 325 /*fix this garbage */  == this.frameIndex){
      launchedNotes.add(new LaunchedNote(nextNote.noteType));
      noteProgressPointer++;
      launchNotes(); //no bitwise math here, just check next index
    }
  }
  private void updateNotes(){
    this.frameIndex +=1;
    for(LaunchedNote launchedNote : launchedNotes){
      launchedNote.frameStep();
    }
  }
  private void clearNotes(){
    for(int i = 0; i < launchedNotes.size(); i++){
      if(launchedNotes.get(i).finished){
        launchedNotes.remove(i);
        i -= 1;
      }
    }
  }
  private void renderNotes(){

    for(LaunchedNote launchedNote : launchedNotes){
      MV mv = new MV();
      mv.identity();
      mv.scale(24);
      mv.transfer.set(launchedNote.position);
      mv.rotateZ(launchedNote.rotation);
      final RenderEngine.QueuedModel<?> indicatorUp =  RENDERER.queueOrthoModel(this.arrow, mv);
      indicatorUp.texture(this.arrowTexture);
    }
  }
  private void renderIndicators(){
    final float y = guitarMode ? 200.0f : 40.0f;
    MV mv = new MV();
    mv.identity();
    mv.scale(24);
    mv.transfer.set(204.0f,y,1.0f);
    final RenderEngine.QueuedModel<?> indicatorUp =  RENDERER.queueOrthoModel(this.arrow, mv);
    indicatorUp.texture(this.arrowTexture);
    mv.rotateZ(1 * 1.5708f);
    mv.transfer.set(244.0f,y,1.0f);
    final RenderEngine.QueuedModel<?> indicatorRight =  RENDERER.queueOrthoModel(this.arrow, mv);
    indicatorRight.texture(this.arrowTexture);
    mv.rotateZ(1 * 1.5708f);
    mv.transfer.set(164.0f,y,1.0f);
    final RenderEngine.QueuedModel<?> indicatorDown =  RENDERER.queueOrthoModel(this.arrow, mv);
    indicatorDown.texture(this.arrowTexture);
    mv.rotateZ(1 * 1.5708f);
    mv.transfer.set(124.0f,y,1.0f);
    final RenderEngine.QueuedModel<?> indicatorLeft =  RENDERER.queueOrthoModel(this.arrow, mv);
    indicatorLeft.texture(this.arrowTexture);
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
    if(this.cut == 1002){
      this.arrow.delete();
      this.arrowTexture.delete();
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
    cameraInfos.put(1000, new CameraInfo(new Vector3f(39.0f,-2599.0f,-2841.0f),new Vector3f(39.0f,-2553.0f,-2786.0f),0,2345));
    cameraInfos.put(1001, new CameraInfo(new Vector3f(9.600E+1f,-8.930E+2f,-1.536E+3f),new Vector3f(9.500E+1f,-8.230E+2f,-1.378E+3f),0,817));
    cameraInfos.put(1002, new CameraInfo(new Vector3f(0.0f, -220.0f, -520.0f),new Vector3f(0.0f, -160.0f, -400.0f),0,500));
    cameraInfos.put(1003, new CameraInfo(new Vector3f( 3.600E+1f, -1.001E+3f, -1.658E+3f),new Vector3f( 0.000E+0f,  0.000E+0f,  0.000E+0f), 0, 938));
    cameraInfos.put(1004, new CameraInfo(new Vector3f( -1.400E+1f , -1.575E+3f, -1.226E+3f),new Vector3f( -1.400E+1f, -1.471E+3f, -1.141E+3f), 0, 1172));

  }


}