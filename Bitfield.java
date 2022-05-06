import java.lang.StringBuilder;

public class Bitfield implements Cloneable {
	private final long [] field;
	private final boolean inverted;
	
	private Bitfield (long [] f, boolean i) {
		field = f;
		inverted = i;
	}
	
	private static int wordLengthFromBitLength (int l) {
		return (int)Math.ceil((double)(l) / 64);
	}
	
	public Bitfield (int l, boolean i) {
		if (l <= 0) {
			throw new IllegalArgumentException("Length must be positive!");
		}
		field = new long [wordLengthFromBitLength(l)];
		inverted = i;
	}
	
	public Bitfield (int l) {
		this (l, false);
	}
	
	public Bitfield () {
		this (1);
	}
	
	public Bitfield (byte [] bits, boolean i, int l) {
		if (l <= 0) {
			throw new IllegalArgumentException("Length must be positive!");
		}
		inverted = i;
		int length = wordLengthFromBitLength(Math.min(l, bits.length * 8));
		field = new long [length];
		for (int j = 0; j < length - 1; j++) {
			for (int k = 0; k < 8; k++) {
				field [j] |= bits [j * 8 + k] << k;
			}
		}
		
		for (int k = 0; k < l % 8; k++) {
			field [length - 1] |= bits [(length - 1) * 8 + k] << k;
		}
	}
	
	public Bitfield (byte [] bits, boolean i) {
		this (bits, i, bits.length * 8);
	}
	
	public Bitfield (byte [] bits, int l) {
		this (bits, false, l);
	}
	
	public Bitfield (byte [] bits) {
		this (bits, false);
	}
	
	public Bitfield (boolean [] bits, boolean i, int l) {
		if (l <= 0) {
			throw new IllegalArgumentException("Length must be positive!");
		}
		inverted = i;
		int length = wordLengthFromBitLength(Math.min(l, bits.length));
		field = new long [length];
		for (int j = 0; j < length - 1; j++) {
			for (int k = 0; k < 64; k++) {
				if (bits [j * 64 + k]) {
					field [j] |= 1 << k;
				}
			}
		}
		
		for (int k = 0; k < l % 64; k++) {
			if (bits [(length - 1) * 64 + k]) {
				field [length - 1] |= 1 << k;
			}
		}
	}
	
	public Bitfield (boolean [] bits, boolean i) {
		this(bits, i, bits.length);
	}
	
	public Bitfield (boolean [] bits, int l) {
		this(bits, false, l);
	}
	
	public Bitfield (boolean [] bits) {
		this(bits, false);
	}
	
	public Bitfield (Bitfield bf) {
		field = new long [bf.field.length];
		System.arraycopy(bf.field, 0, field, 0, bf.field.length);
		inverted = bf.inverted;
	}
	
	public Bitfield not () {
		long [] notField = new long [this.field.length];
		System.arraycopy(this.field, 0, notField, 0, this.field.length);
		return new Bitfield (notField, !this.inverted);
	}
	
	private long getLong (int index) {
		return this.inverted ? ~this.field[index] : this.field[index];
	}
	
	public Bitfield and (Bitfield arg) {
		long [] andField = new long [Math.min(this.field.length, arg.field.length)];
		boolean inv = this.inverted && arg.inverted;
		if (!inv) {
			for (int i = 0; i < andField.length; i++) {
				andField[i] = this.getLong(i) & arg.getLong(i);
			}
		} else {
			for (int i = 0; i < andField.length; i++) {
				andField[i] = ~(~this.field[i] & ~arg.field[i]);
			}
		}
		
		return new Bitfield (andField, inv);
	}
	
	public Bitfield or (Bitfield arg) {
		long [] orField = new long [Math.max(this.field.length, arg.field.length)];
		boolean inv = this.inverted || arg.inverted;
		if (inv) {
			for (int i = 0; i < orField.length; i++) {
				orField[i] = ~(this.getLong(i) | arg.getLong(i));
			}
		} else {
			for (int i = 0; i < orField.length; i++) {
				orField[i] = this.field[i] | arg.field[i];
			}
		}
		
		return new Bitfield (orField, inv);
	}
	
	private Bitfield trim () {
		int i = this.field.length - 1;
		while (i > 0 && this.field[i] == 0) {
			i--;
		}
		
		long [] trimmed = new long [i + 1];
		System.arraycopy(this.field, 0, trimmed, 0, trimmed.length);
		return new Bitfield (trimmed, this.inverted);
	}
	
	public Bitfield xor (Bitfield arg) {
		long [] xorField = new long [Math.max(this.field.length, arg.field.length)];
		boolean inv = this.inverted ^ arg.inverted;
		if (inv) {
			for (int i = 0; i < xorField.length; i++) {
				xorField[i] = ~(this.getLong(i) ^ arg.getLong(i));
			}
		} else {
			for (int i = 0; i < xorField.length; i++) {
				xorField[i] = this.getLong(i) ^ arg.getLong(i);
			}
		}
		
		return new Bitfield (xorField, inv).trim();
	}
	
	public boolean getBit (int i) {
		int wordIndex = i >> 6;
		int inWordIndex = i & 0b111111;
		
		return (wordIndex > (this.field.length - 1) || wordIndex < 0) ? this.inverted : ((this.getLong(wordIndex) << inWordIndex) == 0 ? false : true);
	}
	
	private static void setBitInArray (long [] set, int i) {
		int wordIndex = i >> 6;
		int inWordIndex = i & 0b111111;
		
		set[wordIndex] |= 1L << inWordIndex;
	}
	
	private static void unsetBitInArray (long [] unset, int i) {
		int wordIndex = i >> 6;
		int inWordIndex = i & 0b111111;
		
		unset[wordIndex] &= ~(1L << inWordIndex);
	}
	
	public Bitfield setBit (int i) {
		long [] setField = new long [Math.max(i + 1, this.field.length)];
		System.arraycopy(this.field, 0, setField, 0, this.field.length);
		if (this.inverted) {
			unsetBitInArray(setField, i);
		} else {
			setBitInArray(setField, i);
		}
		return new Bitfield(setField, this.inverted).trim();
	}
	
	public Bitfield unsetBit (int i) {
		long [] setField = new long [Math.max(i + 1, this.field.length)];
		System.arraycopy(this.field, 0, setField, 0, this.field.length);
		if (this.inverted) {
			setBitInArray(setField, i);
		} else {
			unsetBitInArray(setField, i);
		}
		return new Bitfield(setField, this.inverted).trim();
	}
	
	public Bitfield flipBit (int i) {
		return this.getBit(i) ? this.unsetBit(i) : this.setBit(i);
	}
	
	public String toString (boolean littleEndian) {
		String str = "";
		if (littleEndian) {
			StringBuilder sb;
			for (int i = this.field.length - 1; i >= 0; i--) {
				str += Long.toBinaryString(this.getLong(i));
			}
			StringBuilder sb = new StringBuilder (str);
			sb.reverse();
			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder (this.toString(true));
			sb.reverse();
			return sb.toString();
		}
	}
	
	@Override
	public String toString () {
		return this.toString(true);
	}
	
	@Override
	public Bitfield clone () {
		return new Bitfield (this);
	}
}
