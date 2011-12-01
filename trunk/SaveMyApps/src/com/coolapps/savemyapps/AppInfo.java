package com.coolapps.savemyapps;

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

public class AppInfo implements Comparable<AppInfo> {

	// The ID is set only for those apps that are saved
	// with the ID given by the tasks service
	private String id;
	private String name;
	// TODO: add set and get for packageName
	//private String packageName;
	private boolean saved;
	private boolean installed;
	private boolean selected;
	
	public AppInfo(String name) {
		this.name = name;
		this.saved = false;
		this.installed = false;
		this.selected = false;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return this.id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setSaved(boolean saved) {
		this.saved = saved;
	}
	
	public boolean isSaved() {
		return this.saved;
	}
	
	public void setInstalled(boolean installed) {
		this.installed = installed;
	}

	public boolean isInstalled() {
		return this.installed;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public boolean isSelected() {
		return this.selected;
	}
	
	@Override
	public boolean equals(Object o) {
		return this.name.equals(((AppInfo)o).getName());
	}

	public int compareTo(AppInfo anotherAppInfo) {
		return this.name.compareTo(anotherAppInfo.getName());
	}

}
