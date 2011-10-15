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

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;


public class AppsListAdapter extends ArrayAdapter<AppInfo> {

	private final Activity context;
	private final ArrayList<AppInfo> appsList;
	
	/** 
	 * Static to save the reference to the outer class and to avoid access to
	 * any members of the containing class
	 */
	static class ViewHolder {
		public ImageView imageView;
		public TextView textView;
		public CheckBox checkBox;
	}
	
	public AppsListAdapter(Activity context, ArrayList<AppInfo> appsList) {
		super(context, R.layout.apps_list_item, appsList);
		this.context = context;
		this.appsList = appsList;
	}
	
	/**
	 * Return a row view of the element in the positions given 
	 * to be showed in a list view.
	 * ViewHolder is used to increase the method performance 
	 * and resource utilization.
	 */
	@Override
	public View getView(int pos, View convertView, ViewGroup parent) {
		// Get the info of the current app that is been displayed
		AppInfo app = appsList.get(pos);
		// Recycle existing view if passed as parameter
		// This only works if the base layout for all classes are the same
		View rowView = null;
		if (convertView == null) {
			rowView = createNewRowView(app);
		} else {
			rowView = convertView;
			ViewHolder viewHolder = (ViewHolder) rowView.getTag();
			viewHolder.checkBox.setTag(app);
		}
		// Get the row for a last time and set all the values for it
		ViewHolder viewHolder = (ViewHolder) rowView.getTag();
		viewHolder.textView.setText(app.getName());
		viewHolder.checkBox.setChecked(app.isSelected());
		// If the app is saved on the server then assign it the up_arrow image, 
		// if not assign it the down_arrow image
		if (app.isSaved()) {
			viewHolder.imageView.setImageResource(R.drawable.up_arrow);
		}
		else {
			viewHolder.imageView.setImageResource(R.drawable.down_arrow);
		}
		return rowView;
	}
	
	private View createNewRowView(AppInfo app) {
		// Inflate the xml row view (convert it to java code)
		LayoutInflater inflater = context.getLayoutInflater();
		View rowView = inflater.inflate(R.layout.apps_list_item, null);
		// ViewHolder will buffer the assess to the individual fields of the row layout
		final ViewHolder viewHolder = new ViewHolder();
		viewHolder.textView = (TextView) rowView.findViewById(R.id.label);
		viewHolder.imageView = (ImageView) rowView.findViewById(R.id.installed_icon);
		viewHolder.checkBox = (CheckBox) rowView.findViewById(R.id.check); 
		viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				AppInfo app = (AppInfo) viewHolder.checkBox.getTag();
				app.setSelected(buttonView.isChecked());
			}
		
		});
		viewHolder.checkBox.setTag(app);
		rowView.setTag(viewHolder);
		return rowView;
	}
}
