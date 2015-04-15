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

package org.apache.juli.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tomcat的默认log
 * 
 * Hardcoded java.util.logging commons-logging implementation.
 */
class DirectJDKLog implements Log {
    // 没有理由隐藏它，但有理由不隐藏它
	// no reason to hide this - but good reasons to not hide
    public final Logger logger;

    /** Alternate config reader and console format
     */
    private static final String SIMPLE_FMT="java.util.logging.SimpleFormatter";
    private static final String SIMPLE_CFG="org.apache.juli.JdkLoggerConfig"; //doesn't exist 不存在。。囧
    private static final String FORMATTER="org.apache.juli.formatter";

    static {
        if( System.getProperty("java.util.logging.config.class") ==null  &&				//参数默认为空
                System.getProperty("java.util.logging.config.file") ==null ) {
        	
        	// 默认参数 - 糟糕透了，覆盖它让他至少能格式化输出到控制台
            // default configuration - it sucks. Let's override at least the
            // formatter for the console
            try {
                Class.forName(SIMPLE_CFG).newInstance();
            } catch( Throwable t ) {
            }
            try {
                Formatter fmt=(Formatter)Class.forName(System.getProperty(FORMATTER, SIMPLE_FMT)).newInstance();	//获得日志输出formatter 没则使用默认formatter 
                
                //将ConsoleHandler的Formatter设置为fmt
                //也有可能用户修改了logging.properties，但大部分情况下是很愚蠢的 
                // it is also possible that the user modified jre/lib/logging.properties -
                // but that's really stupid in most cases
                Logger root=Logger.getLogger("");																	//获得系统的root logger
                Handler handlers[]=root.getHandlers();																//遍历loggerhandler
                for( int i=0; i< handlers.length; i++ ) {
                	// 我只关心控制台  - 无论如何使用默设置
                    // I only care about console - that's what's used in default config anyway	
                    if( handlers[i] instanceof  ConsoleHandler ) {
                        handlers[i].setFormatter(fmt);
                    }
                }
            } catch( Throwable t ) {
            	//可能不包含，那就使用最丑陋的原始设置
                // maybe it wasn't included - the ugly default will be used.
            }

        }
    }

    public DirectJDKLog(String name ) {
        logger=Logger.getLogger(name);
    }

    @Override
    public final boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    @Override
    public final boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    @Override
    public final boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    @Override
    public final boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    @Override
    public final boolean isFatalEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    @Override
    public final boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINER);
    }

    @Override
    public final void debug(Object message) {
        log(Level.FINE, String.valueOf(message), null);
    }

    @Override
    public final void debug(Object message, Throwable t) {
        log(Level.FINE, String.valueOf(message), t);
    }

    @Override
    public final void trace(Object message) {
        log(Level.FINER, String.valueOf(message), null);
    }

    @Override
    public final void trace(Object message, Throwable t) {
        log(Level.FINER, String.valueOf(message), t);
    }

    @Override
    public final void info(Object message) {
        log(Level.INFO, String.valueOf(message), null);
    }

    @Override
    public final void info(Object message, Throwable t) {
        log(Level.INFO, String.valueOf(message), t);
    }

    @Override
    public final void warn(Object message) {
        log(Level.WARNING, String.valueOf(message), null);
    }

    @Override
    public final void warn(Object message, Throwable t) {
        log(Level.WARNING, String.valueOf(message), t);
    }

    @Override
    public final void error(Object message) {
        log(Level.SEVERE, String.valueOf(message), null);
    }

    @Override
    public final void error(Object message, Throwable t) {
        log(Level.SEVERE, String.valueOf(message), t);
    }

    @Override
    public final void fatal(Object message) {
        log(Level.SEVERE, String.valueOf(message), null);
    }

    @Override
    public final void fatal(Object message, Throwable t) {
        log(Level.SEVERE, String.valueOf(message), t);
    }

    
    // 从公共日志，这是我认为 java.util.logging差的最总要原因 - 通过委员会设计真是糟糕，使用 java.util.logging 对性能的影响
    // 当你需要封装它时的凑楼  他比默认的不友善，不常见的默认格式日志还糟糕
    
    // from commons logging. This would be my number one reason why java.util.logging
    // is bad - design by committee can be really bad ! The impact on performance of
    // using java.util.logging - and the ugliness if you need to wrap it - is far
    // worse than the unfriendly and uncommon default format for logs.

    private void log(Level level, String msg, Throwable ex) {
        if (logger.isLoggable(level)) {
        	//通过黑手段获得堆栈跟踪
            // Hack (?) to get the stack trace.
            Throwable dummyException=new Throwable();
            StackTraceElement locations[]=dummyException.getStackTrace();
            //第三个参数是调用者
            // Caller will be the third element
            String cname = "unknown";								//调用者名称
            String method = "unknown";								//调用方法
            if (locations != null && locations.length >2) {			//如果有三个堆栈元素
                StackTraceElement caller = locations[2];			//获得调用者
                cname = caller.getClassName();						//获得调用者名称
                method = caller.getMethodName();					//获得调用者方法
            }
            //打印
            if (ex==null) {											
                logger.logp(level, cname, method, msg);			
            } else {
                logger.logp(level, cname, method, msg, ex);
            }
        }
    }

    static Log getInstance(String name) {
        return new DirectJDKLog( name );
    }
}


