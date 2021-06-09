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

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

import cn.weforward.buildplugin.UploadProgressBar;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

/**
 * 上传文件处理器
 * 
 * @author daibo
 *
 */
public class UploadFileHandler extends ChannelInboundHandlerAdapter implements Runnable {

	protected ChannelHandlerContext m_Ctx;

	private URI m_Uri;

	private File m_File;

	private String m_Username;

	private String m_Password;

	protected Thread m_Thread;

	protected UploadProgressBar m_ProgressBar;

	public UploadFileHandler(URI uri, File file, UploadProgressBar progressBar, String username, String password) {
		m_Uri = uri;
		m_File = file;
		m_ProgressBar = progressBar;
		m_Username = username;
		m_Password = password;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		m_Ctx = ctx;
		startUpload();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		stopUpload();
	}

	private synchronized void startUpload() {
		if (null == m_Thread) {
			Thread t = new Thread(this, "uploadfile-" + m_File.getName());
			t.start();
			m_Thread = t;
		}
	}

	private synchronized void stopUpload() {
		if (null != m_Thread) {
			m_Thread.interrupt();
			m_Thread = null;
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof HttpResponse) {
			HttpResponse response = (HttpResponse) msg;
			HttpResponseStatus status = response.status();
			int code = status.code();
			String message = "";
			if (response instanceof FullHttpResponse) {
				message = ((FullHttpResponse) response).content().toString(CharsetUtil.UTF_8);
			} else {
				message = status.reasonPhrase();
			}
			m_ProgressBar.response(code, message);
		}
		if (msg instanceof LastHttpContent) {
			ctx.close();
		}
	}

	@Override
	public void run() {
		File file = m_File;
		try (FileInputStream fStream = new FileInputStream(file)) {
			String filename = file.getName();
			DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET,
					m_Uri.getPath() + filename);
			request.setMethod(HttpMethod.POST);
			String end = "\r\n";
			String twoHyphens = "--";
			String boundary = "Weforward_Boundary______" + System.currentTimeMillis();
			StringBuilder startsb = new StringBuilder();
			startsb.append(twoHyphens + boundary + end);
			startsb.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + end);
			startsb.append(end);

			byte[] startbs = startsb.toString().getBytes();

			StringBuilder endsb = new StringBuilder();
			endsb.append(end);
			endsb.append(twoHyphens + boundary + twoHyphens + end);
			byte[] endbs = endsb.toString().getBytes();

			request.headers().add("Charset", "UTF-8");
			request.headers().add(HttpHeaderNames.HOST, m_Uri.getHost());
			request.headers().add(HttpHeaderNames.CONTENT_TYPE, "multipart/form-data;boundary=" + boundary);
			request.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
			if (!StringUtil.isEmpty(m_Username) && !StringUtil.isEmpty(m_Password)) {
				String basic = "Basic " + new String(org.apache.commons.codec.binary.Base64
						.encodeBase64URLSafe((m_Username + ":" + m_Password).getBytes()));
				request.headers().add("Authorization", basic);
			}

			long l = startbs.length + file.length() + endbs.length;
			request.headers().add(HttpHeaderNames.CONTENT_LENGTH, l);
			m_Ctx.writeAndFlush(request);

			ChannelFuture future = m_Ctx.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(startbs)));
			future.await();
			int bufferSize = 1024 * 1024;
			byte[] buffer = new byte[bufferSize];
			int length = -1;
			if (null != m_ProgressBar) {
				m_ProgressBar.start();
			}
			long count = 0;
			long sum = file.length();
			while ((length = fStream.read(buffer)) != -1) {
				count += length;
				if (null != m_ProgressBar) {
					m_ProgressBar.progress(count, sum);
				}
				future = m_Ctx.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(buffer, 0, length))).sync();
				future.await();
			}
			future = m_Ctx.writeAndFlush(new DefaultHttpContent(Unpooled.wrappedBuffer(endbs)));
			future.await();
			if (null != m_ProgressBar) {
				m_ProgressBar.end();
			}
		} catch (Exception e) {
			if (null != m_ProgressBar) {
				m_ProgressBar.exception(e);
			}
		}

	}
}