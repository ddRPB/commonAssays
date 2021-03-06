/*******************************************************************************
 * --------------------------------------------------------------------------- *
 * File: * @(#) SAX2ScanHandler.java * Author: * Robert M. Hubley
 * rhubley@systemsbiology.org
 * ****************************************************************************** * * *
 * This software is provided ``AS IS'' and any express or implied * *
 * warranties, including, but not limited to, the implied warranties of * *
 * merchantability and fitness for a particular purpose, are disclaimed. * * In
 * no event shall the authors or the Institute for Systems Biology * * liable
 * for any direct, indirect, incidental, special, exemplary, or * *
 * consequential damages (including, but not limited to, procurement of * *
 * substitute goods or services; loss of use, data, or profits; or * * business
 * interruption) however caused and on any theory of liability, * * whether in
 * contract, strict liability, or tort (including negligence * * or otherwise)
 * arising in any way out of the use of this software, even * * if advised of
 * the possibility of such damage. * * *
 * ******************************************************************************
 * 
 * ChangeLog
 * 
 * 1-12-2004 Initial import...
 ******************************************************************************/
package org.systemsbiology.jrap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public final class SAX2ScanHeaderHandler extends DefaultHandler
{
	/** A new ScanHeader Object */
	protected ScanHeader tmpScanHeader;

	/** Buffer to hold characters while getting precursorMZ value */
	protected StringBuffer precursorBuffer;

	/** Flag to indicate if we are reading the precursor MZ value */
	protected boolean inPrecursorMZ = false;

	//
	// Getters
	//

	public ScanHeader getScanHeader()
	{
		return (tmpScanHeader);
	}

	private int getIntAttribute(Attributes attrs, String name)
	{
		int result;

		if (attrs.getValue(name) == null) // attribute not present
			return -1;

		try
		{
			result = Integer.parseInt(attrs.getValue(name));
		} catch (NumberFormatException e)
		{
//			logger.error("Numberformatexception!", e);
			result = -1;
		}
		return (result);
	}

    	private long getLongAttribute(Attributes attrs, String name)
	{
		long result;

		if (attrs.getValue(name) == null) // attribute not present
			return -1;

		try
		{
			result = Long.parseLong(attrs.getValue(name));
		} catch (NumberFormatException e)
		{
//			logger.error("Numberformatexception!", e);
			result = -1;
		}
		return (result);
	}

	private float getFloatAttribute(Attributes attrs, String name)
	{
		float result;

		if (attrs.getValue(name) == null) // attribute not present
			return -1;

		try
		{
			result = Float.parseFloat(attrs.getValue(name));
		} catch (NumberFormatException e)
		{
//			logger.error("Numberformatexception!", e);
			result = -1;
		} catch (NullPointerException e1)
		{
//			logger.error("Nullpointerexception!", e1);
			result = -1;
		}
		return (result);
	}

	//
	// ContentHandler Methods
	//

	/** Start document. */
	@Override
    public void startDocument()
    {
		// Nothing to do
	} // startDocument()

	/** Start element. */
	@Override
    public void startElement(
		String uri,
		String local,
		String raw,
		Attributes attrs)
		throws SAXException
	{
		if (raw.equals("scan"))
		{
			tmpScanHeader = new ScanHeader();
			tmpScanHeader.setNum(getIntAttribute(attrs, "num"));
			tmpScanHeader.setMsLevel(getIntAttribute(attrs, "msLevel"));
			tmpScanHeader.setPeaksCount(getIntAttribute(attrs, "peaksCount"));
			tmpScanHeader.setPolarity(attrs.getValue("polarity"));
			tmpScanHeader.setScanType(attrs.getValue("scanType"));
			tmpScanHeader.setCentroided(getIntAttribute(attrs, "centroided"));
			tmpScanHeader.setDeisotoped(getIntAttribute(attrs, "deisotoped"));
			tmpScanHeader.setChargeDeconvoluted(
				getIntAttribute(attrs, "chargeDeconvoluted"));
			tmpScanHeader.setRetentionTime(attrs.getValue("retentionTime"));
			tmpScanHeader.setStartMz(getFloatAttribute(attrs, "startMz"));
			tmpScanHeader.setEndMz(getFloatAttribute(attrs, "endMz"));
			tmpScanHeader.setLowMz(getFloatAttribute(attrs, "lowMz"));
			tmpScanHeader.setHighMz(getFloatAttribute(attrs, "highMz"));
			tmpScanHeader.setBasePeakMz(getFloatAttribute(attrs, "basePeakMz"));
			tmpScanHeader.setBasePeakIntensity(
				getFloatAttribute(attrs, "basePeakIntensity"));
			tmpScanHeader.setTotIonCurrent(getFloatAttribute(attrs, "totIonCurrent"));
            tmpScanHeader.setFilterLine(attrs.getValue("filterLine"));
		} else if (raw.equals("peaks"))
		{
			tmpScanHeader.setPrecision(getIntAttribute(attrs, "precision"));
			tmpScanHeader.setByteOrder(attrs.getValue("byteOrder"));
			tmpScanHeader.setContentType(attrs.getValue("contentType"));
			tmpScanHeader.setCompressionType(attrs.getValue("compressionType"));
			tmpScanHeader.setCompressedLen(getIntAttribute(attrs, "compressedLen"));
			throw (new SAXException("ScanHeaderEndFoundException"));
		} else if (raw.equals("precursorMz"))
		{
			tmpScanHeader.setPrecursorScanNum(
				getIntAttribute(attrs, "precursorScanNum"));
			tmpScanHeader.setPrecursorCharge(
				getIntAttribute(attrs, "precursorCharge"));
			tmpScanHeader.setCollisionEnergy(
				getFloatAttribute(attrs, "collisionEnergy"));
			tmpScanHeader.setIonisationEnergy(
				getFloatAttribute(attrs, "ionisationEnergy"));
			
			precursorBuffer = new StringBuffer();
			inPrecursorMZ = true;
		}
	} // startElement(String,String,StringAttributes)

	@Override
    public void endElement(String uri, String local, String raw)
    {
		if (raw.equals("precursorMz"))
		{
			tmpScanHeader.setPrecursorMz(
				Float.parseFloat(precursorBuffer.toString()));
			precursorBuffer = null; // make available for garbage collection

			inPrecursorMZ = false;
		}
	} // endElement()

	/** Characters. */
	@Override
    public void characters(char ch[], int start, int length)
    {
		if (inPrecursorMZ)
		{
			precursorBuffer.append(ch, start, length);
		}
	} // characters(char[],int,int);

	/** Ignorable whitespace. */
	@Override
    public void ignorableWhitespace(char ch[], int start, int length)
    {
		// Do nothing
	} // ignorableWhitespace(char[],int,int);

	/** Processing instruction. */
	@Override
    public void processingInstruction(String target, String data)
    {
		// Do nothing
	} // processingInstruction(String,String)

	//
	// ErrorHandler methods
	//

	/** Warning. */
	@Override
    public void warning(SAXParseException ex)
    {
		// Do nothing
		//printError("Warning", ex);
	} // warning(SAXParseException)

	/** Error. */
	@Override
    public void error(SAXParseException ex)
    {
		// Do nothing
		//printError("Error", ex);
	} // error(SAXParseException)

	/** Fatal error. */
	@Override
    public void fatalError(SAXParseException ex)
    {
		// Do nothing
		//printError("Fatal Error", ex);
	} // fatalError(SAXParseException)

	//
	// Protected methods
	//

	/** Prints the error message. */
	protected void printError(String type, SAXParseException ex)
	{
		System.err.print("[");
		System.err.print(type);
		System.err.print("] ");
		if (ex == null)
		{
			System.out.println("!!!");
		}
		String systemId = ex.getSystemId();
		if (systemId != null)
		{
			int index = systemId.lastIndexOf('/');
			if (index != -1)
				systemId = systemId.substring(index + 1);
			System.err.print(systemId);
		}
		System.err.print(':');
		System.err.print(ex.getLineNumber());
		System.err.print(':');
		System.err.print(ex.getColumnNumber());
		System.err.print(": ");
		System.err.print(ex.getMessage());
		System.err.println();
		System.err.flush();
	} // printError(String,SAXParseException)

}
