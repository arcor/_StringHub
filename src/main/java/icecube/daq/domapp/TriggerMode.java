package icecube.daq.domapp;

/**
 * Mirrors DSCmessageSPIstatus.h
 *
 * #define TEST_PATTERN_TRIG_MODE 0
 * #define CPU_TRIG_MODE          1
 * #define SPE_DISC_TRIG_MODE     2
 * #define FB_TRIG_MODE           3
 * #define MPE_DISC_TRIG_MODE     4
 * #define FE_PULSER_TRIG_MODE    5
 * #define MB_LED_TRIG_MODE       6
 * #define LC_UP_TRIG_MODE        7
 * #define LC_DOWN_TRIG_MODE      8
 *
 */
public enum TriggerMode {


	TEST_PATTERN(0, "test_pattern"),
	FORCED(1, "forced"),
	SPE(2, "spe"),
	FB(3, "flasher"),
	MPE(4, "mpe"),
	FE_PULSER(5, "pulser"),
	MB_LED(6, "mainboard_led"),
	LC_UP(7, "lc_up"),
	LC_DOWN(8, "lc_down");

	private final byte mode;
	private final String xmlval;

	TriggerMode(int mode, String xmlval)
	{
		this.mode = (byte) mode;
		this.xmlval = xmlval;
	}
	public byte getValue() { return mode; }

	public String getXMLvalue() { return xmlval; }


	public static TriggerMode resolve(byte val)
	{
		switch (val)
		{
			case 0: return TEST_PATTERN;
			case 1: return FORCED;
			case 2: return SPE;
			case 3: return FB;
			case 4: return MPE;
			case 5: return FE_PULSER;
			case 6: return MB_LED;
			case 7: return LC_UP;
			case 8: return LC_DOWN;

			default: return null;
		}
	}

	public static TriggerMode resolve(String xmlval)
	{
		switch (xmlval)
		{
			case "test_pattern": return TEST_PATTERN;
			case "forced": return FORCED;
			case "spe": return SPE;
			case "flasher": return FB;
			case "mpe": return MPE;
			case "pulser": return FE_PULSER;
			case "mainboard_led": return MB_LED;
			case "lc_up": return LC_UP;
			case "lc_down": return LC_DOWN;

			default: return null;
		}
	}

}
