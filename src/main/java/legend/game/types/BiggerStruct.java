package legend.game.types;

import legend.core.memory.Value;
import legend.core.memory.types.ArrayRef;
import legend.core.memory.types.MemoryRef;
import legend.core.memory.types.Pointer;
import legend.core.memory.types.TriFunctionRef;
import legend.core.memory.types.UnsignedIntRef;

import java.util.function.Function;

public class BiggerStruct<T extends MemoryRef> implements MemoryRef {
  public static <T extends MemoryRef> Function<Value, BiggerStruct<T>> of(final Function<Value, T> constructor) {
    return ref -> new BiggerStruct<>(ref, constructor);
  }

  public static <T extends MemoryRef> Class<BiggerStruct<T>> classFor(final Class<T> t) {
    //noinspection unchecked
    return (Class<BiggerStruct<T>>)(Class<?>)BiggerStruct.class;
  }

  private final Value ref;

  public final Pointer<T> innerStruct_00;
  public final Pointer<TriFunctionRef<Integer, BiggerStruct<BigStruct>, BigStruct, Long>> callback_04;
  public final Pointer<TriFunctionRef<Integer, BiggerStruct<BigStruct>, BigStruct, Long>> callback_08;
  public final Pointer<TriFunctionRef<Integer, BiggerStruct<BigStruct>, BigStruct, Long>> callback_0c;
  public final Pointer<TriFunctionRef<Integer, BiggerStruct<BigStruct>, BigStruct, Long>> callback_10;
  /** Pointer to the script file */
  public final Pointer<ScriptFile> scriptPtr_14;
  /** Pointer to the current script command */
  public final Pointer<UnsignedIntRef> scriptCommandPtr_18;
  public final ArrayRef<Pointer<UnsignedIntRef>> commandStack_1c;
  public final ArrayRef<UnsignedIntRef> ui_44;
  /**
   * <p>Bit set - which of the pointers at the start of the struct are set</p>
   *
   * <ul>
   *   <li></li>
   * </ul>
   */
  public final UnsignedIntRef ui_60; // Note: also contained in previous array

  public final UnsignedIntRef ui_f8;
  public final UnsignedIntRef ui_fc;

  public BiggerStruct(final Value ref, final Function<Value, T> innerStructConstructor) {
    this.ref = ref;

    this.innerStruct_00 = ref.offset(4, 0x00L).cast(Pointer.deferred(4, innerStructConstructor));
    this.callback_04 = ref.offset(4, 0x04L).cast(Pointer.deferred(4, TriFunctionRef::new));
    this.callback_08 = ref.offset(4, 0x08L).cast(Pointer.deferred(4, TriFunctionRef::new));
    this.callback_0c = ref.offset(4, 0x0cL).cast(Pointer.deferred(4, TriFunctionRef::new));
    this.callback_10 = ref.offset(4, 0x10L).cast(Pointer.deferred(4, TriFunctionRef::new));
    this.scriptPtr_14 = ref.offset(4, 0x14L).cast(Pointer.deferred(4, ScriptFile::new));
    this.scriptCommandPtr_18 = ref.offset(4, 0x18L).cast(Pointer.deferred(4, UnsignedIntRef::new));
    this.commandStack_1c = ref.offset(4, 0x1cL).cast(ArrayRef.of(Pointer.classFor(UnsignedIntRef.class), 10, 4, Pointer.deferred(4, UnsignedIntRef::new)));
    this.ui_44 = ref.offset(4, 0x44L).cast(ArrayRef.of(UnsignedIntRef.class, 32, 4, UnsignedIntRef::new)); // Dunno how long this should be
    this.ui_60 = ref.offset(4, 0x60L).cast(UnsignedIntRef::new);

    this.ui_f8 = ref.offset(4, 0xf8L).cast(UnsignedIntRef::new);
    this.ui_fc = ref.offset(4, 0xfcL).cast(UnsignedIntRef::new);
  }

  @Override
  public long getAddress() {
    return this.ref.getAddress();
  }
}
