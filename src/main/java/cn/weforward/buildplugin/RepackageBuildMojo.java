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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import cn.weforward.buildplugin.support.AbstractBuildMojo;
import cn.weforward.buildplugin.util.VersionUtil;

/**
 * 打包命令
 * 
 * @author daibo
 */
@Mojo(name = "repackage")
public class RepackageBuildMojo extends AbstractBuildMojo {
	/** 是否自动上传 */
	@Parameter(defaultValue = "${autoupload}")
	protected boolean autoupload;

	@Override
	public void execute() throws MojoFailureException {
		String incVersion = VersionUtil.getIncVersion(project, getMainVersion());
		String tag = getMainVersion() + VersionUtil.VERSION_SPLITE + incVersion;
		// 获取版本号
		String newVersion = project.getVersion();
		if ("jar".equals(project.getPackaging())) {
			// 创建jar包
			createJar(newVersion);
		}
		if (autoupload) {
			File jar = AbstractBuildMojo.getJarFile(outputDirectory, finalName, "");
			getLog().info("Dist " + jar.getName());
			dist(jar, tag);
			getLog().info("Dist 版本 " + newVersion);
		}
	}

}
