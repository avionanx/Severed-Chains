package legend.game.combat.ui;

import legend.core.Config;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.inventory.screens.FontOptions;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.TextColour;
import legend.game.scripting.RunningScript;
import legend.game.scripting.ScriptState;
import legend.game.types.CharacterData2c;

import java.util.ArrayList;
import java.util.List;

import static legend.core.GameEngine.SCRIPTS;
import static legend.game.SItem.characterNames_801142dc;
import static legend.game.Scus94491BpeSegment.charSoundEffectsLoaded;
import static legend.game.Scus94491BpeSegment.getCharacterName;
import static legend.game.Scus94491BpeSegment.loadDir;
import static legend.game.Scus94491BpeSegment.loadFile;
import static legend.game.Scus94491BpeSegment_8002.renderText;
import static legend.game.Scus94491BpeSegment_8006.battleState_8006e398;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;
import static legend.game.combat.SBtld.loadAdditions;

public class CharSwapListMenu extends ListMenu {
  private final FontOptions fontOptions = new FontOptions().colour(TextColour.WHITE);

  private UiBox description;

  //private final List<String> additions = new ArrayList<>();

  private final List<Integer> characters = new ArrayList<>();

  private int playerCount;
  private int oldBentSlot;
  //private final MenuAdditionInfo[] menuAdditions = new MenuAdditionInfo[9];

  public CharSwapListMenu(final BattleHud hud, final PlayerBattleEntity activePlayer, final ListPosition lastPosition, final Runnable onClose) {
    super(hud, activePlayer, 186, modifyLastPosition(activePlayer, lastPosition), onClose);
    this.prepareCharacterList(activePlayer.charId_272);

    //Arrays.setAll(this.menuAdditions, i -> new MenuAdditionInfo());
    //loadAdditions(activePlayer.charId_272, this.menuAdditions);
  }

  private static ListPosition modifyLastPosition(final PlayerBattleEntity player, final ListPosition lastPosition) {
    final CharacterData2c charData = gameState_800babc8.charData_32c[player.charId_272];

    lastPosition.lastListIndex_26 = 0;
    lastPosition.lastListScroll_28 = 0;

    return lastPosition;
  }

  @Override
  protected int getListCount() {
    return this.characters.size();
  }

  @Override
  protected void drawListEntry(final int index, final int x, final int y, final int trim) {
    final CharacterData2c charData = gameState_800babc8.charData_32c[this.player_08.charId_272];

    this.fontOptions.trim(trim);
    this.fontOptions.horizontalAlign(HorizontalAlign.LEFT);
    final String characterName = characterNames_801142dc[this.characters.get(index)];
    renderText(characterName, x, y, this.fontOptions);

    /*
    renderText("/", x + 146, y, this.fontOptions);

    final String max;
    if(charData.additionLevels_1a[index] < 5) {
      max = String.valueOf(charData.additionLevels_1a[index] * 20);
    } else {
      max = "-";
    }

    renderText(max, x + 168, y, this.fontOptions);
    */
  }

  @Override
  protected void onSelection(final int index) {

  }

  @Override
  protected void onUse(final int index) {
    this.playerCount = battleState_8006e398.getPlayerCount();
    this.hud.setBattleHudVisibility(false);
    final int removedState = this.removePlayer();
    this.addPlayer(removedState,this.characters.get(index));

    this.hud.deleteNamesAndPortraits();

    this.flags_02 &= ~0x8;
    this.menuState_00 = 8;
  }
  public int removePlayer(){
    final var removedState = this.hud.battle.currentTurnBent_800c66c8;
    final int removedScriptStateId = removedState.index;
    final int combatantId = removedState.innerStruct_00.combatantIndex_26c;
    removedState.destructor_0c = (a, b) ->{};

    this.oldBentSlot = removedState.innerStruct_00.bentSlot_274;
    this.hud.battle.deallocateCombatant(removedState.innerStruct_00.combatant_144);
    this.hud.battle.removeCombatant(combatantId);
    removedState.deallocateWithChildren();
    //this.combatants_8005e398[battleState_8006e398.getMonsterCount() + removedScriptStateId - 6] = null;
    battleState_8006e398.playerBents_e40[removedState.innerStruct_00.combatant_144.charSlot_19c] = null;
    battleState_8006e398.setPlayerCount(removedState.innerStruct_00.combatant_144.charSlot_19c);
    //battleState_8006e398.allBents_e0c[removedState.index] = null;
    return removedScriptStateId;
  }
  public void addPlayer(final int removedState, final int playerId){

    final int charSlot = removedState - 6;
    //TODO
    gameState_800babc8.charIds_88[charSlot] = playerId;

    int charCount;
    for(charCount = 0; charCount < 3; charCount++) {
      if(gameState_800babc8.charIds_88[charCount] < 0) {
        break;
      }
    }

    loadAdditions();
    final int addedCombatantSlot = this.hud.battle.addCombatant(0x200 + playerId * 2, charSlot);

    final String name = "Char ID " + playerId + " (bent + " + (charSlot + 6) + ')';
    final PlayerBattleEntity bent = new PlayerBattleEntity(name, charSlot + 6, this.hud.battle.playerBattleScript_800c66fc);
    final ScriptState<PlayerBattleEntity> state = SCRIPTS.allocateScriptState(charSlot + 6, name, bent);
    state.setTicker(bent::bentLoadingTicker);
    state.setDestructor(bent::bentDestructor);
    bent.element = this.hud.battle.characterElements_800c706c[playerId].get();
    bent.combatant_144 = this.hud.battle.getCombatant(addedCombatantSlot);
    bent.charId_272 = playerId;
    bent.combatantIndex_26c = addedCombatantSlot;
    bent.model_148.coord2_14.coord.transfer.x = charCount > 2 && charSlot == 0 ? 0x900 : 0xa00;
    bent.model_148.coord2_14.coord.transfer.y = 0.0f;
    // Alternates placing characters to the right and left of the main character (offsets by -0x400 for even character counts)
    bent.model_148.coord2_14.coord.transfer.z = 0x800 * ((charSlot + 1) / 2) * (charSlot % 2 * 2 - 1) + (charCount % 2 - 1) * 0x400;
    bent.model_148.coord2_14.transforms.rotate.zero();
    battleState_8006e398.addPlayer(state);

    battleState_8006e398.setPlayerCount(this.playerCount);

    battleState_8006e398.allBents_e0c[this.oldBentSlot] = battleState_8006e398.allBents_e0c[state.innerStruct_00.bentSlot_274];
    battleState_8006e398.allBents_e0c[state.innerStruct_00.bentSlot_274] = null;
    state.innerStruct_00.bentSlot_274 = this.oldBentSlot;
    battleState_8006e398.allBentCount_800c66d0--;

    this.hud.battle.initPlayerBattleEntityStats();

    this.hud.battle.loadCombatantTmdAndAnims(this.hud.battle.getCombatant(addedCombatantSlot));
    state.innerStruct_00.combatant_144.flags_19e |= 0x2a;
    final String charName = getCharacterName(playerId).toLowerCase();

    loadFile("characters/%s/textures/combat".formatted(charName), files -> this.hud.battle.loadCharacterTim(files, state.innerStruct_00.charSlot_276));
    loadDir("characters/%s/models/combat".formatted(charName), files -> this.hud.battle.loadCharTmdAndAnims(files, state.innerStruct_00.charSlot_276));
    loadDir("characters/%s/sounds/combat".formatted(charName), files -> charSoundEffectsLoaded(files, state.innerStruct_00.charSlot_276));
    this.hud.battle.loadAttackAnimations(this.hud.battle.getCombatant(addedCombatantSlot));
  }
  @Override
  protected void onClose() {

  }

  @Override
  protected int handleTargeting() {
    return 2;
  }

  @Override
  public void getTargetingInfo(final RunningScript<?> script) {

  }

  private void prepareCharacterList(final int charId) {
    //LAB_800f83dc
    this.characters.clear();

    for(int id = 0; id < gameState_800babc8.charData_32c.length; id++){
      final var player = gameState_800babc8.charData_32c[id];

      if((player.partyFlags_04 & 0x1) == 0x1){
        boolean foundInBattle = false;
        for(int i = 0; i < battleState_8006e398.getPlayerCount(); i++){
          final var battlePlayer = battleState_8006e398.playerBents_e40[i];
          if(id == battlePlayer.innerStruct_00.charId_272) {
            foundInBattle = true;
            break;
          }
        }
        if(!foundInBattle){
          this.characters.add(id);
        }
      }

    }
  }

  @Override
  public void draw() {
    super.draw();

    if(this.menuState_00 != 0 && (this.flags_02 & 0x1) != 0) {
      //LAB_800f5f50
      if((this.flags_02 & 0x40) != 0) {
        final int listIndex = this.listScroll_1e + this.listIndex_24;

        final CharacterData2c charData = gameState_800babc8.charData_32c[this.characters.get(listIndex)];
        final int level = charData.level_12;
        final int hp = charData.hp_08;
        final int mp = charData.mp_0a;

        //Selected item description
        if(this.description == null) {
          this.description = new UiBox("Battle UI Character Description", 44, 156, 232, 14);
        }

        this.description.render(Config.changeBattleRgb() ? Config.getBattleRgb() : Config.defaultUiColour);

        this.fontOptions.trim(0);
        this.fontOptions.horizontalAlign(HorizontalAlign.CENTRE);
        renderText("LV: " + level + ", HP: " + hp + ", MP: " + mp, 160, 157, this.fontOptions);
      }
    }
  }

  @Override
  public void delete() {
    super.delete();

    if(this.description != null) {
      this.description.delete();
      this.description = null;
    }
  }
}
