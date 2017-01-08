package lu.maycrosoft.transporteit.lu;

/**
 *  A listener for the main activity so that the finished download of the stations will tell
 *  the main activity to retrieve the information.
 *
 */

public interface FinishedDownloadListener {

    void finishedBusDownload();
    void finishedVelohDownload();
}
