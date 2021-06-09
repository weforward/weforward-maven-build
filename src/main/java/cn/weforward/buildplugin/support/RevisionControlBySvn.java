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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import cn.weforward.buildplugin.RevisionControl;

/**
 * 基于svnkit的版本控制
 * 
 * @author daibo
 *
 */
public class RevisionControlBySvn implements RevisionControl {

	static {
		// 安装
		DAVRepositoryFactory.setup();
	}
	/** 工作目录 */
	protected final File m_Workspace;
	/** 主干链接 */
	protected final String m_Url;
	/** 分支链接 */
	protected final String m_TagUrl;
	/** 日志 */
	protected final Log m_Log;
	/** SVN库 */
	protected final SVNRepository m_Repository;
	/** 客户端管理 */
	protected final SVNClientManager m_ClientManager;

	public RevisionControlBySvn(AbstractBuildMojo mojo, MavenProject project) throws MojoFailureException {
		m_Log = mojo.getLog();
		Scm scm = project.getScm();
		m_Url = scm.getUrl();
		m_TagUrl = scm.getTag();
		m_Workspace = new File(System.getProperty("user.dir"));
		String userName = mojo.getSvnUserName();
		char[] Password = mojo.getSvnPassword().toCharArray();
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(userName, Password);
		try {
			m_Repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(m_Url));
		} catch (SVNException e) {
			throw new MojoFailureException("解析" + m_Url + "出错", e);
		}
		m_Repository.setAuthenticationManager(authManager);
		m_ClientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), authManager);
	}

	/* 日志 */
	private Log getLog() {
		return m_Log;
	}

	@Override
	public String getVersion() throws MojoFailureException {
		try {
			return String.valueOf(m_Repository.getLatestRevision());
		} catch (SVNException e) {
			throw new MojoFailureException("获取版本信息出错:" + e.getMessage(), e);
		}
	}

	@Override
	public boolean isDirty() throws MojoFailureException {
		// 未提交的修改
		SVNStatusClient ssc = m_ClientManager.getStatusClient();
		DirtyList list = new DirtyList();
		try {
			ssc.doStatus(m_Workspace, SVNRevision.create(new Date()), SVNDepth.INFINITY, false, false, false, false,
					list, null);
		} catch (SVNException e) {
			throw new MojoFailureException("获取版本信息出错", e);
		}
		boolean isDirty = false;
		for (File f : list.getModfiles()) {
			getLog().error(f.getAbsolutePath() + "文件修改未提交");
			isDirty = true;
		}
		for (File f : list.getDeleteFiles()) {
			getLog().error(f.getAbsolutePath() + "文件删除未提交");
			isDirty = true;
		}
		for (File f : list.getAddFiles()) {
			getLog().error(f.getAbsolutePath() + "文件增加未提交");
			isDirty = true;
		}
		return isDirty;
	}

	@Override
	public void commit(String message) throws MojoFailureException {
		SVNStatusClient ssc = m_ClientManager.getStatusClient();
		DirtyList list = new DirtyList();
		try {
			ssc.doStatus(m_Workspace, SVNRevision.create(new Date()), SVNDepth.INFINITY, false, false, false, false,
					list, null);
		} catch (SVNException e) {
			throw new MojoFailureException("获取版本信息出错", e);
		}
		SVNWCClient wcc = m_ClientManager.getWCClient();
		if (!list.getDeleteFiles().isEmpty()) {
			File[] fileArr = new File[list.getDeleteFiles().size()];
			fileArr = list.getDeleteFiles().toArray(fileArr);
			for (File f : list.getDeleteFiles()) {
				try {
					wcc.doDelete(f, true, true, true);
				} catch (SVNException e) {
					throw new MojoFailureException("提交删除修改出错", e);
				}
			}
		}
		if (!list.getAddFiles().isEmpty()) {
			for (File f : list.getAddFiles()) {
				try {
					wcc.doAdd(f, true, f.isDirectory(), true, SVNDepth.fromRecurse(true), true, false, true);
				} catch (SVNException e) {
					throw new MojoFailureException("提交添加修改出错", e);
				}
			}
		}
		SVNCommitClient ctc = m_ClientManager.getCommitClient();
		if (!list.getModfiles().isEmpty()) {
			File[] fileArr = new File[list.getModfiles().size()];
			fileArr = list.getModfiles().toArray(fileArr);
			try {
				ctc.doCommit(fileArr, false, message, new SVNProperties(), null, true, true, SVNDepth.INFINITY);
			} catch (SVNException e) {
				throw new MojoFailureException("提交修改出错", e);
			}
		}
	}

	@Override
	public void tag(String tag, String message) throws MojoFailureException {
		String trunkUrl = m_Url;
		String tagsUrl = m_TagUrl + "/" + tag;
		SVNURL trunk;
		try {
			trunk = SVNURL.parseURIEncoded(trunkUrl);
		} catch (SVNException e) {
			throw new MojoFailureException("解析" + trunkUrl + "出错", e);
		}
		SVNURL tags;
		try {
			tags = SVNURL.parseURIEncoded(tagsUrl);
		} catch (SVNException e) {
			throw new MojoFailureException("解析" + tagsUrl + "出错", e);
		}
		SVNCopyClient cpClient = m_ClientManager.getCopyClient();
		long latestRevision;
		try {
			latestRevision = m_Repository.getLatestRevision();
		} catch (SVNException e) {
			throw new MojoFailureException("获取svn版本出错", e);
		}
		SVNRevision pegRevision = SVNRevision.create(latestRevision);
		SVNRevision revision = SVNRevision.create(latestRevision);
		SVNCopySource[] sources = { new SVNCopySource(pegRevision, revision, trunk) };
		try {
			cpClient.doCopy(sources, tags, false, true, true, message, new SVNProperties());
		} catch (SVNException e) {
			throw new MojoFailureException("复制主干出错", e);
		}
	}

	/**
	 * 修改过的svn文件的列表集合
	 * 
	 * @author bo
	 *
	 */
	public static class DirtyList implements ISVNStatusHandler {
		/** 修改过的文件 */
		protected List<File> modfiles;
		/** 删除的文件 */
		protected List<File> deletefiles;
		/** 添加后未提交的文件 */
		protected List<File> addFiles;

		public DirtyList() {
			modfiles = new ArrayList<File>();
			deletefiles = new ArrayList<File>();
			addFiles = new ArrayList<File>();
		}

		public List<File> getModfiles() {
			return modfiles;
		}

		public List<File> getDeleteFiles() {
			return deletefiles;
		}

		public List<File> getAddFiles() {
			return addFiles;
		}

		public void handleStatus(SVNStatus status) throws SVNException {
			SVNStatusType type = status.getNodeStatus();
			if (type.getID() == SVNStatusType.STATUS_MODIFIED.getID()) {
				modfiles.add(status.getFile());
			} else if (type.getID() == SVNStatusType.STATUS_ADDED.getID()) {
				// || type.getID() == SVNStatusType.STATUS_UNVERSIONED.getID())
				// {
				addFiles.add(status.getFile());
				// modfiles.add(status.getFile());
			} else if (type.getID() == SVNStatusType.STATUS_DELETED.getID()) {
				deletefiles.add(status.getFile());
				modfiles.add(status.getFile());
			}
		}

	}
}
