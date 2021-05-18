package de.rwth_aachen.phyphox.NetworkConnection.Mqtt;

import android.content.Context;

import java.util.List;
import java.util.Map;

import de.rwth_aachen.phyphox.NetworkConnection.NetworkConnection;
import de.rwth_aachen.phyphox.NetworkConnection.NetworkService;
import de.rwth_aachen.phyphox.PhyphoxExperiment;

public class MqttJson extends MqttService {
    String sendTopic;
    PhyphoxExperiment experiment;//todo

    public MqttJson(String receiveTopic,
                    String sendTopic,
                    Context context,
                    boolean persistence,
                    boolean clearBuffer,
                    PhyphoxExperiment experiment) {

        this.receiveTopic = receiveTopic;
        this.sendTopic = sendTopic;
        this.context = context;
        this.persistence = persistence;
        this.clientID = "phyphox_" + String.format("%06x", (System.nanoTime() & 0xffffff));
        this.clearBuffer = clearBuffer;
        this.experiment = experiment;

        if(persistence){
            setPersistenceSettings();
        }else {
            mqttConnectOptions.setCleanSession(true);
        }
    }

    public void execute(Map<String, NetworkConnection.NetworkSendableData> send, List<NetworkService.RequestCallback> requestCallbacks) {
        MqttHelper.sendJson(this,sendTopic,send,requestCallbacks,experiment);
    }
}
