/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.app;

import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SystemStateData;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;


import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.ICloudClient;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.ISimpleMessagingClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;
import programmingtheiot.gda.system.SystemPerformanceManager;

/**
 * Shell representation of class for student implementation.
 *
 */
public class DeviceDataManager implements IDataMessageListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManager.class.getName());
	
	// private var's


	private boolean enableMqttClient = true;
	private boolean enableCoapServer = true;
	private boolean enableCloudClient = false;
	private boolean enableSmtpClient = false;
	private boolean enablePersistenceClient = false;
	private boolean enableSystemPerf = false;
	private IActuatorDataListener actuatorDataListener = null;
	private IPubSubClient mqttClient = null;
	private ICloudClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	private ISimpleMessagingClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	private SystemPerformanceManager sysPerfMgr = null;
	private ActuatorData actData = null;
	// constructors
	
	public DeviceDataManager()
	{
		super();
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.enableMqttClient =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);
		
		this.enableCoapServer =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);
		
		this.enableCloudClient =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);
		
		this.enablePersistenceClient =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);
		this.enableSmtpClient=
				configUtil.getBoolean(
						ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SMTP_CLIENT_KEY);
		
		initConnections();
		actData = new ActuatorData();
		actData.setCommand(ConfigConst.OFF_COMMAND);
	}
	
	public DeviceDataManager(
		boolean enableMqttClient,
		boolean enableCoapClient,
		boolean enableCloudClient,
		boolean enableSmtpClient,
		boolean enablePersistenceClient)
	{
		super();
		
		initConnections();
	}
	
	
	// public methods
	
	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.info("Handling actuator response: " + data.getName());
						
			if (data.hasError()) {
				_Logger.warning("Error flag set for ActuatorData instance.");
			}
			if (this.persistenceClient!=null) {
				this.persistenceClient.storeData(resourceName.getResourceName(), 0, data);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		if (resourceName != null && msg != null) {
			try {
				if (resourceName == ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE) {
					_Logger.info("Handling incoming ActuatorData message: " + msg);
					
					// NOTE: it may seem wasteful to convert to ActuatorData and back while
					// the JSON data is already available; however, this provides a validation
					// scheme to ensure the data is actually an 'ActuatorData' instance
					// prior to sending off to the CDA
					ActuatorData ad = DataUtil.getInstance().jsonToActuatorData(msg);
					String jsonData = DataUtil.getInstance().actuatorDataToJson(ad);
					
					if (this.mqttClient != null) {
						// TODO: retrieve the QoS level from the configuration file
						//_Logger.info("Publishing data to MQTT broker: " + jsonData);
						_Logger.info("Publishing data to MQTT broker: " + resourceName.getResourceName());
						return this.mqttClient.publishMessage(resourceName, jsonData, 1);
						
					}
					
					// TODO: If the GDA is hosting a CoAP server (or a CoAP client that
					// will connect to the CDA's CoAP server), you can add that logic here
					// in place of the MQTT client or in addition
					
					
				} else {
					_Logger.warning("Failed to parse incoming message. Unknown type: " + msg);
					
					return false;
				}
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to process incoming message for resource: " + resourceName, e);
			}
		} else {
			_Logger.warning("Incoming message has no data. Ignoring for resource: " + resourceName);
		}
		
		return false;
	}

	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		if (data != null) {
			_Logger.info("Handling sensor message: " + data.getName());
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for SensorData instance.");
			}
			/*if (data.getTypeID() == ConfigConst.HUMIDITY_SENSOR_TYPE) {
				if(data.getValue()> ConfigConst.HUMIDITY_CEILING) {
					actData.setCommand(ConfigConst.ON_COMMAND);
					if (this.enableMqttClient) {
						int qos=ConfigConst.DEFAULT_QOS;
						DataUtil du = DataUtil.getInstance();
						String jSon = du.actuatorDataToJson(actData);
						_Logger.info("Turning ON the Humidifier");
						this.handleUpstreamTransmission(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE,jSon,qos);
					}
				}
				else if (data.getValue() < ConfigConst.HUMIDITY_FLOOR) {
					actData.setCommand(ConfigConst.OFF_COMMAND);
					if (this.enableMqttClient) {
						int qos=ConfigConst.DEFAULT_QOS;
						DataUtil du = DataUtil.getInstance();
						String jSon = du.actuatorDataToJson(actData);
						_Logger.info("Turning OFF the Humidifier");
						this.handleUpstreamTransmission(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE,jSon,qos);
					}
				}
			}*/
			if (this.persistenceClient!=null) {
				this.persistenceClient.storeData(resourceName.getResourceName(), 0, data);
			}
			
			
			if (this.cloudClient != null) {
				// TODO: handle any failures
				
				if (this.cloudClient.sendEdgeDataToCloud(resourceName, data)) {
					_Logger.info("Sent SensorData upstream to CSP.");
				}
			}
			
			if (this.smtpClient != null && this.cloudClient==null) {
				DataUtil du = DataUtil.getInstance();
				String jSon = du.actuatorDataToJson(actData);
				_Logger.info("Sending an email...");
				this.smtpClient.sendMessage(resourceName, jSon, 30);
			}
			return true;
		} else {
			_Logger.warning("Empty Sensor message received");
			return false;
		}
	}

	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		if (data != null) {
			_Logger.info("Handling system performance message: " + data.getName());
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for SystemPerformanceData instance.");
			}
			if (this.cloudClient != null) {
				// TODO: handle any failures
				if (this.cloudClient.sendEdgeDataToCloud(resourceName, data)) {
					_Logger.info("Sent SystemPerformanceData upstream to CSP.");
				}
			}
			if (this.smtpClient != null && this.cloudClient==null) {
				DataUtil du = DataUtil.getInstance();
				String jSon = du.systemPerformanceDataToJson(data);
				this.smtpClient.sendMessage(resourceName, jSon, 30);
			}
			if (this.persistenceClient!=null) {
				this.persistenceClient.storeData(resourceName.getResourceName(), 0, data);
			}
			return true;
		} else {
			return false;
		}
	}
	
	private void handleIncomingDataAnalysis(ResourceNameEnum resource, ActuatorData data)
	{
		_Logger.info("Analyzing incoming actuator data: " + data.getName());
		
		if (data.isResponseFlagEnabled()) {
			// TODO: implement this
		} else {
			if (this.actuatorDataListener != null) {
				this.actuatorDataListener.onActuatorDataUpdate(data);
			}
		}
	}
	
	public boolean handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemStateData data)
	{
		_Logger.fine("Analyzing System state Data");
		return true;
	}
	
	private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, int qos)
	{
		_Logger.fine("Analyzing upstream Data");
		if(this.enableMqttClient) {
			this.mqttClient.publishMessage(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, jsonData, qos);
		}
		return true;
	}
	
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		if (listener != null) {
			// for now, just ignore 'name' - if you need more than one listener,
			// you can use 'name' to create a map of listener instances
			this.actuatorDataListener = listener;
		}
	}
	/**
    * Start the Device Data Manager
    *
    */
	public void startManager()
	{
		_Logger.info("Starting DeviceDataManager");
		if (this.sysPerfMgr != null) {
			
			this.sysPerfMgr.startManager();
		}
		if (this.mqttClient != null) {
			if (this.mqttClient.connectClient()) {
				_Logger.info("Successfully connected MQTT client to broker.");
				
				int qos = ConfigConst.DEFAULT_QOS;
				
				// TODO: check the return value for each and take appropriate action
				this.mqttClient.subscribeToTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, qos);
				this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
				this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
				this.mqttClient.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
			} else {
				_Logger.severe("Failed to connect MQTT client to broker.");
				
				// TODO: take appropriate action
			}
		}
		if (this.enableCoapServer && this.coapServer != null) {
			if (this.coapServer.startServer()) {
				_Logger.info("CoAP server started.");
			} else {
				_Logger.severe("Failed to start CoAP server. Check log file for details.");
			}
		}
		if (this.enablePersistenceClient) {
			if(this.persistenceClient.connectClient()) {
				_Logger.info("Started the Redis storage");
			}
			else {
				_Logger.warning("Could not start redis connection");
			}
		}
		if (this.enableCloudClient && this.cloudClient != null) {
			if (this.cloudClient.connectClient()) {
				_Logger.info("Cloud client started.");
			} else {
				_Logger.severe("Failed to start cloud client. Check log file for details.");
			}
		}
		else {
			_Logger.info("Enable Cloud client = " + this.enableCloudClient);
			_Logger.info("Object of cloudclient" + this.cloudClient);
			_Logger.info("Cloud client not initialised");
		}
	}
	
	/**
    * Stop the Device Data Manager
    *
    */
	public void stopManager()
	{
		_Logger.info("Stopping DeviceDataManager");
		if (this.sysPerfMgr != null) {
			
			this.sysPerfMgr.stopManager();
		}
		if (this.mqttClient != null) {
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE);
			this.mqttClient.unsubscribeFromTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE);
			
			if (this.mqttClient.disconnectClient()) {
				_Logger.info("Successfully disconnected MQTT client from broker.");
			} else {
				_Logger.severe("Failed to disconnect MQTT client from broker.");
			}
		}
		if (this.enableCoapServer && this.coapServer != null) {
			if (this.coapServer.stopServer()) {
				_Logger.info("CoAP server stopped.");
			} else {
				_Logger.severe("Failed to stop CoAP server. Check log file for details.");
			}
		}
		if (this.enableCloudClient && this.cloudClient!=null) {
			if (this.cloudClient.disconnectClient()) {
				_Logger.info("Cloud client stopped.");
			} else {
				_Logger.severe("Failed to stop Cloud client. Check log file for details.");
			}
		}
	}

	
	// private methods
	
	/**
	 * Initializes the enabled connections. This will NOT start them, but only create the
	 * instances that will be used in the {@link #startManager() and #stopManager()) methods.
	 * 
	 */
	private void initConnections()
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.enableSystemPerf =
			configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE,  ConfigConst.ENABLE_SYSTEM_PERF_KEY);
		
		if (this.enableSystemPerf) {
			this.sysPerfMgr = new SystemPerformanceManager();
			this.sysPerfMgr.setDataMessageListener(this);
		}
		
		if (this.enableMqttClient) {
			this.mqttClient = new MqttClientConnector();
			this.mqttClient.setDataMessageListener(this);
		}
		
		if (this.enableCoapServer) {
			this.coapServer = new CoapServerGateway(this);
		}
		
		if (this.enableCloudClient) {
			this.cloudClient = new CloudClientConnector();
			this.cloudClient.setDataMessageListener(this);
		}
		
		if (this.enableSmtpClient) {
			this.smtpClient = new SmtpClientConnector();
		}
		
		if (this.enablePersistenceClient) {
			this.persistenceClient = new RedisPersistenceAdapter();
		}
	}
	
}
