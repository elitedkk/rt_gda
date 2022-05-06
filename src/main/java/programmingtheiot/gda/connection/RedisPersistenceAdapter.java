/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.connection;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class RedisPersistenceAdapter implements IPersistenceClient
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(RedisPersistenceAdapter.class.getName());
	
	// private var's
	//JedisPooled jedis = new JedisPooled("localhost", 6379);
	String host;
	int port;
	JedisPool jpool;
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public RedisPersistenceAdapter()
	{
		
		super();
		
		initClient();
		ConfigUtil configUtil = ConfigUtil.getInstance();
		this.host =
				configUtil.getProperty(
					ConfigConst.DATA_GATEWAY_SERVICE, ConfigConst.HOST_KEY);
		this.port =
				configUtil.getInteger(
					ConfigConst.DATA_GATEWAY_SERVICE, ConfigConst.PORT_KEY);

	}
	
	
	// public methods
	
	@Override
	public boolean connectClient()
	{
		try {
			if(jpool==null) {
				_Logger.warning("Init redis connection");
				jpool = new JedisPool(this.host,this.port);
				
			}
			else {
				_Logger.warning("Redis connection already exists");
			}
			return true;
		}
		catch(Exception ex) {
			_Logger.severe("Error in connecting redis client");
			return false;
		}
		
	}

	@Override
	public boolean disconnectClient()
	{
		return false;
	}

	@Override
	public ActuatorData[] getActuatorData(String topic, Date startDate, Date endDate)
	{
		return null;
	}

	@Override
	public SensorData[] getSensorData(String topic, Date startDate, Date endDate)
	{
		return null;
	}

	@Override
	public void registerDataStorageListener(Class cType, IPersistenceListener listener, String... topics)
	{
	}

	@Override
	public boolean storeData(String topic, int qos, ActuatorData... data)
	{
		for(int i=0; i<data.length; i++) {
			String json = DataUtil.getInstance().actuatorDataToJson(data[i]);
			try(Jedis jedis = this.jpool.getResource()) {
				jedis.set(topic, json);
			}
			catch(Exception ex){
				_Logger.warning("Error putting into redis");
				return false;
			}
		}
		_Logger.info("Successfully put values into redis topic: " + topic);
		return true;
	}

	@Override
	public boolean storeData(String topic, int qos, SensorData... data)
	{
		for(int i=0; i<data.length; i++) {
			String json = DataUtil.getInstance().sensorDataToJson(data[i]);
			try(Jedis jedis = this.jpool.getResource()) {
				jedis.set(topic, json);
			}
			catch(Exception ex){
				_Logger.warning("Error putting into redis");
				return false;
			}
		}
		_Logger.info("Successfully put values into redis topic: " + topic);
		return true;
	}

	/**
	 *
	 */
	@Override
	public boolean storeData(String topic, int qos, SystemPerformanceData... data)
	{
		for(int i=0; i<data.length; i++) {
			String json = DataUtil.getInstance().systemPerformanceDataToJson(data[i]);
			try(Jedis jedis = this.jpool.getResource()) {
				jedis.set(topic, json);
			}
			catch(Exception ex){
				_Logger.warning("Error putting into redis");
				return false;
			}
		}
		_Logger.info("Successfully put values into redis topic: " + topic);
		return true;
	}
	
	
	// private methods
	
	/**
	 * Generates a listener key map from the class type and topic.
	 * The format will be as follows:
	 * <br>'simple class name' + "_" + 'topic name'
	 * <br>e.g. ActuatorData_localhost/fan
	 * <br>e.g. SensorData_localhost/temperature
	 * <p>
	 * If the class type is null, it will simply be dropped and
	 * only the topic name will be used in the key. If the topic
	 * name is also null or invalid (e.g. empty), the 'all' keyword
	 * will be used instead.
	 * 
	 * @param cType The class type to use in the key.
	 * @param topic The topic name to use in the key.
	 * @return String The key derived from cType and topic, as per above.
	 */
	private String getListenerMapKey(Class cType, String topic)
	{
		StringBuilder buf = new StringBuilder();
		
		if (cType != null) {
			buf.append(cType.getSimpleName()).append("_");
		}
		
		if (topic != null && topic.trim().length() > 0) {
			buf.append(topic.trim());
		} else {
			buf.append("all");
		}
		
		String key = buf.toString();
		
		_Logger.info("Generated listener map lookup key from '" + cType + "' and '" + topic + "': " + key);
		
		return key;
	}
	
	private void initClient()
	{
	}
	
	private Long updateRedisDataElement(String topic, double score, String payload)
	{
		return 0L;
	}


}
