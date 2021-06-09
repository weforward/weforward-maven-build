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
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 版本工具
 * 
 * @author daibo
 *
 */
public class VersionUtil {
	/** 版本分隔符 */
	public final static String VERSION_SPLITE = ".";
	/** 版本节点在pom.xml配置文件中的名称 */
	private final static String VERSION_NODE = "version";
	/** 父节点在pom.xml配置文件中的名称 */
	private final static String PARENT_NODE = "parent";
	/** xml文件build工厂 */
	private final static DocumentBuilderFactory BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
	/** xml文件transformer工厂 */
	private final static TransformerFactory TRANS_Factory = TransformerFactory.newInstance();

	private VersionUtil() {

	}

	/**
	 * 获取增量版本号
	 * 
	 * @return 版本号
	 * @throws MojoFailureException
	 */
	public static String getIncVersion(MavenProject project, String mainVersion) throws MojoFailureException {
		int num = getIncVersionNum(project.getVersion(), mainVersion);
		String newVersion = String.valueOf(num);
		return newVersion;
	}

	public static int getIncVersionNum(String projectVersion, String mainVersion) {
		int num;
		try {
			if (!projectVersion.startsWith(mainVersion)) {
				return 0;// 主版本号变更，从0开始
			}
			String projectVersionArr[] = projectVersion.split("\\.");
			String mainVersionArr[] = mainVersion.split("\\.");
			if (projectVersionArr.length > mainVersionArr.length) {
				num = Integer.parseInt(projectVersionArr[mainVersionArr.length]);
			} else {
				num = 0;
			}
		} catch (NumberFormatException e) {
			num = 0;
		}
		return num;
	}

	/**
	 * 生成版本号（递增增量版本号）
	 * 
	 * @return 版本号
	 * @throws MojoFailureException
	 */
	public static String genIncVersion(MavenProject project, String mainVersion) throws MojoFailureException {
		int num = getIncVersionNum(project.getVersion(), mainVersion);
		String newVersion = String.valueOf((++num));
		return newVersion;
	}

	/**
	 * 设置版本号(修改pom.xml中的version节点)
	 * 
	 * @param version
	 * @throws MojoFailureException
	 */
	public static void setVersion(MavenProject project, String version) throws MojoFailureException {
		project.setVersion(version);
		Artifact a = project.getArtifact();
		if (null != a) {
			a.setVersion(version);
			a.setVersionRange(VersionRange.createFromVersion(version));
		}
		boolean change = false;
		File file = project.getFile();
		Document doc;
		try {
			doc = BUILDER_FACTORY.newDocumentBuilder().parse(file);
			NodeList nodeList = doc.getDocumentElement().getChildNodes();
			String prop = null;
			// 先找version节点
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (VERSION_NODE.equals(node.getNodeName())) {
					String content = node.getTextContent();
					if (null != content && content.startsWith("${") && content.endsWith("}")) {
						prop = content.substring(2, content.length() - 1);
						break;
					}
					node.setTextContent(version);
					change = true;
					break;
				}
			}
			// 没找到就找parent的version
			if (!change) {
				for (int i = 0; i < nodeList.getLength(); i++) {
					Node node = nodeList.item(i);
					if (PARENT_NODE.equals(node.getNodeName())) {
						NodeList childNodeList = node.getChildNodes();
						for (int j = 0; j < childNodeList.getLength(); j++) {
							Node childnode = childNodeList.item(j);
							if (VERSION_NODE.equals(childnode.getNodeName())) {
								String content = childnode.getTextContent();
								if (null != content && content.startsWith("${") && content.endsWith("}")) {
									prop = content.substring(2, content.length() - 1);
									break;
								}
								childnode.setTextContent(version);
								change = true;
								break;
							}
						}
					}
				}
			}
			if (null != prop) {
				project.getProperties().setProperty(prop, version);
				for (int i = 0; i < nodeList.getLength(); i++) {
					Node node = nodeList.item(i);
					if ("properties".equals(node.getNodeName())) {
						NodeList childNodeList = node.getChildNodes();
						for (int j = 0; j < childNodeList.getLength(); j++) {
							Node childnode = childNodeList.item(j);
							if (prop.equals(childnode.getNodeName())) {
								childnode.setTextContent(version);
								change = true;
								break;
							}
						}

					}
				}

			}
		} catch (ParserConfigurationException e) {
			throw new MojoFailureException("解析配置异常", e);
		} catch (SAXException e) {
			throw new MojoFailureException("解析异常", e);
		} catch (IOException e) {
			throw new MojoFailureException("读取文件异常", e);
		}
		if (!change) {
			return;
		}
		DOMSource domSource = new DOMSource(doc);
		Transformer transFormer;
		try {
			transFormer = TRANS_Factory.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new MojoFailureException("转换配置异常:" + e.getMessage(), e);
		}
		// 输出xml文件
		try (FileOutputStream out = new FileOutputStream(file)) {
			transFormer.transform(domSource, new StreamResult(out));
		} catch (TransformerException e) {
			throw new MojoFailureException("转换异常:" + e.getMessage(), e);
		} catch (IOException e) {
			throw new MojoFailureException("IO异常:" + e.getMessage(), e);
		}
	}

	/**
	 * 设置版本号(修改pom.xml中的version节点)
	 * 
	 * @param version
	 * @throws MojoFailureException
	 */
	public static void setParentVersion(File file, String version) throws MojoFailureException {
		Document doc;
		try {
			doc = BUILDER_FACTORY.newDocumentBuilder().parse(file);
			NodeList nodeList = doc.getDocumentElement().getChildNodes();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				if (PARENT_NODE.contentEquals(node.getNodeName())) {
					NodeList childList = node.getChildNodes();
					for (int j = 0; j < childList.getLength(); j++) {
						Node child = childList.item(j);
						if (VERSION_NODE.equals(child.getNodeName())) {
							child.setTextContent(version);
						}
					}
				}
			}
		} catch (ParserConfigurationException e) {
			throw new MojoFailureException("解析配置异常", e);
		} catch (SAXException e) {
			throw new MojoFailureException("解析异常", e);
		} catch (IOException e) {
			throw new MojoFailureException("读取文件异常", e);
		}
		DOMSource domSource = new DOMSource(doc);
		Transformer transFormer;
		try {
			transFormer = TRANS_Factory.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new MojoFailureException("转换配置异常:" + e.getMessage(), e);
		}
		// 输出xml文件
		try (FileOutputStream out = new FileOutputStream(file)) {
			transFormer.transform(domSource, new StreamResult(out));
		} catch (TransformerException e) {
			throw new MojoFailureException("转换异常:" + e.getMessage(), e);
		} catch (IOException e) {
			throw new MojoFailureException("IO异常:" + e.getMessage(), e);
		}
	}

}
