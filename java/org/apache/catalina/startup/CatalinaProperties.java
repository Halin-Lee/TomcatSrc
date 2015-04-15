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
 * 工具类，负责加载启动参数
 * 调试时使用的是D:\Program Files\Apache Software Foundation\Tomcat 8.0\conf\catalina.properties
 * 
 * Utility class to read the bootstrap Catalina configuration.
 *
 * @author Remy Maucherat
 */
public class CatalinaProperties {

    private static final org.apache.juli.logging.Log log=
        org.apache.juli.logging.LogFactory.getLog( CatalinaProperties.class );

    /**从配置文件读取的参数信息*/
    private static Properties properties = null;


    static {
    	//加载参数
    	loadProperties();		
    }


    /**
     * Return specified property value.
     */
    public static String getProperty(String name) {
        return properties.getProperty(name);
    }


    /**
     * 加载参数
     * 
     * Load properties.
     */
    private static void loadProperties() {

    	
    	//加载properties
        InputStream is = null;
        Throwable error = null;

        try {
            String configUrl = System.getProperty("catalina.config");		//获得系统参数（此处得到空）
            if (configUrl != null) {
                is = (new URL(configUrl)).openStream();
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        if (is == null) {
            try {
                File home = new File(Bootstrap.getCatalinaBase());			//获得根路径(此处使用该文件)
                File conf = new File(home, "conf");							//conf文件夹
                File propsFile = new File(conf, "catalina.properties");		//读取catalina。properties文件
                is = new FileInputStream(propsFile);						//创建流

            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        if (is == null) {													//仍然为空，使用默认的properties
            try {
                is = CatalinaProperties.class.getResourceAsStream
                    ("/org/apache/catalina/startup/catalina.properties");
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        //初始化properties
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

    	//输入流为空，报错
        if ((is == null) || (error != null)) {							
            // Do something
            log.warn("Failed to load catalina.properties", error);
            // That's fine - we have reasonable defaults.
            properties = new Properties();
        }

        //将properties写入系统参数
        // Register the properties as system properties
        Enumeration<?> enumeration = properties.propertyNames();	
        while (enumeration.hasMoreElements()) {						//遍历properties的name
            String name = (String) enumeration.nextElement();		//获得name
            String value = properties.getProperty(name);			//获得value
            if (value != null) {
                System.setProperty(name, value);					//写入property
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
