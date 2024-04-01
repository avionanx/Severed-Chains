package legend.game;

import com.github.slugify.Slugify;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import legend.core.GameEngine;
import legend.game.characters.ElementSet;
import legend.game.inventory.Equipment;
import legend.game.inventory.EquipmentRegistryEvent;
import legend.game.modding.coremod.CoreMod;
import legend.game.types.EquipmentSlot;
import legend.lodmod.LodMod;
import org.legendofdragoon.modloader.Mod;
import org.legendofdragoon.modloader.registries.RegistryId;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

import static legend.core.GameEngine.REGISTRIES;
import static legend.game.SItem.dragoonGoodsBits_800fbd08;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;

@Mod(id = BossRush.MOD_ID)
public class BossRush {
  public static final String MOD_ID = "avibossrush";
  private static final Slugify slug = Slugify.builder().underscoreSeparator(true).customReplacement("'", "").customReplacement("-", "_").build();
  public static RegistryId id(final String entryId) {
    return new RegistryId(MOD_ID, entryId);
  }
  public BossRush(){
    GameEngine.EVENTS.register(this);
  }

  public static void attachEquipments(final EquipmentRegistryEvent event){
    event.register(id(slug.slugify("old-rapier")), new Equipment(
      "Old Rapier",
      "Rusty",
      10,
      0,
      0x80,
      0x80,
      0x80,
      CoreMod.DARK_ELEMENT.get(),
      0,
      new ElementSet().add(CoreMod.DIVINE_ELEMENT.get()),
      new ElementSet().add(CoreMod.DIVINE_ELEMENT.get()),
      0,
      0,
      10,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      false,
      false,
      false,
      false,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      5,
      5,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0,
      0)
    );
  }
  public static void prepareGameState(){
    int stage = getStageIndexFromFlags();
    hideParty();
    resetPartyStats();
    setAllDLevels(0);
    switch(stage){
      case 0 -> { //Commander
        setAllLevels(1);
        gameState_800babc8.charData_32c[0].partyFlags_04 = 0x3;
        gameState_800babc8.charIds_88[0] = 0;
        gameState_800babc8.charIds_88[1] = -1;
        gameState_800babc8.charIds_88[2] = -1;
      }
      case 1 -> { //Fruegel
        setAllLevels(3);
        gameState_800babc8.charData_32c[0].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[1].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[2].partyFlags_04 = 0x3;
        gameState_800babc8.charIds_88[0] = 0;
        gameState_800babc8.charIds_88[1] = 1;
        gameState_800babc8.charIds_88[2] = 2;
      }
      case 2 -> { //Urobolus
        setAllLevels(4);
        gameState_800babc8.charData_32c[0].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[1].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[2].partyFlags_04 = 0x3;
        gameState_800babc8.charIds_88[0] = 0;
        gameState_800babc8.charIds_88[1] = 1;
        gameState_800babc8.charIds_88[2] = 2;
      }
      case 3 -> { //Sandora Elite I
        setAllLevels(5);
        gameState_800babc8.charData_32c[0].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[1].partyFlags_04 = 0x3;
        gameState_800babc8.charIds_88[0] = 0;
        gameState_800babc8.charIds_88[1] = 1;
        gameState_800babc8.charIds_88[2] = -1;
      }
      case 4 -> { //Kongol I
        setAllLevels(6);
        gameState_800babc8.charData_32c[0].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[1].partyFlags_04 = 0x3;
        gameState_800babc8.charIds_88[0] = 0;
        gameState_800babc8.charIds_88[1] = 1;
      }
      case 5 -> { //Virage I
        setAllLevels(7);
        setSpecificDLevel(0, 1);
        setSpecificDLevel(3, 1);
        gameState_800babc8.charData_32c[0].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[1].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[2].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[3].partyFlags_04 = 0x3;
        gameState_800babc8.charIds_88[0] = 0;
        gameState_800babc8.charIds_88[1] = 1;
        gameState_800babc8.charIds_88[2] = 3;
      }
      case 6 -> { //Fire Bird
        setAllLevels(8);
        setSpecificDLevel(0, 1);
        setSpecificDLevel(3, 1);
        gameState_800babc8.charData_32c[0].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[1].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[2].partyFlags_04 = 0x3;
        gameState_800babc8.charData_32c[3].partyFlags_04 = 0x3;
        gameState_800babc8.charIds_88[0] = 0;
        gameState_800babc8.charIds_88[1] = 1;
        gameState_800babc8.charIds_88[2] = 3;
      }
      default -> {
      }
    }
    prepareEquipment(stage);
    prepareBonusEquipment(stage);
    prepareInventory(stage);
    advanceStage();
  }
  private static void unlockParty(){
    for(int i = 0; i < 9; i++){
      gameState_800babc8.charData_32c[i].partyFlags_04 = 3;
    }
  }
  private static void resetPartyStats(){
    for(int i = 0; i < 9; i++){
      gameState_800babc8.charData_32c[i].dlevel_13 = 1;
      gameState_800babc8.charData_32c[i].level_12 = 1;
      gameState_800babc8.charData_32c[i].xp_00 = 0;
    }
  }
  private static void hideParty(){
    for(int i = 0; i < 9; i++){
      gameState_800babc8.charData_32c[i].partyFlags_04 = 0;
    }
  }
  private static void setAllLevels(int level){
    for(int i = 0; i < 9; i++){
      gameState_800babc8.charData_32c[i].level_12 = level;
    }
  }
  private static void setAllDLevels(int dlevel){
    gameState_800babc8.goods_19c[0] = 0;
    for(int i = 0; i < 9; i++){
      if((gameState_800babc8.charData_32c[i].partyFlags_04 & 0x1) == 1 && dlevel != 0){
        gameState_800babc8.goods_19c[0] |= (1 << dragoonGoodsBits_800fbd08[i]);
        gameState_800babc8.charData_32c[i].dlevel_13 = dlevel;
        gameState_800babc8.charData_32c[i].sp_0c = dlevel * 100;
      }
    }
  }
  private static void setSpecificDLevel(int charId, int dlevel){
    gameState_800babc8.goods_19c[0] |= (1 << dragoonGoodsBits_800fbd08[charId]);
    gameState_800babc8.charData_32c[charId].dlevel_13 = dlevel;
    gameState_800babc8.charData_32c[charId].sp_0c = dlevel * 100;
  }
  private static void prepareInventory(int stage){
    try (Reader reader = Files.newBufferedReader(Path.of("assets","bossrush","items.csv"))) {
      CSVReader csvReader = new CSVReaderBuilder(reader)
        .withSkipLines(stage)
        .build();
      String[] items = csvReader.readNext();
      if(items == null || items[0].equals("")) return;
      Iterator<String> e = Arrays.stream(items).iterator();
      while(e.hasNext()){
        String itemName = e.next();
        gameState_800babc8.items_2e9.add(REGISTRIES.items.getEntry(LodMod.MOD_ID + ":" + itemName).get());
      }
    }catch (IOException | CsvValidationException err){
      err.printStackTrace();
    }
  }
  private static void prepareEquipment(int stage){
    try (Reader reader = Files.newBufferedReader(Path.of("assets","bossrush","equipments.csv"))) {
      CSVReader csvReader = new CSVReaderBuilder(reader)
        .withSkipLines(stage)
        .build();
      String[] equipments = csvReader.readNext();
      System.out.println("length " + equipments.length);
      Iterator<String> e = Arrays.stream(equipments).iterator();
      int c = 0;
      while(e.hasNext()){
        EquipmentSlot slot = EquipmentSlot.values()[c % 5];
        gameState_800babc8.charData_32c[c/5].equipment_14.put(slot, REGISTRIES.equipment.getEntry(LodMod.MOD_ID + ":" + e.next()).get());
        c++;
      }
    }catch (IOException | CsvValidationException err){
      err.printStackTrace();
    }
  }
  private static void prepareBonusEquipment(int stage){
    try (Reader reader = Files.newBufferedReader(Path.of("assets","bossrush","bonus_equipment.csv"))) {
      CSVReader csvReader = new CSVReaderBuilder(reader)
        .withSkipLines(stage)
        .build();
      String[] bonus_equipments = csvReader.readNext();
      if(bonus_equipments == null || bonus_equipments[0].equals("")) return;
      Iterator<String> e = Arrays.stream(bonus_equipments).iterator();
      while(e.hasNext()){
        String itemName = e.next();
        gameState_800babc8.equipment_1e8.add(REGISTRIES.equipment.getEntry(LodMod.MOD_ID + ":" + itemName).get());
      }
    }catch (IOException | CsvValidationException err){
      err.printStackTrace();
    }
  }
  public static void clearItems(){
    gameState_800babc8.items_2e9.clear();
  }
  public static void clearEquipments(){
    gameState_800babc8.equipment_1e8.clear();
  }
  private static void prepareAdditions(){}

  public static int getStageIndexFromFlags(){
    int b1 = gameState_800babc8.scriptFlags2_bc.get(0x320)? 8 : 0;
    int b2 = gameState_800babc8.scriptFlags2_bc.get(0x321)? 4 : 0;
    int b3 = gameState_800babc8.scriptFlags2_bc.get(0x322)? 2 : 0;
    int b4 = gameState_800babc8.scriptFlags2_bc.get(0x323)? 1 : 0;
    return b1 + b2 + b3 + b4;
  }
  public static void advanceStage(){
    int s = getStageIndexFromFlags() + 1;

    gameState_800babc8.scriptFlags2_bc.set(0x320, (s & 8) == 8);
    gameState_800babc8.scriptFlags2_bc.set(0x321, (s & 4) == 4);
    gameState_800babc8.scriptFlags2_bc.set(0x322, (s & 2) == 2);
    gameState_800babc8.scriptFlags2_bc.set(0x323, (s & 1) == 1);
  }
}
