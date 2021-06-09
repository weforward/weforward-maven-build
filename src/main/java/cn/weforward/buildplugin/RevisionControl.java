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
package cn.weforward.buildplugin;

import org.apache.maven.plugin.MojoFailureException;

/**
 * Revision control工具
 * 
 * @author daibo
 *
 */
public interface RevisionControl {
	/**
	 * 获取最后修改的版本号
	 * 
	 * @return 版本号
	 * @throws MojoFailureException mojo异常
	 */
	String getVersion() throws MojoFailureException;

	/**
	 * 是否有修改未提交
	 * 
	 * @return 有修改未提交返回true
	 * @throws MojoFailureException mojo异常
	 */
	boolean isDirty() throws MojoFailureException;

	/**
	 * 提交修改
	 * 
	 * @param message 信息
	 * @throws MojoFailureException mojo异常
	 */
	void commit(String message) throws MojoFailureException;

	/**
	 * 创建标签
	 * 
	 * @param tag     标签
	 * @param message 信息
	 * @throws MojoFailureException mojo异常
	 */
	void tag(String tag, String message) throws MojoFailureException;

}
