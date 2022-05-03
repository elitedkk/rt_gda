package programmingtheiot.gda.connection;

import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

public interface ISimpleMessagingClient {

	boolean sendMessage(ResourceNameEnum resource, String payload, int timeout);

	boolean disconnectClient();

	boolean connectClient();

	boolean sendMessage(ResourceNameEnum resource, SensorData data);

	boolean sendMessage(ResourceNameEnum resource, ActuatorData data);

	boolean sendMessage(ResourceNameEnum resource, SystemPerformanceData data);

}
