package icecube.daq.domapp;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalCoincidenceConfiguration
{
    /**
     * Enumeration of constants which control the behavior of 
     * ATWD and FADC waveform transmission depending on state
     * of LC signals from upper / lower neighbors
     * @author kael
     */
	public enum RxMode
	{
	    /**
	     * No LC required to send WF
	     */
		RXNONE((byte)0, "none"),
		/**
		 * Either UP or DOWN LC signal will cause WF x-mit
		 */
		RXEITHER((byte)1, "up-or-down"),
		/**
		 * Only state of UP LC signal matters
		 */
		RXUP((byte)2, "up"),
		/**
		 * Only state of DOWN LC signal matters
		 */
		RXDOWN((byte)3, "down"),
		/**
		 * Need both UP.AND.DOWN LC signals simultaneously
		 */
		RXBOTH((byte)4, "up-and-down"),
		/**
		 * Only send SLC header packets no matter what.
		 */
		RXHDRS((byte)5, "headers-only");

		// domapp interface value
		final byte val;

		// xml config value
		final String xmlval;

		static Map<String, RxMode> XML_LOOKUP_MAP;
		static Map<Byte, RxMode> WIRE_VAL_LOOKUP_MAP;

		static {
			Map<String,RxMode> xmlMap = new ConcurrentHashMap<>();
			Map<Byte,RxMode> ordinalMap = new ConcurrentHashMap<>();
			for (RxMode t : RxMode.values()) {
				xmlMap.put(t.xmlval, t);
				ordinalMap.put(t.val, t);
			}
			XML_LOOKUP_MAP = Collections.unmodifiableMap(xmlMap);
			WIRE_VAL_LOOKUP_MAP = Collections.unmodifiableMap(ordinalMap);
		}

		RxMode(byte val, String xmlval)
		{
			this.val = val;
			this.xmlval = xmlval;
		}

		public byte asByte() { return val; }


		public String asXML() { return xmlval; }

		public static RxMode resolve(byte val)
		{
			return WIRE_VAL_LOOKUP_MAP.get(val);
		}

		public static RxMode resolve(String xmlval)
		{
			return XML_LOOKUP_MAP.get(xmlval);
		}
	}

	public enum Source
	{
		SPE, MPE;
		public byte asByte() { return (byte) ordinal(); }
	}

	public enum TxMode
	{
		TXNONE, TXDOWN, TXUP, TXBOTH;
		public byte asByte() { return (byte) ordinal(); }
	}

	/**
	 * Enum class to handle LC types
	 * @author krokodil
	 *
	 */
	public enum Type
	{
		SOFT(1), HARD(2), FLABBY(3);
		private byte value;
		Type(int val) { this.value = (byte) val; }
		public byte asByte() { return this.value; }
	}

	private Type	type;
	private RxMode	rxMode;
	private TxMode	txMode;
	private Source	source;
	private int		preTrigger, postTrigger;
	private short[]	cableLengthUp, cableLengthDn;
	private byte	span;

	public LocalCoincidenceConfiguration()
	{
		type 		= Type.HARD;
		rxMode 		= RxMode.RXNONE;
		txMode 		= TxMode.TXBOTH;
		source		= Source.SPE;
		preTrigger	= 1000;
		postTrigger	= 1000;
		cableLengthUp = new short[] { 1000, 1000, 1000, 1000 };
		cableLengthDn = new short[] { 1000, 1000, 1000, 1000 };
		span = 1;
	}

	public short[] getCableLengthDn() {
		return cableLengthDn;
	}

	public short[] getCableLengthUp() {
		return cableLengthUp;
	}

	public int getPostTrigger()
	{
		return postTrigger;
	}

	public int getPreTrigger()
	{
		return preTrigger;
	}

	/**
	 * Returns LC mode as DOMApp byte.
	 * @return LC mode byte
	 */
	public RxMode getRxMode() { return rxMode; }

	public Source getSource()
	{
		return source;
	}

	public byte getSpan() {
		return span;
	}

	/**
	 * Returns LC Tx setting as DOMApp byte.
	 * @return byte repr of LC Tx setting
	 */
	public TxMode getTxMode()
	{
		return txMode;
	}

	/**
	 * Returns the LC type in byte format compatible with DOMApp message SET_LC_TYPE
	 * @return DOMApp code for LC type setting
	 */
	public Type getType() { return type; }

	public void setCableLengthDn(int dist, short delay) {
		this.cableLengthDn[dist] = delay;
	}

	public void setCableLengthUp(int dist, short delay) {
		this.cableLengthUp[dist] = delay;
	}

	public void setPostTrigger(int postTrigger) {
		this.postTrigger = postTrigger;
	}

	public void setPreTrigger(int preTrigger) {
		this.preTrigger = preTrigger;
	}

	public void setRxMode(RxMode mode) { rxMode = mode; }

	public void setSpan(byte span) 
	{
		this.span = span;
	}
	
	public void setSource(Source source)
	{
	    this.source = source;
	}

	/**
	 * Sets the local coincidence transmit mode.
	 * @param mode the LC transmit mode
	 */
	public void setTxMode(TxMode mode) { txMode = mode; }

	public void setType(Type type) { this.type = type; }

	public String prettyPrint(String ident)
	{
		return "LocalCoincidenceConfiguration{" + "\n" +
				ident + "  type            =   " + type + "\n" +
				ident + "  rxMode          =   " + rxMode + "\n" +
				ident + "  txMode          =   " + txMode + "\n" +
				ident + "  source          =   " + source + "\n" +
				ident + "  preTrigger      =   " + preTrigger + "\n" +
				ident + "  postTrigger     =   " + postTrigger + "\n" +
				ident + "  cableLengthUp   =   " + Arrays.toString(cableLengthUp) + "\n" +
				ident + "  cableLengthDn   =   " + Arrays.toString(cableLengthDn) + "\n" +
				ident + "  span            =   " + span + "\n" +
				ident + '}';
	}
}
