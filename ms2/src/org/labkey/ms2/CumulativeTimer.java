package org.labkey.ms2;

import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.labkey.api.util.Formats;

/**
* User: adam
* Date: Mar 6, 2008
* Time: 2:40:36 PM
*/

// Tracks cumulative time for a set of sequential tasks.  Each task invocation logs elapsed time for previous
// task and starting information for current task.  Summary logs cumulative time for each task.
// NOTE: This approach assumes that only one task is active at a time (no parallel tasks)
public class CumulativeTimer
{
    // Map of TimerTask -> Long (cumulative task time)
    private final OrderedMap _cumulativeTime = new ListOrderedMap();
    private Task _currentTask = null;
    private Logger _log;

    public CumulativeTimer(Logger log)
    {
        _log = log;
    }

    public void setCurrentTask(TimerTask tt, String extraDescription)
    {
        endCurrentTask();
        _currentTask = new Task(tt, extraDescription);
        _currentTask.start();
    }

    public void setCurrentTask(TimerTask tt)
    {
        setCurrentTask(tt, null);
    }

    public void endCurrentTask()
    {
        if (null != _currentTask)
            _currentTask.end();

        _currentTask = null;
    }

    public boolean hasTask(TimerTask tt)
    {
        return null != _cumulativeTime.get(tt);
    }

    public void logSummary(String description)
    {
        endCurrentTask();
        long totalTime = 0;

        _log.info("========================================");
        _log.info("Summary of all timed tasks:");
        _log.info("");

        for (Object key : _cumulativeTime.keySet())
        {
            TimerTask tt = (TimerTask)key;
            long time = ((Long)_cumulativeTime.get(tt)).longValue();
            logElapsedTime(time, tt.getAction());
            totalTime += time;
        }

        _log.info("");
        logElapsedTime(totalTime, description);
        _log.info("========================================");
    }

    protected void logElapsedTime(long elapsedTimeNano, String action)
    {
        double seconds = (double)elapsedTimeNano / 1000000000;
        double minutes = seconds / 60;

        _log.info(Formats.f2.format(seconds) + " seconds " + ((minutes > 1) ? ("(" + Formats.f2.format(seconds / 60) + " minutes) ") : "") + "to " + action);
    }

    private class Task
    {
        private TimerTask _tt;
        private long _startTime;
        private String _extraDescription;

        private Task(TimerTask tt, String extraDescription)
        {
            _tt = tt;
            _extraDescription = extraDescription;
        }

        private void start()
        {
            _startTime = System.nanoTime();
            _log.info("Starting to " + getDescription());
        }

        private void end()
        {
            long elapsed = System.nanoTime() - _startTime;

            synchronized(_cumulativeTime)
            {
                Long cumulative = (Long)_cumulativeTime.get(_tt);

                _cumulativeTime.put(_tt, (null == cumulative ? elapsed : cumulative.longValue() + elapsed));
            }

            logElapsedTime(elapsed, getDescription());
        }

        private String getDescription()
        {
            return _tt.getAction() + (null != _extraDescription ? " " + _extraDescription : "");
        }
    }

    public interface TimerTask
    {
        public String getAction();
    }
}
