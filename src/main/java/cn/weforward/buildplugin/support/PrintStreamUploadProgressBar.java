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
package cn.weforward.buildplugin.support;

import java.io.PrintStream;

import cn.weforward.buildplugin.UploadProgressBar;

/**
 * 打印流进度条
 * 
 * @author daibo
 *
 */
public class PrintStreamUploadProgressBar implements UploadProgressBar {

	private PrintStream m_Out;

	private boolean m_PrintTitle = true;
	private long m_Current = 0;

	public PrintStreamUploadProgressBar(PrintStream out) {
		m_Out = out;
	}

	@Override
	public void start() {
		m_Out.println("开始上传文件");
	}

	@Override
	public void progress(long count, long sum) {
		if (m_PrintTitle) {
			System.out.print("文件长度(%):");
			for (int i = 0; i < 100; i++) {
				System.out.print('=');
			}
			System.out.println();
			m_PrintTitle = false;
		}
		if (m_Current == 0) {
			System.out.print("当前进度(%):");
		}
		long step = ((count * 100 / sum) * 100) / 100;
		for (int i = 0; i < step - m_Current; i++) {
			System.out.print('-');
		}
		m_Current = step;
		if (count == sum) {
			System.out.println();
		}
	}

	@Override
	public void end() {
		m_Out.println("上传文件结束");
	}

	@Override
	public void response(int code, String message) {
		m_Out.println("上传结果:" + code + "/" + message);
	}

	@Override
	public void exception(Exception e) {
		m_Out.println("上传异常:" + e.getMessage());
		e.printStackTrace(m_Out);
	}
}
