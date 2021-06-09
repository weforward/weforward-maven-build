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

import org.apache.maven.model.Parent;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import cn.weforward.buildplugin.support.AbstractBuildMojo;
import cn.weforward.buildplugin.util.StringUtil;
import cn.weforward.buildplugin.util.VersionUtil;

/**
 * 版本命令
 * 
 * @author daibo
 */
@Mojo(name = "version")
public class VersionBuildMojo extends AbstractBuildMojo {
	/** 是否增加版本 */
	@Parameter(defaultValue = "${weforward.build.growVersion}")
	protected boolean growVersion = false;
	/** 是否流水版本 */
	@Parameter(defaultValue = "${weforward.build.growSerialVersion}")
	protected boolean growSerialVersion = true;

	@Parameter(defaultValue = "${weforward.build.version.skip}")
	protected boolean skip = false;
	/** 记录版本号 */
	protected static String VERSION;

	public VersionBuildMojo() {
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}
		if (!project.isExecutionRoot()) {
			if (null != VERSION) {
				VersionUtil.setVersion(project, VERSION);
				MavenProject parent = project.getParent();
				if (null != parent) {
					parent.setVersion(VERSION);
				}
				Parent modelParent = project.getModel().getParent();
				if (null != modelParent) {
					modelParent.setVersion(VERSION);
				}
			}
			return;
		}
		RevisionControl rc = getRC();
		boolean isDirty = rc.isDirty();
		if (isDirty) {
			if (growVersion) {
				throw new MojoExecutionException("版本不干净,不能上线");
			}
			getLog().warn("版本不干净,注意不能上线");
		}
		String version;
		String incVersion;
		if (growVersion) {
			// 叠加版本
			incVersion = VersionUtil.genIncVersion(project, getMainVersion());
		} else {
			incVersion = VersionUtil.getIncVersion(project, getMainVersion());
		}
		// 生成版本号
		version = getMainVersion() + VersionUtil.VERSION_SPLITE + incVersion
				+ (growSerialVersion ? VersionUtil.VERSION_SPLITE + rc.getVersion() : "") + (isDirty ? "M" : "");
		if (StringUtil.eq(project.getVersion(), version)) {
			return;
		}
		// 更新版本号
		VersionUtil.setVersion(project, version);
		VERSION = version;
		getLog().info("提交版本 " + version);
		rc.commit("提交版本 " + version);
		String tagVersion = getMainVersion() + VersionUtil.VERSION_SPLITE + incVersion;
		if (growVersion) {
			String message = "生成标签 " + tagVersion + " ";
			getLog().info(message);
			rc.tag(tagVersion, message);
		}
	}

}
