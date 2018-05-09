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
package ca.appbox.monitoring.jmx.jmxbox;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import ca.appbox.monitoring.jmx.jmxbox.commands.JmxCommand;
import ca.appbox.monitoring.jmx.jmxbox.commands.JmxCommandResult;
import ca.appbox.monitoring.jmx.jmxbox.commons.JmxException;
import ca.appbox.monitoring.jmx.jmxbox.commons.SingletonHolder;
import ca.appbox.monitoring.jmx.jmxbox.commons.context.JmxContext;
import ca.appbox.monitoring.jmx.jmxbox.output.OutputStrategy;

/**
 * Invokes all the commands in the context and output the results in the output file
 * or to the standard output accordingly.
 * 
 */
public class JmxClientImpl implements JmxClient {
    
    private static final Logger logger = LoggerFactory.getLogger(JmxClientImpl.class);

	private static final JmxClientImpl instance = new JmxClientImpl();

	private JmxClientImpl() {
		super();
	}

	public static JmxClientImpl getInstance() {
		return instance;
	}

	public void run(JmxContext context) throws JmxException {

		OutputStrategy outputStrategy = SingletonHolder.getOutputStragey(context);
		
		try {

			MBeanServerConnection mBeanServerConnection = createJmxConnection(context);

			for (Integer i = 0; i < context.getRepetitions(); i++) {

				List<JmxCommandResult> currentResults = new ArrayList<JmxCommandResult>();
				
				for (JmxCommand command : context.getCommands()) {

					JmxCommandResult result = command
							.invoke(mBeanServerConnection);

					currentResults.add(result);
					
				}
				
				outputStrategy.writeOutput(context, currentResults);
				
				Thread.sleep(context.getIntervalInMiliseconds());
			}

		} catch (Exception e) {
			throw new JmxException("Problem while invoking commands." + e);
		} 
	}

	private MBeanServerConnection createJmxConnection(JmxContext context)
			throws MalformedURLException, IOException {
		JMXServiceURL target;

		Map<String, Object> env = new HashMap<String, Object>();

		if (context.hasCredentials()) {
			String[] credentials = new String[]{context.getUser(), context.getPassword()};
			env.put(JMXConnector.CREDENTIALS, credentials);
		}

		if(context.getPid() != null){
		    target = getLocalJmx(context.getPid());
		}else{
		    target = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"
		            + context.getHost() + ":" + context.getPort() + "/jmxrmi");
		}
		
		JMXConnector connector = JMXConnectorFactory.connect(target,env);
		MBeanServerConnection mBeanServerConnection = connector
				.getMBeanServerConnection();
		return mBeanServerConnection;
	}
	
    private JMXServiceURL getLocalJmx(String pid) {
        VirtualMachine vm;
        JMXServiceURL url = null;
        try {
            vm = VirtualMachine.attach(pid);

            String connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
            if (connectorAddress == null) {
                logger.info("connectorAddress  is  null, need load management agent");
                String javaHome = vm.getSystemProperties().getProperty("java.home");
                String agentPath = javaHome + File.separator + "jre" + File.separator + "lib" + File.separator + "management-agent.jar";
                File file = new File(agentPath);
                if (!file.exists()) {
                    agentPath = javaHome + File.separator + "lib" + File.separator + "management-agent.jar";
                    file = new File(agentPath);
                    if (!file.exists()) {
                        throw new IOException("Management agent not found");
                    }
                }

                agentPath = file.getCanonicalPath();
                vm.loadAgent(agentPath, "com.sun.management.jmxremote");
                // jdk1.8之后可以使用该方法替代上面加载manager的方式
                // vm.startLocalManagementAgent();
                connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
            }
            logger.info("local connector address :{}", connectorAddress);
            // 以下代码用于连接指定的jmx，本地或者远程
            url = new JMXServiceURL(connectorAddress);
        } catch (AttachNotSupportedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AgentLoadException e) {
            e.printStackTrace();
        } catch (AgentInitializationException e) {
            e.printStackTrace();
        }
        return url;
	}
}
