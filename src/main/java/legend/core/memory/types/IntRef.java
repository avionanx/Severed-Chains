package legend.core.memory.types;

public class IntRef {
  private int val;

  public int get() {
    return this.val;
  }

  public IntRef set(final int val) {
    this.val = val;
    return this;
  }

  @Deprecated
  public IntRef set(final IntRef val) {
    return this.set(val.get());
  }

  public IntRef add(final int amount) {
    return this.set(this.get() + amount);
  }

  public IntRef add(final IntRef amount) {
    return this.add(amount.get());
  }

  public IntRef sub(final int amount) {
    return this.set(this.get() - amount);
  }

  public IntRef sub(final IntRef amount) {
    return this.sub(amount.get());
  }

  public IntRef mul(final int amount) {
    return this.set(this.get() * amount);
  }

  public IntRef mul(final IntRef amount) {
    return this.mul(amount.get());
  }

  public IntRef div(final int amount) {
    return this.set(this.get() / amount);
  }

  public IntRef div(final IntRef amount) {
    return this.div(amount.get());
  }

  public IntRef mod(final int amount) {
    return this.set(this.get() % amount);
  }

  public IntRef mod(final IntRef amount) {
    return this.mod(amount.get());
  }

  public IntRef incr() {
    return this.add(1);
  }

  public IntRef decr() {
    return this.sub(1);
  }

  public IntRef not() {
    return this.set(~this.get());
  }

  public IntRef neg() {
    return this.set(-this.get());
  }

  public IntRef and(final int val) {
    return this.set(this.get() & val);
  }

  public IntRef and(final IntRef val) {
    return this.and(val.get());
  }

  public IntRef or(final int val) {
    return this.set(this.get() | val);
  }

  public IntRef or(final IntRef val) {
    return this.or(val.get());
  }

  public IntRef xor(final int val) {
    return this.set(this.get() ^ val);
  }

  public IntRef xor(final IntRef val) {
    return this.xor(val.get());
  }

  public IntRef shl(final int bits) {
    return this.set(this.get() << bits);
  }

  public IntRef shl(final IntRef bits) {
    return this.shl(bits.get());
  }

  public IntRef shr(final int bits) {
    return this.set(this.get() >>> bits);
  }

  public IntRef shr(final IntRef bits) {
    return this.shr(bits.get());
  }

  public IntRef shra(final int bits) {
    return this.set(this.get() >> bits);
  }

  public IntRef shra(final IntRef bits) {
    return this.shra(bits.get());
  }

  public IntRef abs() {
    this.val = Math.abs(this.val);
    return this;
  }

  @Override
  public String toString() {
    return Integer.toHexString(this.get());
  }
}
