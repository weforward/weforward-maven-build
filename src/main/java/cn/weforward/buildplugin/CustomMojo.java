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

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import cn.weforward.buildplugin.support.AbstractBuildMojo;
import cn.weforward.buildplugin.util.VersionUtil;

/**
 * 定制的打包方式
 * 
 * @author daibo
 */
@Mojo(name = "custom")
public class CustomMojo extends AbstractBuildMojo {
	/** 是否生成版本 */
	protected final boolean m_IsGenVersion;
	/** 是否增加版本 */
	protected final boolean m_IsGrowVersion;
	/** 是否打包jar */
	protected final boolean m_IsPackage;
	/** 是否提交修改 */
	protected final boolean m_IsCommit;
	/** 是否生成标签 */
	protected final boolean m_IsTag;
	/** 是否发布编码包 */
	protected final boolean m_IsDist;
	/** 是否安装 */
	protected final boolean m_IsInstall;
	/** 是否发布 */
	protected final boolean m_IsDeploy;

	public CustomMojo() {
		m_IsGenVersion = !"false".equalsIgnoreCase(System.getProperty("genVersion"));
		m_IsGrowVersion = "true".equalsIgnoreCase(System.getProperty("growVersion"));
		m_IsPackage = "true".equalsIgnoreCase(System.getProperty("package"));
		m_IsCommit = "true".equalsIgnoreCase(System.getProperty("commit"));
		m_IsTag = m_IsGrowVersion || "true".equalsIgnoreCase(System.getProperty("tag"));
		m_IsDist = "true".equalsIgnoreCase(System.getProperty("dist"));
		m_IsInstall = "true".equalsIgnoreCase(System.getProperty("install"));
		m_IsDeploy = "true".equalsIgnoreCase(System.getProperty("deploy"));
	}

	/**
	 * 执行方法
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		RevisionControl rc = getRC();

		String version;
		String incVersion;
		if (m_IsGrowVersion && m_IsGenVersion) {
			// 叠加版本
			incVersion = VersionUtil.genIncVersion(project, getMainVersion());
		} else {
			incVersion = VersionUtil.getIncVersion(project, getMainVersion());
		}
		if (m_IsGenVersion) {
			boolean isDirty = rc.isDirty();
			if (isDirty) {
				if (m_IsGrowVersion) {
					throw new MojoExecutionException("版本不干净,不能上线");
				}
				getLog().warn("版本不干净,注意不能上线");
			}
			// 生成版本号
			version = genVersion(incVersion) + (isDirty ? "M" : "");
			// 更新版本号
			VersionUtil.setVersion(project, version);
		} else {
			version = project.getVersion();
		}
		if (m_IsPackage) {
			if ("jar".equals(project.getPackaging())) {
				// 创建jar包
				createJar(version);
			}
		} else {
			version = project.getVersion();
		}
		if (m_IsCommit && !m_IsTag) {
			getLog().info("Commit 版本 " + version);
			rc.commit("build 版本 " + version);
		}
		String tag = getMainVersion() + VersionUtil.VERSION_SPLITE + incVersion;
		if (m_IsTag) {
			getLog().info("Commit 版本 " + version);
			rc.commit("build 版本 " + version);
			String message = "Create Tag " + tag + " ";
			getLog().info(message);
			rc.tag(tag, message);
		}
		if (m_IsDist) {
			File jar = AbstractBuildMojo.getJarFile(outputDirectory, finalName, "");
			getLog().info("Dist " + jar.getName());
			dist(jar, tag);
			getLog().info("Dist 版本 " + version);
		}
		if (m_IsInstall) {
			File jar = AbstractBuildMojo.getJarFile(outputDirectory, finalName, "");
			File source = AbstractBuildMojo.getJarFile(outputDirectory, finalName, "sources");
			install(project.getArtifact(), project.getPackaging(), project.getFile(), jar, source);
			getLog().info("Install 版本 " + project.getArtifact().getVersion());
		}
		if (m_IsDeploy) {
			File jar = AbstractBuildMojo.getJarFile(outputDirectory, finalName, "");
			File source = AbstractBuildMojo.getJarFile(outputDirectory, finalName, "sources");
			deploy(project.getArtifact(), project.getPackaging(), project.getFile(), jar, source);
			getLog().info("Deploy 版本 " + project.getArtifact().getVersion());
		}
	}

}
