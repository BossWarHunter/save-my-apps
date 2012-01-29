package com.cooldroidapps.savemyapps;

/*
 * Copyright 2011 Franco Sabadini - fsabadi@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at	
 * 
 * http://www.apache.org/licenses/LICENSE-2.0	
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
**/

import java.io.Serializable;
import java.util.Comparator;

public class AppInfoComparator implements Comparator<AppInfo>, Serializable {

	private static final long serialVersionUID = 1L;
	private static AppInfoComparator instance = null;
	private int comparisonType;
	
	private AppInfoComparator() { 
		// By default the comparison type is by app name
		comparisonType = SaveMyApps.SORT_APP_NAME;
	}
	
	public static synchronized AppInfoComparator getInstance() {
		if (instance == null) {
			instance = new AppInfoComparator();
		}
		return instance;
	}
	
	public void setComparisonType(int comparisonType) {
		this.comparisonType = comparisonType;
	}
	
	public int compare(AppInfo appInfo1, AppInfo appInfo2) {
		int compareResult = 0;
		switch(comparisonType) {
			case SaveMyApps.SORT_APP_NAME:
				compareResult = appInfo1.getName().toLowerCase().compareTo(appInfo2.
						getName().toLowerCase());
				break;
			case SaveMyApps.SORT_NOT_SAVED_FIRST:
				// The result of the function is inverted (multiplying by -1)
				compareResult = compareSavedStatus(appInfo1, appInfo2) * -1;
				break;
			case SaveMyApps.SORT_SAVED_FIRST:
				compareResult = compareSavedStatus(appInfo1, appInfo2);
				break;
		}
		return compareResult;
	}
	
	private int compareSavedStatus(AppInfo appInfo1, AppInfo appInfo2) {
		if (appInfo1.isSaved() && !appInfo2.isSaved()) {
			return -1;
		}
		if (!appInfo1.isSaved() && appInfo2.isSaved()) {
			return 1;
		}
		return 0;
	}

}
