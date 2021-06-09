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

import org.apache.maven.plugin.logging.Log;

import cn.weforward.buildplugin.UploadProgressBar;

public class MojoLogUploadProgressBar implements UploadProgressBar {

	Log m_Out;

	public MojoLogUploadProgressBar(Log log) {
		m_Out = log;
	}

	@Override
	public void start() {
		m_Out.info("开始上传文件");
	}

	@Override
	public void progress(long count, long sum) {
		long step = ((count * 100 / sum) * 100) / 100;
		m_Out.info("当前进度:" + step + "%");
	}

	@Override
	public void end() {
		m_Out.info("上传文件结束，等待处理结果");
	}

	@Override
	public void response(int code, String message) {
		if (code != 200) {
			m_Out.error("上传结果:" + code + "/" + message);
		} else {
			m_Out.info("上传结果:" + code + "/" + message);
		}
	}

	@Override
	public void exception(Exception e) {
		m_Out.info("上传异常:" + e.getMessage(), e);
	}

}
