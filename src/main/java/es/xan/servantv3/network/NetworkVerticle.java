package es.xan.servantv3.network;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.network.RouterPageManager.Device;

public class NetworkVerticle  extends AbstractServantVerticle {
	
	private static final Logger LOG = LoggerFactory.getLogger(NetworkVerticle.class);
	
	private RouterPageManager mManager = null;
	
	private List<Device> devices;
	private List<Device> quarantine;
	
	public NetworkVerticle() {
		super(Constant.NETWORK_VERTICLE);
		
		supportedActions(Actions.values());

	}
	
	public enum Actions implements Action {
		CHECK_NETWORK(null)
		;

		Class<?> mBeanClass;
		private Actions(Class<?> beanClass) {
			this.mBeanClass = beanClass;
		}
		
		@Override
		public Class<?> getPayloadClass() {
			return this.mBeanClass;
		}
		
	}
	
	public void start() {
		super.start();
		
		devices = new ArrayList<RouterPageManager.Device>();
		quarantine = new ArrayList<RouterPageManager.Device>();
		
		mManager = new RouterPageManager(Vertx.currentContext().config().getJsonObject("NetworkVerticle"));
	
		vertx.setPeriodic(300000, id -> {
			publishAction(Actions.CHECK_NETWORK);
		});
		
		LOG.info("started NetworkVerticle");
	}
		
	public void check_network() {
			try {
				List<Device> newDevices = mManager.getDevices();
				
				List<Device> newItems = resolveDiffsDevices(devices, newDevices);
				List<Device> newsInQuarantine = resolveIntersectionDevices(newItems, quarantine);
				List<Device> newsToNotify = resolveDiffsDevices(quarantine, newItems);
				
				List<Device> removedToNotify = resolveDiffsDevices(newsInQuarantine, quarantine);
				
				List<Device> lost = resolveDiffsDevices(newDevices, devices);
				
				if (!newsToNotify.isEmpty()) {
					for (Device item : newsToNotify)
						publishEvent(Events.NEW_NETWORK_DEVICES_MESSAGE, item);
				}
				
				if (!removedToNotify.isEmpty()) {
					for (Device item : removedToNotify)
						publishEvent(Events.REM_NETWORK_DEVICES_MESSAGE, item);
				}
				
				devices = newDevices;
				quarantine = lost;
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	private List<Device> resolveIntersectionDevices(List<Device> set1, List<Device> set2) {
		return set1.stream().filter(it -> exists(it, set2)).collect(Collectors.toList());
	}


	private List<Device> resolveDiffsDevices(List<Device> superSet, List<Device> setToCheck) {
		return setToCheck.stream().filter(it -> !exists(it, superSet)).collect(Collectors.toList());
	}

	private boolean exists(Device actual, List<Device> set) {
		return set.stream().anyMatch(it -> it.mac.equals(actual.mac));
	}
}
