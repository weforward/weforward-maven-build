/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.buildplugin.util;

/**
 * 字符串工具
 * 
 * @author daibo
 *
 */
public class StringUtil {

	private StringUtil() {

	}

	/**
	 * 是否为空
	 * 
	 * @param str
	 * @return 为空返回true
	 */
	public static boolean isEmpty(String str) {
		return null == str || str.trim().length() == 0;
	}

	/**
	 * 比较字符串
	 * 
	 * @param s1
	 * @param s2
	 * @return 相同返回true
	 */
	public static boolean eq(String s1, String s2) {
		if (null == s1) {
			return null == s2;
		}
		return s1.equals(s2);
	}

}
