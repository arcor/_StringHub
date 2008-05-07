package icecube.daq.domapp;

import java.io.Serializable;

public class DOMConfiguration implements Serializable
{
	private static final long serialVersionUID = 2L;

	private int hardwareMonitorInterval = 30*40000000;
	private int configMonitorInterval = 2000000000;
	private int fastMonitorInterval   = 40000000;
	private TriggerMode triggerMode = TriggerMode.SPE;
	private boolean compressionEnabled = false;
	private EngineeringRecordFormat engFormat = new EngineeringRecordFormat();
	private short[] dacs = new short[] {
			850, 2300,  350, 2250,  850, 2300,  350, 2130,
			600,  560,  800,    0, 1023,  800,  450, 1023,
			};
	private MuxState mux = MuxState.OFF;
	private short pmt_hv = -1;
	private PulserMode pulserMode = PulserMode.BEACON;
	private short pulserRate = 5;
	private LocalCoincidenceConfiguration lc = new LocalCoincidenceConfiguration();
	private boolean supernovaEnabled = false;
	private boolean supernovaSpe = true;
	private int supernovaDeadtime = 51200;
	private int scalerDeadtime = 51200;
	private boolean pedestalSubtract = false;
	private boolean simulation = false;
	private double simNoiseRate = 25.0;
	
	/** The fraction of hits that have HLC bit set (simulation only) */ 
	private double simHLCFrac = 1.0;   
	private int histoInterval = 10;
	private short histoPrescale = (short) 8;
	private boolean chargeStampATWD = false;
	private byte chargeStampAtwdChannel = -1;
	
	/** Set for injecting supernova simulation signal */
	private	boolean 		snSigEnabled = true;					
	private double 			snDistance = 10.;				
	private boolean 		effVolumeEnabled = true;		

	public DOMConfiguration()
	{

	}

	/**
	 * Copy constructor
	 */
	public DOMConfiguration(DOMConfiguration c)
	{
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
        this.snSigEnabled = c.isSnSigEnabled();
        this.snDistance = c.getSnDistance();
        this.effVolumeEnabled = c.isEffVolumeEnabled();
	}

	/**
	 * Turn on delta compression in the DOM.  Calling this function
	 * also turns off engineering format.
	 */
	public void enableDeltaCompression() { compressionEnabled = true; }

	public boolean isDeltaCompressionEnabled() { return compressionEnabled; }

	/**
	 * Turn on engineering data format and set the readout data.
	 * @param fmt the engineering data format specification.
	 */
	public void setEngineeringFormat(EngineeringRecordFormat fmt)
	{
		compressionEnabled = false;
		engFormat = fmt;
	}

	public EngineeringRecordFormat getEngineeringFormat() { return engFormat; }

	/**
	 * Set the photomultiplier tube high voltage.
	 * @param hv the PMT HV setting in DAC counts (0.5 V units).
	 */
	public void setHV(int hv) { pmt_hv = (short) hv; }

	public short getHV() { return pmt_hv; }

	public MuxState getMux() { return mux; }

	public void setLC(LocalCoincidenceConfiguration lcConfig)
	{
		lc = lcConfig;
	}

	public LocalCoincidenceConfiguration getLC() { return lc; }

	/**
	 * Set DAC
	 * @param dac the DAC channel.  Must be in range [0:15]
	 * @param val the value.
	 */
	public void setDAC(int dac, int val)
	{
		dacs[dac] = (short) val;
	}

	public short getDAC(int dac)
	{
		return dacs[dac];
	}

	/**
	 * Set the DOM triggering mode
	 * @param mode the trigger mode
	 */
	public void setTriggerMode(TriggerMode mode)
	{
		triggerMode = mode;
	}

	public TriggerMode getTriggerMode()
	{
		return triggerMode;
	}

	public void setPulserRate(int rate)
	{
		pulserRate = (short) rate;
	}

	public short getPulserRate()
	{
		return pulserRate;
	}

	public void setPulserMode(PulserMode mode)
	{
		pulserMode = mode;
	}

	public PulserMode getPulserMode()
	{
		return pulserMode;
	}

	/**
	 * Enable readout of the supernova scalers.
	 *
	 */
	public void enableSupernova()
	{
		supernovaEnabled = true;
	}

	/**
	 * Disable readout of the supernova scalers.  Normally, this is not necessary
	 * since these scalers are disabled by default.
	 *
	 */
	public void disableSupernova()
	{
		supernovaEnabled = false;
	}

	public boolean isSupernovaEnabled() { return supernovaEnabled; }

	/**
	 * Returns the value of the configuration monitoring interval.
	 * @return the configMonitorInterval
	 */
	public int getConfigMonitorInterval() {
		return configMonitorInterval;
	}

	/**
	 * @param configMonitorInterval the configMonitorInterval to set
	 */
	public void setConfigMonitorInterval(int configMonitorInterval) {
		this.configMonitorInterval = configMonitorInterval;
	}

	/**
	 * @return the hardwareMonitorInterval
	 */
	public int getHardwareMonitorInterval() {
		return hardwareMonitorInterval;
	}

	/**
	 * @param hardwareMonitorInterval the hardwareMonitorInterval to set
	 */
	public void setHardwareMonitorInterval(int hardwareMonitorInterval) {
		this.hardwareMonitorInterval = hardwareMonitorInterval;
	}

	public int getFastMonitorInterval()
	{
	    return fastMonitorInterval;
	}

	public void setFastMonitorInterval(int fastIval)
	{
	    fastMonitorInterval = fastIval;
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
	 * @return the scalerDeadtime
	 */
	public int getScalerDeadtime() {
		return scalerDeadtime;
	}

	/**
	 * @param scalerDeadtime the scalerDeadtime to set
	 */
	public void setScalerDeadtime(int scalerDeadtime) {
		this.scalerDeadtime = scalerDeadtime;
	}

	/**
	 * @return the supernovaDeadtime
	 */
	public int getSupernovaDeadtime() {
		return supernovaDeadtime;
	}

	/**
	 * @param supernovaDeadtime the supernovaDeadtime to set
	 */
	public void setSupernovaDeadtime(int supernovaDeadtime) {
		this.supernovaDeadtime = supernovaDeadtime;
	}

	/**
	 * @return the supernovaSpe
	 */
	public boolean isSupernovaSpe() {
		return supernovaSpe;
	}

	/**
	 * @param supernovaSpe the supernovaSpe to set
	 */
	public void setSupernovaSpe(boolean supernovaSpe) {
		this.supernovaSpe = supernovaSpe;
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

	/**
	 * Returns the current value of the pedestal subtraction flag
	 * @return true if the DOM
	 */
	public boolean getPedestalSubtraction()
	{
		return pedestalSubtract;
	}

	/**
	 * @return the simNoiseRate
	 */
	public double getSimNoiseRate()
	{
		return simNoiseRate;
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
	 * Returns true if this is a simulated DOM or false if it is real.
	 * @return true if simDOM, false if not
	 */
	public boolean isSimulation()
	{
		return simulation;
	}

    public short getHistoPrescale()
    {
        return histoPrescale;
    }
    
    public int getHistoInterval()
    {
        return histoInterval;
    }
    
    public boolean isAtwdChargeStamp()
    {
        return chargeStampATWD;
    }
    
    public boolean isAutoRangeChargeStamp()
    {
        return chargeStampAtwdChannel == -1;
    }

    public void useAtwdChargeStamp() 
    {
        chargeStampATWD = true;
    }
    
    public void useFadcChargeStamp()
    {
        chargeStampATWD = false;
    }
    
    public void setChargeStampAutoRange()
    {
        chargeStampAtwdChannel = -1;
    }
    
    public void setChargeStampAtwdFixedChannel(byte chan)
    {
        chargeStampAtwdChannel = chan;
    }
   
    public byte getChargeStampFixedChannel() { return (byte) (chargeStampAtwdChannel == (byte) 1 ? 1 : 0); }
    
    public void setHistoInterval(int interval) { histoInterval = interval; }
    public void setHistoPrescale(short prescale) { histoPrescale = prescale; }

    public double getSimHLCFrac()
    {
        return simHLCFrac;
    }

    public void setSimHLCFrac(double simHLCFrac)
    {
        this.simHLCFrac = simHLCFrac;
    }
 
    public boolean isSnSigEnabled() {
		return snSigEnabled;
	}

	public double getSnDistance() {
		return snDistance;
	}

	public boolean isEffVolumeEnabled() {
		return effVolumeEnabled;
	}

	public void setSnSigEnabled(boolean snSigEnabled) {
		this.snSigEnabled = snSigEnabled;
	}

	public void setSnDistance(double snDistance) {
		this.snDistance = snDistance;
	}

	public void setEffVolumeEnabled(boolean effVolumeEnabled) {
		this.effVolumeEnabled = effVolumeEnabled;
	}

}


