package legend.game.modding.coremod.config;

import legend.game.saves.BoolConfigEntry;
import legend.game.saves.ConfigStorageLocation;

public class InverseArrowMovementConfigEntry extends BoolConfigEntry {
  public InverseArrowMovementConfigEntry(){
    super(false, ConfigStorageLocation.GLOBAL);
  }
}
