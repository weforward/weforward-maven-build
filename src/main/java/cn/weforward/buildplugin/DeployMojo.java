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

import cn.weforward.buildplugin.support.AbstractBuildMojo;

/**
 * 发布命令
 * 
 * @author daibo
 *
 */
@Mojo(name = "deploy")
public class DeployMojo extends AbstractBuildMojo {

	@Override
	public void execute() throws MojoFailureException {
		if (fatjar) {
			getLog().info("项目为fatjar忽略Install");
			return;
		}
		File jar = AbstractBuildMojo.getJarFile(outputDirectory, finalName, "");
		File source = AbstractBuildMojo.getJarFile(outputDirectory, finalName, "sources");
		deploy(project.getArtifact(), project.getPackaging(), project.getFile(), jar, source);
		getLog().info("Deploy 版本 " + project.getArtifact().getVersion());
	}

}
