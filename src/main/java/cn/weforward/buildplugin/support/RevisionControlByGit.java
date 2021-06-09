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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.jcraft.jsch.Session;

import cn.weforward.buildplugin.RevisionControl;
import cn.weforward.buildplugin.util.StringUtil;

/**
 * 基于git的版本控制器
 * 
 * @author daibo
 *
 */
public class RevisionControlByGit implements RevisionControl {

	static {
		// -Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2
		System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
	}
	/** 日志 */
	protected Log m_Log;
	/** git库 */
	protected Git m_Git;
	/** 主干链接 */
	protected final String m_Url;
	/** 分支链接 */
	protected final String m_TagUrl;
	/** 认证供应商 */
	protected UsernamePasswordCredentialsProvider m_CredentialsProvider;
	static {
		JschConfigSessionFactory jschConfigSessionFactory = new JschConfigSessionFactory() {

			@Override
			protected void configure(Host hc, Session session) {
				session.setConfig("StrictHostKeyChecking", "no");

			}
		};
		SshSessionFactory.setInstance(jschConfigSessionFactory);
	}

	public RevisionControlByGit(AbstractBuildMojo mojo, MavenProject project) throws MojoFailureException {
		this(new File(System.getProperty("user.dir")), project.getScm().getUrl(), project.getScm().getTag(),
				mojo.getGitUserName(), mojo.getGitPassword());
		setLog(mojo.getLog());
	}

	public RevisionControlByGit(File workspace, String url, String tagUrl, String username, String password)
			throws MojoFailureException {
		m_Url = url;
		m_TagUrl = tagUrl;
		while (null != workspace && workspace.exists()) {
			try {
				m_Git = Git.open(workspace);
				break;
			} catch (RepositoryNotFoundException e) {
				workspace = workspace.getParentFile();
			} catch (IOException e) {
				throw new MojoFailureException("开启git失败:" + e.getMessage(), e);
			}
		}
		if (null != username && null != password) {
			m_CredentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
		}
	}

	public static boolean isGit(String url) {
		return url.endsWith(".git") || url.startsWith("git@");
	}

	private boolean isHttp(String url) {
		return url.startsWith("http");
	}

	public void setLog(Log log) {
		m_Log = log;
	}

	protected void info(String message) {
		if (null != m_Log) {
			m_Log.info(message);
		} else {
			// debug用
			System.out.println(message);
		}
	}

	private static DateFormat FORMAT = new SimpleDateFormat("yyyyMMdd");

	@Override
	public String getVersion() throws MojoFailureException {
		Iterable<RevCommit> it;
		try {
			it = m_Git.log().setMaxCount(1).all().call();
		} catch (GitAPIException | IOException e) {
			throw new MojoFailureException("调用Git日志失败:" + e.getMessage(), e);
		}
		for (RevCommit r : it) {
			// 取提交时间（秒）后6位，纯粹为了好看，没别的目的
			synchronized (FORMAT) {
				return FORMAT.format(new Date(r.getCommitTime() * 1000l));
			}
			// return v.substring(v.length() - 6);
		}
		throw new MojoFailureException("未有Git提交日志");
	}

	@Override
	public boolean isDirty() throws MojoFailureException {
		try {
			Status status = m_Git.status().call();
			boolean uncommit = status.hasUncommittedChanges();
			if (uncommit) {
				for (String add : status.getAdded()) {
					info("add " + add);
				}
				for (String changed : status.getChanged()) {
					info("changed " + changed);
				}
				for (String removed : status.getRemoved()) {
					info("removed " + removed);
				}
				for (String conflicting : status.getMissing()) {
					info("conflicting " + conflicting);
				}
				for (String modified : status.getModified()) {
					info("modified " + modified);
				}
			}
			return uncommit;
		} catch (NoWorkTreeException | GitAPIException e) {
			throw new MojoFailureException("调用Git状态失败:" + e.getMessage(), e);
		}
	}

	@Override
	public void commit(String message) throws MojoFailureException {
		try {
			AddCommand add = m_Git.add().addFilepattern(".");
			add.call();
		} catch (GitAPIException e) {
			throw new MojoFailureException("调用Git添加失败:" + e.getMessage(), e);
		}
		try {
			RevCommit commitCommand = m_Git.commit().setMessage(message).call();
			info(commitCommand.getId().toString());
		} catch (GitAPIException e) {
			throw new MojoFailureException("调用Git提交失败:" + e.getMessage(), e);
		}
		Iterable<PushResult> rr;
		try {
			rr = createPushCommond(m_Url).call();
		} catch (GitAPIException e) {
			throw new MojoFailureException("调用Git推送失败:" + e.getMessage(), e);
		}
		checkPushResult(rr);
	}

	@Override
	public void tag(String tag, String message) throws MojoFailureException {
		org.eclipse.jgit.lib.Ref ref;
		try {
			ref = m_Git.tag().setForceUpdate(true).setName(tag).setMessage(message).call();
		} catch (GitAPIException e) {
			throw new MojoFailureException("调用Git标签失败:" + e.getMessage(), e);
		}
		RefSpec specs = new RefSpec(ref.getName());
		Iterable<PushResult> rr;
		try {
			PushCommand push = createPushCommond(m_TagUrl);
			push.setRefSpecs(specs);
			rr = push.call();
		} catch (GitAPIException e) {
			throw new MojoFailureException("调用Git推送失败:" + e.getMessage(), e);
		}
		checkPushResult(rr);
	}

	private PushCommand createPushCommond(String url) {
		PushCommand push = m_Git.push();
		if (isHttp(url)) {
			push.setCredentialsProvider(m_CredentialsProvider);
		}
		push.setRemote(url);
		return push;
	}

	private void checkPushResult(Iterable<PushResult> rr) throws MojoFailureException {
		for (PushResult r : rr) {
			if (!StringUtil.isEmpty(r.getMessages())) {
				info(r.getMessages());
			}
			for (RemoteRefUpdate update : r.getRemoteUpdates()) {
				if (!StringUtil.isEmpty(update.getMessage())) {
					info(update.getRemoteName() + "->" + update.getMessage());
				} else {
					info(update.getRemoteName() + "->" + update.getStatus());
				}
			}
		}
	}

}
