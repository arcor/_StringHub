package icecube.daq.domapp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

public class DOMConfiguration implements Serializable
{
	private static final long serialVersionUID = 2L;

	private int   hardwareMonitorInterval  = 30 * 40000000;
    private int   configMonitorInterval    = 2000000000;
    private int   fastMonitorInterval      = 40000000;
	private TriggerMode   triggerMode      = TriggerMode.SPE;
	private TriggerMode   altTriggerMode   = TriggerMode.FORCED;
	private DAQMode daqMode                = DAQMode.ATWD_FADC;
	private boolean mainboardLEDOn         = false;
    private boolean       compressionEnabled   = false;
    
    private EngineeringRecordFormat engFormat = new EngineeringRecordFormat();
    
    private short[] dacs = new short[] { 
            850, 2300, 350, 2250, 
            850, 2300, 350, 2130, 
            600,  560, 800,    0, 
            1023, 800, 450, 1023 
            };
    private MuxState      mux                     = MuxState.OFF;
    private short         pmt_hv                  = -1;
    private PulserMode    pulserMode              = PulserMode.BEACON;
    private short         pulserRate              = 5;


	private LocalCoincidenceConfiguration lc      = new LocalCoincidenceConfiguration();
	private SelfLCConfiguration selfLC      = new SelfLCConfiguration();


	private boolean       supernovaEnabled        = false;
    private boolean       supernovaSpe            = true;
    private int           supernovaDeadtime       = 51200;
    private int           scalerDeadtime          = 51200;
    private boolean       pedestalSubtract        = false;
    private boolean       simulation              = false;
    private double        simNoiseRate            = 25.0;
    private Integer[]     averagePedestal;
    
    /** Boolean flag for selection of ICETOP MINBIAS mode */
    private boolean       enableMinBias           = false;

    /** Switch selecting which ATWD is used (or both) */
    private AtwdChipSelect        atwdSelect              = AtwdChipSelect.PING_PONG;

    /** The fraction of hits that have HLC bit set (simulation only) */
    private double        simHLCFrac              = 1.0;
    private int           histoInterval           = 10;
    private short      histoPrescale           = (short) 8;
    private boolean    chargeStampATWD         = false;
    private byte       chargeStampAtwdChannel  = -2;
	
	/** Set for injecting supernova simulation signal */
	private	boolean       snSigEnabled = false;					
	private double        snDistance = 10.;				
	private boolean       effVolumeEnabled = true;		

	public DOMConfiguration()
	{
	    averagePedestal = new Integer[6];
	    averagePedestal[0] = null;
        averagePedestal[1] = null;
        averagePedestal[2] = null;
        averagePedestal[3] = null;
        averagePedestal[4] = null;
        averagePedestal[5] = null;
	}

	/**
	 * Copy constructor
	 */
	public DOMConfiguration(DOMConfiguration c)
	{
	    this();
	    this.hardwareMonitorInterval = c.hardwareMonitorInterval;
	    this.compressionEnabled = c.compressionEnabled;
	    this.configMonitorInterval = c.configMonitorInterval;
	    this.dacs = c.dacs;
	    this.engFormat = c.engFormat;
	    this.lc = c.lc;
	    this.mux = c.mux;
	    this.pedestalSubtract = c.pedestalSubtract;
	    this.pmt_hv = c.pmt_hv;
	    this.pulserMode = c.pulserMode;
	    this.pulserRate = c.pulserRate;
	    this.scalerDeadtime = c.scalerDeadtime;
	    this.simNoiseRate = c.simNoiseRate;
	    this.simulation = c.simulation;
	    this.supernovaDeadtime = c.supernovaDeadtime;
	    this.supernovaEnabled = c.supernovaEnabled;
	    this.supernovaSpe = c.supernovaSpe;
	    this.triggerMode  = c.triggerMode;
	    this.altTriggerMode = c.altTriggerMode;
	    this.snSigEnabled = c.isSnSigEnabled();
	    this.snDistance = c.getSnDistance();
	    this.effVolumeEnabled = c.isEffVolumeEnabled();
	}

	public void disableMinBias() { enableMinBias = false; }
	
	/**
	 * Disable readout of the supernova scalers.  Normally, this is not necessary
	 * since these scalers are disabled by default.
	 *
	 */
	public void disableSupernova()
	{
		supernovaEnabled = false;
	}

	/**
	 * Turn on delta compression in the DOM.  Calling this function
	 * also turns off engineering format.
	 */
	public void enableDeltaCompression() { compressionEnabled = true; }

	public void enableMinBias() { enableMinBias = true; }
	/**
	 * Enable readout of the supernova scalers.
	 *
	 */
	public void enableSupernova()
	{
		supernovaEnabled = true;
	}

	public AtwdChipSelect getAtwdChipSelect() { return atwdSelect; }
	
	public byte getChargeStampChannel() 
	{ 
	    if (chargeStampAtwdChannel < 0) 
	        return (byte) (-chargeStampAtwdChannel);
	    return chargeStampAtwdChannel; 
    }

	/**
	 * Returns the value of the configuration monitoring interval.
	 * @return the configMonitorInterval
	 */
	public int getConfigMonitorInterval() {
		return configMonitorInterval;
	}

	public short getDAC(int dac)
	{
		return dacs[dac];
	}

	public EngineeringRecordFormat getEngineeringFormat() { return engFormat; }

	public int getFastMonitorInterval()
	{
	    return fastMonitorInterval;
	}

	/**
	 * @return the hardwareMonitorInterval
	 */
	public int getHardwareMonitorInterval() {
		return hardwareMonitorInterval;
	}

	public int getHistoInterval()
    {
        return histoInterval;
    }

	public short getHistoPrescale()
    {
        return histoPrescale;
    }

	public short getHV() { return pmt_hv; }

	public LocalCoincidenceConfiguration getLC() { return lc; }

	public SelfLCConfiguration getSelfLC() { return selfLC; }

	public MuxState getMux() { return mux; }

	/**
	 * Returns the current value of the pedestal subtraction flag
	 * @return true if the DOM
	 */
	public boolean getPedestalSubtraction()
	{
		return pedestalSubtract;
	}

	public PulserMode getPulserMode()
	{
		return pulserMode;
	}

	public short getPulserRate()
	{
		return pulserRate;
	}

	/**
	 * @return the scalerDeadtime
	 */
	public int getScalerDeadtime() {
		return scalerDeadtime;
	}

	public double getSimHLCFrac()
    {
        return simHLCFrac;
    }

	/**
	 * @return the simNoiseRate
	 */
	public double getSimNoiseRate()
	{
		return simNoiseRate;
	}

	public double getSnDistance() {
	    return snDistance;
    }

	/**
	 * @return the supernovaDeadtime
	 */
	public int getSupernovaDeadtime() {
		return supernovaDeadtime;
	}

	public TriggerMode getTriggerMode()
	{
		return triggerMode;
	}

	public TriggerMode getAltTriggerMode()
	{
		return altTriggerMode;
	}

	public DAQMode getDaqMode()
	{
		return daqMode;
	}

	public boolean isAtwdChargeStamp()
    {
        return chargeStampATWD;
    }
	
	public boolean isAutoRangeChargeStamp()
    {
        return chargeStampAtwdChannel < 0;
    }
	
	public boolean isDeltaCompressionEnabled() { return compressionEnabled; }

	public boolean isEffVolumeEnabled() { return effVolumeEnabled;}

	public boolean isMinBiasEnabled() { return enableMinBias; }
	
	/**
	 * Returns true if this is a simulated DOM or false if it is real.
	 * @return true if simDOM, false if not
	 */
	public boolean isSimulation()
	{
		return simulation;
	}

	public boolean isSnSigEnabled() {
	    return snSigEnabled;
    }

	public boolean isSupernovaEnabled() { return supernovaEnabled; }

	/**
	 * @return the supernovaSpe
	 */
	public boolean isSupernovaSpe() { return supernovaSpe; }

	public void setAtwdChargeStamp(boolean setval)
	{
	    chargeStampATWD = setval;
	}
	
	public void setAtwdChipSelect(AtwdChipSelect cs) { atwdSelect = cs; }

	public void setChargeStampAtwdChannel(byte chan)
    {
        chargeStampAtwdChannel = chan;
    }

	public void setChargeStampAutoRange()
    {
	    chargeStampAtwdChannel = (byte) -2;
    }

	/**
	 * @param configMonitorInterval the configMonitorInterval to set
	 */
	public void setConfigMonitorInterval(int configMonitorInterval) {
		this.configMonitorInterval = configMonitorInterval;
	}

	/**
	 * Set DAC
	 * @param dac the DAC channel.  Must be in range [0:15]
	 * @param val the value.
	 */
	public void setDAC(int dac, int val)
	{
		dacs[dac] = (short) val;
	}

	public void setEffVolumeEnabled(boolean effVolumeEnabled) {
	    this.effVolumeEnabled = effVolumeEnabled;
    }

	/**
	 * Turn on engineering data format and set the readout data.
	 * @param fmt the engineering data format specification.
	 */
	public void setEngineeringFormat(EngineeringRecordFormat fmt)
	{
		compressionEnabled = false;
		engFormat = fmt;
	}

	public void setFastMonitorInterval(int fastIval)
	{
	    fastMonitorInterval = fastIval;
	}

    /**
	 * @param hardwareMonitorInterval the hardwareMonitorInterval to set
	 */
	public void setHardwareMonitorInterval(int hardwareMonitorInterval) {
		this.hardwareMonitorInterval = hardwareMonitorInterval;
	}
    
    public void setHistoInterval(int interval) { histoInterval = interval; }
    
    public void setHistoPrescale(short prescale) { histoPrescale = prescale; }
    
    /**
	 * Set the photomultiplier tube high voltage.
	 * @param hv the PMT HV setting in DAC counts (0.5 V units).
	 */
	public void setHV(int hv) { pmt_hv = (short) hv; }

    public void setLC(LocalCoincidenceConfiguration lcConfig)
	{
		lc = lcConfig;
	}
    
    /**
	 * Set the multiplexer state
	 * @param mux
	 */
	public void setMux(MuxState mux)
	{
		this.mux = mux;
	}
    
    /**
	 * Enable / disable pedestal subtraction.  If this flag
	 * is set (true) the DataCollector will execute a
	 * pedestal computation at configure time and store the
	 * averaged pedestals into <code>$90001000:$90002000</code>
	 * @param enabled
	 */
	public void setPedestalSubtraction(boolean enabled)
	{
		pedestalSubtract = enabled;
	}
    
    public void setPulserMode(PulserMode mode)
	{
		pulserMode = mode;
	}
   
    public void setPulserRate(int rate)
	{
		pulserRate = (short) rate;
	}
    
    /**
	 * @param scalerDeadtime the scalerDeadtime to set
	 */
	public void setScalerDeadtime(int scalerDeadtime) {
		this.scalerDeadtime = scalerDeadtime;
	}
    public void setSimHLCFrac(double simHLCFrac)
    {
        this.simHLCFrac = simHLCFrac;
    }

    /**
	 * @param simNoiseRate the simNoiseRate to set
	 */
	public void setSimNoiseRate(double simNoiseRate)
	{
		this.simulation   = true;
		this.simNoiseRate = simNoiseRate;
	}

	/**
	 * Roll your own Type-II (or Type-Ib/c) stellar collapse.  This
	 * will set the simulation distance to the event in kPc.
	 * @param snDistance distance to supernova in kilo-parsec.
	 */
    public void setSnDistance(double snDistance) {
        this.snDistance = snDistance;
    }
 
    public void setSnSigEnabled(boolean snSigEnabled) {
        this.snSigEnabled = snSigEnabled;
    }

    /**
	 * @param supernovaDeadtime the supernovaDeadtime to set
	 */
	public void setSupernovaDeadtime(int supernovaDeadtime) {
		this.supernovaDeadtime = supernovaDeadtime;
	}

    /**
	 * @param supernovaSpe the supernovaSpe to set
	 */
	public void setSupernovaSpe(boolean supernovaSpe) {
		this.supernovaSpe = supernovaSpe;
	}

    /**
	 * Set the DOM triggering mode
	 * @param mode the trigger mode
	 */
	public void setTriggerMode(TriggerMode mode)
	{
		triggerMode = mode;
	}

	/**
	 * Set the alt triggering mode
	 * @param mode the alt trigger mode
	 */
	public void setAltTriggerMode(TriggerMode mode)
	{
		altTriggerMode = mode;
	}


	public void setDaqMode(DAQMode mode) { daqMode = mode; }

    public void useAtwdChargeStamp() 
    {
        chargeStampATWD = true;
    }

    public void useFadcChargeStamp()
    {
        chargeStampATWD = false;
    }

    public void setAveragePedestal(int atwdChannel, int val)
    {
        averagePedestal[atwdChannel] = val;
    }
    
    public int getAveragePedestal(int atwdChannel)
    {
        return averagePedestal[atwdChannel]; 
    }
    
    public Integer[] getAveragePedestals()
    {
        // Don't return anything unless all channels have been programmed
        if (averagePedestal[0] == null 
                || averagePedestal[1] == null 
                || averagePedestal[2] == null
                || averagePedestal[3] == null
                || averagePedestal[4] == null
                || averagePedestal[5] == null) return new Integer[0];
        return averagePedestal;
    }


    public void setMainboardLED(boolean on)
	{
		mainboardLEDOn = on;
	}

	public boolean getMainboardLEDOn()
	{
		return mainboardLEDOn;
	}

	public String prettyPrint(String ident)
	{
		return "DOMConfiguration{" + "\n" +
				ident + "  hardwareMonitorInterval   =   " + hardwareMonitorInterval + "\n" +
				ident + "  configMonitorInterval     =   " + configMonitorInterval + "\n" +
				ident + "  fastMonitorInterval       =   " + fastMonitorInterval + "\n" +
				ident + "  triggerMode               =   " + triggerMode + "\n" +
				ident + "  altTriggerMode            =   " + altTriggerMode + "\n" +
				ident + "  daqMode                   =   " + daqMode + "\n" +
				ident + "  mainboardLEDOn            =   " + mainboardLEDOn + "\n" +
				ident + "  compressionEnabled        =   " + compressionEnabled + "\n" +
//				ident + "  engFormat                 =   " + engFormat + "\n" +
				ident + "  dacs                      =   " + Arrays.toString(dacs) + "\n" +
				ident + "  mux                       =   " + mux + "\n" +
				ident + "  pmt_hv                    =   " + pmt_hv + "\n" +
				ident + "  pulserMode                =   " + pulserMode + "\n" +
				ident + "  pulserRate                =   " + pulserRate + "\n" +
				ident + "  lc                        =   " + lc.prettyPrint(ident + "                               ") + "\n" +
				ident + "  selfLc                    =   " + selfLC.prettyPrint(ident + "                               ") + "\n" +
				ident + "  supernovaEnabled          =   " + supernovaEnabled + "\n" +
				ident + "  supernovaSpe              =   " + supernovaSpe + "\n" +
				ident + "  supernovaDeadtime         =   " + supernovaDeadtime + "\n" +
				ident + "  scalerDeadtime            =   " + scalerDeadtime + "\n" +
				ident + "  pedestalSubtract          =   " + pedestalSubtract + "\n" +
				ident + "  simulation                =   " + simulation + "\n" +
				ident + "  simNoiseRate              =   " + simNoiseRate + "\n" +
				ident + "  averagePedestal           =   " + Arrays.toString(averagePedestal) + "\n" +
				ident + "  enableMinBias             =   " + enableMinBias + "\n" +
				ident + "  atwdSelect                =   " + atwdSelect + "\n" +
				ident + "  simHLCFrac                =   " + simHLCFrac + "\n" +
				ident + "  histoInterval             =   " + histoInterval + "\n" +
				ident + "  histoPrescale             =   " + histoPrescale + "\n" +
				ident + "  chargeStampATWD           =   " + chargeStampATWD + "\n" +
				ident + "  chargeStampAtwdChannel    =   " + chargeStampAtwdChannel + "\n" +
				ident + "  snSigEnabled              =   " + snSigEnabled + "\n" +
				ident + "  snDistance                =   " + snDistance + "\n" +
				ident + "  effVolumeEnabled          =   " + effVolumeEnabled + "\n" +
				ident + '}';
	}


	/**
	 * Advises on whether this configuration requires extended mode features on DOMApp.
	 *
	 * Should be considered as advisory with DOMapp providing the true enforcement
	 * of extended mode features
	 */
	public boolean requiresExtendedMode(Map<String, String> why)
	{
		// The following extended mode policies are in effect as of Nov 2020
        //            DAQ MODE
        //            fadc_atwd   : DAQ_MODE_ATWD_FADC   : 0
		//            fadc        : DAQ_MODE_FADC        : 1
		//            timestamp   : DAQ_MODE_TS          : 2
        //
		//            ALT TRIGGER MODE
		//            test_pattern     : TEST_PATTERN_TRIG_MODE   : 0<--- requires extended mode
		//            forced           : CPU_TRIG_MODE            : 1
		//            spe              : SPE_DISC_TRIG_MODE       : 2<--- requires extended mode
		//            flasher          : FB_TRIG_MODE             : 3<--- requires extended mode
		//            mpe              : MPE_DISC_TRIG_MODE       : 4<--- requires extended mode
		//            pulser           : FE_PULSER_TRIG_MODE      : 5<--- requires extended mode
		//            mainboard_led    : MB_LED_TRIG_MODE         : 6<--- requires extended mode
		//            lc_up            : LC_UP_TRIG_MODE          : 7<--- requires extended mode
		//            lc_down          : LC_DOWN_TRIG_MODE        : 8<--- requires extended mode
		//
		//            SELF LC MODE
		//            none   : SELF_LC_MODE_NONE   : 0
		//            spe    : SELF_LC_MODE_SPE    : 1
		//            mpe    : SELF_LC_MODE_MPE    : 2
        //
		//            SELF LC WINDOW
		//            <int>
		//
		//            MAINBOARD LED
		//            on    : DSC_SET_MB_LED_ON    : mst 71  <--- requires extended mode
		//            off   : DSC_SET_MB_LED_OFF   : mst 72

		boolean required = (altTriggerMode != TriggerMode.FORCED) || (mainboardLEDOn);

		if(why == null)
		{
			return required;
		}
		else
		{
			if(altTriggerMode != TriggerMode.FORCED)
			{
				why.put("altTriggerMode", altTriggerMode.getXMLvalue());
			}
			if(mainboardLEDOn)
			{
				why.put("mainboardLED", "on");
			}
		}

		return required;
	}
}


