package legend.game.combat.environment;

import legend.core.memory.Value;
import legend.core.memory.types.MemoryRef;
import legend.core.memory.types.ShortRef;

public class BattleMenuTextMetrics08 implements MemoryRef {
  public final Value ref;

  public final ShortRef u_00;
  public final ShortRef v_02;
  public final ShortRef w_04;
  public final ShortRef clutOffset_06;

  public BattleMenuTextMetrics08(final Value ref) {
    this.ref = ref;

    this.u_00 = ref.offset(2, 0x00).cast(ShortRef::new);
    this.v_02 = ref.offset(2, 0x02).cast(ShortRef::new);
    this.w_04 = ref.offset(2, 0x04).cast(ShortRef::new);
    this.clutOffset_06 = ref.offset(2, 0x06).cast(ShortRef::new);
  }

  @Override
  public long getAddress() {
    return this.ref.getAddress();
  }
}