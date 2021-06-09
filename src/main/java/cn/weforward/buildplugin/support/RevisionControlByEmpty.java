package cn.weforward.buildplugin.support;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.maven.plugin.MojoFailureException;

import cn.weforward.buildplugin.RevisionControl;

/**
 * 未配置scm实现
 * 
 * @author daibo
 *
 */
public class RevisionControlByEmpty implements RevisionControl {

	private static DateFormat FORMAT = new SimpleDateFormat("yyyyMMdd");

	@Override
	public String getVersion() throws MojoFailureException {
		synchronized (FORMAT) {
			return FORMAT.format(new Date());
		}
	}

	@Override
	public boolean isDirty() throws MojoFailureException {
		return false;
	}

	@Override
	public void commit(String message) throws MojoFailureException {
	}

	@Override
	public void tag(String tag, String message) throws MojoFailureException {

	}

}
