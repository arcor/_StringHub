package icecube.daq.domapp;

import icecube.daq.domapp.LocalCoincidenceConfiguration.Source;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.apache.log4j.Logger;

public class DOMApp implements IDOMApp
{

    private static Logger logger = Logger.getLogger(DOMApp.class);
    private DOMIO         devIO;
    private ByteBuffer    msgBuffer;
    private ByteBuffer    msgBufferOut;

    /**
     * Create a new DOMApp connection object for DOR channel provided
     * 
     * @param ch -
     *            DOR channel serial # - for 'standard' DOR = 8*card + 2*pair +
     *            dom
     */
    public DOMApp(int card, int pair, char dom) throws IOException
    {
        devIO = new DOMIO(card, pair, dom);
        msgBuffer = ByteBuffer.allocate(4092);
        msgBufferOut = ByteBuffer.allocate(4092);
    }

    public void close()
    {
        devIO.close();
    }

    public void beginFlasherRun(short brightness, short width, short delay, short mask, short rate)
            throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.putShort(brightness).putShort(width).putShort(delay).putShort(mask).putShort(rate).flip();
        sendMessage(MessageType.BEGIN_FB_RUN, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#beginRun()
     */
    public void beginRun() throws MessageException
    {
        sendMessage(MessageType.BEGIN_RUN);
    }

    public void collectPedestals(int nAtwd0, int nAtwd1, int nFadc) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.putInt(nAtwd0).putInt(nAtwd1).putInt(nFadc).flip();
        sendMessage(MessageType.COLLECT_PEDESTALS, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#disableHV()
     */
    public void disableHV() throws MessageException
    {
        sendMessage(MessageType.DISABLE_HV);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#disableSupernova()
     */
    public void disableSupernova() throws MessageException
    {
        sendMessage(MessageType.DISABLE_SN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#enableHV()
     */
    public void enableHV() throws MessageException
    {
        sendMessage(MessageType.ENABLE_HV);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#enableSupernova(int, boolean)
     */
    public void enableSupernova(int deadtime, boolean speDisc) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(5);
        byte mode = 1;
        if (speDisc) mode = 0;
        buf.putInt(deadtime).put(mode).flip();
        sendMessage(MessageType.ENABLE_SN, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#endRun()
     */
    public void endRun() throws MessageException
    {
        sendMessage(MessageType.END_RUN);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#getData()
     */
    public ByteBuffer getData() throws MessageException
    {
        return sendMessage(MessageType.GET_DATA);
    }

    /**
     * Pack multiple messages to get data
     */
    public ArrayList<ByteBuffer> getData(int n) throws MessageException
    {
        ByteBuffer buf = (ByteBuffer) msgBuffer.clear();
        for (int i = 0; i < n; i++)
        {
            buf.put(MessageType.GET_DATA.getFacility());
            buf.put(MessageType.GET_DATA.getSubtype());
            buf.putShort((short) 0).putShort((short) 0);
            buf.put((byte) 0).put((byte) 0);
        }

        buf.flip();
        logger.debug("Sending multimessage request.");
        try
        {
            devIO.send(buf);
        }
        catch (IOException e)
        {
            throw new MessageException(e);
        }

        ArrayList<ByteBuffer> outC = new ArrayList<ByteBuffer>();

        for (int i = 0; i < n; i++)
        {
            try
            {
                Thread.yield();
                ByteBuffer out = devIO.recv();
                logger.debug("Received part " + i + " of multimessage.");
                int status = out.get(7);
                if (status != 1) throw new MessageException(MessageType.GET_DATA, status);
                if (out.remaining() > 8)
                {
                    ByteBuffer x = ByteBuffer.allocate(out.remaining() - 8);
                    out.position(8);
                    x.put(out);
                    outC.add((ByteBuffer) x.flip());
                }
            }
            catch (IOException e)
            {
                throw new MessageException(e);
            }
        }
        return outC;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#getMainboardID()
     */
    public String getMainboardID() throws MessageException
    {
        ByteBuffer buf = sendMessage(MessageType.GET_DOM_ID);
        int len = buf.remaining();
        byte[] mbid = new byte[len];
        buf.get(mbid);
        return new String(mbid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#getMoni()
     */
    public ByteBuffer getMoni() throws MessageException
    {
        return sendMessage(MessageType.GET_MONI);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#getPulserRate()
     */
    public short getPulserRate() throws MessageException
    {
        // TODO - implement
        return (short) 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#getRelease()
     */
    public String getRelease() throws MessageException
    {
        ByteBuffer buf = sendMessage(MessageType.GET_DOMAPP_RELEASE);
        int len = buf.remaining();
        byte[] rel = new byte[len];
        buf.get(rel);
        return new String(rel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#getScalerDeadtime()
     */
    public int getScalerDeadtime() throws MessageException
    {
        // TODO - implement
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#getSupernova()
     */
    public ByteBuffer getSupernova() throws MessageException
    {
        return sendMessage(MessageType.GET_SN_DATA);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#pulserOff()
     */
    public void pulserOff() throws MessageException
    {
        sendMessage(MessageType.SET_PULSER_OFF);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#pulserOn()
     */
    public void pulserOn() throws MessageException
    {
        sendMessage(MessageType.SET_PULSER_ON);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#queryHV()
     */
    public short[] queryHV() throws MessageException
    {
        ByteBuffer buf = sendMessage(MessageType.QUERY_HV);
        short[] hv = new short[2];
        hv[0] = buf.getShort();
        hv[1] = buf.getShort();
        return hv;
    }

    /**
     * Sends a message with no data payload.
     * 
     * @param type
     *            the message type and subtype
     * @return the return data payload.
     */
    protected ByteBuffer sendMessage(MessageType type) throws MessageException
    {
        return sendMessage(type, ByteBuffer.allocate(0));
    }

    /**
     * Send DOMApp message and receive response
     * 
     * @param type -
     *            the message type / subtype
     * @param in -
     *            ByteBuffer object holding data to be appended to message
     * @return ByteBuffer holding the message return data payload
     */
    protected ByteBuffer sendMessage(MessageType type, ByteBuffer in) throws MessageException
    {
        ByteBuffer buf = (ByteBuffer) msgBuffer.clear();
        // Put the message id and subtypes in 1st two bytes
        buf.put(type.getFacility()).put(type.getSubtype());
        // Then comes length of data payload
        buf.putShort((short) in.remaining());
        // Then comes 16-bits of reserved
        buf.putShort((short) 0);
        // Then comes message ID (?? - zero)
        buf.put((byte) 0);
        // Then comes the status byte - should be zero going down and return 1
        buf.put((byte) 0);
        // Tack on the data payload
        buf.put(in);
        buf.flip();
        logger.debug("sendMessage [" + type.name() + "]");
        
        msgBufferOut.clear();
        
        try
        {
            devIO.send(buf);
            
            // Loop on receive - allow partial receives
            while (msgBufferOut.position() < 8 || msgBufferOut.position() < msgBufferOut.getShort(2) + 8)
            {
                Thread.yield();
                ByteBuffer out = devIO.recv();
                msgBufferOut.put(out);
            }
            msgBufferOut.flip();
            byte r_type = msgBufferOut.get();
            byte r_subt = msgBufferOut.get();
            short dataLength = msgBufferOut.getShort();
            if (r_type != type.getFacility() || r_subt != type.getSubtype())
            {
                logger.error("Return message type/subtype does not match outgoing "
                        + "message (" 
                        + r_type + ", "
                        + r_subt + ").");
                throw new MessageException(type, 1001);
            }
            int status = msgBufferOut.get(7);
            msgBufferOut.position(8);
            if (status != 1) throw new MessageException(type, 1);
            return msgBufferOut.slice();
        }
        catch (IOException e)
        {
            throw new MessageException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setCableLengths(short[], short[])
     */
    public void setCableLengths(short[] up, short[] dn) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(16);
        for (int i = 0; i < 4; i++)
            buf.putShort(up[i]);
        for (int i = 0; i < 4; i++)
            buf.putShort(dn[i]);
        buf.flip();
        sendMessage(MessageType.SET_LC_CABLELEN, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setDeltaCompressionFormat()
     */
    public void setDeltaCompressionFormat() throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put((byte) 2).flip();
        sendMessage(MessageType.SET_COMPRESSION_MODE, buf);
        buf.rewind();
        sendMessage(MessageType.SET_DATA_FORMAT, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setEngineeringFormat(ic3.daq.domapp.EngineeringRecordFormat)
     */
    public void setEngineeringFormat(EngineeringRecordFormat fmt) throws MessageException
    {
        byte[] enc = fmt.encode();
        ByteBuffer buf = ByteBuffer.allocate(3);
        buf.put((byte) 0).flip();
        sendMessage(MessageType.SET_COMPRESSION_MODE, buf);
        buf.rewind();
        sendMessage(MessageType.SET_DATA_FORMAT, buf);
        buf.clear();
        buf.put(enc[0]).put(enc[1]).put(enc[2]).flip();
        logger.debug("Setting engineering format bytes to (" + enc[0] + ", " + enc[1] + ", " + enc[2] + ").");
        sendMessage(MessageType.SET_ENG_FORMAT, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setHV(short)
     */
    public void setHV(short dac) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(2);
        buf.putShort(dac).flip();
        sendMessage(MessageType.SET_HV, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setLCMode(ic3.daq.domapp.LocalCoincidenceConfiguration.RxMode)
     */
    public void setLCMode(LocalCoincidenceConfiguration.RxMode mode) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(mode.asByte()).flip();
        sendMessage(MessageType.SET_LC_MODE, buf);
    }

    public void setLCSource(Source src) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(src.asByte()).flip();
        sendMessage(MessageType.SET_LC_SRC, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setLCSpan(byte)
     */
    public void setLCSpan(byte span) throws MessageException
    {
        // TODO Auto-generated method stub
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(span).flip();
        sendMessage(MessageType.SET_LC_SPAN, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setLCTx(ic3.daq.domapp.LocalCoincidenceConfiguration.TxMode)
     */
    public void setLCTx(LocalCoincidenceConfiguration.TxMode mode) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(mode.asByte()).flip();
        sendMessage(MessageType.SET_LC_TX, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setLCType(ic3.daq.domapp.LocalCoincidenceConfiguration.Type)
     */
    public void setLCType(LocalCoincidenceConfiguration.Type type) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(type.asByte()).flip();
        sendMessage(MessageType.SET_LC_TYPE, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setLCWindow(int, int)
     */
    public void setLCWindow(int pre, int post) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putInt(pre).putInt(post).flip();
        sendMessage(MessageType.SET_LC_WIN, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setMoniIntervals(int, int)
     */
    public void setMoniIntervals(int hw, int config) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putInt(hw).putInt(config).flip();
        sendMessage(MessageType.SET_MONI_IVAL, buf);
    }

    public void setMoniIntervals(int hw, int config, int fast) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.putInt(hw).putInt(config).putInt(fast).flip();
        sendMessage(MessageType.SET_MONI_IVAL, buf);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setMux(ic3.daq.domapp.MuxState)
     */
    public void setMux(MuxState mode) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(mode.getValue()).flip();
        sendMessage(MessageType.MUX_SELECT, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setPulserRate(short)
     */
    public void setPulserRate(short rate) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(2);
        buf.putShort(rate).flip();
        sendMessage(MessageType.SET_PULSER_RATE, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setScalerDeadtime(int)
     */
    public void setScalerDeadtime(int deadtime) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(deadtime).flip();
        sendMessage(MessageType.SET_SCALER_DEADTIME, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#setTriggerMode(ic3.daq.domapp.TriggerMode)
     */
    public void setTriggerMode(TriggerMode mode) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(mode.getValue()).flip();
        sendMessage(MessageType.SET_TRIG_MODE, buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#transitionToDOMApp()
     */
    public boolean transitionToDOMApp() throws IOException, InterruptedException
    {
        ByteBuffer cmd = ByteBuffer.allocate(20);
        // Issue a clear - something gets out-of-sorts in the iceboot
        // command decoder
        talkToIceboot("s\" domapp.sbi.gz\" find if gunzip fpga endif .");
        // Exec DOMApp & wait for "DOMAPP READY" message from DOMApp
        talkToIceboot("s\" domapp.gz\" find if gunzip exec endif", "DOMAPP READY\n");
        return true;
    }

    private String talkToIceboot(String cmd) throws IOException
    {
        return talkToIceboot(cmd, "> \n");
    }
    
    private String talkToIceboot(String cmd, String expect) throws IOException
    {
        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.put(cmd.getBytes());
        buf.put("\r\n".getBytes()).flip();
        int n = buf.remaining();
        devIO.send(buf);
        logger.debug("Sending: " + cmd);
        StringBuffer txt = new StringBuffer();
        while (true)
        {
            ByteBuffer ret = devIO.recv();
            byte[] bytearray = new byte[ret.remaining()];
            ret.get(bytearray);
            String fragment = new String(bytearray);
            logger.debug("Received: " + fragment);
            if (fragment.equals(expect)) break;
            txt.append(fragment);
        } 
        return txt.toString();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see ic3.daq.domapp.IDOMApp#writeDAC(byte, short)
     */
    public void writeDAC(byte dac, short val) throws MessageException
    {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.put(dac).put((byte) 0).putShort(val).flip();
        sendMessage(MessageType.WRITE_DAC, buf);
    }

}
