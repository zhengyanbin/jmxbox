/*   Copyright 2011 Appbox inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.appbox.monitoring.jmx.jmxbox.commands;

import java.io.IOException;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import ca.appbox.monitoring.jmx.jmxbox.commons.JmxException;

/**
 * A mxBean operation invocation.
 * 
 */
public class JmxGetMBeanCommandImpl implements JmxCommand {

    private String jmxBean;
    
	public JmxGetMBeanCommandImpl(String jmxBean) {
	    this.jmxBean = jmxBean;
	}

	@Override
	public boolean hasOutput() {
		return false;
	}

	@Override
	public JmxCommandResult invoke(MBeanServerConnection mBeanServerConnection) throws JmxException {

        if (mBeanServerConnection == null) {
            throw new IllegalArgumentException("mBeanServer cannot be null");
        }

        ObjectName objectName = null;

        try {
            objectName = new ObjectName(jmxBean);
        } catch (Exception e) {
            throw new JmxException("Invalid mBean name : " + jmxBean);
        }

        try {
            Set<ObjectName> beanSet = mBeanServerConnection.queryNames(objectName, null);
            for(ObjectName on : beanSet){
                System.out.println(on.getCanonicalName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return new JmxCommandResultImpl(null, this);
    }
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("domain : ");
		sb.append(getJmxBean());
		return sb.toString();
	}

    @Override
    public String getJmxBean() {
        return jmxBean;
    }
}
