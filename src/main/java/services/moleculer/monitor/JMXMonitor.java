/**
 * This software is licensed under MIT license.<br>
 * <br>
 * Copyright 2017 Andras Berkes [andras.berkes@programmer.net]<br>
 * <br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.monitor;

import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Name;

/**
 * JMX-based System Monitor. {@link SigarMonitor} is more accurate than this
 * monitor.
 * 
 * @see SigarMonitor
 */
@Name("JMX System Monitor")
public final class JMXMonitor extends Monitor {

	// --- PROPERTIES ---

	private MBeanServer mbs;
	private ObjectName os;

	// --- START MONITOR ---

	/**
	 * Initializes monitor instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public final void start(ServiceBroker broker, Tree config) throws Exception {
		mbs = ManagementFactory.getPlatformMBeanServer();
		os = ObjectName.getInstance("java.lang:type=OperatingSystem");
	}

	// --- SYSTEM MONITORING METHODS ---

	/**
	 * Returns the system CPU usage, in percents, between 0 and 100.
	 * 
	 * @return total CPU usage of the current OS
	 */
	@Override
	public final int getTotalCpuPercent() {
		try {
			AttributeList list = mbs.getAttributes(os, new String[] { "ProcessCpuLoad" });
			if (list.isEmpty()) {
				return 0;
			}
			Attribute att = (Attribute) list.get(0);
			Double value = (Double) att.getValue();
			return (int) Math.max(value * 100d, 0d);
		} catch (Exception cause) {
			logger.warn("Unable to get CPU usage!", cause);
		}
		return 0;
	}

}