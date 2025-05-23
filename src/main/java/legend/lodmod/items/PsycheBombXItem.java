package legend.lodmod.items;

import legend.game.inventory.ItemIcon;
import legend.lodmod.LodMod;

public class PsycheBombXItem extends AttackItem {
  public PsycheBombXItem() {
    super(ItemIcon.MAGIC, 200, true, LodMod.NO_ELEMENT.get(), 0x20);
  }

  @Override
  public boolean isRepeat() {
    return true;
  }

  @Override
  public boolean isProtected() {
    return true;
  }
}
