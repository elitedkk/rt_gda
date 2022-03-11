/**
 * This class is part of the Programming the Internet of Things project.
 * 
 * It is provided as a simple shell to guide the student and assist with
 * implementation for the Programming the Internet of Things exercises,
 * and designed to be modified by the student as needed.
 */ 

package programmingtheiot.gda.system;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.gda.app.GatewayDeviceApp;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shell representation of class for student implementation.
 * 
 */
public class SystemPerformanceManager
{
	// private var's
	private int pollRate = ConfigConst.DEFAULT_POLL_CYCLES;
	private static final Logger _Logger = Logger.getLogger(GatewayDeviceApp.class.getName());
	private ScheduledExecutorService schedExecSvc = null;
	private SystemCpuUtilTask cpuUtilTask = null;
	private SystemMemUtilTask memUtilTask = null;
	private String locationID = ConfigConst.NOT_SET;
	private IDataMessageListener dataMsgListener = null;
	
	private Runnable taskRunner = null;
	private boolean isStarted = false;
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public SystemPerformanceManager()
	{
		this.pollRate = ConfigUtil.getInstance().getInteger(ConfigConst.GATEWAY_DEVICE, ConfigConst.POLL_CYCLES_KEY,ConfigConst.DEFAULT_POLL_CYCLES);
		this.locationID =
				ConfigUtil.getInstance().getProperty(
					ConfigConst.GATEWAY_DEVICE, ConfigConst.LOCATION_ID_PROP, ConfigConst.NOT_SET);
		this.schedExecSvc = Executors.newScheduledThreadPool(1);
		this.cpuUtilTask = new SystemCpuUtilTask();
		this.memUtilTask = new SystemMemUtilTask();
		//Start a thread that gets CPU and Memory utilization while the application is running
		this.taskRunner = () -> {
		    this.handleTelemetry();
		};
	}
	
	
	// public methods
	/**
    * Get CPU and memory utilization and set it into this class so that requesting class can access it
    * 
    */
	public void handleTelemetry()
	{
		//Gets CPU and memory utilization
		
		float cpuUtilPct = this.cpuUtilTask.getTelemetryValue();
		float memUtilPct = this.memUtilTask.getTelemetryValue();
		_Logger.info("CPU Utilization = " + cpuUtilPct);
		_Logger.info("Memory Utilization = " + memUtilPct);
		SystemPerformanceData spd = new SystemPerformanceData();
		spd.setLocationID(this.locationID);
		spd.setCpuUtilization(cpuUtilPct);
		spd.setMemoryUtilization(memUtilPct);

		if (this.dataMsgListener != null) {
			this.dataMsgListener.handleSystemPerformanceMessage(
				ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE, spd);
		}
	}
	
	public void setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
		}
	}
	
	public void startManager()
	{
		if (! this.isStarted) {//Start only if not already started
			//Schedule a task that will run the runnable at a fixed rate defined by the constants
		    ScheduledFuture<?> futureTask = this.schedExecSvc.scheduleAtFixedRate(this.taskRunner, 0L, this.pollRate, TimeUnit.SECONDS);

		    this.isStarted = true;
		    _Logger.log(Level.INFO,"Started the Performance Manager");
		}
		else {
			_Logger.log(Level.INFO,"Already running the Performance Manager");
		}
	}
	
	public void stopManager()
	{
		//Stop the scheduled tasks
		this.schedExecSvc.shutdown();
		_Logger.log(Level.INFO,"Stopped the Performance Manager");
	}
	
}
