package service;

import hoppie.AcarsMessage;
import java.util.List;

public interface CpdlcListener {
    void onMessageReceived(AcarsMessage message);
    void onMessagesUpdated(List<AcarsMessage> messages);
    void onConnectionStatusChanged(boolean isConnected);
    void onAtsUnitChanged(String atsUnit);
    void onError(String message);
}
