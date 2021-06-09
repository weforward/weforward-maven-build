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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.FilteringDependencyNodeVisitor;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

import cn.weforward.buildplugin.RevisionControl;
import cn.weforward.buildplugin.util.DistUtil;
import cn.weforward.buildplugin.util.FileUtil;
import cn.weforward.buildplugin.util.JarUtil;
import cn.weforward.buildplugin.util.StringUtil;
import cn.weforward.buildplugin.util.VersionUtil;

/**
 * 抽象build插件
 * 
 * @author daibo
 * 
 */
public abstract class AbstractBuildMojo extends AbstractMojo {
	/** 默认主版本 */
	private static final String DEFAULT_MAIN_VERSION = "1";
	/** 依赖构建者 */
	@Component(hint = "default")
	protected DependencyGraphBuilder m_DependencyGraphBuilder;
	/** 项目 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;
	/** 会话 */
	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	protected MavenSession session;
	/** 本地库 */
	@Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
	protected ArtifactRepository localRepository;
	/** 主版本号 */
	@Parameter(defaultValue = "${main.version}")
	private String mainVersion;
	/** 主类 */
	@Parameter
	protected String mainClass;
	/** 是否将依赖打包进jar做成一个可执行的jar包 */
	@Parameter
	protected boolean fatjar;
	/** 是否打包源码 */
	@Parameter(defaultValue = "true")
	protected boolean withSource;
	/** 包含文件 */
	@Parameter
	protected String[] includes;
	/** 排除文件 */
	@Parameter
	protected String[] excludes;
	/** 是否生成pom文件 */
	@Parameter
	protected boolean generatePomFile;
	/** 自定义的项目名 */
	@Parameter
	protected String artifactId;

	/** 输出目录 */
	@Parameter(defaultValue = "${project.build.directory}", required = true)
	protected File outputDirectory;
	/** 源代码目录 */
	@Parameter(defaultValue = "${project.build.sourceDirectory}")
	protected File sourceDirectory;
	/** 测试代码目录 */
	@Parameter(defaultValue = "${project.build.testSourceDirectory}")
	protected File testSourceDirectory;
	/** 类文件的输出目录 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	protected File classDirectory;
	/** 外部依赖的jar包 */
	@Parameter(defaultValue = "${project.build.libDirectory}")
	protected String libDirectory;
	/** 文件名 */
	@Parameter(defaultValue = "${project.build.finalName}", readonly = true)
	protected String finalName;
	/** svn用户名 */
	@Parameter(defaultValue = "${svn.username}")
	protected String svnUserName;
	/** svn密码 */
	@Parameter(defaultValue = "${svn.password}")
	protected String svnPassword;
	/** git用户名 */
	@Parameter(defaultValue = "${git.username}")
	protected String gitUserName;
	/** svn密码 */
	@Parameter(defaultValue = "${git.password}")
	protected String gitPassword;
	/** 版本控制组件 */
	protected RevisionControl m_RC;
	/** 格式化为yyyy-MM-dd HH:mm:ss */
	private final static SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Parameter(defaultValue = "${disthub.url}")
	protected String distHubUrl;
	@Parameter(defaultValue = "${dist.username}")
	protected String distUsername;
	@Parameter(defaultValue = "${dist.password}")
	protected String distPassword;
	/** artifact工厂 */
	@Component
	protected ArtifactFactory m_ArtifactFactory;
	/** repository工厂 */
	@Component
	protected ArtifactRepositoryFactory m_RepositoryFactory;
	/** repository布局 */
	@Component(role = ArtifactRepositoryLayout.class)
	private Map<String, ArtifactRepositoryLayout> m_RepositoryLayouts;
	/** artifact发布者 */
	@Component
	private ArtifactDeployer m_Deployer;
	/** artifact安装者 */
	@Component
	protected ArtifactInstaller m_Installer;

	/** repositoryid */
	@Parameter(defaultValue = "${distribution.repository.id}")
	protected String repositoryId;
	/** repository链接 */
	@Parameter(defaultValue = "${distribution.repository.url}")
	protected String repositoryUrl;
	/** repository快照链接 */
	@Parameter(defaultValue = "${distribution.repository.snapshot.url}")
	protected String repositorySnapshotUrl;

	/**
	 * 格式化日期
	 * 
	 * @param date
	 * @return
	 */
	private static String format(Date date) {
		synchronized (FORMAT) {
			return FORMAT.format(date);
		}
	}

	/**
	 * svn用户名
	 * 
	 */
	public String getSvnUserName() {
		return svnUserName;
	}

	/**
	 * svn密码
	 * 
	 */
	public String getSvnPassword() {
		return svnPassword;
	}

	/**
	 * git用户名
	 * 
	 */
	public String getGitUserName() {
		return gitUserName;
	}

	/**
	 * git密码
	 * 
	 */
	public String getGitPassword() {
		return gitPassword;
	}

	/** 包含类文件路径 */
	protected String[] getIncludes() {
		return includes;
	}

	/** 排除类文件路径 */
	protected String[] getExcludes() {
		return excludes;
	}

	/** 获取主版本 */
	public String getMainVersion() {
		return null == mainVersion ? DEFAULT_MAIN_VERSION : mainVersion;
	}

	public String getCopyright() {
		Calendar cal = Calendar.getInstance();
		return "honintech (c) " + cal.get(Calendar.YEAR);
	}

	/**
	 * 获取版本控制器
	 * 
	 * @return 版本控制器对象
	 * @throws MojoFailureException
	 */
	public RevisionControl getRC() throws MojoFailureException {
		if (null != m_RC) {
			return m_RC;
		}
		Scm scm = project.getScm();
		if (null == scm) {
			m_RC = new RevisionControlByEmpty();
		} else {
			String url = scm.getUrl();
			if (StringUtil.isEmpty(url)) {
				throw new MojoFailureException("请先配置<scm><url>");
			}
			if (RevisionControlByGit.isGit(url)) {
				m_RC = new RevisionControlByGit(this, project);
			} else {
				m_RC = new RevisionControlBySvn(this, project);
			}
		}
		return m_RC;
	}

	/**
	 * 生成版本号
	 * 
	 * @param incVersion 增量版本
	 * @return 版本号
	 * @throws MojoFailureException
	 */
	protected String genVersion(String incVersion) throws MojoFailureException {
		String newVersion = null;
		newVersion = getMainVersion() + VersionUtil.VERSION_SPLITE + incVersion + VersionUtil.VERSION_SPLITE
				+ getRC().getVersion();
		return newVersion;
	}

	/**
	 * 创建jar包
	 * 
	 * @param version 版本号
	 * @throws MojoFailureException
	 */
	protected void createJar(String version) throws MojoFailureException {
		Map<String, String> map = new HashMap<String, String>();
		map.put("Implementation-Version", version);
		map.put("Main-Version", getMainVersion());
		map.put("Copyright", getCopyright());
		map.put("Built-Date", format(new Date()));
		if (null != mainClass) {
			map.put("Main-Class", mainClass);
		}
		try {
			File jarFile = getJarFile(outputDirectory, finalName, "");
			if (jarFile.exists()) {
				File original = new File(jarFile.getAbsoluteFile() + ".original");
				if (original.exists()) {
					original.delete();
				}
				jarFile.renameTo(original);
			}
			JarArchiver archiver = new JarArchiver();
			archiver.setDestFile(jarFile);
			if (fatjar) {
				JarUtil.packageSpringBootJar(archiver, classDirectory, map, getLibFiles(), getIncludes(),
						getExcludes());
			} else {
				JarUtil.packageJar(archiver, classDirectory, map, getIncludes(), getExcludes());
			}
			archiver.createArchive();
		} catch (Throwable e) {
			throw new MojoFailureException("生成jar包失败:" + e.getMessage(), e);
		}
		if (withSource && !fatjar) {// fatjar不带源码
			File sourceJarFile = getJarFile(outputDirectory, finalName, "sources");
			if (sourceJarFile.exists()) {
				File original = new File(sourceJarFile.getAbsoluteFile() + ".original");
				if (original.exists()) {
					original.delete();
				}
				sourceJarFile.renameTo(original);
			}
			JarArchiver sourceArchiver = new JarArchiver();
			sourceArchiver.setDestFile(sourceJarFile);
			try {
				JarUtil.packageJar(sourceArchiver, sourceDirectory, map, getIncludes(), getExcludes());
				sourceArchiver.createArchive();
			} catch (Exception e) {
				throw new MojoFailureException("生成source包异常:" + e.getMessage(), e);
			}
		}
		getLog().info("Build 版本 " + version);
	}

	/**
	 * 获取一个jar包文件
	 * 
	 * @param dir        目录
	 * 
	 * @param name       包名
	 * 
	 * @param classifier 分类
	 * 
	 * @return 文件
	 */
	public static File getJarFile(File dir, String name, String classifier) {
		if (dir == null) {
			throw new NullPointerException("目录不能为空");
		}
		if (name == null) {
			throw new NullPointerException("名称不能为空");
		}
		StringBuilder fileName = new StringBuilder(name);
		if (!StringUtil.isEmpty(classifier)) {
			fileName.append("-").append(classifier);
		}
		fileName.append(".jar");
		return new File(dir, fileName.toString());
	}

	/**
	 * 获取依赖节点
	 * 
	 * @return
	 * @throws MojoExecutionException
	 */
	private List<DependencyNode> getDependencys() throws MojoFailureException {
		ArtifactFilter artifactFilter = null;
		DependencyNode rootNode;
		try {
			rootNode = m_DependencyGraphBuilder.buildDependencyGraph(project, artifactFilter);
		} catch (DependencyGraphBuilderException e) {
			throw new MojoFailureException("获取依赖异常:" + e.getMessage(), e);
		}
		CollectingDependencyNodeVisitor collecting = new CollectingDependencyNodeVisitor();
		/* 过滤自己 */
		DependencyNodeFilter _NoMySelf = new DependencyNodeFilter() {

			@Override
			public boolean accept(DependencyNode node) {
				Artifact a = node.getArtifact();
				return !(StringUtil.eq(a.getGroupId(), project.getGroupId())
						&& StringUtil.eq(a.getArtifactId(), project.getArtifactId())
						&& StringUtil.eq(a.getVersion(), project.getVersion()));
			}
		};
		DependencyNodeVisitor visitor = new FilteringDependencyNodeVisitor(collecting, _NoMySelf);
		rootNode.accept(visitor);
		return collecting.getNodes();
	}

	/**
	 * 包含的lib文件
	 * 
	 * @return
	 * @throws MojoFailureException
	 */
	private List<File> getLibFiles() throws MojoFailureException {
		List<File> libs = new ArrayList<>();
		for (DependencyNode node : getDependencys()) {
			// * compile，缺省值，适用于所有阶段，会随着项目一起发布。
			// * provided，类似compile，期望JDK、容器或使用者会提供这个依赖。如servlet.jar。
			// * runtime，只在运行时使用，如JDBC驱动，适用运行和测试阶段。
			// * test，只在测试时使用，用于编译和运行测试代码。不会随项目发布。
			// * system，类似provided，需要显式提供包含依赖的jar，Maven不会在Repository中查找它。
			Artifact a = node.getArtifact();
			if (!StringUtil.eq(a.getScope(), "runtime") && !StringUtil.eq(a.getScope(), "compile")) {
				continue;
			}
			if (!StringUtil.eq(a.getType(), "jar")) {
				throw new MojoFailureException("暂不支持" + a.getType() + "类型的依赖导出," + a);
			}
			File f = new File(localRepository.getBasedir(), localRepository.pathOf(a));
			libs.add(f);
		}
		if (StringUtil.isEmpty(libDirectory)) {
			return libs;
		}
		File dir = new File(FileUtil.getAbsolutePath(libDirectory, null));
		if (!dir.exists() || !dir.isDirectory()) {
			return libs;
		}
		File[] fs = dir.listFiles();
		if (null == fs || 0 == fs.length) {
			return libs;
		}
		List<File> list = new ArrayList<File>();
		for (File f : fs) {
			if (!f.isFile() || !f.getName().endsWith(".jar")) {
				continue;
			}
			list.add(f);
		}
		return list;
	}

	protected void dist(File file, String tag) throws MojoFailureException {
		String url = distHubUrl + project.getName() + "/" + tag + "/";
		try {
			DistUtil.uploadFile(url, file, distUsername, distPassword, new MojoLogUploadProgressBar(getLog()));
		} catch (IOException | InterruptedException e) {
			throw new MojoFailureException("上传文件异常:" + e.getMessage(), e);
		}
	}

	/**
	 * 安装项目
	 * 
	 * @param artifact  元件
	 * @param packaging 打包方式
	 * @param pom       pom文件
	 * @param jar       执行包
	 * @param source    源码包
	 * @throws MojoFailureException
	 */
	protected void install(Artifact artifact, String packaging, File pom, File jar, File source)
			throws MojoFailureException {
		try {
			if ("pom".equals(packaging)) {// pom项目
				m_Installer.install(pom, artifact, localRepository);
			} else {
				if (null != jar && jar.isFile()) {
					Artifact myArtifact;
					if (generatePomFile) {
						myArtifact = m_ArtifactFactory.createArtifactWithClassifier(artifact.getGroupId(),
								artifact.getArtifactId(), artifact.getVersion(), packaging, null);
						myArtifact.addMetadata(new ProjectArtifactMetadata(artifact, generatePomFile()));
					} else {
						artifact.addMetadata(new ProjectArtifactMetadata(artifact, pom));
						myArtifact = artifact;
					}
					m_Installer.install(jar, artifact, localRepository);
				} else {
					getLog().warn("没有jar包可安装");
				}
				if (null != source && source.isFile()) {
					Artifact sourceArtifact = m_ArtifactFactory.createArtifactWithClassifier(artifact.getGroupId(),
							artifact.getArtifactId(), artifact.getVersion(), "jar", "sources");
					m_Installer.install(source, sourceArtifact, localRepository);
				}
			}
		} catch (Exception e) {
			throw new MojoFailureException("安装项目异常:" + e.getMessage(), e);
		}
	}

	public String getArtifactId() {
		return null == artifactId ? project.getArtifactId() : artifactId;
	}

	/**
	 * 发布项目
	 * 
	 * 
	 * @param artifact  元件
	 * @param packaging 打包方式
	 * @param pom       pom文件
	 * @param jar       执行包
	 * @param source    源码包
	 * @throws MojoFailureException
	 */
	protected void deploy(Artifact artifact, String packaging, File pom, File jar, File source)
			throws MojoFailureException {
		ArtifactRepository remoteRepository = getRemoteRepository();
		if (remoteRepository == null) {
			throw new MojoFailureException("未配置发布用的仓库");
		}
		String protocol = remoteRepository.getProtocol();
		if (protocol.equalsIgnoreCase("scp")) {
			File sshFile = new File(System.getProperty("user.home"), ".ssh");
			if (!sshFile.exists()) {
				sshFile.mkdirs();
			}
		}
		try {
			if ("pom".equals(packaging)) {
				m_Deployer.deploy(pom, artifact, remoteRepository, localRepository);
			} else if ("jar".equals(packaging)) {
				if (null != jar && jar.isFile()) {
					Artifact myArtifact;
					if (generatePomFile) {
						myArtifact = m_ArtifactFactory.createArtifactWithClassifier(artifact.getGroupId(),
								getArtifactId(), artifact.getVersion(), packaging, null);
						myArtifact.addMetadata(new ProjectArtifactMetadata(artifact, generatePomFile()));
					} else {
						artifact.addMetadata(new ProjectArtifactMetadata(artifact, pom));
						myArtifact = artifact;
					}
					m_Deployer.deploy(jar, myArtifact, remoteRepository, localRepository);
				} else {
					getLog().warn("没有jar包可发布");
				}
				if (null != source && source.isFile()) {
					Artifact sourceArtifact = m_ArtifactFactory.createArtifactWithClassifier(artifact.getGroupId(),
							artifact.getArtifactId(), artifact.getVersion(), packaging, "sources");
					m_Deployer.deploy(source, sourceArtifact, remoteRepository, localRepository);
				}
			}
		} catch (Exception e) {
			throw new MojoFailureException("安装项目异常:" + e.getMessage(), e);
		}

	}

	private File generatePomFile() throws MojoExecutionException {
		Model model = new Model();
		model.setModelVersion("4.0.0");
		model.setGroupId(project.getGroupId());
		model.setArtifactId(project.getArtifactId());
		model.setVersion(project.getVersion());
		model.setPackaging(project.getPackaging());
		model.setDescription(project.getDescription());
		Writer fw = null;
		try {
			File tempFile = File.createTempFile("mvndeploy", ".pom");
			tempFile.deleteOnExit();
			fw = WriterFactory.newXmlWriter(tempFile);
			new MavenXpp3Writer().write(fw, model);
			return tempFile;
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing temporary pom file: " + e.getMessage(), e);
		} finally {
			IOUtil.close(fw);
		}
	}

	private ArtifactRepository getRemoteRepository() {
		String repid = repositoryId;
		String repourl;
		if (ArtifactUtils.isSnapshot(project.getVersion()) && !StringUtil.isEmpty(repositorySnapshotUrl)) {
			repourl = repositorySnapshotUrl;
		} else {
			repourl = repositoryUrl;
		}
		ArtifactRepository repository = null;
		if (!StringUtil.isEmpty(repourl) && !StringUtil.isEmpty(repid)) {
			repository = m_RepositoryFactory.createDeploymentArtifactRepository(repid, repourl,
					m_RepositoryLayouts.get("default"), true);
		}
		if (repository == null) {
			repository = project.getDistributionManagementArtifactRepository();
		}
		return repository;
	}
}
