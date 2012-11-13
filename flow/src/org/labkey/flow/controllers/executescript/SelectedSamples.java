package org.labkey.flow.controllers.executescript;

import org.apache.commons.collections15.FactoryUtils;
import org.apache.commons.collections15.MapUtils;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.data.FlowFCSFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: 10/21/12
 */
public class SelectedSamples
{
    // non-form posted values used to initialize the SamplesConfirmGridView
    private Set<String> _keywords;
    private List<Workspace.SampleInfo> _samples;

    // form posted values
    // workspace sample id -> resolved info
    private Map<String, ResolvedSample> _rows = MapUtils.lazyMap(new HashMap<String, ResolvedSample>(), FactoryUtils.instantiateFactory(ResolvedSample.class));

    public static class ResolvedSample
    {
        private boolean _selected;
        // FlowFCSFile rowid (may be 0 or null if there is no match)
        private Integer _matchedFile;

        // FlowFCSFile rowid (may be null if there are no candidates)
        private int[] _candidateFile;
        private List<FlowFCSFile> _candidateFCSFiles;

        public ResolvedSample()
        {
        }

        public ResolvedSample(boolean selected, int matchedFile, List<FlowFCSFile> candidateFCSFiles)
        {
            _selected = selected;
            _matchedFile = matchedFile;
            if (candidateFCSFiles == null)
            {
                _candidateFCSFiles = null;
                _candidateFile = null;
            }
            else
            {
                _candidateFCSFiles = candidateFCSFiles;
                _candidateFile = new int[candidateFCSFiles.size()];
                for (int i = 0, len = candidateFCSFiles.size(); i < len; i++)
                    _candidateFile[i] = candidateFCSFiles.get(i).getRowId();
            }
        }

        public boolean isSelected()
        {
            return _selected;
        }

        public void setSelected(boolean selected)
        {
            _selected = selected;
        }

        public Integer getMatchedFile()
        {
            return _matchedFile;
        }

        public void setMatchedFile(Integer matchedFile)
        {
            _matchedFile = matchedFile;
        }

        public boolean hasMatchedFile()
        {
            return _matchedFile != null && _matchedFile > 0;
        }

        public int[] getCandidateFile()
        {
            return _candidateFile;
        }

        public List<FlowFCSFile> getCandidateFCSFiles()
        {
            if (_candidateFCSFiles == null && _candidateFile != null)
            {
                _candidateFCSFiles = FlowFCSFile.fromWellIds(_candidateFile);
            }
            return _candidateFCSFiles;
        }

        public void setCandidateFile(int[] candidateFile)
        {
            _candidateFile = candidateFile;
        }
    }

    public SelectedSamples()
    {
    }

    public Set<String> getKeywords()
    {
        return _keywords;
    }

    public void setKeywords(Set<String> keywords)
    {
        _keywords = keywords;
    }

    public List<Workspace.SampleInfo> getSamples()
    {
        return _samples;
    }

    public void setSamples(List<Workspace.SampleInfo> samples)
    {
        _samples = samples;
    }

    public void setRows(Map<String, ResolvedSample> rows)
    {
        _rows = rows;
    }

    public Map<String, ResolvedSample> getRows()
    {
        return _rows;
    }

    public Map<String, String> getHiddenFields()
    {
        if (_rows.isEmpty())
            return Collections.emptyMap();

        Map<String, String> hidden = new HashMap<String, String>();
        for (Map.Entry<String, ResolvedSample> entry : _rows.entrySet())
        {
            String sampleId = entry.getKey();
            ResolvedSample resolvedSample = entry.getValue();
            hidden.put("rows[" + sampleId + "].selected", String.valueOf(resolvedSample.isSelected()));
            hidden.put("rows[" + sampleId + "].matchedFile", resolvedSample.hasMatchedFile() ? String.valueOf(resolvedSample.getMatchedFile()) : "");
        }
        return hidden;
    }
}
