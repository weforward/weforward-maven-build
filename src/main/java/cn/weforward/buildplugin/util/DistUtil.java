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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import cn.weforward.buildplugin.UploadProgressBar;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * 发布工具
 * 
 * @author daibo
 *
 */
public class DistUtil {

	/**
	 * 文件上传的方法
	 * 
	 * @param disturl     上传链接
	 * @param file        文件
	 * @param username    用户名
	 * @param password    密码
	 * @param progressBar 进度条
	 * @throws IOException          IO异常
	 * @throws InterruptedException 中断异常
	 */
	public static void uploadFile(String disturl, File file, String username, String password,
			UploadProgressBar progressBar) throws IOException, InterruptedException {
		EventLoopGroup group = null;
		URI uri;
		try {
			uri = new URI(disturl);
		} catch (URISyntaxException e) {
			throw new UnsupportedOperationException(disturl + "不合法");
		}
		try {
			final UploadFileHandler handler = new UploadFileHandler(uri, file, progressBar, username, password);
			group = new NioEventLoopGroup();
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true)
					.handler(new ChannelInitializer<Channel>() {

						@Override
						protected void initChannel(Channel channel) throws Exception {
							channel.pipeline().addLast(new IdleStateHandler(61, 30, 0, TimeUnit.SECONDS));
							channel.pipeline().addLast("s-encoder", new HttpRequestEncoder());
							channel.pipeline().addLast("s-decoder", new HttpResponseDecoder());
							channel.pipeline().addLast(handler);
						}
					});
			int port = uri.getPort();
			if (port == -1) {
				if (StringUtil.eq(uri.getScheme(), "https")) {
					port = 443;
				} else {
					port = 80;
				}
			}
			String host = uri.getHost();
			ChannelFuture future = bootstrap.connect(host, port).sync();
			future.channel().closeFuture().sync();
		} finally {
			if (null != group) {
				group.shutdownGracefully();
			}

		}
	}

	/**
	 * 文件上传的方法
	 * 
	 * @param disturl  上传链接
	 * @param file     文件
	 * @param username 用户名
	 * @param password 密码
	 * @throws IOException IO异常
	 */
	public static void uploadFile(String disturl, File file, String username, String password) throws IOException {
		String end = "\r\n";
		String twoHyphens = "--";
		String boundary = "----WeforwardBoundary" + System.currentTimeMillis();
		DataOutputStream ds = null;
		InputStream inputStream = null;
		String actionUrl = disturl + file.getName();
		try {
			// 统一资源
			URL url = new URL(actionUrl);
			// 连接类的父类，抽象类
			URLConnection urlConnection = url.openConnection();
			// http的连接类
			HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
			// 设置是否从httpUrlConnection读入，默认情况下是true;
			httpURLConnection.setDoInput(true);
			// 设置是否向httpUrlConnection输出
			httpURLConnection.setDoOutput(true);
			// Post 请求不能使用缓存
			httpURLConnection.setUseCaches(false);
			// 设定请求的方法，默认是GET
			httpURLConnection.setRequestMethod("POST");
			// 设置字符编码连接参数
			httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
			// 设置字符编码
			httpURLConnection.setRequestProperty("Charset", "UTF-8");
			// 设置请求内容类型
			httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			if (!StringUtil.isEmpty(username) && !StringUtil.isEmpty(password)) {
				String basic = "Basic " + new String(org.apache.commons.codec.binary.Base64
						.encodeBase64URLSafe((username + ":" + password).getBytes()));
				httpURLConnection.setRequestProperty("Authorization", basic);
			}

			httpURLConnection.setReadTimeout(30 * 60 * 1000);

			// 设置DataOutputStream
			ds = new DataOutputStream(httpURLConnection.getOutputStream());
			ds.writeBytes(twoHyphens + boundary + end);
			ds.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + file.getName() + "\"" + end);
			ds.writeBytes(end);
			FileInputStream fStream = new FileInputStream(file);
			int bufferSize = 4 * 1024;
			byte[] buffer = new byte[bufferSize];
			int length = -1;
			while ((length = fStream.read(buffer)) != -1) {
				ds.write(buffer, 0, length);
			}
			ds.writeBytes(end);
			/* close streams */
			fStream.close();
			ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
			/* close streams */
			ds.flush();
			if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new IOException(
						"HTTP Request is not success, Response code is " + httpURLConnection.getResponseCode());
			}

		} finally {
			if (ds != null) {
				try {
					ds.close();
				} catch (IOException e) {
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
