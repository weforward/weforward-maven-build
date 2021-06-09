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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.plugin.logging.Log;

/**
 * 文件工具
 * 
 * @author daibo
 *
 */
public class FileUtil {

	private FileUtil() {

	}

	/**
	 * 默认文件过滤器
	 */
	public static FilenameFilter DEFAULT_FILTER = new FilenameFilter() {

		public boolean accept(File dir, String name) {
			return !name.equals(".svn") && !name.equals("classes");
		}
	};

	/**
	 * 取得绝对路径
	 * 
	 * @param path 相对路径，支持”../“
	 * @param root 给相对路径作对照的绝对根路径，或为null则使用System.getProperty("user.dir")
	 * @return 转换后的绝对路径
	 */
	static public String getAbsolutePath(String path, String root) {
		// 使用user.dir作为相对目录的根目录
		path = null == path ? "" : path.trim();
		char ch = (path.length() > 0) ? path.charAt(0) : ' ';
		// 要注意windows的路径 c:\xxx 之类
		if ('/' != ch && '\\' != ch && (path.length() < 2 || path.charAt(1) != ':')) {
			if (null == root) {
				root = System.getProperty("user.dir");
			}
			String sp = System.getProperty("file.separator");
			if (null == sp || 0 == sp.length()) {
				sp = "/";
			}
			ch = root.charAt(root.length() - 1);
			if ('/' == ch || '\\' == ch || sp.charAt(0) == ch) {
				root = root.substring(0, root.length() - 1);
			}
			// 转换上层路径“../”
			while (path.startsWith("../")) {
				int idx = root.lastIndexOf(sp);
				if (-1 == idx) {
					break;
				}
				root = root.substring(0, idx);
				path = path.substring(3, path.length());
			}
			path = root + sp + path;
		}
		return path;
	}

	/**
	 * 关闭
	 * 
	 * @param able 可关闭接口
	 * @throws IOException IO异常
	 */
	public static void close(Closeable able) throws IOException {
		if (null != able) {
			able.close();
		}
	}

	/**
	 * 复制文件,支持递归复制
	 * 
	 * @param file    文件
	 * @param newPath 新路径
	 * @param log     日志
	 * @param options 选项
	 * @throws IOException IO异常
	 */
	public static void copy(File file, String newPath, Log log, int options) throws IOException {
		if (file.isDirectory()) {
			new File(newPath).mkdirs();
			if (null != log) {
				log.debug("mkdir " + newPath);
			}
			for (File f : file.listFiles(DEFAULT_FILTER)) {
				copy(f, newPath + File.separator + f.getName(), log, options);
			}
		} else {
			copyFile(file, newPath, log);
		}
	}

	/**
	 * 删除文件支持递归删除
	 * 
	 * @param file 文件
	 * @param log  日志
	 * @throws IOException IO异常
	 */
	public static void delate(File file, Log log) throws IOException {
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				delate(f, log);
			}
			boolean isSuccess = file.delete();
			if (null != log) {
				log.debug("delete " + file.getAbsolutePath() + " " + isSuccess);
			}
			if (!isSuccess) {
				throw new IOException("删除" + file.getAbsolutePath() + "失败");
			}
		} else {
			boolean isSuccess = file.delete();
			if (null != log) {
				log.debug("delete " + file.getAbsolutePath() + " " + isSuccess);
			}
			if (!isSuccess) {
				throw new IOException("删除" + file.getAbsolutePath() + "失败");
			}
		}
	}

	/**
	 * 复制文件，只支持文件类型，不支持目录
	 * 
	 * @param file    文件
	 * @param newPath 路径
	 * @param log     日志
	 * @throws IOException IO异常
	 */
	public static void copyFile(File file, String newPath, Log log) throws IOException {
		InputStream in = null;
		FileOutputStream out = null;
		try {
			in = new FileInputStream(file);
			out = new FileOutputStream(newPath);
			int byteread = 0;
			byte[] buffer = new byte[1024];
			while ((byteread = in.read(buffer)) != -1) {
				out.write(buffer, 0, byteread);
			}
			if (null != log) {
				log.debug("copy  " + file.getAbsolutePath() + " to " + newPath);
			}
		} finally {
			close(in);
			close(out);
		}
	}

	/**
	 * 确保目录存在
	 * 
	 * @param file 文件
	 */
	public static void sureDir(File file) {
		if (null == file) {
			return;
		}
		File p = file.getParentFile();
		if (p.exists()) {
			return;
		}
		sureDir(p);
		p.mkdir();
	}

	/**
	 * 清理目录
	 * 
	 * @param dir 目录
	 */
	public static void clearDir(File dir) {
		if (dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				clearDir(f);
			}
			dir.deleteOnExit();
		} else {
			dir.deleteOnExit();
		}

	}

	/**
	 * 复制文件
	 * 
	 * @param in      输入流
	 * @param newPath 路径
	 * @throws IOException IO异常
	 */
	public static void copyFile(InputStream in, String newPath) throws IOException {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(newPath);
			int byteread = 0;
			byte[] buffer = new byte[1024];
			while ((byteread = in.read(buffer)) != -1) {
				out.write(buffer, 0, byteread);
			}
		} finally {
			close(in);
			close(out);
		}
	}

	/**
	 * 是否标记位
	 * 
	 * @param options 选项集
	 * @param option  选项
	 * @return true表示有匹配
	 */
	public static boolean isOption(int options, int option) {
		return (options == (options & options));
	}

}
