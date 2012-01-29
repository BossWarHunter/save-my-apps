package com.cooldroidapps.savemyapps;

import java.io.Serializable;
import java.util.Comparator;

public class AppNameComparator implements Comparator<AppInfo>, Serializable {

	private static final long serialVersionUID = 1L;

	public int compare(AppInfo appInfo1, AppInfo appInfo2) {
		return appInfo1.getName().compareTo(appInfo2.getName());
	}

}
