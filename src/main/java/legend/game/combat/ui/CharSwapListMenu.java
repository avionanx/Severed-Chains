package legend.game.combat.ui;

import legend.core.Config;
import legend.game.combat.bent.PlayerBattleEntity;
import legend.game.inventory.screens.FontOptions;
import legend.game.inventory.screens.HorizontalAlign;
import legend.game.inventory.screens.TextColour;
import legend.game.scripting.RunningScript;
import legend.game.types.CharacterData2c;

import java.util.ArrayList;
import java.util.List;

import static legend.game.SItem.characterNames_801142dc;
import static legend.game.Scus94491BpeSegment_8002.renderText;
import static legend.game.Scus94491BpeSegment_8006.battleState_8006e398;
import static legend.game.Scus94491BpeSegment_800b.gameState_800babc8;

public class CharSwapListMenu extends ListMenu {
  private final FontOptions fontOptions = new FontOptions().colour(TextColour.WHITE);

  private UiBox description;

  //private final List<String> additions = new ArrayList<>();

  private final List<Integer> characters = new ArrayList<>();
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

    final int removedState = this.hud.battle.removePlayer();
    this.hud.battle.addPlayer(removedState,this.characters.get(index));

    this.hud.deleteNamesAndPortraits();

    this.flags_02 &= ~0x8;
    this.menuState_00 = 8;
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
