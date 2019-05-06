/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.username.remotecontrol.connections;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.example.username.remotecontrol.entities.NetworkDevice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class NetworkServiceManager {
	private final String TAG = "CustomServices";

	private final String TYPE = "_http._tcp.local.";
	private final String SERVICE_NAME = "Client";
	private final int PORT = 49151;
	private final String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;

	private enum serviceInfoTags {
		TYPE_TAG, DEVICE_NAME_TAG
	}

	private enum deviceTypes {
		PC, PHONE
	}

	private Context localContext;
	private JmDNS jmDNS;
	private WifiManager.MulticastLock mMulticastLock;
	private ServiceInfo mServiceInfo;
	private ServiceListener mServiceListener;

	public NetworkServiceManager(Context context) {
		localContext = context;
		try {
			registerService();
		} catch (IOException ex) {
			Log.d(TAG, "Service register exception", ex);
		}
	}

	private void registerService() throws UnknownHostException, IOException{
		WifiManager wifiManager = (WifiManager) localContext.getSystemService(Context.WIFI_SERVICE);
		mMulticastLock = wifiManager.createMulticastLock(SERVICE_NAME);
		mMulticastLock.setReferenceCounted(true);
		mMulticastLock.acquire();

		jmDNS = JmDNS.create(getCurrentInetAddress());
		mServiceInfo = ServiceInfo.create(TYPE, DEVICE_NAME + ":" + deviceTypes.PHONE.toString(), PORT, SERVICE_NAME);

		Map<String, String> serviceInfoMap = new LinkedHashMap<String, String>(){{
			put(serviceInfoTags.TYPE_TAG.toString(), deviceTypes.PHONE.toString());
			put(serviceInfoTags.DEVICE_NAME_TAG.toString(), DEVICE_NAME);
		}};
		mServiceInfo.setText(serviceInfoMap);

		jmDNS.registerService(mServiceInfo);
	}
	
	public void discoveryServices(DiscoveryServiceListener listener){
		mServiceListener = new ServiceListener() {
			@Override
			public void serviceAdded(ServiceEvent event) {
				jmDNS.requestServiceInfo(event.getType(), event.getName(), 1);
			}

			@Override
			public void serviceRemoved(ServiceEvent event) {
				ServiceInfo serviceInfo = event.getInfo();
				Log.d(TAG, serviceInfo.getPropertyString(serviceInfoTags.TYPE_TAG.toString()));
				Log.d(TAG, "Removed " + serviceInfo.getName());
				Log.d(TAG, "------------------------");
				String deviceType = serviceInfo.getPropertyString(serviceInfoTags.TYPE_TAG.toString());
				String deviceName = serviceInfo.getPropertyString(serviceInfoTags.DEVICE_NAME_TAG.toString());
				String hostName = serviceInfo.getInetAddresses()[0].getHostAddress();

				Log.d(TAG, deviceType + ":" + deviceName + ":" + hostName);

				NetworkDevice device = new NetworkDevice()
						.setType(deviceType)
						.setName(deviceName)
						.setIp(hostName);

				Log.d(TAG, String.valueOf(device == null));
				Log.d(TAG, device.toString());

				if(device.getType().equals(deviceTypes.PC.toString())) {
					listener.onRemoved(device);
				}
			}

			@Override
			public void serviceResolved(ServiceEvent event) {
				ServiceInfo serviceInfo = event.getInfo();
                Log.d(TAG, "Added " + serviceInfo.getName());
				String deviceType = serviceInfo.getPropertyString(serviceInfoTags.TYPE_TAG.toString());
				String deviceName = serviceInfo.getPropertyString(serviceInfoTags.DEVICE_NAME_TAG.toString());
				String hostName = serviceInfo.getInetAddresses()[0].getHostAddress();

				NetworkDevice device = new NetworkDevice()
						.setType(deviceType)
						.setName(deviceName)
						.setIp(hostName);

				if(device.getType().equals(deviceTypes.PC.toString())) {
					listener.onFound(device);
				}
			}
		};
		jmDNS.addServiceListener(TYPE, mServiceListener);
	}

	public void unregisterServices() {
		if (jmDNS != null) {
			if (mServiceListener != null) {
				jmDNS.removeServiceListener(TYPE, mServiceListener);
				mServiceListener = null;
			}
			jmDNS.unregisterAllServices();
			try {
				jmDNS.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (mMulticastLock != null && mMulticastLock.isHeld()) {
			mMulticastLock.release();
		}
	}

	private InetAddress getCurrentInetAddress() throws UnknownHostException {
		WifiManager wifiManager = (WifiManager) localContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo connectionInfo = wifiManager.getConnectionInfo();
		int ip = connectionInfo.getIpAddress();
		String ipAddress = String.format("%d.%d.%d.%d",
						(ip & 0xff),
						(ip >> 8 & 0xff),
						(ip >> 16 & 0xff),
						(ip >> 24 & 0xff));
		return InetAddress.getByName(ipAddress);
	}
}