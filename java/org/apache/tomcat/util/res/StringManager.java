/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.tomcat.util.res;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * An internationalization / localization helper class which reduces
 * the bother of handling ResourceBundles and takes care of the
 * common cases of message formating which otherwise require the
 * creation of Object arrays and such.
 *
 * <p>The StringManager operates on a package basis. One StringManager
 * per package can be created and accessed via the getManager method
 * call.
 *
 * <p>The StringManager will look for a ResourceBundle named by
 * the package name given plus the suffix of "LocalStrings". In
 * practice, this means that the localized information will be contained
 * in a LocalStrings.properties file located in the package
 * directory of the classpath.
 *
 * <p>Please see the documentation for java.util.ResourceBundle for
 * more information.
 *
 * @author James Duncan Davidson [duncan@eng.sun.com]
 * @author James Todd [gonzo@eng.sun.com]
 * @author Mel Martinez [mmartinez@g1440.com]
 * @see java.util.ResourceBundle
 */
public class StringManager {

    private static int LOCALE_CACHE_SIZE = 10;

    /**
     * The ResourceBundle for this StringManager.
     */
    private final ResourceBundle bundle;
    private final Locale locale;

    /**
     * 根据包名构建一个新的StringManager，这是一个私有方法，只能通过getManager的静态方法获得实例，从而使得每个包名只有一个StringManager
     * 
     * Creates a new StringManager for a given package. This is a
     * private method and all access to it is arbitrated by the
     * static getManager method call so that only one StringManager
     * per package will be created.
     *
     * @param packageName Name of package to create StringManager for.
     */
    private StringManager(String packageName, Locale locale) {
        String bundleName = packageName + ".LocalStrings";
        ResourceBundle bnd = null;
        try {
        	//默认加载stringBundle方法，从包内加载
            bnd = ResourceBundle.getBundle(bundleName, locale);
        } catch( MissingResourceException ex ) {
        	//尝试从当前的类加载器加载，（只为信任的app）
        	//TC5是什么？不懂
        	
            // Try from the current loader (that's the case for trusted apps)
            // Should only be required if using a TC5 style classloader structure
            // where common != shared != server
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if( cl != null ) {
                try {
                    bnd = ResourceBundle.getBundle(bundleName, locale, cl);
                } catch(MissingResourceException ex2) {
                    // Ignore
                }
            }
        }
        bundle = bnd;
        //获得实际的可能与请求的语言不通的语言，设置自身的locale
        // Get the actual locale, which may be different from the requested one
        if (bundle != null) {
            Locale bundleLocale = bundle.getLocale();
            if (bundleLocale.equals(Locale.ROOT)) {
                this.locale = Locale.ENGLISH;
            } else {
                this.locale = bundleLocale;
            }
        } else {
            this.locale = null;
        }
    }

    /**
     * 返回存在的资源文本，不存在返回空
     * 
     * 
        Get a string from the underlying resource bundle or return
        null if the String is not found.

        @param key to desired resource String
        @return resource String matching <i>key</i> from underlying
                bundle or null if not found.
        @throws IllegalArgumentException if <i>key</i> is null.
     */
    public String getString(String key) {
        if(key == null){
            String msg = "key may not have a null value";

            throw new IllegalArgumentException(msg);
        }

        String str = null;

        try {
        	//避免空指针错误，使他变成一个类似于MissingResourceException错误
            // Avoid NPE if bundle is null and treat it like an MRE
            if (bundle != null) {
                str = bundle.getString(key);
            }
        } catch(MissingResourceException mre) {
        	
        	//缺点：实际错误将会被隐藏
        	//优点：不会产生冗余代码
        	//更好：返回空，后续代码只需做空指针判断
        	
        	
            //bad: shouldn't mask an exception the following way:
            //   str = "[cannot find message associated with key '" + key +
            //         "' due to " + mre + "]";
            //     because it hides the fact that the String was missing
            //     from the calling code.
            //good: could just throw the exception (or wrap it in another)
            //      but that would probably cause much havoc on existing
            //      code.
            //better: consistent with container pattern to
            //      simply return null.  Calling code can then do
            //      a null check.
            str = null;
        }

        return str;
    }

    /**
     * 
     * 根据key获得文本并根据MessageFormat参数格式化，获得value为null将直接使用key格式化
     * 
     * 
     * Get a string from the underlying resource bundle and format
     * it with the given set of arguments.
     *
     * @param key
     * @param args
     */
    public String getString(final String key, final Object... args) {
        String value = getString(key);
        if (value == null) {
            value = key;
        }

        MessageFormat mf = new MessageFormat(value);
        mf.setLocale(locale);
        return mf.format(args, new StringBuffer(), null).toString();
    }

    /**
     * Identify the Locale this StringManager is associated with
     */
    public Locale getLocale() {
        return locale;
    }

    // --------------------------------------------------------------
    // STATIC SUPPORT METHODS 静态支持
    // --------------------------------------------------------------

    /**
     * 使用一个HashTable作为对象管理，（出于线程安全考虑？）
     * 每个包对应一个表
     * 
     * */
    private static final Map<String, Map<Locale,StringManager>> managers =
            new Hashtable<>();

    /**
     * 获根据包名得一个StringManager，如果存在包对应的manager，使用旧有的，否则构建一个新的管理器
     * 
     * 
     * Get the StringManager for a particular package. If a manager for
     * a package already exists, it will be reused, else a new
     * StringManager will be created and returned.
     *
     * @param packageName The package name
     */
    public static final synchronized StringManager getManager(
            String packageName) {
        return getManager(packageName, Locale.getDefault());
    }

    /**
     * 
     * 获根据包名得一个StringManager，如果存在包对应的manager，使用旧有的，否则构建一个新的管理器
     * 
     *  
     * Get the StringManager for a particular package and Locale. If a manager
     * for a package/Locale combination already exists, it will be reused, else
     * a new StringManager will be created and returned.
     *
     * @param packageName The package name
     * @param locale      The Locale
     */
    public static final synchronized StringManager getManager(
            String packageName, Locale locale) {

        Map<Locale,StringManager> map = managers.get(packageName);
        if (map == null) {
            /*
             * 不想让HashMap超过 LOCALE_CACHE_SIZE
             * 
             * Don't want the HashMap to be expanded beyond LOCALE_CACHE_SIZE.
             * Expansion occurs when size() exceeds capacity. Therefore keep
             * size at or below capacity.
             * removeEldestEntry() executes after insertion therefore the test
             * for removal needs to use one less than the maximum desired size
             *
             */
            map = new LinkedHashMap<Locale,StringManager>(LOCALE_CACHE_SIZE, 1, true) {
                private static final long serialVersionUID = 1L;
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<Locale,StringManager> eldest) {
                    if (size() > (LOCALE_CACHE_SIZE - 1)) {				//如果超过最大容量，丢弃最老的entry
                        return true;
                    }
                    return false;
                }
            };
            managers.put(packageName, map);	//将表插入managers
        }

        //根据语言获得管理器，不存在则重新构建插入
        StringManager mgr = map.get(locale);				
        if (mgr == null) {
            mgr = new StringManager(packageName, locale);
            map.put(locale, mgr);
        }
        return mgr;
    }

    /**
     * 
     * 从多个语言中找出一个StringManager，第一个找到的StringManager会被返回，全部找不到返回默认的
     * 
     * Retrieve the StringManager for a list of Locales. The first StringManager
     * found will be returned.
     *
     * @param requestedLocales the list of Locales
     *
     * @return the found StringManager or the default StringManager
     */
    public static StringManager getManager(String packageName,
            Enumeration<Locale> requestedLocales) {
        while (requestedLocales.hasMoreElements()) {
            Locale locale = requestedLocales.nextElement();
            StringManager result = getManager(packageName, locale);
            if (result.getLocale().equals(locale)) {
                return result;
            }
        }
        // Return the default
        return getManager(packageName);
    }
}
