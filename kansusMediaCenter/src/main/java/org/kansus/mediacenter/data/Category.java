package org.kansus.mediacenter.data;

public class Category extends ListItem {

	public int imageID;

	public Category(String name, int imageID) {
		this.name = name;
		this.imageID = imageID;
	}

	public Category(String name) {
		this.name = name;
		this.imageID = -1;
	}
}
