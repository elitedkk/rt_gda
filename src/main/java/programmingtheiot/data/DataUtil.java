/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.data;
import com.google.gson.Gson;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;

/**
 * Shell representation of class for student implementation.
 *
 */
public class DataUtil
{
	// static
	
	private static final DataUtil _Instance = new DataUtil();

	/**
	 * Returns the Singleton instance of this class.
	 * 
	 * @return ConfigUtil
	 */
	public static final DataUtil getInstance()
	{
		return _Instance;
	}
	
	
	// private var's
	
	
	// constructors
	
	/**
	 * Default (private).
	 * 
	 */
	private DataUtil()
	{
		super();
	}
	
	
	// public methods
	/**
    * Convert the actuator data to Json format
    *
    * @param  actuatorData   the data which is to be converted to Json
    * @return jsonData		 the Json value
    */
	public String actuatorDataToJson(ActuatorData actuatorData)
	{
		Gson gson = new Gson();
		String jsonData = gson.toJson(actuatorData);
		return jsonData;
	}
	
	/**
    * Convert the sensor data to Json format
    *
    * @param  sensorData   the data which is to be converted to Json
    * @return jsonData		 the Json value
    */
	public String sensorDataToJson(SensorData sensorData)
	{
		Gson gson = new Gson();
		String jsonData = gson.toJson(sensorData);
		return jsonData;
	}
	
	/**
    * Convert the system performance data to Json format
    *
    * @param  sysPerfData   the data which is to be converted to Json
    * @return jsonData		 the Json value
    */
	public String systemPerformanceDataToJson(SystemPerformanceData sysPerfData)
	{
		Gson gson = new Gson();
		String jsonData = gson.toJson(sysPerfData);
		return jsonData;
	}
	
	/**
    * Convert the System State data to Json format
    *
    * @param  actuatorData   the data which is to be converted to Json
    * @return jsonData		 the Json value
    */
	public String systemStateDataToJson(SystemStateData sysStateData)
	{
		Gson gson = new Gson();
		String jsonData = gson.toJson(sysStateData);
		return jsonData;
	}
	
	/**
    * Convert the Json format to Actuator Data type
    *
    * @param  jsonData   	the Json format which is to be converted
    * @return actuatorData	the actuator data 
    */
	public ActuatorData jsonToActuatorData(String jsonData)
	{
		Gson gson = new Gson();
		ActuatorData actuatorData = gson.fromJson(jsonData, ActuatorData.class);
		return actuatorData;
	}
	
	/**
    * Convert the Json format to Sensor Data type
    *
    * @param  jsonData   	the Json format which is to be converted
    * @return sensorData	the sensor data 
    */
	public SensorData jsonToSensorData(String jsonData)
	{
		Gson gson = new Gson();
		SensorData sensorData = gson.fromJson(jsonData, SensorData.class);
		return sensorData;
	}
	
	/**
    * Convert the Json format to SystemPerformanceData type
    *
    * @param  jsonData   	the Json format which is to be converted
    * @return sysPerfData	the system performance data 
    */
	public SystemPerformanceData jsonToSystemPerformanceData(String jsonData)
	{
		Gson gson = new Gson();
		SystemPerformanceData sysPerfData = gson.fromJson(jsonData, SystemPerformanceData.class);
		return sysPerfData;
	}
	
	/**
    * Convert the Json format to SystemStateData type
    *
    * @param  jsonData   	the Json format which is to be converted
    * @return sysStateData	the system state data 
    */
	public SystemStateData jsonToSystemStateData(String jsonData)
	{
		Gson gson = new Gson();
		SystemStateData sysStateData = gson.fromJson(jsonData, SystemStateData.class);
		return sysStateData;
	}
	
}
