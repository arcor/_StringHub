/* -*- mode: java; indent-tabs-mode:t; tab-width:4 -*- */
package icecube.daq.configuration;

import icecube.daq.domapp.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLConfig extends DefaultHandler
{
	private ParserState internalState;

	private StringBuilder xmlChars;
	private short fadcSamples;
	private final short[] defaultAtwdSamples = { 128, 128, 128, 0 };
	private short[] atwdSamples;
	private final short[] defaultAtwdWidth = { 2, 2, 2, 2 };
	private short[] atwdWidth;
	private int atwdChannel;

	private enum Direction { UP, DOWN };
	private Direction direction;
	private int delayDistance;
	private HashMap<String, DOMConfiguration> definedDOMConfigs;
	private DOMConfiguration currentConfig;
	private static final Logger logger = Logger.getLogger(XMLConfig.class);
	private static final String[] dacNames = {
		"atwd0TriggerBias", "atwd0RampTop", "atwd0RampRate", "atwdAnalogRef",
		"atwd1TriggerBias", "atwd1RampTop", "atwd1RampRate", "frontEndPedestal",
		"mpeTriggerDiscriminator", "speTriggerDiscriminator", "fastAdcRef", "internalPulser",
		"ledBrightness", "frontEndAmpLowerClamp", "flasherDelay", "muxBias", "flasherRef"
	};
	private static final int[] dacChannels = {
		0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 14
	};

	public XMLConfig()
	{
		definedDOMConfigs = new HashMap<String, DOMConfiguration>();
		xmlChars = new StringBuilder();

	}

	/**
	 * @return a set of DOM mainboard Ids that contains the full set of
	 * DOMs covered by this configuration collection
	 */
	public Set<String> getConfiguredDomIds()
	{
		return definedDOMConfigs.keySet();
	}

	public void characters(char[] ch, int start, int length) throws SAXException
	{
		xmlChars.append(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException
	{
		String text = xmlChars.toString().trim();

		if (localName.equals("triggerMode"))
		{
			TriggerMode mode = TriggerMode.resolve(text);
			if(mode != null)
			{
				currentConfig.setTriggerMode(mode);
			}
			else
			{
				throw new SAXException(String.format("Unrecognized trigger mode: [%s]", text));
			}
		}
		else if (localName.equals("altTriggerMode"))
		{

				final TriggerMode mode = TriggerMode.resolve(text);
				if(mode != null)
				{
					currentConfig.setAltTriggerMode(mode);
				}
				else
				{
					throw new SAXException(String.format("unrecognized alt trigger mode: [%s]", text));
				}
		}
		else if (localName.equals("daqMode"))
		{
			final DAQMode mode = DAQMode.resolve(text);
			if(mode != null)
			{
				currentConfig.setDaqMode(mode);
			}
			else
			{
				throw new SAXException(String.format("unrecognized daq mode: [%s]", text));
			}
		}
		else if (localName.equals("mainboardLED"))
		{
			if (text.equals("on"))
				currentConfig.setMainboardLED(true);
			else if (text.equals("off"))
				currentConfig.setMainboardLED(false);
			else throw new SAXException(String.format("unrecognized mainboard led: [%s]", text));
		}
		else if (localName.equals("pmtHighVoltage"))
		{
			short val = Short.parseShort(text);
			currentConfig.setHV(val);
		}
		else if (localName.equals("atwdChipSelect"))
		{
			if (text.equals("A"))
				currentConfig.setAtwdChipSelect(AtwdChipSelect.ATWD_A);
			else if (text.equals("B"))
				currentConfig.setAtwdChipSelect(AtwdChipSelect.ATWD_B);
			else
				currentConfig.setAtwdChipSelect(AtwdChipSelect.PING_PONG);
		}
		else if (localName.equals("enableIceTopMinBias"))
		{
			currentConfig.enableMinBias();
		}
		else if (localName.equals("disableIceTopMinBias"))
		{
			currentConfig.disableMinBias();
		}
		else if (localName.equals("analogMux"))
		{
			if (text.equals("off"))
				currentConfig.setMux(MuxState.OFF);
			else if (text.equals("clock"))
				currentConfig.setMux(MuxState.OSC_OUTPUT);
			else if (text.equals("clock2x"))
				currentConfig.setMux(MuxState.SQUARE_40MHZ);
			else if (text.equals("onboardLedCurrent"))
				currentConfig.setMux(MuxState.LED_CURRENT);
			else if (text.equals("flasherLedCurrent"))
				currentConfig.setMux(MuxState.FB_CURRENT);
			else if (text.equals("localCoincidenceDown"))
				currentConfig.setMux(MuxState.LOWER_LC);
			else if (text.equals("localCoincidenceUp"))
				currentConfig.setMux(MuxState.UPPER_LC);
			else if (text.equals("commAdc"))
				currentConfig.setMux(MuxState.COMM_ADC_INPUT);
			else if (text.equals("internalPulser"))
				currentConfig.setMux(MuxState.FE_PULSER);
		}
		else if (localName.equals("samples"))
			atwdSamples[atwdChannel] = Short.parseShort(text);
		else if (localName.equals("width"))
			atwdWidth[atwdChannel] = Short.parseShort(text);
		else if (localName.equals("fadcSamples"))
			fadcSamples = Short.parseShort(text);
		else if (localName.equals("engineeringFormat"))
		{
			try
			{
				currentConfig.setEngineeringFormat(new EngineeringRecordFormat(fadcSamples, atwdSamples, atwdWidth));
			}
			catch (BadEngineeringFormat bex)
			{
				logger.error("Bad engineering format.");
				throw new IllegalStateException(bex);
			}
		}
		else if (localName.equals("localCoincidence"))
		{
			internalState = ParserState.DOM_CONFIG;
		}
		else if (localName.equals("selfLocalCoincidence"))
		{
			internalState = ParserState.DOM_CONFIG;
		}
		else if (localName.equals("pedestalSubtract"))
		{
			if (text.equals("true"))
				currentConfig.setPedestalSubtraction(true);
			else
				currentConfig.setPedestalSubtraction(false);
		}
		else if (localName.equals("averagePedestal"))
		{
			currentConfig.setAveragePedestal(atwdChannel, Integer.parseInt(text));
		}
		else if (localName.equals("pulserMode"))
		{
			if (text.equals("beacon"))
				currentConfig.setPulserMode(PulserMode.BEACON);
			else if (text.equals("pulser"))
				currentConfig.setPulserMode(PulserMode.PULSER);
		}
		else if (localName.equals("pulserRate"))
		{
			currentConfig.setPulserRate(Integer.parseInt(text));
		}
		else if (internalState == ParserState.CHARGE_HISTOGRAM)
		{
			if (localName.equals("source"))
			{
				if (text.equals("atwd"))
					currentConfig.useAtwdChargeStamp();
				else if (text.equals("fadc"))
					currentConfig.useFadcChargeStamp();
			}
			else if (localName.equals("prescale"))
			{
				currentConfig.setHistoPrescale(Short.parseShort(text));
			}
			else if (localName.equals("interval"))
			{
				currentConfig.setHistoInterval(Integer.parseInt(text));
			}
			else if (localName.equals("channel"))
			{
				if (text.equals("auto"))
					currentConfig.setChargeStampAutoRange();
				else
					currentConfig.setChargeStampAtwdChannel(Byte.parseByte(text));
			}
			else if (localName.equals("chargeHistogram"))
			{
				internalState = ParserState.DOM_CONFIG;
			}
		}
		else if (internalState == ParserState.LOCAL_COINCIDENCE)
		{
			if (localName.equals("type"))
			{
				if (text.equals("soft"))
					currentConfig.getLC().setType(LocalCoincidenceConfiguration.Type.SOFT);
				else if (text.equals("hard"))
					currentConfig.getLC().setType(LocalCoincidenceConfiguration.Type.HARD);
			}
			else if (localName.equals("mode"))
			{
				if (text.equals("none"))
					currentConfig.getLC().setRxMode(LocalCoincidenceConfiguration.RxMode.RXNONE);
				else if (text.equals("up-or-down"))
					currentConfig.getLC().setRxMode(LocalCoincidenceConfiguration.RxMode.RXEITHER);
				else if (text.equals("up"))
					currentConfig.getLC().setRxMode(LocalCoincidenceConfiguration.RxMode.RXUP);
				else if (text.equals("down"))
					currentConfig.getLC().setRxMode(LocalCoincidenceConfiguration.RxMode.RXDOWN);
				else if (text.equals("up-and-down"))
					currentConfig.getLC().setRxMode(LocalCoincidenceConfiguration.RxMode.RXBOTH);
				else if (text.equals("headers-only"))
					currentConfig.getLC().setRxMode(LocalCoincidenceConfiguration.RxMode.RXHDRS);
			}
			else if (localName.equals("txMode"))
			{
				if (text.equals("none"))
					currentConfig.getLC().setTxMode(LocalCoincidenceConfiguration.TxMode.TXNONE);
				else if (text.equals("both"))
					currentConfig.getLC().setTxMode(LocalCoincidenceConfiguration.TxMode.TXBOTH);
				else if (text.equals("up"))
					currentConfig.getLC().setTxMode(LocalCoincidenceConfiguration.TxMode.TXUP);
				else if (text.equals("down"))
					currentConfig.getLC().setTxMode(LocalCoincidenceConfiguration.TxMode.TXDOWN);
			}
			else if (localName.equals("source"))
			{
				if (text.equals("spe"))
					currentConfig.getLC().setSource(LocalCoincidenceConfiguration.Source.SPE);
				else if (text.equals("mpe"))
					currentConfig.getLC().setSource(LocalCoincidenceConfiguration.Source.MPE);
				else
					throw new IllegalArgumentException("LC source specifier " + text + " invalid.");
			}
			else if (localName.equals("span"))
			{
				currentConfig.getLC().setSpan(Byte.parseByte(text));
			}
			else if (localName.equals("preTrigger"))
			{
				currentConfig.getLC().setPreTrigger(Integer.parseInt(text));
			}
			else if (localName.equals("postTrigger"))
			{
				currentConfig.getLC().setPostTrigger(Integer.parseInt(text));
			}
			else if (localName.equals("cableLength"))
			{
				if (direction == Direction.DOWN)
					currentConfig.getLC().setCableLengthDn(delayDistance - 1, Short.parseShort(text));
				else
					currentConfig.getLC().setCableLengthUp(delayDistance - 1, Short.parseShort(text));
			}
		}
		else if (internalState == ParserState.SELF_LOCAL_COINCIDENCE)
		{
			if (localName.equals("mode"))
			{
				if (text.equals("mpe")) {
					currentConfig.getSelfLC().setMode(SelfLCConfiguration.SelfLCMode.SELF_LC_MODE_MPE);
				}
				else if (text.equals("spe")) {
					currentConfig.getSelfLC().setMode(SelfLCConfiguration.SelfLCMode.SELF_LC_MODE_SPE);
				}
				else if (text.equals("none")) {
					currentConfig.getSelfLC().setMode(SelfLCConfiguration.SelfLCMode.SELF_LC_MODE_NONE);
				}
				else {
					throw new SAXException(String.format("unrecognized self lc mode: [%s]", text));
				}

			}
			else if (localName.equals("window"))
			{
				currentConfig.getSelfLC().setWindow(Integer.parseInt(text));
			}
		}
		else if (localName.equals("deadtime"))
		{
			currentConfig.setSupernovaDeadtime(Integer.parseInt(text));
		}
		else if (localName.equals("disc"))
		{
			boolean spe;
			if (text.equals("spe"))
				spe = true;
			else
				spe = false;
			currentConfig.setSupernovaSpe(spe);
		}
		else if (localName.equals("scalerDeadtime"))
		{
			currentConfig.setScalerDeadtime(Integer.parseInt(text));
		}
		else if (localName.equals("hardwareMonitorInterval"))
		{
			currentConfig.setHardwareMonitorInterval((int) (40000000 * Double.parseDouble(text)));
		}
		else if (localName.equals("fastMonitorInterval"))
		{
			currentConfig.setFastMonitorInterval((int) (40000000 * Double.parseDouble(text)));
		}
		else if (localName.equals("noiseRate"))
		{
			currentConfig.setSimNoiseRate(Double.parseDouble(text));
		}
		else if (localName.equals("hlcFraction"))
		{
			currentConfig.setSimHLCFrac(Double.parseDouble(text));
		}
		else if (internalState == ParserState.DOM_CONFIG)
		{
			// Name not found - try the DACs
			for (int idac = 0; idac < dacNames.length; idac++)
			{
				if (localName.equals(dacNames[idac]))
				{
					short val = Short.parseShort(text);
					int ch = dacChannels[idac];
					currentConfig.setDAC(ch, val);
					break;
				}
			}
		}

	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
	{
		xmlChars.setLength(0);
		if (localName.equals("atwd"))
		{
			atwdChannel = Integer.parseInt(attributes.getValue("ch"));
		}
		else if (localName.equals("deltaCompressed"))
		{
			currentConfig.enableDeltaCompression();
		}
		else if (localName.equals("supernovaMode"))
		{
			int index = attributes.getIndex("enabled");
			if (index >= 0 && attributes.getValue(index).equals("true"))
				currentConfig.enableSupernova();
			else
				currentConfig.disableSupernova();

		}
		else if (localName.equals("engineeringFormat"))
		{
			fadcSamples = 250;
			atwdSamples = new short[4];
			System.arraycopy(defaultAtwdSamples, 0, atwdSamples, 0, 4);
			atwdWidth = new short[4];
			System.arraycopy(defaultAtwdWidth, 0, atwdWidth, 0, 4);
		}
		else if (localName.equals("localCoincidence"))
		{
			internalState = ParserState.LOCAL_COINCIDENCE;
		}
		else if (localName.equals("selfLocalCoincidence"))
		{
			internalState = ParserState.SELF_LOCAL_COINCIDENCE;
		}
		else if (localName.equals("chargeHistogram"))
		{
			internalState = ParserState.CHARGE_HISTOGRAM;
		}
		else if (localName.equals("chargeStamp"))
		{
			if (attributes.getValue("type").equals("atwd"))
				currentConfig.setAtwdChargeStamp(true);
			else
				currentConfig.setAtwdChargeStamp(false);
			String channel = attributes.getValue("channel");
			if (channel == null || channel.equals("auto"))
				currentConfig.setChargeStampAtwdChannel((byte) -2);
			else
				currentConfig.setChargeStampAtwdChannel(Byte.parseByte(channel));
		}
		else if (localName.equals("cableLength"))
		{
			if (attributes.getValue("dir").equals("up"))
				direction = Direction.UP;
			else
				direction = Direction.DOWN;
			delayDistance = Integer.parseInt(attributes.getValue("dist"));
		}
		else if (localName.equals("domConfig"))
		{
			currentConfig = new DOMConfiguration();
			final String mbid = attributes.getValue("mbid");
			addDOMConfig(mbid, currentConfig);
			internalState = ParserState.DOM_CONFIG;
		}
		else if (localName.equals("averagePedestal"))
		{
			String atwdChip = attributes.getValue("atwd");
			int channel = Integer.parseInt(attributes.getValue("ch"));
			if (atwdChip.equalsIgnoreCase("A"))
				atwdChannel = channel;
			else if (atwdChip.equalsIgnoreCase("B"))
				atwdChannel = channel + 3;
		}
	}

	public void parseXMLConfig(InputStream xmlIn) throws Exception
	{
		// final String schemaPath = "domconfig.xsd";
		SAXParserFactory saxFactory = SAXParserFactory.newInstance();
		// SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		// ClassLoader cl = Thread.currentThread().getContextClassLoader();
		// InputStream schemaStream = XMLConfig.class.getResourceAsStream(schemaPath);
		// if (schemaStream == null) throw new FileNotFoundException(schemaPath);
		// Schema schema = schemaFactory.newSchema(new StreamSource(schemaStream));
		// saxFactory.setSchema(schema);
		saxFactory.setNamespaceAware(true);
		SAXParser parser = saxFactory.newSAXParser();
		try
		{
			long t0 = System.currentTimeMillis();
			parser.parse(xmlIn, this);
			if (logger.isDebugEnabled()) {
				logger.debug("XML parsing completed - took " +
							 (System.currentTimeMillis() - t0) +
							 " milliseconds.");
			}
		}
		catch (Exception except)
		{
			except.printStackTrace();
			throw except;
		}
	}

	public void addDOMConfig(String mbid, DOMConfiguration domCfg)
	{
		if (definedDOMConfigs.containsKey(mbid)) {
			logger.error("Warning, overwriting DOM " + mbid +
						 " configuration");
		}

		definedDOMConfigs.put(mbid, domCfg);
	}

	public DOMConfiguration getDOMConfig(String mbid)
	{
		return definedDOMConfigs.get(mbid);
	}

}

enum ParserState
{
	INIT, DOM_CONFIG, LOCAL_COINCIDENCE, CHARGE_HISTOGRAM, SELF_LOCAL_COINCIDENCE
};
