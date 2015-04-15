/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import me.halin.lee.debug.DebugLog;


/**
 * �����࣬���������������
 * ����ʱʹ�õ���D:\Program Files\Apache Software Foundation\Tomcat 8.0\conf\catalina.properties
 * 
 * Utility class to read the bootstrap Catalina configuration.
 *
 * @author Remy Maucherat
 */
public class CatalinaProperties {

    private static final org.apache.juli.logging.Log log=
        org.apache.juli.logging.LogFactory.getLog( CatalinaProperties.class );

    /**�������ļ���ȡ�Ĳ�����Ϣ*/
    private static Properties properties = null;


    static {
    	//���ز���
    	loadProperties();		
    }


    /**
     * Return specified property value.
     */
    public static String getProperty(String name) {
        return properties.getProperty(name);
    }


    /**
     * ���ز���
     * 
     * Load properties.
     */
    private static void loadProperties() {

    	
    	//����properties
        InputStream is = null;
        Throwable error = null;

        try {
            String configUrl = System.getProperty("catalina.config");		//���ϵͳ�������˴��õ��գ�
            if (configUrl != null) {
                is = (new URL(configUrl)).openStream();
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        if (is == null) {
            try {
                File home = new File(Bootstrap.getCatalinaBase());			//��ø�·��(�˴�ʹ�ø��ļ�)
                File conf = new File(home, "conf");							//conf�ļ���
                File propsFile = new File(conf, "catalina.properties");		//��ȡcatalina��properties�ļ�
                is = new FileInputStream(propsFile);						//������

            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        if (is == null) {													//��ȻΪ�գ�ʹ��Ĭ�ϵ�properties
            try {
                is = CatalinaProperties.class.getResourceAsStream
                    ("/org/apache/catalina/startup/catalina.properties");
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        //��ʼ��properties
        if (is != null) {													
            try {
                properties = new Properties();
                properties.load(is);
            } catch (Throwable t) {
                handleThrowable(t);
                error = t;
            } finally {
                try {
                    is.close();
                } catch (IOException ioe) {
                    log.warn("Could not close catalina.properties", ioe);
                }
            }
        }

    	//������Ϊ�գ�����
        if ((is == null) || (error != null)) {							
            // Do something
            log.warn("Failed to load catalina.properties", error);
            // That's fine - we have reasonable defaults.
            properties = new Properties();
        }

        //��propertiesд��ϵͳ����
        // Register the properties as system properties
        Enumeration<?> enumeration = properties.propertyNames();	
        while (enumeration.hasMoreElements()) {						//����properties��name
            String name = (String) enumeration.nextElement();		//���name
            String value = properties.getProperty(name);			//���value
            if (value != null) {
                System.setProperty(name, value);					//д��property
            }
        }
    }


    // Copied from ExceptionUtils since that class is not visible during start
    private static void handleThrowable(Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        }
        // All other instances of Throwable will be silently swallowed
    }
}
