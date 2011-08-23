package org.labkey.ms2.reader;

import org.labkey.api.reader.SimpleXMLStreamReader;

import javax.xml.stream.XMLStreamException;

/**
 * User: jeckels
 * Date: Aug 14, 2011
 */
public class LibraQuantHandler extends PepXmlAnalysisResultHandler
{
    public static final String ANALYSIS_TYPE = "libra";

    @Override
    protected String getAnalysisType()
    {
        return ANALYSIS_TYPE;
    }

    @Override
    protected PepXmlAnalysisResultHandler.PepXmlAnalysisResult getResult(SimpleXMLStreamReader parser) throws XMLStreamException
    {
        parser.skipToStart("libra_result");
        LibraQuantResult result = new LibraQuantResult();
        while (!parser.isEndElement() || !parser.getLocalName().equals("libra_result"))
        {
            parser.next();
            if (parser.isStartElement() && parser.getLocalName().equals("intensity"))
            {
                String channel = parser.getAttributeValue(null, "channel");
                if ("1".equals(channel))
                {
                    result.setTargetMass1(getTargetMass(parser));
                    result.setAbsoluteIntensity1(getAbsoluteMass(parser));
                    result.setNormalized1(getNormalized(parser));
                }
                if ("2".equals(channel))
                {
                    result.setTargetMass2(getTargetMass(parser));
                    result.setAbsoluteIntensity2(getAbsoluteMass(parser));
                    result.setNormalized2(getNormalized(parser));
                }
                if ("3".equals(channel))
                {
                    result.setTargetMass3(getTargetMass(parser));
                    result.setAbsoluteIntensity3(getAbsoluteMass(parser));
                    result.setNormalized3(getNormalized(parser));
                }
                if ("4".equals(channel))
                {
                    result.setTargetMass4(getTargetMass(parser));
                    result.setAbsoluteIntensity4(getAbsoluteMass(parser));
                    result.setNormalized4(getNormalized(parser));
                }
                if ("5".equals(channel))
                {
                    result.setTargetMass5(getTargetMass(parser));
                    result.setAbsoluteIntensity5(getAbsoluteMass(parser));
                    result.setNormalized5(getNormalized(parser));
                }
                if ("6".equals(channel))
                {
                    result.setTargetMass6(getTargetMass(parser));
                    result.setAbsoluteIntensity6(getAbsoluteMass(parser));
                    result.setNormalized6(getNormalized(parser));
                }
                if ("7".equals(channel))
                {
                    result.setTargetMass7(getTargetMass(parser));
                    result.setAbsoluteIntensity7(getAbsoluteMass(parser));
                    result.setNormalized7(getNormalized(parser));
                }
                if ("8".equals(channel))
                {
                    result.setTargetMass8(getTargetMass(parser));
                    result.setAbsoluteIntensity8(getAbsoluteMass(parser));
                    result.setNormalized8(getNormalized(parser));
                }
            }
        }

        return result;
    }

    private double getTargetMass(SimpleXMLStreamReader parser)
    {
        return getDouble(parser, "target_mass");
    }

    private double getAbsoluteMass(SimpleXMLStreamReader parser)
    {
        return getDouble(parser, "absolute");
    }

    private double getNormalized(SimpleXMLStreamReader parser)
    {
        return getDouble(parser, "normalized");
    }

    private double getDouble(SimpleXMLStreamReader parser, String attributeName)
    {
        return Double.parseDouble(parser.getAttributeValue(null, attributeName));
    }

    public static RelativeQuantAnalysisSummary load(SimpleXMLStreamReader parser) throws XMLStreamException
    {
        // We must be on the analysis_summary start element when called.
        String analysisTime = parser.getAttributeValue(null, "time");

        RelativeQuantAnalysisSummary summary = new RelativeQuantAnalysisSummary();
        summary.setAnalysisType(parser.getAttributeValue(null, "analysis"));

        if (!parser.skipToStart("libra_summary"))
            throw new XMLStreamException("Did not find required q3ratio_summary tag in analysis result");

        summary.setMassTol(Q3AnalysisSummary.parseMassTol(parser.getAttributeValue(null, "mass_tolerance")));

        if (null != analysisTime)
            summary.setAnalysisTime(SimpleXMLEventRewriter.convertXMLTimeToDate(analysisTime));

        return summary;
    }
}
