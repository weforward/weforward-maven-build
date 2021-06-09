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
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest.Attribute;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.springframework.boot.loader.JarLauncher;
import org.springframework.boot.loader.Launcher;

/**
 * jar包工具类
 * 
 * @author daibo
 * 
 */
public class JarUtil {

	private JarUtil() {

	}

	/**
	 * 打包jar
	 * 
	 * @param archiver 归档工具
	 * @param classdir 类文件目录
	 * @param mainfest 清单
	 * @param includes 包含文件
	 * @param excludes 排除文件
	 * @throws ArchiverException 异常
	 * @throws ManifestException 异常
	 * @throws IOException       异常
	 */
	public static void packageJar(JarArchiver archiver, File classdir, Map<String, String> mainfest, String[] includes,
			String[] excludes) throws ArchiverException, ManifestException, IOException {
		if (classdir.isDirectory()) {
			archiver.addDirectory(classdir, includes, excludes);
		}
		if (null != mainfest) {
			org.codehaus.plexus.archiver.jar.Manifest newManifest = org.codehaus.plexus.archiver.jar.Manifest
					.getDefaultManifest();
			for (Entry<String, String> e : mainfest.entrySet()) {
				newManifest.addConfiguredAttribute(
						new org.codehaus.plexus.archiver.jar.Manifest.Attribute(e.getKey(), e.getValue()));
			}
			archiver.addConfiguredManifest(newManifest);
		}
	}

	/**
	 * 打包jar
	 * 
	 * @param archiver 归档工具
	 * @param classdir 类文件目录
	 * @param mainfest 清单
	 * @param libs     依赖包
	 * @param includes 包含文件
	 * @param excludes 排除文件
	 * @throws ArchiverException 异常
	 * @throws ManifestException 异常
	 * @throws IOException       异常
	 * @return jar打包类
	 */
	public static JarArchiver packageSpringBootJar(JarArchiver archiver, File classdir, Map<String, String> mainfest,
			List<File> libs, String[] includes, String[] excludes)
			throws ArchiverException, ManifestException, IOException {
		File boottmp = new File(System.getProperty("java.io.tmpdir"), "springbootloader");
		if (!boottmp.exists()) {
			boottmp.mkdir();
		}
		initSpringBootLoader(boottmp, archiver);
		String classes = "BOOT-INF/classes/";
		String lib = "BOOT-INF/lib/";
		archiver.addDirectory(classdir, classes, includes, concat(excludes, "**/META-INF/**"));
		for (File f : classdir.listFiles()) {
			if (StringUtil.eq(f.getName(), "META-INF")) {
				archiver.addDirectory(f, "/META-INF/", null, concat(null, "**/MANIFEST.MF/**"));
				break;
			}
		}
		if (null != libs) {
			String prefix = lib;
			for (File f : libs) {
				String name = prefix + f.getName();
				archiver.addFile(f, name);
			}
		}
		org.codehaus.plexus.archiver.jar.Manifest newManifest = org.codehaus.plexus.archiver.jar.Manifest
				.getDefaultManifest();
		String mainClass = mainfest.get("Main-Class");
		mainfest.remove("Main-Class");
		newManifest.addConfiguredAttribute(new Attribute("Start-Class", mainClass));
		newManifest.addConfiguredAttribute(new Attribute("Main-Class", JarLauncher.class.getName()));
		newManifest.addConfiguredAttribute(new Attribute("Spring-Boot-Classes", classes));
		newManifest.addConfiguredAttribute(new Attribute("Spring-Boot-Lib", lib));
		newManifest.addConfiguredAttribute(new Attribute("Spring-Boot-Version", "1.5.9.RELEASE"));
		for (Entry<String, String> e : mainfest.entrySet()) {
			newManifest.addConfiguredAttribute(new Attribute(e.getKey(), e.getValue()));
		}
		archiver.addConfiguredManifest(newManifest);
		archiver.setCompress(false);
		return archiver;
	}

	private static String[] concat(String[] orgs, String v) {
		String[] arr;
		if (null == orgs) {
			arr = new String[1];
			arr[0] = v;
			return arr;
		}
		arr = new String[orgs.length + 1];
		System.arraycopy(orgs, 0, arr, 0, orgs.length);
		arr[arr.length - 1] = v;
		return arr;
	}

	private static void initSpringBootLoader(File boottmp, JarArchiver archiver) throws IOException {
		URL url = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
		File jar = new File(URLDecoder.decode(url.getFile(), Charset.defaultCharset().name()));
		JarFile jfile = new JarFile(jar);
		Enumeration<JarEntry> e = jfile.entries();
		while (e.hasMoreElements()) {
			JarEntry entry = e.nextElement();
			String name = entry.getName();
			if (name.startsWith("META-INF/") || name.endsWith("/")) {
				continue;
			}
			File f = new File(boottmp, name);
			FileUtil.sureDir(f);
			FileUtil.copyFile(jfile.getInputStream(entry), f.getAbsolutePath());
			archiver.addFile(f, name);
		}
		jfile.close();
	}

}
